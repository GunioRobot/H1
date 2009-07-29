package com.talis.concurrent.zookeeper;

import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.classextension.EasyMock.createStrictMock;
import static org.easymock.classextension.EasyMock.replay;
import static org.easymock.classextension.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import sun.util.LocaleServiceProviderPool.LocalizedObjectGetter;

import com.talis.platform.sequencing.zookeeper.NullWatcher;
import com.talis.platform.sequencing.zookeeper.ZkClock;
import com.talis.platform.sequencing.zookeeper.ZkTestHelper;

public class ZkLockTest {

	private static ZkTestHelper TEST_HELPER;
	private static ZooKeeper ZK;
	private String key;
	
	@BeforeClass
	public static void startZkServer() throws Exception{
		TEST_HELPER = new ZkTestHelper();
		TEST_HELPER.startServer();
		ZK = new ZooKeeper(	ZkTestHelper.DEFAULT_HOST_PORT, 
            				ZkTestHelper.CONNECTION_TIMEOUT, 
            				new NullWatcher());
	}
	
	@Before 
	public void setup() throws Exception{
		key = "/key-" + new Random().nextInt(10000);
		ZK.create(key, ZkClock.EMPTY_DATA, 
				ZkClock.DEFAULT_ACL, CreateMode.PERSISTENT);
	}
	
	@AfterClass
	public static void tearDown() throws Exception{
		TEST_HELPER.cleanUp();
	}
	
	@Test (expected=UnsupportedOperationException.class)
	public void obtainingLockWithoutPossibilityOfInterruptionIsUnsupported()
	throws Exception{
		new ZkLock(ZK, key).lock();
	}
	
	@Test (expected=UnsupportedOperationException.class)
	public void newConditionIsUnsupported()
	throws Exception{
		new ZkLock(ZK, key).newCondition();
	}
	
	@Test (expected=UnsupportedOperationException.class)
	public void tryLockWithTimeoutIsUnsupported()
	throws Exception{
		new ZkLock(ZK, key).tryLock(100, TimeUnit.MILLISECONDS);
	}
	
	@Test
	public void obtainLockImmediatelyIfNotAlreadyLocked() throws Exception{
		ZkLock theLock = new ZkLock(ZK, key);
		theLock.lockInterruptibly();
		assertNotNull(ZK.exists(key + "/lock/a0000000000", false));
	}
	
	@Test
	public void lockRequestsAreCreatedSequentialAndEphemeral() throws Exception{
		ZooKeeper mockKeeper = createStrictMock(ZooKeeper.class);
		mockKeeper.exists(key + "/lock" , false);
		expectLastCall().andReturn(new Stat());
		mockKeeper.create(key + "/lock/a" , ZkClock.EMPTY_DATA, 
					ZkClock.DEFAULT_ACL, CreateMode.EPHEMERAL_SEQUENTIAL);
		expectLastCall().andReturn(key + "lock/a0000000000");
		
		mockKeeper.getChildren(key + "/lock", false); 
		expectLastCall().andReturn(new ArrayList<String>(){{ add("a0000000000");}});		

		replay(mockKeeper);
		
		Lock theLock = new ZkLock(mockKeeper, key);
		theLock.lockInterruptibly();
		verify(mockKeeper);
	}
	
	@Test
	public void waitForLockBlocksIfKeyAlreadyLocked() throws Exception{
		long timeout = 200l;
		final Lock theLock = new ZkLock(ZK, key);
		Thread client = new Thread(){
			@Override
			public void run() {
				try {
					theLock.lockInterruptibly();
				} catch (InterruptedException e) {
					// we expect this to happen
				}
		    }
		};
		
		try{
			client.start();
			Thread.sleep(timeout);
			client.interrupt();
			client.join(timeout);
			assertFalse(client.isAlive());
		}catch(Exception e){
			fail("Didn't expect an exception here");
		}
	}

