package com.talis.concurrent.zookeeper;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.talis.platform.sequencing.zookeeper.ZkClock;


public class ZkLock implements Lock, Watcher{

	static final Logger LOG = LoggerFactory.getLogger(ZkLock.class);
	private static final Comparator<String> SEQUENTIAL_NODE_COMPARATOR = 
		new Comparator<String>(){
			@Override
			public int compare(String first, String second) {
				return new Long(Long.parseLong(first))
							.compareTo(Long.parseLong(second));
			}
		};
	public static final int ALL_VERSIONS = -1;
		
	private final ZooKeeper myKeeper;
	private final String myKey;
	private String myLockRequest;
	
	public ZkLock(ZooKeeper keeper, String key){
		LOG.info("Initialising Lock instance");
		myKeeper = keeper;
		myKey = key;
	}
	
	@Override
	public void lock(){
		throw new UnsupportedOperationException(
				"Not implemented, use lockInterruptibly instead");
	}
	
	@Override
	public Condition newCondition() {
		throw new UnsupportedOperationException("Not Supported");
	}
	
	@Override
	public void lockInterruptibly() throws InterruptedException {
		if (null != myLockRequest){
			String message =
				String.format("Lock already has lock request active for %s : %s",
						myKey, myLockRequest);
			throw new AlreadyLockedException(message);
		}
		
		LOG.info(String.format("Placing blocking lock request for key %s", 
								myKey));
		try {
			requestLock();
			waitForLock();
		} catch (KeeperException e) {
			//TODO: generated
			LOG.error("ERROR:", e);
		} catch (InterruptedException e){
			LOG.warn("Thread attempting to obtain distributed Lock " +
						"thread was interrupted", e);
			throw e;
		}		
	}

	@Override
	public boolean tryLock() {
		if (null != myLockRequest){
			String message =
				String.format("Lock already has lock request active for %s : %s",
						myKey, myLockRequest);
			throw new AlreadyLockedException(message);
		}
		
		LOG.info(String.format("Obtaining lock for key %s " +
								"only if immediately available", myKey));
		try {
			requestLock();
		} catch (KeeperException e) {
			// TODO Auto-generated catch block
			LOG.error("ERROR:", e);
			return false;
		} catch (InterruptedException e) {
			LOG.error("Unable to obtain lock due to exception", e);
			return false;
		}
		
		try{
			List<String> activeRequests = getOrderedLockRequests();
			if (myLockRequest.equals(activeRequests.get(0))){
				LOG.info(String.format("Obtained Lock for key %s", myKey));
				return true;
			}else{
				LOG.info(String.format("Couldn't obtain lock for key %s", myKey));
				removeLockRequest();
				return false;
			}
		} catch (KeeperException e) {
			// TODO Auto-generated catch block
			LOG.error("ERROR:", e);
			removeLockRequest();
			return false;
		}catch(InterruptedException e){
			removeLockRequest();
			return false;
		}
	}

	@Override
	public boolean tryLock(long arg0, TimeUnit arg1)
			throws InterruptedException {
		throw new UnsupportedOperationException(
				"Timeout values are not supported, use tryLock() instead");
	}

	@Override
	public void unlock() {
		if (null == myLockRequest){
			LOG.info(String.format("No lock request made for %s, returning",
									myKey));
			return;
		}
		removeLockRequest();
	}

	private void removeLockRequest(){
		LOG.info(String.format("Relinquishing lock request for %s : %s", 
				myKey, myLockRequest));
		try{
			myKeeper.delete(myKey + "/" + myLockRequest, ALL_VERSIONS);
			myLockRequest = null;
		} catch (InterruptedException e) {
			LOG.warn("Unable to cancel lock request", e);
		} catch (KeeperException e) {
			// TODO Auto-generated catch block
			LOG.error("ERROR:", e);
		}
	}
	
	private void requestLock() throws KeeperException, InterruptedException{
		myLockRequest = myKeeper.create(myKey + "/", 
				ZkClock.EMPTY_DATA, 
				ZkClock.DEFAULT_ACL, 
				CreateMode.EPHEMERAL_SEQUENTIAL);
		myLockRequest = myLockRequest.substring(
				myLockRequest.lastIndexOf("/") + 1);
	}
	
	private List<String> getOrderedLockRequests() 
	throws InterruptedException, KeeperException{
		List<String> lockRequests = myKeeper.getChildren(myKey, false);
		Collections.sort(lockRequests, SEQUENTIAL_NODE_COMPARATOR);
		LOG.info(String.format("Currently active lock requests for %s : %s",
				myKey, lockRequests));
		return lockRequests;
	}
	
	private void waitForLock() 
	throws KeeperException, InterruptedException{
		LOG.info("Checking lock request queue status");
		List<String> lockRequests = getOrderedLockRequests(); 
		if (myLockRequest.equals(lockRequests.get(0))){
			LOG.info(
				String.format("I own the lock on %s with request %s", 
								myKey, myLockRequest));
			return;
		}else{
			int nextLowestLockIndex = lockRequests.indexOf(myLockRequest) - 1;
			Stat nextLowestLockStat = 
				myKeeper.exists(myKey + "/" + 
									lockRequests.get(nextLowestLockIndex), 
								this);
			if (null != nextLowestLockStat){
				LOG.info(
						String.format("My Lock request # was %s, waiting on %s", 
										myLockRequest, 
										lockRequests.get(nextLowestLockIndex)));
				waitForNotification();
			}
			waitForLock();
		}	
	}
	
	private void waitForNotification() throws InterruptedException{
		// wait for notification from event when  
		// next lowest lock node is deleted
		synchronized(this){
			wait();
			LOG.info("Thread came out of waiting state, " +
					 "checking if I own the lock");
		}
	}
	
	@Override
	public void process(WatchedEvent event) {
		LOG.info("WatchedEvent fired: " + event.toString());
		synchronized (this) {
			notifyAll();	
		}
	}
	
}
