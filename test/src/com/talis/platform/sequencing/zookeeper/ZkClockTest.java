package com.talis.platform.sequencing.zookeeper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.data.Stat;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.talis.platform.sequencing.SequencingException;

public class ZkClockTest {

	public static final Logger LOG = LoggerFactory.getLogger(ZkClockTest.class);
	
	private ZkTestHelper myTestHelper;
	private ZooKeeper myKeeper;
	private String key;
	
	@BeforeClass
	public static void startZkServer() throws Exception {
		
	}
	
	@Before 
	public void setup() throws Exception{
		myTestHelper = new ZkTestHelper();
		myTestHelper.startServer();
		myKeeper = new ZooKeeper(	ZkTestHelper.DEFAULT_HOST_PORT, 
            						ZkTestHelper.CONNECTION_TIMEOUT, 
            						new NullWatcher());
		key = "/test-key-" + new Random().nextInt(10000);
	}
	
	@After
	public void tearDown() throws Exception{
		myTestHelper.cleanUp();
	}
	
	@Test
	public void createNodeForKeyIfRequired() throws Exception{
		KeeperException expectedException =	new NoNodeException(); 
		ZkClock clock = new ZkClock(myKeeper);
		assertNull(myKeeper.exists(key, false));

		long sequence = clock.getNextSequence(key);
		assertEquals(0, sequence);
		assertNotNull(myKeeper.exists(key, false));
		assertEquals(sequence, getNodeDataAsLong(key));
	}
	
	@Test 
	public void createNextSequenceForExistingKey() throws Exception{
		ZkClock clock = new ZkClock(myKeeper);
		clock.getNextSequence(key);
		assertNotNull(myKeeper.exists(key, false));
		
		long sequence = clock.getNextSequence(key);
		assertEquals(1, sequence);
		assertEquals(sequence, getNodeDataAsLong(key));
	}
	
	@Test
	public void clockSurvivesDisconnectionFromServer() throws Exception{
		ZkClock clock = new ZkClock(myKeeper);
		assertEquals(0, clock.getNextSequence(key));
		myTestHelper.stopServer();
		Thread.sleep(5000l);
		myTestHelper.startServer();
		assertEquals(1, clock.getNextSequence(key));
	}
	
	@Test 
	public void retryOperationsThenFailWhileDisconnected() throws Exception{
		System.setProperty(ZkClock.RETRY_DELAY_PROPERTY, "100");
		System.setProperty(ZkClock.RETRY_COUNT_PROPERTY, "2");
		ZkClock clock = new ZkClock(myKeeper);
		assertEquals(0, clock.getNextSequence(key));
		myTestHelper.stopServer();
		Thread.sleep(5000l);
		try{
			clock.getNextSequence(key);
			fail("Expected an exception here");
		}catch(Exception e){
			assertTrue(e instanceof SequencingException);
		}
		myTestHelper.startServer();
		assertEquals(1, clock.getNextSequence(key));
	}

	@Test
	public void hammerClock() throws Exception{
		int iterations = 10000;
		ZkClock clock = new ZkClock(myKeeper);

		CountDownLatch startGate = new CountDownLatch(1);
		CountDownLatch endGate = new CountDownLatch(5);
		new Thread(new Driver("/first-key", clock, iterations, startGate, endGate)).start();
		new Thread(new Driver("/second-key", clock, iterations, startGate, endGate)).start();
		new Thread(new Driver("/third-key", clock, iterations, startGate, endGate)).start();
		new Thread(new Driver("/fourth-key", clock, iterations, startGate, endGate)).start();
		new Thread(new Driver("/first-key", clock, iterations, startGate, endGate)).start();
		
		LOG.info("Waiting before starting 5 threads each doing " 
							+ iterations + " iterations (2 contending)");
		Thread.sleep(1000);
		long start = System.currentTimeMillis();
		startGate.countDown();
		endGate.await();
		long end = System.currentTimeMillis();
		LOG.info("Done in :" + (end - start) + " ms");
		
		assertEquals((2 * iterations) , clock.getNextSequence("/first-key") );
		assertEquals(iterations, clock.getNextSequence("/second-key") );
		assertEquals(iterations, clock.getNextSequence("/third-key") );
		assertEquals(iterations, clock.getNextSequence("/fourth-key") );
		
	}
	
	private long getNodeDataAsLong(String key) throws Exception{
		byte[] data = myKeeper.getData(key, false, new Stat());
		ByteBuffer buf = ByteBuffer.wrap(data);
		return buf.getLong();
	}
	
	private class Driver implements Runnable{
		ZkClock clock;
		int iterations;
		String key;
		CountDownLatch startGate;
		CountDownLatch endGate;
		long sequence;
		
		Driver(String key, ZkClock clock, int iterations, 
				CountDownLatch startGate, CountDownLatch endGate){
			this.key = key;
			this.clock = clock;
			this.iterations = iterations;
			this.startGate = startGate;
			this.endGate = endGate;
		}
		
		@Override
		public void run() {
			try{
				startGate.await();
				LOG.info("Running test");
				for (int i = 0; i < iterations; i++){
					sequence = clock.getNextSequence(key);
				}
				LOG.info("Test finished");
				endGate.countDown();
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}
	
}