	@Test
	public void obtainLockOnceLockedKeyBecomesAvailable() throws Exception{
		ZK.create(key + "/lock" , ZkClock.EMPTY_DATA, 
				ZkClock.DEFAULT_ACL, CreateMode.PERSISTENT);
		String firstLock = 
			ZK.create(key + "/lock/a" , ZkClock.EMPTY_DATA, 
					ZkClock.DEFAULT_ACL, CreateMode.EPHEMERAL_SEQUENTIAL);
		assertEquals(key + "/lock/a0000000000", firstLock);
		String secondLock = key + "/lock/a0000000001";
		long blockWaitTime = 500l;
		final Lock theLock = new ZkLock(ZK, key);
		TimedLockClient client = new TimedLockClient(ZK, key); 
		Thread t = new Thread(client);
		t.start();
		Thread.sleep(10l); // give the client a change to make a lock request
		assertNotNull(ZK.exists(firstLock, false));
		assertNotNull(ZK.exists(secondLock, false));
		Thread.sleep(blockWaitTime);
		assertEquals(-1l, client.lockObtainedTime);
		ZK.delete(firstLock, -1);
		
		Thread.sleep(100l); // give the client time to obtain the lock
		long clientBlockingDuration = client.lockObtainedTime - client.startTime;
		assertTrue(clientBlockingDuration >= blockWaitTime);
		assertNull(ZK.exists(firstLock, false));
		assertNotNull(ZK.exists(secondLock, false));
	}
	
	@Test
	public void waitForMultipleOtherClientsBeforeObtainingLock() throws Exception{
		ZK.create(key + "/lock" , ZkClock.EMPTY_DATA, 
				ZkClock.DEFAULT_ACL, CreateMode.PERSISTENT);
		String firstLock = 
			ZK.create(key + "/lock/a" , ZkClock.EMPTY_DATA, 
					ZkClock.DEFAULT_ACL, CreateMode.EPHEMERAL_SEQUENTIAL);
		String secondLock = 
			ZK.create(key + "/lock/a" , ZkClock.EMPTY_DATA, 
					ZkClock.DEFAULT_ACL, CreateMode.EPHEMERAL_SEQUENTIAL);
		String thirdLock = 
			ZK.create(key + "/lock/a" , ZkClock.EMPTY_DATA, 
					ZkClock.DEFAULT_ACL, CreateMode.EPHEMERAL_SEQUENTIAL);
		assertEquals(key + "/lock/a0000000000", firstLock);
		assertEquals(key + "/lock/a0000000001", secondLock);
		assertEquals(key + "/lock/a0000000002", thirdLock);
		String fourthLock = key + "/lock/a0000000003";
		assertNull(ZK.exists(fourthLock, false));
		long blockWaitTime = 100l;
		
		final Lock theLock = new ZkLock(ZK, key);
		TimedLockClient client = new TimedLockClient(ZK, key); 
		Thread t = new Thread(client);
		t.start();

		Thread.sleep(blockWaitTime);
		ZK.delete(firstLock, -1);
		Thread.sleep(blockWaitTime);
		ZK.delete(secondLock, -1);

		//once the final locknode is removed, the client 
		//should be able to obtain the lock, so lets grab the 
		// timestamp before we do that
		long lastBlockedTimestamp = System.currentTimeMillis();
		Thread.sleep(blockWaitTime);
		ZK.delete(thirdLock, -1);
		
		Thread.sleep(100l); // give the client time to obtain the lock
		long totalBlockingWaitTime = client.lockObtainedTime - client.startTime;
		assertTrue(totalBlockingWaitTime >= (blockWaitTime * 3));
		assertTrue(client.lockObtainedTime > lastBlockedTimestamp);
		assertNull(ZK.exists(firstLock, false));
		assertNull(ZK.exists(secondLock, false));
		assertNull(ZK.exists(thirdLock, false));
		assertNotNull(ZK.exists(fourthLock, false));
	}
	
