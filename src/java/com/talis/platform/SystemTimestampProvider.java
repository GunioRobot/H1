package com.talis.platform;


public class SystemTimestampProvider implements TimestampProvider{

	@Override
	public long getCurrentTimeInMillis() {
		return System.currentTimeMillis();
	}
}
