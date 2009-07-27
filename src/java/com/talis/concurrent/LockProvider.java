package com.talis.concurrent;

import java.util.concurrent.locks.Lock;

public interface LockProvider {

	public Lock getLock(String key);
	
}
