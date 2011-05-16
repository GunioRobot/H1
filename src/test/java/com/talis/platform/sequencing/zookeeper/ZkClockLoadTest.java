package com.talis.platform.sequencing.zookeeper;

import org.apache.zookeeper.ZooKeeper;
import org.junit.After;
import org.junit.Before;

import com.talis.platform.sequencing.Clock;
import com.talis.platform.sequencing.LoadTest;

public class ZkClockLoadTest extends LoadTest {

	ZkTestHelper myTestHelper;
	ZooKeeper myKeeper;
	ZooKeeperProvider myKeeperProvider;
	
	@Before
	public void setup() throws Exception{
		myTestHelper = new ZkTestHelper();
		myTestHelper.startServer();
		myKeeper = new ZooKeeper(	ZkTestHelper.DEFAULT_HOST_PORT, 
            						ZkTestHelper.CONNECTION_TIMEOUT, 
            						new NullWatcher());
		myKeeperProvider = new ZooKeeperProvider(){
			@Override
			public ZooKeeper get(){
				return myKeeper;
			}
		};
	}
	
	@After
	public void tearDown() throws Exception{
		myKeeper.close();		
		myTestHelper.cleanUp();
	}
	
	@Override
	public Clock getClock() throws Exception {
		return new ZkClock(myKeeperProvider, new ZkClockTest.NullMetrics());
	}

}
