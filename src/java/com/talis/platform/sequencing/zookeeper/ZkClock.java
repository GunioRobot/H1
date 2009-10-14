package com.talis.platform.sequencing.zookeeper;

import java.nio.ByteBuffer;
import java.util.List;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.talis.platform.sequencing.Clock;
import com.talis.platform.sequencing.SequencingException;
import com.talis.platform.sequencing.zookeeper.metrics.ZooKeeperMetrics;

public class ZkClock implements Clock {

	static final Logger LOG = LoggerFactory.getLogger(ZkClock.class);

	public static final byte[] DEFAULT_DATA = ByteBuffer.allocate(8)
			.putLong(-1).array();
	public static final List<ACL> DEFAULT_ACL = ZooDefs.Ids.OPEN_ACL_UNSAFE;

	public static final String RETRY_DELAY_PROPERTY = 
			"com.talis.platform.sequencing.zookeeper.retrydelay";
	public static final String RETRY_COUNT_PROPERTY = 
			"com.talis.platform.sequencing.zookeeper.retrycount";

	private final ZooKeeper myZooKeeper;
	private final ZooKeeperMetrics myMetrics;
	
	private final long retryDelay = Long.getLong(RETRY_DELAY_PROPERTY, 100l);
	private final int retryCount = Integer.getInteger(RETRY_COUNT_PROPERTY, 10);

	@Inject
	public ZkClock(ZooKeeper zooKeeper, ZooKeeperMetrics metrics) throws SequencingException {
		LOG.info("Initialising ZooKeeper backed Clock instance");
		myZooKeeper = zooKeeper;
		myMetrics = metrics;
	}

	@Override
	public long getNextSequence(String key) throws SequencingException {
		KeeperException mostRecentException = null;

		for (int i = 0; i < retryCount; i++) {

			try {
				return getAndIncrement(key);
			} catch (KeeperException.SessionExpiredException e) {
				LOG.warn("Session expired for: " + myZooKeeper
						+ " so reconnecting due to: " + e, e);
				throw new SequencingException("Session expired", e);
			} catch (KeeperException.ConnectionLossException e) {
				mostRecentException = e;
				LOG.debug("Attempt " + i + " failed with connection loss so "
						+ "attempting to reconnect: " + e, e);
				retryWithDelay(i);
			} catch (KeeperException e) {
				LOG.error(String.format("Caught an unexpected error when "
						+ "incrementing sequence for key %s", key), e);
				mostRecentException = e;
			}
		}
		throw new SequencingException(String.format(
				"Failed to obtain next sequence for key %s", key), 
				mostRecentException);
	}

	private long getAndIncrement(String key) throws KeeperException {
		LOG.debug(String.format("Incrementing sequence for key %s", key));
		Stat stat = new Stat();
		boolean committed = false;
		long id = 0;
		while (!committed) {
			try {
				byte[] data = myZooKeeper.getData(key, false, stat);
				ByteBuffer buf = ByteBuffer.wrap(data);
				id = buf.getLong();
				buf.rewind();
				buf.putLong(++id);
				myZooKeeper.setData(key, buf.array(), stat.getVersion());
				committed = true;
			} catch (KeeperException.NoNodeException e) {
				createKey(key);
				committed = false;
			} catch (KeeperException.BadVersionException e) {
				myMetrics.incrementKeyCollisions();
				LOG.debug(String.format(
						"Another client updated key %s, retrying", key));
				committed = false;
			} catch (InterruptedException e) {
				// at this point, we don't know that our update happened.
				// we will err on the side of caution and assume that it
				// didn't. In the worst case, we'll end up with a wasted
				// sequence number that we'll have to apply compensating
				// measures to deal with
				LOG.error(String.format(
						"Unable to determine status counter increment for key %s. "
								+ "This may result in unused sequences", key),
						e);
				committed = false;
			}
		}
		LOG.debug(String.format("Key:Seq => %s, %s", key, id));
		return id;
	}

	private void createKey(String key) throws KeeperException {
		LOG.debug(String.format("Creating new node for key %s", key));
		try {
			myZooKeeper.create(key, DEFAULT_DATA,
								DEFAULT_ACL, CreateMode.PERSISTENT);
		} catch (InterruptedException e) {
			LOG.error(String.format(
							"Caught InterruptedException when creating key %s",
							key), e);
		} catch (KeeperException e) {
			if (e.code().equals(KeeperException.Code.NODEEXISTS)) {
				LOG.info(String.format(
						"Tried to create %s, but it already exists. "
								+ "Probably a (harmless) race condition", key));
			} else {
				throw e;
			}
		}
	}

	private void retryWithDelay(int attemptCount) {
		if (attemptCount > 0) {
			try {
				Thread.sleep(attemptCount * retryDelay);
			} catch (InterruptedException e) {
				LOG.debug("Failed to sleep: " + e, e);
			}
		}
	}
}
