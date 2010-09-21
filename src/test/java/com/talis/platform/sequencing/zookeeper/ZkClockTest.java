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

import static org.easymock.EasyMock.anyInt;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.aryEq;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.talis.platform.sequencing.SequencingException;
import com.talis.platform.sequencing.zookeeper.metrics.ZooKeeperMetrics;

public class ZkClockTest {

	public static final Logger LOG = LoggerFactory.getLogger(ZkClockTest.class);
	
	private ZkTestHelper myTestHelper;
	private ZooKeeper myKeeper;
	private ZooKeeperProvider myKeeperProvider;
	private String key;
	
	private byte[] firstDataValue = new byte[] { 0,0,0,0,0,0,0,10 };
	private byte[] secondDataValue = new byte[] { 0,0,0,0,0,0,0,11 };
	private byte[] thirdDataValue = new byte[] { 0,0,0,0,0,0,0,12 };
	
	private static int TEST_INDEX = 0;
	private static int KEY_SEED = new Random().nextInt(10000); 
	
	@Before 
	public void setup() throws Exception{
		myTestHelper = new ZkTestHelper();
		myTestHelper.startServer();
		myKeeper = new ZooKeeper(	ZkTestHelper.DEFAULT_HOST_PORT, 
            						ZkTestHelper.CONNECTION_TIMEOUT, 
            						new NullWatcher());
		myKeeperProvider = getProviderForZooKeeper(myKeeper);
		key = String.format("/test-key-%s-%s", KEY_SEED, TEST_INDEX++);
	}
	
	@After
	public void tearDown() throws Exception{
		myKeeper.close();		
		myTestHelper.cleanUp();
	}
	
	@Test
	public void createNodeForKeyIfRequired() throws Exception{
		ZkClock clock = new ZkClock(myKeeperProvider, new NullMetrics());
		assertNull(myKeeper.exists(key, false));

		long sequence = clock.getNextSequence(key);
		assertEquals(0, sequence);
		assertNotNull(myKeeper.exists(key, false));
		assertEquals(sequence, getNodeDataAsLong(key));
	}
	
