package com.talis.platform.sequencing.zookeeper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.talis.platform.sequencing.SequencingException;

public class RealZooKeeperProvider implements ZooKeeperProvider, Watcher{

	static final Logger LOG = LoggerFactory.getLogger(RealZooKeeperProvider.class);
	
	private String myEnsembleList;
	private ZooKeeper myZookeeper;
	private boolean connected = false;

	@Override
	public void process(WatchedEvent event) {
		switch (event.getType()) {
		case None:
			processNoneTypeEvent(event.getState());
		default:
			// do nothing
		}
	}

	private void processNoneTypeEvent(Event.KeeperState state) {
		switch (state) {
		case SyncConnected:
			LOG.info("Received connected event, notifying waiting thread "
					+ "(there should only be one)");
			synchronized (this) {
				connected = true;
				notify();
			}
			break;
		case Expired:
			LOG.info("Session expired event, disposing of client instance");
			reset();
			break;
		default:
			// do nothing
		}
	}

	@Override
	public ZooKeeper get() throws SequencingException {
		if (null == myZookeeper) {
			LOG.info("No ZooKeeper instance cached");
			synchronized (this) {
				if (null == myZookeeper) {
					try {
						myZookeeper = newKeeperInstance();
						LOG.info("Waiting for connection to zookeeper server");
						waitForConnection();
					} catch (IOException e) {
						LOG.error("Unable to create ZooKeeper instance", e);
						throw new SequencingException(
								"Unable to provide client for sequence generation",
								e);
					}
				}
			}
		}else{
			LOG.info("Returing cached ZooKeeper instance");
		}
		return myZookeeper;
	}

	private void waitForConnection() throws SequencingException {
		long connectionTimeout = Long.getLong(CONNECTION_TIMEOUT_PROPERTY,
				DEFAULT_CONNECTION_TIMEOUT);
		synchronized (this) {
			try {
				wait(connectionTimeout);
			} catch (InterruptedException e) {
				LOG.info("Interrupted while waiting for connection");
			}
		}
		if (!connected) {
			throw new SequencingException("Connection timed out or interrupted");
		}
	}

	public String getEnsembleList() throws IOException {
		if (null == myEnsembleList) {
			myEnsembleList = readEnsembleList();
		}
		return myEnsembleList;
	}

	public void reset() {
		myEnsembleList = null;
		myZookeeper = null;
	}

	private String readEnsembleList() throws IOException {
		InputStream ensembleListStream = this.getClass().getResourceAsStream(
				DEFAULT_SERVER_LIST_LOCATION);
		String theFilename = System.getProperty(SERVER_LIST_LOCATION_PROPERTY);
		if (null == theFilename) {
			if (LOG.isInfoEnabled()) {
				LOG.info("No server list specified in system "
						+ "property using default");
			}
		} else {
			if (LOG.isInfoEnabled()) {
				LOG.info(String.format("Initialising server list from file %s",
						theFilename));
			}
			ensembleListStream = FileUtils
					.openInputStream(new File(theFilename));
		}

		String list = ((String) IOUtils.readLines(ensembleListStream).get(0))
				.trim();
		if (LOG.isInfoEnabled()) {
			LOG.info(String.format("Read ensemble list => %s", list));
		}
		return list;
	}

	private ZooKeeper newKeeperInstance() throws IOException {
		int sessionTimeout = Integer.getInteger(SESSION_TIMEOUT_PROPERTY,
				DEFAULT_SESSION_TIMEOUT);

		if (LOG.isInfoEnabled()) {
			LOG.info(String.format(
					"Creating new ZooKeeper instance. Servers: %s | Session Timeout: %s",
					getEnsembleList(), sessionTimeout));
		}
		connected = false;
		ZooKeeper keeper = new ZooKeeper(myEnsembleList, sessionTimeout, this);

		if (LOG.isInfoEnabled()) {
			LOG.info("Created ZooKeeper instance");
		}
		return keeper;
	}
   }