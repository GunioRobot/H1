package com.talis.platform.sequencing.metrics;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.talis.platform.metrics.JmxMetricsReporterBaseTest;

public class SequencingMetricsJmxTest extends JmxMetricsReporterBaseTest {

	@Override
	public SequencingMetricsJmx getReporter() throws Exception{
		return new SequencingMetricsJmx();
	}
	
	@Test
	public void recordingSequenceWriteLatenciesGivesAverage() throws Exception{
		SequencingMetricsJmx reporter = getReporter();
		reporter.recordSequenceWriteLatency(5);
		reporter.recordSequenceWriteLatency(5);
		reporter.recordSequenceWriteLatency(10);
		reporter.recordSequenceWriteLatency(10);
		reporter.recordSequenceWriteLatency(20);
		assertEquals(10, reporter.getAverageWriteSequenceLatency());
	}
	
	@Test
	public void retrievingAverageSequenceWriteLatencyResetsCounts()
	throws Exception{
		SequencingMetricsJmx reporter = getReporter();
		reporter.recordSequenceWriteLatency(5);
		assertEquals(5, reporter.getAverageWriteSequenceLatency());
		assertEquals(0, reporter.getAverageWriteSequenceLatency());
	}
	
	@Test
	public void averageSequenceWriteLatencyIsZeroIfNoOperationsRecorded()
	throws Exception{
		SequencingMetricsJmx reporter = getReporter();
		assertEquals(0, reporter.getAverageWriteSequenceLatency());
	}
	
	@Test
	public void recordingSequenceWriteLatencyIncrementsOperationCount()
	throws Exception{
		SequencingMetricsJmx reporter = getReporter();
		reporter.recordSequenceWriteLatency(5);
		reporter.recordSequenceWriteLatency(4);
		reporter.recordSequenceWriteLatency(6);
		assertEquals(3, reporter.getWriteSequenceOperations());
	}
	
	@Test
	public void recordingSequenceWriteLatencyWithNewFloorValue()
	throws Exception{
		SequencingMetricsJmx reporter = getReporter();
		reporter.recordSequenceWriteLatency(6);
		reporter.recordSequenceWriteLatency(5);
		assertEquals(5, reporter.getMinWriteSequenceLatency());
	}
	
	@Test
	public void recordingSequenceWriteLatencyWithNewCeilingValue()
	throws Exception{
		SequencingMetricsJmx reporter = getReporter();
		reporter.recordSequenceWriteLatency(5);
		reporter.recordSequenceWriteLatency(6);
		assertEquals(6, reporter.getMaxWriteSequenceLatency());
	}	
	
		
	@Test
	public void retrievingSequenceWriteMinLatencyResetsCounter()
	throws Exception{
		SequencingMetricsJmx reporter = getReporter();
		reporter.recordSequenceWriteLatency(5);
		assertEquals(5, reporter.getMinWriteSequenceLatency());
		assertEquals(Long.MAX_VALUE, reporter.getMinWriteSequenceLatency());
	}

	@Test
	public void retrievingSequenceWriteMaxLatencyResetsCounter()
	throws Exception{
		SequencingMetricsJmx reporter = getReporter();
		reporter.recordSequenceWriteLatency(5);
		assertEquals(5, reporter.getMaxWriteSequenceLatency());
		assertEquals(0, reporter.getMaxWriteSequenceLatency());
	}
		
	@Test
	public void retrievingSequenceWriteCountResetsCounter()
	throws Exception{
		SequencingMetricsJmx reporter = getReporter();
		reporter.recordSequenceWriteLatency(5);
		assertEquals(1, reporter.getWriteSequenceOperations());
		assertEquals(0, reporter.getWriteSequenceOperations());
	}
}
