package com.talis.platform.sequencing.zookeeper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
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
	
	@Test (expected=SequencingException.class)
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
		class Tuple{
			long time1;
			long time2;
			ZooKeeper client;
		}
	
		Callable<Tuple> requester = new Callable<Tuple>(){
			@Override
			public Tuple call() {
				Tuple t = new Tuple();
				t.time1 = System.currentTimeMillis();
				try {
					t.time1 = System.currentTimeMillis();
					t.client = provider.get();
					t.time2 = System.currentTimeMillis();
				} catch (SequencingException e) {
					fail("Caught unexpected exception");
				}
				return t;
			}
		};
		ExecutorService executor = Executors.newSingleThreadScheduledExecutor();
  	    Future<Tuple> future = executor.submit(requester);
  	    
		WatchedEvent connectedEvent = 
			new WatchedEvent(Watcher.Event.EventType.None, 
							 Watcher.Event.KeeperState.SyncConnected, 
							 null);
		Thread.sleep(waitPeriod);
		provider.process(connectedEvent);
		
		Tuple t = future.get();
		assertNotNull(t.client);
		t.client.close();
		assertTrue( (t.time2 - t.time1) >= waitPeriod);
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
			assertTrue(e.getCause() instanceof SequencingException);
		} finally {
			System.clearProperty(ZooKeeperProvider.CONNECTION_TIMEOUT_PROPERTY);
		}
	}
	
	@Test
	public void ifExpiredEventReceivedDisposeOfInstance() throws Exception{
		System.setProperty(ZooKeeperProvider.CONNECTION_TIMEOUT_PROPERTY, "100");
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
