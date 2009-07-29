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

public class ZkClock implements Clock {
	
	static final Logger LOG = LoggerFactory.getLogger(ZkClock.class);
	
	public static final byte[] DEFAULT_DATA = 
		ByteBuffer.allocate(8).putLong(-1).array();
	public static final List<ACL> DEFAULT_ACL = ZooDefs.Ids.OPEN_ACL_UNSAFE;
	
	private ZooKeeper myZooKeeper;
	private long retryDelay = 500L;
    private int retryCount = 10;
    
	@Inject
	public ZkClock(ZooKeeper zooKeeper){
		LOG.info("Initialising ZooKeeper backed Clock instance");
		myZooKeeper = zooKeeper;
	}
	
	@Override
	public long getNextSequence(String key) throws InterruptedException, Exception{ 
		KeeperException exception = null;
		for (int i = 0; i < retryCount; i++) {
			try {
				return incrementCounter(key);
			} catch (KeeperException.SessionExpiredException e) {
				LOG.warn("Session expired for: " + myZooKeeper + 
						" so reconnecting due to: " + e, e);
				throw e;
			} catch (KeeperException.ConnectionLossException e) {
				if (exception == null) {
					exception = e;
				}
				LOG.debug("Attempt " + i + " failed with connection loss so " +
						"attempting to reconnect: " + e, e);
				retryWithDelay(i);
			}
		}
		throw exception;
	}
	
	private long incrementCounter(String key) 
	throws KeeperException, InterruptedException{
		LOG.debug(String.format("Getting next sequence for key %s", key));
		Stat stat = new Stat();
		boolean committed = false;
		long id = 0;
		while (!committed) {
			try{
				byte[] data = myZooKeeper.getData(key, false, stat);
				ByteBuffer buf = ByteBuffer.wrap(data);
				id = buf.getLong();
				buf.rewind();
				id++;
				buf.putLong(id);
				myZooKeeper.setData(key, buf.array(), stat.getVersion());
				committed = true;
			}catch( KeeperException.NoNodeException e){
				createKey(key);
				return incrementCounter(key);
			} catch (KeeperException.BadVersionException e) {
				LOG.debug(String.format("Another client updated key %s, retrying", 
										key));
				committed = false;
			} catch (InterruptedException e) {
				// at this point, we don't know that our update happened.
				// we will err on the side of caution and assume that our
				// update didn't happen. In the worst case, we'll end up
				// with a wasted sequence number that we'll have to 
				// apply compensating measures to deal with
				LOG.error(
					String.format("Unable to determine status counter increment for key %s. " + 
						"This may result in unused sequences", key), e);
				committed = false;
			}
		}
		return id;
	}

	private void createKey(String key) 
	throws KeeperException, InterruptedException{
		LOG.debug(String.format("Creating new node for key %s", key));
		try {
			String keyNode = myZooKeeper.create(key, 
											DEFAULT_DATA, 
											DEFAULT_ACL, 
											CreateMode.PERSISTENT);
		} catch (InterruptedException e) {
			LOG.error("ERROR:", e);
			throw e;
		} catch (KeeperException e) {
			if (e.code().equals(KeeperException.Code.NODEEXISTS)){
				LOG.info(
					String.format("Tried to create %s, but it already exists. " +
								"Possibly a race condition", key));
			}else{
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
