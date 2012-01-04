package com.talis.platform.sequencing.zookeeper;

import java.io.File;

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
	protected void before() throws Throwable {
		tmpFolder.create();
		zkTestHelper = new ZkTestHelper();
		zkTestHelper.startServer();
		zooKeeper = new ZooKeeper(	ZkTestHelper.DEFAULT_HOST_PORT, 
            						ZkTestHelper.CONNECTION_TIMEOUT, 
            						new NullWatcher());
		zkServersFile = new File(tmpFolder.getRoot(), "zkServers");
		FileUtils.write(zkServersFile, ZkTestHelper.DEFAULT_HOST_PORT + "\n");
	}

	@Override
	protected void after() {
		tmpFolder.delete();
		try {
			zooKeeper.close();
			zkTestHelper.cleanUp();
		} catch (Exception e) {
			throw new RuntimeException("Exception in teardown", e);
		}		
	}

	public void startServer() throws Exception {
		zkTestHelper.startServer();
	}
	
	public void stopServer() throws Exception {
		zkTestHelper.stopServer();		
	}

	public ZooKeeper getZookeeper() {
		return zooKeeper;
	}

	public String getZkServersFileLocation() {
		return zkServersFile.getAbsolutePath();
	}
}