	@Test
	public void reportKeyCreationsViaMetricsObject() throws Exception{
		ZooKeeperMetrics mockMetrics = createStrictMock(ZooKeeperMetrics.class);
		mockMetrics.incrementKeyCreations();
		replay(mockMetrics);
		
		ZkClock clock = new ZkClock(myKeeperProvider, mockMetrics);
		assertNull(myKeeper.exists(key, false));
		clock.getNextSequence(key);
		verify(mockMetrics);
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
			ZkClock clock = new ZkClock(getProviderForZooKeeper(mockKeeper), 
											new NullMetrics());
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
			ZkClock clock = new ZkClock(getProviderForZooKeeper(mockKeeper), 
										new NullMetrics());
			long sequence = clock.getNextSequence(key);
			assertEquals(0, sequence);
		}finally{
			verify(mockKeeper);
		}
	}
		
	@Test 
	public void clientThrowsInterruptedExceptionWhenCreatingNewKeyAndKeyIsntCreated()
	throws Exception{
		ZooKeeperMetrics mockMetrics = createStrictMock(ZooKeeperMetrics.class);
		mockMetrics.incrementInterruptedExceptions();
		mockMetrics.incrementKeyCreations();
		replay(mockMetrics);
		
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
			ZkClock clock = new ZkClock(getProviderForZooKeeper(mockKeeper), 
													mockMetrics);
			long sequence = clock.getNextSequence(key);
			assertEquals(0, sequence);
		}finally{
			verify(mockKeeper);
			verify(mockMetrics);
		}
	}
	
	@Test 
	public void createNextSequenceForExistingKey() throws Exception{
		ZkClock clock = new ZkClock(myKeeperProvider, new NullMetrics());
		ByteBuffer buf = ByteBuffer.wrap(ZkClock.DEFAULT_DATA);
		clock.getNextSequence(key);
		assertNotNull(myKeeper.exists(key, false));
		long sequence = clock.getNextSequence(key);
		assertEquals(1, sequence);
		assertEquals(sequence, getNodeDataAsLong(key));
	}
		
	@Test
	public void clockSurvivesDisconnectionFromServer() throws Exception{
		ZkClock clock = new ZkClock(myKeeperProvider, new NullMetrics());
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
		ZkClock clock = new ZkClock(myKeeperProvider, new NullMetrics());
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
	public void surviveSessionExpiration() throws Exception{
		System.setProperty(ZooKeeperProvider.SESSION_TIMEOUT_PROPERTY, "100");
		try{
			ZkClock clock = new ZkClock(myKeeperProvider, new NullMetrics());
			assertEquals(0, clock.getNextSequence(key));
			myTestHelper.stopServer();
			Thread.sleep(300l);
			myTestHelper.startServer();
			assertEquals(1, clock.getNextSequence(key));
		}finally{
			System.clearProperty(ZooKeeperProvider.SESSION_TIMEOUT_PROPERTY);
		}
	}
	
	
	@Test (expected=SequencingException.class)
	public void sessionExpiryExceptionThrownWhenIncrementingSequence() 
	throws Exception{
		ZooKeeperMetrics mockMetrics = createStrictMock(ZooKeeperMetrics.class);
		mockMetrics.incrementSessionExpiredEvents();
		replay(mockMetrics);
		
		ZooKeeper mockKeeper = createStrictMock(ZooKeeper.class);
		mockKeeper.getData(key, false, new Stat());
		expectLastCall().andThrow(new KeeperException.SessionExpiredException());
		replay(mockKeeper);
		ZkClock clock = new ZkClock(getProviderForZooKeeper(mockKeeper), 
										mockMetrics);
		try{
			clock.getNextSequence(key);
		}finally{
			verify(mockKeeper);
			verify(mockMetrics);
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
		ZkClock clock = new ZkClock(getProviderForZooKeeper(mockKeeper), 
										new NullMetrics());
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
		ZooKeeperMetrics mockMetrics = createStrictMock(ZooKeeperMetrics.class);
		mockMetrics.incrementInterruptedExceptions();
		replay(mockMetrics);
		
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
		
		ZkClock clock = new ZkClock(getProviderForZooKeeper(mockKeeper), 
										mockMetrics);
		try{
			assertEquals(12, clock.getNextSequence(key));
		}finally{
			verify(mockKeeper);
			verify(mockMetrics);
		}
	}
	
	@Test
	public void clientThrowsInterruptedExceptionWhenIncrementingSequenceAndDataIsNotWritten() 
	throws Exception{
		
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
		
		ZkClock clock = new ZkClock(getProviderForZooKeeper(mockKeeper), 
										new NullMetrics());
		try{
			assertEquals(11, clock.getNextSequence(key));
		}finally{
			verify(mockKeeper);
		}
	}
		
	@Test
	public void reportKeyCollisionsViaMetricsObject() throws Exception{
		ZooKeeperMetrics mockMetrics = createStrictMock(ZooKeeperMetrics.class);
		mockMetrics.incrementKeyCollisions();
		replay(mockMetrics);
		
		Stat stat = new Stat();
		ZooKeeper mockKeeper = createStrictMock(ZooKeeper.class);
		mockKeeper.getData(key, false, stat);
		expectLastCall().andReturn(Arrays.copyOf(firstDataValue, 8));
		mockKeeper.setData(eq(key), aryEq(secondDataValue), anyInt());
		expectLastCall().andThrow(new KeeperException.BadVersionException());
		mockKeeper.getData(key, false, stat);
		expectLastCall().andReturn(Arrays.copyOf(secondDataValue, 8));
		mockKeeper.setData(eq(key), aryEq(thirdDataValue), anyInt());
		expectLastCall().andReturn(stat);
		replay(mockKeeper);
		
		ZkClock clock = new ZkClock(getProviderForZooKeeper(mockKeeper), 
										mockMetrics);
		try{
			assertEquals(12, clock.getNextSequence(key));
		}finally{
			verify(mockKeeper);
			verify(mockMetrics);
		}
	}
		
	@Test @Ignore
	public void testManyIterations() throws Exception{
		int iterations = 1000000;
		ZkClock clock = new ZkClock(myKeeperProvider, new NullMetrics());
		long seq = 0;
		long start = System.currentTimeMillis();
		for (int i = 0 ; i < iterations ; i++){
			seq = clock.getNextSequence(key);
		}
		long end = System.currentTimeMillis();
		System.out.println(String.format("Made %s increments in %s ms", iterations, (end - start)));
		assertEquals(iterations, seq + 1);
	}
	
	@Test @Ignore
	public void testConcurrentClients() throws Exception{
		int iterations = 10000;
		ZkClock clock = new ZkClock(myKeeperProvider, new NullMetrics());

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
	
	private ZooKeeperProvider getProviderForZooKeeper(final ZooKeeper keeper){
		return new ZooKeeperProvider(){
			@Override
			public ZooKeeper get(){
				return keeper;
			}
		};
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
	
	class NullMetrics implements ZooKeeperMetrics{
		@Override
		public void incrementKeyCollisions() {}

		@Override
		public void incrementConnectionLossEvents() {}

		@Override
		public void incrementSessionExpiredEvents() {}

		@Override
		public void incrementInterruptedExceptions() {}

		@Override
		public void incrementKeeperExceptions() {}

		@Override
		public void incrementKeyCreations() {}
	}
}