	@Test
	public void unlockWhenLockIsHeldRemovesZooKeeperNode() 
	throws InterruptedException, KeeperException{
		Lock theLock = new ZkLock(ZK, key);
		String lockNode = key + "/lock/a0000000000";
		assertNull(ZK.exists(lockNode, false));
		
		theLock.lockInterruptibly();
		assertNotNull(ZK.exists(lockNode, false));
		theLock.unlock();
		assertNull(ZK.exists(lockNode, false));
	}
	
	@Test
	public void unlockWhenLockIsNotHeldIsNoop() 
	throws InterruptedException, KeeperException{
		Lock theLock = new ZkLock(ZK, key);
		String lockNode = key + "/lock/a0000000000";
		assertNull(ZK.exists(lockNode, false));
		theLock.unlock();
		assertNull(ZK.exists(lockNode, false));
	}
	
	@Test
	public void afterLockingLocksCannotBeReusedUntilUnlocked()
	throws Exception{
		Lock theLock = new ZkLock(ZK, key);
		theLock.lockInterruptibly();
		try{
			theLock.lockInterruptibly();
			fail("Expected an exception here");
		}catch(AlreadyLockedException e){
			// we expect this exception
		}
		theLock.unlock();
		theLock.lockInterruptibly();
	}

	@Test
	public void tryLockReturnsFalseIfKeyAlreadyLocked()
	throws Exception{
		ZK.create(key + "/lock" , ZkClock.EMPTY_DATA, 
				ZkClock.DEFAULT_ACL, CreateMode.PERSISTENT);
		String firstLock = 
			ZK.create(key + "/lock/a" , ZkClock.EMPTY_DATA, 
					ZkClock.DEFAULT_ACL, CreateMode.EPHEMERAL_SEQUENTIAL);
		Lock theLock = new ZkLock(ZK, key);
		assertFalse(theLock.tryLock());
		assertEquals(1, ZK.getChildren(key, false).size());
	}
	
	@Test
	public void tryLockCreatesRequestNodeIfSuccessful()
	throws Exception{
		assertEquals(0, ZK.getChildren(key, false).size());
		Lock theLock = new ZkLock(ZK, key);
		assertTrue(theLock.tryLock());
		assertEquals(1, ZK.getChildren(key, false).size());
	}

	@Test
	public void tryLockReturnsFalseIfEncountersInterruptedExceptionWhenMakingRequest()
	throws Exception{
		ZooKeeper mockKeeper = createStrictMock(ZooKeeper.class);
		mockKeeper.exists(key + "/lock", false);
		expectLastCall().andReturn(new Stat());
		mockKeeper.create(	key + "/lock/a",
							ZkClock.EMPTY_DATA, 
							ZkClock.DEFAULT_ACL,
							CreateMode.EPHEMERAL_SEQUENTIAL);
		expectLastCall().andThrow(new InterruptedException("TEST"));
		replay(mockKeeper);
		
		Lock theLock = new ZkLock(mockKeeper, key);
		assertFalse(theLock.tryLock());
		verify(mockKeeper);
	}
	
	@Test
	public void tryLockReturnsFalseIfEncountersKeeperExceptionWhenMakingRequest()
	throws Exception{
		ZooKeeper mockKeeper = createStrictMock(ZooKeeper.class);
		mockKeeper.exists(key + "/lock", false);
		expectLastCall().andReturn(new Stat());
		mockKeeper.create(	key + "/lock/a",
							ZkClock.EMPTY_DATA, 
							ZkClock.DEFAULT_ACL,
							CreateMode.EPHEMERAL_SEQUENTIAL);
		expectLastCall().andThrow(new DummyKeeperException());
		replay(mockKeeper);
		
		Lock theLock = new ZkLock(mockKeeper, key);
		assertFalse(theLock.tryLock());
		verify(mockKeeper);
	}
	
