/*
 *    Copyright 2010 Talis Systems Ltd
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.talis.platform.sequencing.metrics;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.talis.jmx.AbstractJmxSupportTest;
import com.talis.jmx.JmxSupport;

public class SequencingMetricsJmxTest extends AbstractJmxSupportTest{

	@Override
	public JmxSupport getReporter() throws Exception{
		return new SequencingMetricsJmx();
	}
	
	@Test
	public void recordingSequenceWriteLatenciesGivesAverage() throws Exception{
		SequencingMetricsJmx reporter = (SequencingMetricsJmx)getReporter();
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
		SequencingMetricsJmx reporter = (SequencingMetricsJmx)getReporter();
		reporter.recordSequenceWriteLatency(5);
		assertEquals(5, reporter.getAverageWriteSequenceLatency());
		assertEquals(0, reporter.getAverageWriteSequenceLatency());
	}
	
	@Test
	public void averageSequenceWriteLatencyIsZeroIfNoOperationsRecorded()
	throws Exception{
		SequencingMetricsJmx reporter = (SequencingMetricsJmx)getReporter();
		assertEquals(0, reporter.getAverageWriteSequenceLatency());
	}
	
	@Test
	public void recordingSequenceWriteLatencyIncrementsOperationCount()
	throws Exception{
		SequencingMetricsJmx reporter = (SequencingMetricsJmx)getReporter();
		reporter.recordSequenceWriteLatency(5);
		reporter.recordSequenceWriteLatency(4);
		reporter.recordSequenceWriteLatency(6);
		assertEquals(3, reporter.getWriteSequenceOperations());
	}
	
	@Test
	public void recordingSequenceWriteLatencyWithNewFloorValue()
	throws Exception{
		SequencingMetricsJmx reporter = (SequencingMetricsJmx)getReporter();
		reporter.recordSequenceWriteLatency(6);
		reporter.recordSequenceWriteLatency(5);
		assertEquals(5, reporter.getMinWriteSequenceLatency());
	}
	
	@Test
	public void recordingSequenceWriteLatencyWithNewCeilingValue()
	throws Exception{
		SequencingMetricsJmx reporter = (SequencingMetricsJmx)getReporter();
		reporter.recordSequenceWriteLatency(5);
		reporter.recordSequenceWriteLatency(6);
		assertEquals(6, reporter.getMaxWriteSequenceLatency());
	}	
	
		
	@Test
	public void retrievingSequenceWriteMinLatencyResetsCounter()
	throws Exception{
		SequencingMetricsJmx reporter = (SequencingMetricsJmx)getReporter();
		reporter.recordSequenceWriteLatency(5);
		assertEquals(5, reporter.getMinWriteSequenceLatency());
		assertEquals(0, reporter.getMinWriteSequenceLatency());
	}

	@Test
	public void retrievingSequenceWriteMaxLatencyResetsCounter()
	throws Exception{
		SequencingMetricsJmx reporter = (SequencingMetricsJmx)getReporter();
		reporter.recordSequenceWriteLatency(5);
		assertEquals(5, reporter.getMaxWriteSequenceLatency());
		assertEquals(0, reporter.getMaxWriteSequenceLatency());
	}
		
	@Test
	public void retrievingSequenceWriteCountResetsCounter()
	throws Exception{
		SequencingMetricsJmx reporter = (SequencingMetricsJmx)getReporter();
		reporter.recordSequenceWriteLatency(5);
		assertEquals(1, reporter.getWriteSequenceOperations());
		assertEquals(0, reporter.getWriteSequenceOperations());
	}
	
	@Test
	public void incrementErrorResponses() throws Exception{
		SequencingMetricsJmx reporter = (SequencingMetricsJmx)getReporter();
		reporter.incrementErrorResponses();
		reporter.incrementErrorResponses();
		reporter.incrementErrorResponses();
		reporter.incrementErrorResponses();
		assertEquals(4, reporter.getErrorResponseCount());
	}
	
	@Test
	public void retrievingErrorResponsesResetsCounts()
	throws Exception{
		SequencingMetricsJmx reporter = (SequencingMetricsJmx)getReporter();
		reporter.incrementErrorResponses();
		assertEquals(1, reporter.getErrorResponseCount());
		assertEquals(0, reporter.getErrorResponseCount());
	}
	
	@Test
	public void errorResponseCountIsZeroIfNoOperationsRecorded()
	throws Exception{
		SequencingMetricsJmx reporter = (SequencingMetricsJmx)getReporter();
		assertEquals(0, reporter.getErrorResponseCount());
	}
}
