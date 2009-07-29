package com.talis.platform.sequencing.zookeeper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Random;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.jmx.ZKMBeanInfo;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Provider;
import com.talis.concurrent.LockProvider;
import com.talis.concurrent.zookeeper.ZkLockProvider;

public class ZkClockTest {

	public static final Logger LOG = LoggerFactory.getLogger(ZkClockTest.class);
	
	/**
	/foo
	/foo/buckets/
	/foo/lock
	/foo/buckets/0
	/foo/buckets/0/1
	/foo/buckets/0/nnnnn
	/foo/buckets/1
	/foo/buckets/1/1
	/foo/buckets/1/nnnnn
	/foo/buckets/nnnnn  

	*/
	
	private static ZkTestHelper TEST_HELPER;
	private static ZooKeeper ZK;
	private Provider<ZooKeeper> zkProvider;
	private LockProvider lockProvider;
	private String key;
	
	@BeforeClass
	public static void startZkServer() throws Exception {
		TEST_HELPER = new ZkTestHelper();
		TEST_HELPER.startServer();
		ZK = new ZooKeeper(	ZkTestHelper.DEFAULT_HOST_PORT, 
            				ZkTestHelper.CONNECTION_TIMEOUT, 
            				new NullWatcher());
		ZK.create("/root-lock", ZkClock.EMPTY_DATA, 
				ZkClock.DEFAULT_ACL, CreateMode.PERSISTENT);
	}
	
	@Before 
	public void setup() throws Exception{
		zkProvider = getProviderForZooKeeper(ZK);
		lockProvider = new ZkLockProvider(zkProvider);
		key = "test-key-" + new Random().nextInt(10000);
	}
	
	@AfterClass
	public static void tearDown() throws Exception{
		TEST_HELPER.cleanUp();
	}
	
	@Test
	public void createBucketForKeyIfRequired() throws Exception{
		KeeperException expectedException =	new NoNodeException(); 
		ZkClock clock = new ZkClock(zkProvider, lockProvider);
		assertNull(ZK.exists("/" + key, false));

		long sequence = clock.getNextSequence(key);
		assertEquals(1, sequence);
		assertNotNull(ZK.exists("/" + key + "/buckets/b0000000000/s0000000001", false));
	}

	@Test
	public void createLockNodeForKeyIfRequired() throws Exception{
		KeeperException expectedException =	new NoNodeException(); 
		ZkClock clock = new ZkClock(zkProvider, lockProvider);
		assertNull(ZK.exists("/" + key, false));

		long sequence = clock.getNextSequence(key);
		assertEquals(1, sequence);
		assertNotNull(ZK.exists("/" + key + "/lock", false));
	}
	
	@Test 
	public void createNextSequenceInExistingBucket() throws Exception{
		ZkClock clock = new ZkClock(zkProvider, lockProvider);
		long firstseq = clock.getNextSequence(key);
		
		assertNotNull(ZK.exists("/" + key + "/buckets/b0000000000/s0000000001", false));
		
		long sequence = clock.getNextSequence(key);
		assertEquals(2, sequence);
		assertNotNull(ZK.exists("/" + key + "/buckets/b0000000000/s0000000002", false));
	}
	
	@Test
	public void rolloverToNextBucketWhenThresholdReached() throws Exception{
		
		ZkClock clock = new ZkClock(zkProvider, lockProvider){
			@Override
			protected Long getRolloverValue(){
				return 2l;
			}
		};
		
		clock.getNextSequence(key);
		clock.getNextSequence(key);
		clock.getNextSequence(key);
		
		long sequence = clock.getNextSequence(key);
		assertEquals(4, sequence);
		assertNotNull(ZK.exists("/" + key + "/buckets/b0000000000/s0000000001", false));
		assertNotNull(ZK.exists("/" + key + "/buckets/b0000000000/s0000000002", false));
		assertNotNull(ZK.exists("/" + key + "/buckets/b0000000001/s0000000001", false));
	}
	
	@Test
	public void keyIsLockedWhenRollingOverBucket() throws Exception{
		/* TODO the problem now seems to be that getActiveBucket assumes
		 * that the only way it can fail is because the key hasn't yet 
		 *  been created. Need to also handle the situation when the key
		 *  exists, but no buckets. As this occurs during the check, lock, 
		 *  check again, create dance. 
		 */
		ZkClock clock = new ZkClock(zkProvider, lockProvider);
		CountDownLatch startGate = new CountDownLatch(1);
		CountDownLatch endGate = new CountDownLatch(2);
		new Thread(new Driver(key, clock, 1, startGate, endGate)).start();
		new Thread(new Driver(key, clock, 1, startGate, endGate)).start();
		
		Thread.sleep(100l);
		startGate.countDown();
		endGate.await();
		assertEquals(3, clock.getNextSequence(key));
	}
	
	@Test
	public void deletePreviousSequenceWhenCreatingNewOne() throws Exception{
		ZkClock clock = new ZkClock(zkProvider, lockProvider);
		clock.getNextSequence(key);
		assertNotNull(ZK.exists("/" + key + "/buckets/b0000000000/s0000000001", false));
		
		long sequence = clock.getNextSequence(key);
		assertEquals(2, sequence);
		assertNotNull(ZK.exists("/" + key + "/buckets/b0000000000/s0000000002", false));
		assertNull(ZK.exists("/" + key + "/buckets/b0000000000/s0000000001", false));
	}
	
	@Test @Ignore
	public void timings() throws Exception{
		int iterations = 10000;
		ZkClock clock = new ZkClock(zkProvider, lockProvider){
			@Override
			protected Long getRolloverValue(){
				return 100l;
			}
		};

		CountDownLatch startGate = new CountDownLatch(1);
		CountDownLatch endGate = new CountDownLatch(5);
		new Thread(new Driver("first-key", clock, iterations, startGate, endGate)).start();
		new Thread(new Driver("second-key", clock, iterations, startGate, endGate)).start();
		new Thread(new Driver("third-key", clock, iterations, startGate, endGate)).start();
		new Thread(new Driver("fourth-key", clock, iterations, startGate, endGate)).start();
		new Thread(new Driver("first-key", clock, iterations, startGate, endGate)).start();
		
		LOG.info("Waiting before starting 5 threads each doing " 
							+ iterations + " iterations (2 contending)");
		Thread.sleep(1000);
		long start = System.currentTimeMillis();
		startGate.countDown();
		endGate.await();
		long end = System.currentTimeMillis();
		LOG.info("Done in :" + (end - start) + " ms");
		
		assertEquals((2 * iterations ) + 1, clock.getNextSequence("first-key") );
		assertEquals(iterations + 1, clock.getNextSequence("second-key") );
		assertEquals(iterations + 1, clock.getNextSequence("third-key") );
		assertEquals(iterations + 1, clock.getNextSequence("fourth-key") );
		
//		long start = System.currentTimeMillis();
//		for (int i = 0; i < iterations; i++){
//			zk.exists("/" + key, false);
//		}
//		long end = System.currentTimeMillis();
//		System.out.println("Reads : " + (end - start));
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
	
	private Provider<ZooKeeper> getProviderForZooKeeper(final ZooKeeper keeper){
		return new Provider<ZooKeeper>(){
			@Override
			public ZooKeeper get() {
				return keeper;
			}
		};
	}
}
