package com.talis.platform.sequencing.metrics;

public interface SequencingMetricsJmxMBean {

	public int getWriteSequenceOperations();
	public long getAverageWriteSequenceLatency();
}
