package com.talis.platform.sequencing.metrics;

public class NullSequencingMetrics implements SequencingMetrics {

	@Override
	public void recordSequenceWriteLatency(long latency) {}

	@Override
	public void incrementErrorResponses() {}

}
