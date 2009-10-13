package com.talis.platform.sequencing.metrics;

public interface SequencingMetrics {

	public void recordSequenceWriteLatency(long latency);
}
