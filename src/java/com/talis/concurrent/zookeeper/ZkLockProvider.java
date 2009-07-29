package com.talis.concurrent.zookeeper;

import java.util.concurrent.locks.Lock;

import org.apache.zookeeper.ZooKeeper;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.talis.concurrent.LockProvider;

public class ZkLockProvider implements LockProvider {

	private final Provider<ZooKeeper> myKeeperProvider;
	
	@Inject
	public ZkLockProvider(Provider<ZooKeeper> keeperProvider){
		myKeeperProvider = keeperProvider;
	}
	
	@Override
	public Lock getLock(String key) {
		return new ZkLock(myKeeperProvider.get(), key);
	}

}
