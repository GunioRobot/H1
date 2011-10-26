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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.talis.platform.sequencing.SequencingException;

public class ZooKeeperProviderTest {
	
	private static final String DEFAULT_HOST_PORT = "127.0.0.1:9000";
	private ZkTestHelper myHelper;
	private ZooKeeper myKeeper;
	
	@Before
	public void setup() throws Exception{
		myHelper = new ZkTestHelper();
		ensureServerStarted(DEFAULT_HOST_PORT);
	}
	
	@After
	public void tearDown() throws Exception{
		if (null != myKeeper){
			myKeeper.close();
		}
		ensureServerStopped(DEFAULT_HOST_PORT);
	}

	@Test
	public void readServerListFromDefaultLocation() throws Exception{
		ZooKeeperProvider provider = new ZooKeeperProvider();
		assertEquals(DEFAULT_HOST_PORT, provider.getEnsembleList());
	}
		
	@Test
	public void setServerListLocationUsingSystemProperty() 
	throws IOException, SequencingException{
		String expected = "172.30.3.172:9001,172.30.3.173:9002"; 
		File tmpFile = File.createTempFile("zk-servers", "_tmp");
		tmpFile.deleteOnExit();
		FileWriter out = new FileWriter(tmpFile);
		out.write(expected);
		out.flush();
		out.close();
		
		System.setProperty(ZooKeeperProvider.SERVER_LIST_LOCATION_PROPERTY, 
							tmpFile.getAbsolutePath());
		try{
			ZooKeeperProvider provider = new ZooKeeperProvider();
			assertEquals(expected, provider.getEnsembleList());
		}finally{
			System.clearProperty(ZooKeeperProvider.SERVER_LIST_LOCATION_PROPERTY);
		}
	}
	
	@Test (expected=ZooKeeperInitialisationException.class)
	public void unableToReadServerListFromSpecifiedLocation() throws SequencingException{
		System.setProperty(ZooKeeperProvider.SERVER_LIST_LOCATION_PROPERTY,
				"/this/doesnt/exist");
		try {
			ZooKeeperProvider provider = new ZooKeeperProvider();
			provider.get();
			fail("Expected an exception here");
		} finally {
			System.clearProperty(ZooKeeperProvider.SERVER_LIST_LOCATION_PROPERTY);
		}
	}
	
	@Test
	public void reuseZooKeeperClientInstance() throws SequencingException{
		ZooKeeperProvider provider = new ZooKeeperProvider();
		assertSame(provider.get(), provider.get());
	}
	
	@Test
	public void resetDisposesOfZookeeperClientInstance() 
	throws Exception{
		ZooKeeperProvider provider = new ZooKeeperProvider();
		ZooKeeper first = provider.get();
		provider.reset();
		ZooKeeper second = provider.get();
		assertNotSame(first, second);
	}
	
	@Test
	public void waitForConnectedEventBeforeReturningInstance()
	throws Exception{
		ensureServerStopped(DEFAULT_HOST_PORT);
		final ZooKeeperProvider provider = new ZooKeeperProvider();
		final long waitPeriod = 300; 
//		class Long{
//			long time1 = 0;
//			long time2 = 0;
//			ZooKeeper client;
//		}
		
		Callable<Long> requester = new Callable<Long>() {
			@Override
			public Long call() throws ZooKeeperInitialisationException {
				try {
					long time1 = System.currentTimeMillis();
					provider.get();
					long time2 = System.currentTimeMillis();
					return (time2 - time1);
				} catch (ZooKeeperInitialisationException e) {
					e.printStackTrace();
					fail("Caught unexpected exception");
					throw e;
				}
			}
		};
		ExecutorService executor = Executors.newSingleThreadScheduledExecutor();
  	    Future<Long> future = executor.submit(requester);
  	    
		WatchedEvent connectedEvent = 
			new WatchedEvent(Watcher.Event.EventType.None, 
							 Watcher.Event.KeeperState.SyncConnected, 
							 null);
		Thread.sleep(waitPeriod + 10);
		provider.process(connectedEvent);
		Long fetchTime = future.get();
//		Tuple t = future.get();
//		assertNotNull(t.client);
//		t.client.close();
		try{
//			assertTrue( (t.time2 - t.time1) >= waitPeriod);
			assertTrue( fetchTime >= waitPeriod);
		}finally{
			System.out.println(provider.getEnsembleList());
			System.out.println("Fetch time = " + fetchTime);
		}
	}
	
	@Test
	public void ifNoConnectedEventReceivedDuringMaxWaitPeriodThrowException(){
		ensureServerStopped(DEFAULT_HOST_PORT);
		System.setProperty(ZooKeeperProvider.CONNECTION_TIMEOUT_PROPERTY, "100");
		final ZooKeeperProvider provider = new ZooKeeperProvider();
		try {
  	  		Executors.newSingleThreadScheduledExecutor().submit(
  	  			new Callable<Void>(){
  	  				@Override
  	  				public Void call() throws SequencingException{
  	  					provider.get();
  	  					return null;
  	  				}
  	  		}).get();
			fail("Expected an exception here");
		} catch (InterruptedException e) {
			fail("Unexpected interrupted exception");
		} catch (ExecutionException e) {
			assertTrue(e.getCause() instanceof ZooKeeperInitialisationException);
		} finally {
			System.clearProperty(ZooKeeperProvider.CONNECTION_TIMEOUT_PROPERTY);
		}
	}
	
	@Test
	public void ifExpiredEventReceivedDisposeOfInstance() throws Exception{
		final ZooKeeperProvider provider = new ZooKeeperProvider();
		ZooKeeper first = provider.get();
		WatchedEvent sessionExpiredEvent = 
			new WatchedEvent(Watcher.Event.EventType.None, 
					 		 Watcher.Event.KeeperState.Expired, 
					 		 null);
		provider.process(sessionExpiredEvent);
		ZooKeeper second = provider.get();
		assertNotSame(first, second);
	}
	
	@Test 
	public void explicitSessionExpiryTriggersDisposalOfInstance() throws Exception{
		Logger logger = LoggerFactory.getLogger(ZooKeeperProviderTest.class);
		final ZooKeeperProvider provider = new ZooKeeperProvider();
		ZooKeeper first = provider.get();
		
		ZooKeeper second = new ZooKeeper( 	provider.getEnsembleList(),
		  				   					ZkTestHelper.CONNECTION_TIMEOUT, 
		  				   					new NullWatcher(),
		  				   					first.getSessionId(),
		  				   					first.getSessionPasswd());
		while(second.getState() != ZooKeeper.States.CONNECTED){
			Thread.sleep(10l);
		}
		second.close();
		logger.info("Closed second client");
		Thread.sleep(3000l);
		ZooKeeper third = provider.get();
		assertNotSame(first, third);
	}
	
	private void ensureServerStarted(String hostPort){
		try{
			myHelper.startServer(hostPort);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	private void ensureServerStopped(String hostPort){
		try{
			myHelper.stopServer(hostPort);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
}
