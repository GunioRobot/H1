package com.talis.platform.sequencing.zookeeper;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.commons.io.FileUtils;
import org.apache.zookeeper.ZooKeeper;
import org.junit.rules.ExternalResource;
import org.junit.rules.TemporaryFolder;

public class EmbeddedZookeeper extends ExternalResource {
	
	private TemporaryFolder tmpFolder = new TemporaryFolder();
	private ZkTestHelper zkTestHelper;
	private ZooKeeper zooKeeper;
	private File zkServersFile;
	
	@Override
	protected synchronized void before() throws Throwable {
		tmpFolder.create();
		zkTestHelper = new ZkTestHelper();
		startServer();
		zooKeeper = new ZooKeeper(	ZkTestHelper.DEFAULT_HOST_PORT, 
            						ZkTestHelper.CONNECTION_TIMEOUT, 
            						new NullWatcher());
		zkServersFile = new File(tmpFolder.getRoot(), "zkServers");
		FileUtils.write(zkServersFile, ZkTestHelper.DEFAULT_HOST_PORT + "\n");
	}

	@Override
	protected synchronized void after() {
		tmpFolder.delete();
		try {
			zooKeeper.close();
			zkTestHelper.cleanUp();
		} catch (Exception e) {
			throw new RuntimeException("Exception in teardown", e);
		}		
		zkTestHelper.waitForServerDown(ZkTestHelper.DEFAULT_HOST_PORT, 10000);
	}

	public synchronized void startServer() throws Exception {
		zkTestHelper.startServer();
		zkTestHelper.waitForServerUp(ZkTestHelper.DEFAULT_HOST_PORT, 10000);
	}
	
	public synchronized void stopServer() throws Exception {
		zkTestHelper.stopServer();		
		zkTestHelper.waitForServerDown(ZkTestHelper.DEFAULT_HOST_PORT, 10000);
	}

	public ZooKeeper getZookeeper() {
		return zooKeeper;
	}

	public String getZkServersFileLocation() {
		return zkServersFile.getAbsolutePath();
	}
}
