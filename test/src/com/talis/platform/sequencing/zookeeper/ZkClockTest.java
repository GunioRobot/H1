package com.talis.platform.sequencing.zookeeper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.data.Stat;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZkClockTest {

	public static final Logger LOG = LoggerFactory.getLogger(ZkClockTest.class);
	
	private static ZkTestHelper TEST_HELPER;
	private static ZooKeeper ZK;
	private String key;
	
	@BeforeClass
	public static void startZkServer() throws Exception {
		TEST_HELPER = new ZkTestHelper();
		TEST_HELPER.startServer();
		ZK = new ZooKeeper(	ZkTestHelper.DEFAULT_HOST_PORT, 
            				ZkTestHelper.CONNECTION_TIMEOUT, 
            				new NullWatcher());
	}
	
	@Before 
	public void setup() throws Exception{
		key = "/test-key-" + new Random().nextInt(10000);
	}
	
	@AfterClass
	public static void tearDown() throws Exception{
		TEST_HELPER.cleanUp();
	}
	
	@Test
	public void createNodeForKeyIfRequired() throws Exception{
		KeeperException expectedException =	new NoNodeException(); 
		ZkClock clock = new ZkClock(ZK);
		assertNull(ZK.exists(key, false));

		long sequence = clock.getNextSequence(key);
		assertEquals(0, sequence);
		assertNotNull(ZK.exists(key, false));
		assertEquals(sequence, getNodeDataAsLong(key));
	}
	
	@Test 
	public void createNextSequenceForExistingKey() throws Exception{
		ZkClock clock = new ZkClock(ZK);
		clock.getNextSequence(key);
		assertNotNull(ZK.exists(key, false));
		
		long sequence = clock.getNextSequence(key);
		assertEquals(1, sequence);
		assertEquals(sequence, getNodeDataAsLong(key));
	}

	@Test
	public void hammerClock() throws Exception{
		int iterations = 10000;
		ZkClock clock = new ZkClock(ZK);

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
		byte[] data = ZK.getData(key, false, new Stat());
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
