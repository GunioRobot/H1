package com.talis.concurrent.zookeeper;

public class AlreadyLockedException extends IllegalStateException {

	public AlreadyLockedException(String s) {
		super(s);
	}
}
