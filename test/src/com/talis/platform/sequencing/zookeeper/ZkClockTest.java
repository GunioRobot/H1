package com.talis.platform.sequencing.zookeeper;

import static org.easymock.classextension.EasyMock.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.KeeperException.NodeExistsException;
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
	
	private static int TEST_INDEX = 0;
	private static int KEY_SEED = new Random().nextInt(10000); 
	
	@BeforeClass
	public static void startServer() throws Exception{
			
	}
	
	@Before 
	public void setup() throws Exception{
		myTestHelper = new ZkTestHelper();
		myTestHelper.startServer();
		myKeeper = new ZooKeeper(	ZkTestHelper.DEFAULT_HOST_PORT, 
            						ZkTestHelper.CONNECTION_TIMEOUT, 
            						new NullWatcher());
		key = String.format("/test-key-%s-%s", KEY_SEED, TEST_INDEX++);
	}
	
	@After
	public void tearDown() throws Exception{
		myKeeper.close();		
		myTestHelper.cleanUp();
	}
	
	@Test
	public void createNodeForKeyIfRequired() throws Exception{
		ZkClock clock = new ZkClock(getProviderForZooKeeper(myKeeper));
		assertNull(myKeeper.exists(key, false));

		long sequence = clock.getNextSequence(key);
		assertEquals(0, sequence);
		assertNotNull(myKeeper.exists(key, false));
		assertEquals(sequence, getNodeDataAsLong(key));
	}
	
	@Test 
	public void handleRaceConditionWhereKeyAlreadyExistsWhenClientTriesToCreateIt() 
	throws Exception{
		ZooKeeper mockKeeper = createStrictMock(ZooKeeper.class);
		mockKeeper.getData(key, false, new Stat());
		expectLastCall().andThrow(new NoNodeException());
		mockKeeper.create(key, ZkClock.DEFAULT_DATA, 
							ZkClock.DEFAULT_ACL, CreateMode.PERSISTENT);
		expectLastCall().andThrow(new NodeExistsException());
		mockKeeper.getData(key, false, new Stat());
		expectLastCall().andReturn(Arrays.copyOf(ZkClock.DEFAULT_DATA,
										ZkClock.DEFAULT_DATA.length));
		mockKeeper.setData(eq(key), (byte[])anyObject(), anyInt());
		expectLastCall().andReturn(new Stat());
		replay(mockKeeper);
		
		try{
			ZkClock clock = new ZkClock(getProviderForZooKeeper(mockKeeper));
			long sequence = clock.getNextSequence(key);
			assertEquals(0, sequence);
		}finally{
			verify(mockKeeper);
		}
	}
	
	@Test
	public void unexpectedExceptionThrownWhenCreatingNewKey() throws Exception{
		ZooKeeper mockKeeper = createStrictMock(ZooKeeper.class);
		mockKeeper.getData(key, false, new Stat());
		expectLastCall().andThrow(new NoNodeException());
		mockKeeper.create(key, ZkClock.DEFAULT_DATA, 
							ZkClock.DEFAULT_ACL, CreateMode.PERSISTENT);
		expectLastCall().andThrow(new KeeperException.MarshallingErrorException());
		mockKeeper.getData(key, false, new Stat());
		expectLastCall().andReturn(Arrays.copyOf(ZkClock.DEFAULT_DATA,
										ZkClock.DEFAULT_DATA.length));
		mockKeeper.setData(eq(key), (byte[])anyObject(), anyInt());
		expectLastCall().andReturn(new Stat());
		replay(mockKeeper);
		
		try{
			ZkClock clock = new ZkClock(getProviderForZooKeeper(mockKeeper));
			long sequence = clock.getNextSequence(key);
			assertEquals(0, sequence);
		}finally{
			verify(mockKeeper);
		}
	}
		
	@Test 
	public void clientThrowsInterruptedExceptionWhenCreatingNewKeyAndKeyIsntCreated()
	throws Exception{
		ZooKeeper mockKeeper = createStrictMock(ZooKeeper.class);
		
		mockKeeper.getData(key, false, new Stat());
		expectLastCall().andThrow(new NoNodeException());
		
		mockKeeper.create(key, ZkClock.DEFAULT_DATA, 
					ZkClock.DEFAULT_ACL, CreateMode.PERSISTENT);
		expectLastCall().andThrow(new InterruptedException());

		mockKeeper.getData(key, false, new Stat());
		expectLastCall().andThrow(new NoNodeException());
		
		mockKeeper.create(key, ZkClock.DEFAULT_DATA, 
					ZkClock.DEFAULT_ACL, CreateMode.PERSISTENT);
		expectLastCall().andReturn(key);
		
		mockKeeper.getData(key, false, new Stat());
		expectLastCall().andReturn(Arrays.copyOf(ZkClock.DEFAULT_DATA, 
										ZkClock.DEFAULT_DATA.length));
		
		mockKeeper.setData(eq(key), (byte[])anyObject(), anyInt());
		expectLastCall().andReturn(new Stat());
		replay(mockKeeper);
		
		try{
			ZkClock clock = new ZkClock(getProviderForZooKeeper(mockKeeper));
			long sequence = clock.getNextSequence(key);
			assertEquals(0, sequence);
		}finally{
			verify(mockKeeper);
		}
	}
	
	@Test 
	public void createNextSequenceForExistingKey() throws Exception{
		ZkClock clock = new ZkClock(getProviderForZooKeeper(myKeeper));
		ByteBuffer buf = ByteBuffer.wrap(ZkClock.DEFAULT_DATA);
		System.out.println(buf.getLong());
		System.out.println(clock.getNextSequence(key));
		assertNotNull(myKeeper.exists(key, false));
		long sequence = clock.getNextSequence(key);
		assertEquals(1, sequence);
		assertEquals(sequence, getNodeDataAsLong(key));
	}
		
	@Test
	public void clockSurvivesDisconnectionFromServer() throws Exception{
		ZkClock clock = new ZkClock(getProviderForZooKeeper(myKeeper));
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
		ZkClock clock = new ZkClock(getProviderForZooKeeper(myKeeper));
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
	
	@Test (expected=SequencingException.class)
	public void sessionExpiryExceptionThrownWhenIncrementingSequence() 
	throws Exception{
		ZooKeeper mockKeeper = createStrictMock(ZooKeeper.class);
		mockKeeper.getData(key, false, new Stat());
		expectLastCall().andThrow(new KeeperException.SessionExpiredException());
		replay(mockKeeper);
		ZkClock clock = new ZkClock(getProviderForZooKeeper(mockKeeper));
		try{
			clock.getNextSequence(key);
		}finally{
			verify(mockKeeper);
		}
	}
	
	@Test (expected=SequencingException.class)
	public void ifUnexpectedExceptionEncounteredWhenIncrementingSeqRetryMaxTimes() 
	throws Exception{
		System.setProperty(ZkClock.RETRY_COUNT_PROPERTY, "3");
		ZooKeeper mockKeeper = createStrictMock(ZooKeeper.class);
		mockKeeper.getData(key, false, new Stat());
		expectLastCall().andThrow(new KeeperException.InvalidACLException());
		expectLastCall().andThrow(new KeeperException.MarshallingErrorException());
		expectLastCall().andThrow(new KeeperException.BadArgumentsException());
		replay(mockKeeper);
		ZkClock clock = new ZkClock(getProviderForZooKeeper(mockKeeper));
		try{
			clock.getNextSequence(key);
		}catch (SequencingException e){
			assertTrue(e.getCause() instanceof 
					KeeperException.BadArgumentsException);
			throw e;
		}finally{
			verify(mockKeeper);
			System.clearProperty(ZkClock.RETRY_COUNT_PROPERTY);
		}
	}
	
	@Test
	public void clientThrowsInterruptedExceptionWhenIncrementingSequenceAndAllDataIsWritten() 
	throws Exception{
		
		byte[] firstDataValue = new byte[] { 0,0,0,0,0,0,0,10 };
		byte[] secondDataValue = new byte[] { 0,0,0,0,0,0,0,11 };
		byte[] thirdDataValue = new byte[] { 0,0,0,0,0,0,0,12 };
		
		Stat stat = new Stat();
		ZooKeeper mockKeeper = createStrictMock(ZooKeeper.class);
		mockKeeper.getData(key, false, stat);
		expectLastCall().andReturn(Arrays.copyOf(firstDataValue,8));
		mockKeeper.setData(eq(key), aryEq(secondDataValue), anyInt());
		expectLastCall().andThrow(new InterruptedException());
		mockKeeper.getData(key, false, stat);
		expectLastCall().andReturn(Arrays.copyOf(secondDataValue, 8));
		mockKeeper.setData(eq(key), aryEq(thirdDataValue), anyInt());
		expectLastCall().andReturn(stat);
		replay(mockKeeper);
		
		ZkClock clock = new ZkClock(getProviderForZooKeeper(mockKeeper));
		try{
			assertEquals(12, clock.getNextSequence(key));
		}finally{
			verify(mockKeeper);
		}
	}
	
	@Test
	public void clientThrowsInterruptedExceptionWhenIncrementingSequenceAndSomeDataIsNotWritten() 
	throws Exception{
		
		byte[] firstDataValue = new byte[] { 0,0,0,0,0,0,0,10 };
		byte[] secondDataValue = new byte[] { 0,0,0,0,0,0,0,11 };
		
		Stat stat = new Stat();
		ZooKeeper mockKeeper = createStrictMock(ZooKeeper.class);
		mockKeeper.getData(key, false, stat);
		expectLastCall().andReturn(Arrays.copyOf(firstDataValue, 8));
		mockKeeper.setData(eq(key), aryEq(secondDataValue), anyInt());
		expectLastCall().andThrow(new InterruptedException("KABOOM!"));
		mockKeeper.getData(key, false, stat);
		expectLastCall().andReturn(Arrays.copyOf(firstDataValue, 8));
		mockKeeper.setData(eq(key), aryEq(secondDataValue), anyInt());
		expectLastCall().andReturn(stat);
		replay(mockKeeper);
		
		ZkClock clock = new ZkClock(getProviderForZooKeeper(mockKeeper));
		try{
			assertEquals(11, clock.getNextSequence(key));
		}finally{
			verify(mockKeeper);
		}
	}
	
	@Test @Ignore
	public void testManyIterations() throws Exception{
		int iterations = 1000000;
		ZkClock clock = new ZkClock(getProviderForZooKeeper(myKeeper));
		long seq = 0;
		long start = System.currentTimeMillis();
		for (int i = 0 ; i < iterations ; i++){
			seq = clock.getNextSequence(key);
		}
		long end = System.currentTimeMillis();
		System.out.println(String.format("Made %s increments in %s ms", iterations, (end - start)));
		assertEquals(iterations, seq + 1);
	}
	
	@Test
	public void testConcurrentClients() throws Exception{
		int iterations = 10000;
		ZkClock clock = new ZkClock(getProviderForZooKeeper(myKeeper));

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
	
	private ZooKeeperProvider getProviderForZooKeeper(final ZooKeeper keeper){
		return new ZooKeeperProvider(){
			@Override
			public ZooKeeper get() throws SequencingException {
				return keeper;
			}
		};
	}
	
}
