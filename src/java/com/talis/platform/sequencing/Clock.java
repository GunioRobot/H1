package com.talis.platform.sequencing;

public interface Clock {

	public long getNextSequence(String key) throws SequencingException;

}
