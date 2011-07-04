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
	public void recordingSequenceReadLatenciesGivesAverage() throws Exception{
		SequencingMetricsJmx reporter = (SequencingMetricsJmx)getReporter();
		reporter.recordSequenceReadLatency(5);
		reporter.recordSequenceReadLatency(5);
		reporter.recordSequenceReadLatency(10);
		reporter.recordSequenceReadLatency(10);
		reporter.recordSequenceReadLatency(20);
		assertEquals(10, reporter.getAverageReadSequenceLatency());
	}
	
	@Test
	public void retrievingAverageSequenceReadLatencyResetsCounts()
	throws Exception{
		SequencingMetricsJmx reporter = (SequencingMetricsJmx)getReporter();
		reporter.recordSequenceReadLatency(5);
		assertEquals(5, reporter.getAverageReadSequenceLatency());
		assertEquals(0, reporter.getAverageReadSequenceLatency());
	}
	
	@Test
	public void averageSequenceReadLatencyIsZeroIfNoOperationsRecorded()
	throws Exception{
		SequencingMetricsJmx reporter = (SequencingMetricsJmx)getReporter();
		assertEquals(0, reporter.getAverageReadSequenceLatency());
	}
	
	@Test
	public void recordingSequenceReadLatencyIncrementsOperationCount()
	throws Exception{
		SequencingMetricsJmx reporter = (SequencingMetricsJmx)getReporter();
		reporter.recordSequenceReadLatency(5);
		reporter.recordSequenceReadLatency(4);
		reporter.recordSequenceReadLatency(6);
		assertEquals(3, reporter.getReadSequenceOperations());
	}
	
	@Test
	public void recordingSequenceReadLatencyWithNewFloorValue()
	throws Exception{
		SequencingMetricsJmx reporter = (SequencingMetricsJmx)getReporter();
		reporter.recordSequenceReadLatency(6);
		reporter.recordSequenceReadLatency(5);
		assertEquals(5, reporter.getMinReadSequenceLatency());
	}
	
	@Test
	public void recordingSequenceReadLatencyWithNewCeilingValue()
	throws Exception{
		SequencingMetricsJmx reporter = (SequencingMetricsJmx)getReporter();
		reporter.recordSequenceReadLatency(5);
		reporter.recordSequenceReadLatency(6);
		assertEquals(6, reporter.getMaxReadSequenceLatency());
	}	
	
		
	@Test
	public void retrievingSequenceReadMinLatencyResetsCounter()
	throws Exception{
		SequencingMetricsJmx reporter = (SequencingMetricsJmx)getReporter();
		reporter.recordSequenceReadLatency(5);
		assertEquals(5, reporter.getMinReadSequenceLatency());
		assertEquals(0, reporter.getMinReadSequenceLatency());
	}

	@Test
	public void retrievingSequenceReadMaxLatencyResetsCounter()
	throws Exception{
		SequencingMetricsJmx reporter = (SequencingMetricsJmx)getReporter();
		reporter.recordSequenceReadLatency(5);
		assertEquals(5, reporter.getMaxReadSequenceLatency());
		assertEquals(0, reporter.getMaxReadSequenceLatency());
	}
		
	@Test
	public void retrievingSequenceReadCountResetsCounter()
	throws Exception{
		SequencingMetricsJmx reporter = (SequencingMetricsJmx)getReporter();
		reporter.recordSequenceReadLatency(5);
		assertEquals(1, reporter.getReadSequenceOperations());
		assertEquals(0, reporter.getReadSequenceOperations());
	}
	
	@Test
	public void incrementReadErrorResponses() throws Exception{
		SequencingMetricsJmx reporter = (SequencingMetricsJmx)getReporter();
		reporter.incrementReadErrorResponses();
		reporter.incrementReadErrorResponses();
		reporter.incrementReadErrorResponses();
		reporter.incrementReadErrorResponses();
		assertEquals(4, reporter.getReadErrorResponseCount());
	}
	
	@Test
	public void retrievingReadErrorResponsesResetsCounts()
	throws Exception{
		SequencingMetricsJmx reporter = (SequencingMetricsJmx)getReporter();
		reporter.incrementReadErrorResponses();
		assertEquals(1, reporter.getReadErrorResponseCount());
		assertEquals(0, reporter.getReadErrorResponseCount());
	}
	
	@Test
	public void errorResponseCountersAreZeroIfNoOperationsRecorded()
	throws Exception{
		SequencingMetricsJmx reporter = (SequencingMetricsJmx)getReporter();
		assertEquals(0, reporter.getErrorResponseCount());
		assertEquals(0, reporter.getReadErrorResponseCount());
	}
}