	@Test
	public void tryLockReturnsFalseAndCleansUpIfEncountersInterruptedExceptionWhenCheckingQueue()
	throws Exception{
		ZooKeeper mockKeeper = createStrictMock(ZooKeeper.class);
		mockKeeper.exists(key + "/lock", false);
		expectLastCall().andReturn(new Stat());
		mockKeeper.create(	key + "/lock/a",
							ZkClock.EMPTY_DATA, 
							ZkClock.DEFAULT_ACL,
							CreateMode.EPHEMERAL_SEQUENTIAL);
		expectLastCall().andReturn(key + "/lock/a0000000000");
		mockKeeper.getChildren(key + "/lock", false);
		expectLastCall().andThrow(new InterruptedException("TEST"));
		mockKeeper.delete(key + "/lock/a0000000000", ZkLock.ALL_VERSIONS);
		replay(mockKeeper);
		
		Lock theLock = new ZkLock(mockKeeper, key);
		assertFalse(theLock.tryLock());
		verify(mockKeeper);
	}

	@Test
	public void tryLockReturnsFalseAndCleansUpIfEncountersKeeperExceptionWhenCheckingQueue()
	throws Exception{
		ZooKeeper mockKeeper = createStrictMock(ZooKeeper.class);
		mockKeeper.exists(key + "/lock", false);
		expectLastCall().andReturn(new Stat());
		mockKeeper.create(	key + "/lock/a",
							ZkClock.EMPTY_DATA, 
							ZkClock.DEFAULT_ACL,
							CreateMode.EPHEMERAL_SEQUENTIAL);
		expectLastCall().andReturn(key + "/lock/a0000000001");
		mockKeeper.getChildren(key + "/lock", false);
		expectLastCall().andThrow(new DummyKeeperException());
		mockKeeper.delete(key + "/lock/a0000000001", ZkLock.ALL_VERSIONS);
		replay(mockKeeper);
		
		Lock theLock = new ZkLock(mockKeeper, key);
		assertFalse(theLock.tryLock());
		verify(mockKeeper);
	}
	
	@Test
	public void afterFailedTryLockClientsCanStillObtainLocks()
	throws Exception{
		ZK.create(key + "/lock" , ZkClock.EMPTY_DATA, 
				ZkClock.DEFAULT_ACL, CreateMode.PERSISTENT);
		String firstLock = 
			ZK.create(key + "/lock/a" , ZkClock.EMPTY_DATA, 
					ZkClock.DEFAULT_ACL, CreateMode.EPHEMERAL_SEQUENTIAL);
		Lock theLock = new ZkLock(ZK, key);
		assertFalse(theLock.tryLock());
		assertEquals(1, ZK.getChildren(key, false).size());
		ZK.delete(firstLock, ZkLock.ALL_VERSIONS);
		assertEquals(0, ZK.getChildren(key + "/lock", false).size());
		assertTrue(theLock.tryLock());
		assertEquals(1, ZK.getChildren(key + "/lock", false).size());
	}
	
	@Test
	public void afterSuccessfulTryLockMustUnlockBeforeReusing() 
	throws Exception{
		Lock theLock = new ZkLock(ZK, key);
		assertTrue(theLock.tryLock());
		try{
			theLock.tryLock();
			fail("Expected an exception here");
		}catch(AlreadyLockedException e){
			// we expect this exception
		}
		theLock.unlock();
		assertTrue(theLock.tryLock());
	}
	
	static class TimedLockClient implements Runnable{
		long startTime = -1;
		long lockObtainedTime = -1;
		String key;
		ZooKeeper zk;
		
		TimedLockClient (ZooKeeper zk, String key){
			this.zk = zk;
			this.key = key;
		}
		
		@Override
		public void run() {
			Lock theLock = new ZkLock(zk, key);
			startTime = System.currentTimeMillis();
			try {
				theLock.lockInterruptibly();
			} catch (InterruptedException e) {
				fail("Didn't expect an exception here");
			}
			lockObtainedTime = System.currentTimeMillis();
		}
	}
	
	static class DummyKeeperException extends KeeperException{

		public DummyKeeperException() {
			super(Code.APIERROR);
		}
		
	}
}
