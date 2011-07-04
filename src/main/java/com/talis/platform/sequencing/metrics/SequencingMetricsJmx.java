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

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;

import com.talis.jmx.JmxSupport;

public class SequencingMetricsJmx extends JmxSupport 
implements SequencingMetrics, SequencingMetricsJmxMBean {
	
	private final LatencyMetric writeLatencyMetrics = new LatencyMetric();
	private final LatencyMetric readLatencyMetrics = new LatencyMetric();

	private final AtomicInteger errorResponses = new AtomicInteger(0);
	private final AtomicInteger readErrorResponses = new AtomicInteger(0);
	
	public SequencingMetricsJmx() throws MalformedObjectNameException,
			InstanceAlreadyExistsException, MBeanRegistrationException,
			NotCompliantMBeanException, NullPointerException, IOException {
		super();
	}
	
	@Override
	public String getBeanName() {
		return "com.talis:name=SequencingMetrics";
	}
	
	// Write latency metrics
	
	@Override
	public int getWriteSequenceOperations() {
		return writeLatencyMetrics.getCount();
	}

	@Override
	public long getMinWriteSequenceLatency() {
		return writeLatencyMetrics.getMinLatency();
	}
	
	@Override
	public long getMaxWriteSequenceLatency() {
		return writeLatencyMetrics.getMaxLatency();
	}

	@Override
	public long getAverageWriteSequenceLatency() {
		return writeLatencyMetrics.getAverageLatency();
	}

	@Override
	public void recordSequenceWriteLatency(long latency) {
		writeLatencyMetrics.recordLatency(latency);
	}
	
	// Read latency metrix
	
	@Override
	public int getReadSequenceOperations() {
		return readLatencyMetrics.getCount();
	}

	@Override
	public long getMinReadSequenceLatency() {
		return readLatencyMetrics.getMinLatency();
	}

	@Override
	public long getMaxReadSequenceLatency() {
		return readLatencyMetrics.getMaxLatency();
	}
	
	@Override
	public long getAverageReadSequenceLatency() {
		return readLatencyMetrics.getAverageLatency();
	}

	@Override
	public void recordSequenceReadLatency(long latency) {
		readLatencyMetrics.recordLatency(latency);
	}
	
	// Error metrics

	@Override
	public void incrementErrorResponses() {
		errorResponses.incrementAndGet();
	}

	@Override
	public void incrementReadErrorResponses() {
		readErrorResponses.incrementAndGet();
	}
	
	@Override
	public int getErrorResponseCount() {
		int valueToReturn = errorResponses.get();
		errorResponses.set(0);
		return valueToReturn;
	}

	@Override
	public int getReadErrorResponseCount() {
		int valueToReturn = readErrorResponses.get();
		readErrorResponses.set(0);
		return valueToReturn;
	}

}
