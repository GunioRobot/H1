package com.talis.platform.sequencing.zookeeper;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.Lock;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.recipes.lock.WriteLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.talis.concurrent.LockProvider;
import com.talis.platform.sequencing.Clock;

public class ZkClock implements Clock {
	
	static final Logger LOG = LoggerFactory.getLogger(ZkClock.class);
	
	/**
	 * TODO: Can watches be used to keep track of latest bucket?
	 * This would save doing a lookup each time, but need to guarantee
	 * currency - poss use sync() when rolling over/creating new
	 * bucket as this will happen infreqently (once every 2^127 writes)
	 * 
	 * TODO: Also need to lock on key when creating a new bucket - otherwise
	 * there's a race between instances and we could end up with partially
	 * filled buckets
	 */
	
	public static final byte[] EMPTY_DATA = new byte[0];
	public static final List<ACL> DEFAULT_ACL = ZooDefs.Ids.OPEN_ACL_UNSAFE;
	public static final int DEFAULT_ROLLOVER_VALUE = Integer.MAX_VALUE - 1000000;
	public static final String ROLLOVER_VALUE_PROPERTY = 
		"com.talis.platform.sequencing.zookeeper.rollover";
	
	private static final Long ROLLOVER_VALUE = 
		Long.getLong(ROLLOVER_VALUE_PROPERTY) != null ?
				Long.getLong(ROLLOVER_VALUE_PROPERTY) :
					DEFAULT_ROLLOVER_VALUE;
	
	private static final Comparator<String> BUCKET_COMPARATOR = 
		new Comparator<String>(){
			@Override
			public int compare(String first, String second) {
				return new Long(Long.parseLong(second))
							.compareTo(Long.parseLong(first));
			}
		}; 			
				
	private Provider<ZooKeeper> myZooKeeperProvider;
	private LockProvider myLockProvider; 
	private ZooKeeper myZooKeeper;
	
	@Inject
	public ZkClock(Provider<ZooKeeper> zooKeeperProvider, 
					LockProvider lockProvider){
		LOG.info("Initialising ZooKeeper backed Clock instance");
		myZooKeeperProvider = zooKeeperProvider;
		myLockProvider = lockProvider;
	}
	
	@Override
	public long getNextSequence(String key) throws InterruptedException, Exception{
		if (null == myZooKeeper){
			myZooKeeper = myZooKeeperProvider.get();
		}
		String activeBucket = null;
		try{
			activeBucket = getActiveBucket(key);
		}catch(KeeperException e){
			if (e.code().equals(KeeperException.Code.NONODE)){
				if (null == myZooKeeper.exists("/" + key, false)){
					createKey(key);
				}
				Lock lock = myLockProvider.getLock("/" + key + "/lock");
				try{
					lock.lockInterruptibly();
					LOG.info(String.format("Acquired lock on key /%s/lock", key));
					activeBucket = getActiveBucket(key);
					if (null == activeBucket){
						activeBucket = newBucketNode(key);
					}
				}catch(KeeperException e1){
					//TODO : fix this
					LOG.error("ERROR", e);
				}finally{
					LOG.info(String.format("Releasing lock on key /%s/lock", key));
					lock.unlock();
				}
			}
		}
		
		return newSequenceNode(key, activeBucket); 
	}
	
