package com.talis.platform.sequencing.metrics;

public interface SequencingMetricsJmxMBean {

	public int getWriteSequenceOperations();
	public long getAverageWriteSequenceLatency();
	public long getMinWriteSequenceLatency();
	public long getMaxWriteSequenceLatency();
	public int getErrorResponseCount();
}
