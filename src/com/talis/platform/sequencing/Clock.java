package com.talis.platform.sequencing;

public interface Clock {

	// TODO fix throws clause (too general)
	public long getNextSequence(String key) throws Exception;
	
}
