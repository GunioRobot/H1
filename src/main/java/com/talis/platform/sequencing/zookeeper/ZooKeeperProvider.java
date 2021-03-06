/*
 *    Copyright 2010 Talis Systems Ltd
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

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

import com.google.inject.Provider;


public class ZooKeeperProvider implements Provider<ZooKeeper>, Watcher{

	static final Logger LOG = LoggerFactory.getLogger(ZooKeeperProvider.class);
	
	public static final String DEFAULT_SERVER_LIST_LOCATION = "/zkservers";
	public static final String SERVER_LIST_LOCATION_PROPERTY = 
		"com.talis.platform.sequencing.zookeeper.servers";

	public static final int DEFAULT_SESSION_TIMEOUT = 10 * 1000;
	public static final String SESSION_TIMEOUT_PROPERTY = 
		"com.talis.platform.sequencing.zookeeper.session.timeout";

	public static final int DEFAULT_CONNECTION_TIMEOUT = 10 * 1000;
	public static final String CONNECTION_TIMEOUT_PROPERTY = 
		"com.talis.platform.sequencing.zookeeper.connection.timeout";
	
	private String ensembleList;
	private volatile ZooKeeper zookeeper;
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
	public ZooKeeper get(){
		if (null == zookeeper) {
			LOG.info("No ZooKeeper instance cached");
			synchronized (this) {
				if (null == zookeeper) {
					try {
						zookeeper = newKeeperInstance();
						LOG.info("Waiting for connection to zookeeper server");
						waitForConnection();
					} catch (IOException e) {
						LOG.error("Unable to create ZooKeeper instance", e);
						throw new ZooKeeperInitialisationException(
								"Unable to provide client for sequence generation",
								e);
					}
				}
			}
		}else{
			LOG.info("Returning cached ZooKeeper instance");
		}
		return zookeeper;
	}

	private void waitForConnection() throws ZooKeeperInitialisationException {
		long connectionTimeout = Long.getLong(CONNECTION_TIMEOUT_PROPERTY,
												DEFAULT_CONNECTION_TIMEOUT);
		synchronized (this) {
			try {
				wait(connectionTimeout);
			} catch (InterruptedException e) {
				LOG.info("Interrupted while waiting for connection");
			}
			
			if (!connected) {
				zookeeper = null;
				throw new ZooKeeperInitialisationException("Connection timed out or interrupted");
			}
		}
	}

	public String getEnsembleList() throws IOException {
		if (null == ensembleList) {
			ensembleList = readEnsembleList();
		}
		return ensembleList;
	}

	public synchronized void reset() {
		ensembleList = null;
		zookeeper = null;
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
		ZooKeeper keeper = new ZooKeeper(getEnsembleList(), sessionTimeout, this);

		if (LOG.isInfoEnabled()) {
			LOG.info("Created ZooKeeper instance");
		}
		return keeper;
	}
}