	private String getActiveBucket(String key) 
	throws KeeperException, InterruptedException{
		LOG.debug(String.format("Getting active bucket for key %s", key));
		try {
			List<String> buckets = 
				myZooKeeper.getChildren("/" + key + "/buckets", false);
			if (buckets.isEmpty()){
				LOG.debug(String.format("No active bucket found for key %s", key));
				return null;
			}else{
				Collections.sort(buckets, BUCKET_COMPARATOR);
				LOG.debug(
					String.format("Bucket %s is active for key %s", 
									buckets.get(0), key));
				return buckets.get(0);
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw e;
		}catch(KeeperException e){
			LOG.info(String.format("Caught Exception when getting active Bucket for %s", key));
			throw e;
		}
	}
	
	private String createKey(String key) 
	throws KeeperException, InterruptedException{
		LOG.debug(String.format("Creating new root node for key %s", key));
		Lock lock = myLockProvider.getLock("/root-lock"); // TODO - fix this hardcoding
		LOG.info(String.format("Locking root to create key %s", key));
		try {
			lock.lockInterruptibly();
			LOG.info(String.format("Acquired lock on root, creating key %s",
									key));
			
			String keyNode = myZooKeeper.create("/" + key, 
											EMPTY_DATA, 
											DEFAULT_ACL, 
											CreateMode.PERSISTENT);
			String lockNode = myZooKeeper.create("/" + key + "/lock", 
											EMPTY_DATA, 
											DEFAULT_ACL, 
											CreateMode.PERSISTENT);
			String bucketsNode = 
							  myZooKeeper.create("/" + key + "/buckets", 
									  		EMPTY_DATA, 
									  		DEFAULT_ACL, 
									  		CreateMode.PERSISTENT);
			return keyNode;
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			LOG.error("ERROR:", e);
			throw e;
		} catch (KeeperException e) {
			if (e.code().equals(KeeperException.Code.NODEEXISTS)){
				LOG.info(
					String.format("Tried to create %s, but it already exists. " +
								"Possibly a race condition", key));
				return "/" + key;
			}else{
				throw e;
			}
		}finally{
			lock.unlock();
			LOG.info(String.format("Released root lock after creating key %s", 
									key));
		}
	}
	
	private String newBucketNode(String key) 
	throws InterruptedException, KeeperException{
		LOG.debug(String.format("Creating new Bucket for Key %s", key));
		try {
			String bucketNode = myZooKeeper.create("/" + key + "/buckets/", 
											EMPTY_DATA, 
											DEFAULT_ACL, 
											CreateMode.PERSISTENT_SEQUENTIAL);
			// now get a sequential child of the bucket - which we'll discard
			// as we don't want 0 sequences in a bucket
			String bucket = 
				bucketNode.substring(bucketNode.lastIndexOf("/") + 1);
			newSequenceNode(key, bucket);
			return bucket;
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			LOG.error("ERROR:", e);
		} catch (KeeperException e) {
			if (e.code().equals(KeeperException.Code.NONODE)){
				createKey(key);
				return newBucketNode(key);
			}else{
				throw e;
			}
		}
		throw new IllegalStateException("Should not get here");
	}
	
	private String rolloverBucket(String key, String bucket) 
	throws InterruptedException, KeeperException{
		return newBucketNode(key);
	}
	
	protected Long getRolloverValue(){
		return ROLLOVER_VALUE;
	}
	
	private Long newSequenceNode(String key, String bucket) 
	throws InterruptedException, KeeperException{
		try {
			String sequenceNode = 
				myZooKeeper.create( "/" + key + "/buckets/" + bucket + "/", 
									EMPTY_DATA, 
									DEFAULT_ACL, 
									CreateMode.PERSISTENT_SEQUENTIAL);

			Long sequence = getNumericTail(sequenceNode);
			Long rolloverValue = getRolloverValue(); 
			
			if (sequence > rolloverValue){
				LOG.info(
					String.format("Sequence %s exceeds max for bucket (%s)",
									sequence, rolloverValue));

				Lock lock = myLockProvider.getLock("/" + key + "/lock");
				try{
					lock.lockInterruptibly();
					LOG.info(
						String.format("Obtained lock for key %s, proceeding",
										key));
					String activeBucket = getActiveBucket(key); 
					if (bucket.equals(activeBucket)){
						LOG.info(
							String.format("Bucket %s is still active for key %s, " +
											"rolling over", key, bucket));
						activeBucket = rolloverBucket(key, bucket);
					}else{
						LOG.info(
							String.format("Bucket was rolled over to %s for " +
										  "key %s while obtaining lock", 
										  activeBucket, key));
					}
					return newSequenceNode(key, activeBucket);
				}finally{
					LOG.info(String.format("Releasing lock on key %s", key));
					lock.unlock();
				}
			}else{
				long multiplier = Long.parseLong(bucket);
				return (multiplier * rolloverValue) + sequence;
			}
				
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			LOG.error("ERROR:", e);
			throw e;
		} catch (KeeperException e) {
			// TODO Auto-generated catch block
			LOG.error("ERROR:", e);
			throw e;
		}
	}

	private static Long getNumericTail(String path){
		return Long.parseLong(path.substring(
				 path.lastIndexOf("/") + 1));
	}
}
