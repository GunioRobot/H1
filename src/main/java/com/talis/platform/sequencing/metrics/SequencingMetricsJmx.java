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

	private final AtomicInteger writeSequenceOperations = new AtomicInteger(0);
	private final AtomicLong writeSequenceLatencyTotal = new AtomicLong(0);
	private final AtomicLong writeSequenceLatencyMin = new AtomicLong(0);
	private final AtomicLong writeSequenceLatencyMax = new AtomicLong(0);
	private final AtomicInteger writeSequenceLatencySample = new AtomicInteger(0);
	
	private final AtomicInteger readSequenceOperations = new AtomicInteger(0);
	private final AtomicLong readSequenceLatencyTotal = new AtomicLong(0);
	private final AtomicLong readSequenceLatencyMin = new AtomicLong(0);
	private final AtomicLong readSequenceLatencyMax = new AtomicLong(0);
	private final AtomicInteger readSequenceLatencySample = new AtomicInteger(0);

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
	
	@Override
	public int getWriteSequenceOperations() {
		int valueToReturn = writeSequenceOperations.get();
		writeSequenceOperations.set(0);
		return valueToReturn;
	}

	@Override
	public int getReadSequenceOperations() {
		int valueToReturn = readSequenceOperations.get();
		readSequenceOperations.set(0);
		return valueToReturn;
	}

	@Override
	public long getAverageWriteSequenceLatency() {
		long averageWriteLatency = 0;
		if (writeSequenceLatencySample.get() > 0){
			averageWriteLatency = 
				writeSequenceLatencyTotal.get() / writeSequenceLatencySample.get();
			writeSequenceLatencySample.set(0);
			writeSequenceLatencyTotal.set(0);
		}
		return averageWriteLatency;
	}
	
	@Override
	public long getAverageReadSequenceLatency() {
		long averageReadLatency = 0;
		if (readSequenceLatencySample.get() > 0){
			averageReadLatency = 
				readSequenceLatencyTotal.get() / readSequenceLatencySample.get();
			readSequenceLatencySample.set(0);
			readSequenceLatencyTotal.set(0);
		}
		return averageReadLatency;
	}

	@Override
	public void recordSequenceWriteLatency(long latency) {
		writeSequenceOperations.incrementAndGet();
		writeSequenceLatencySample.incrementAndGet();
		writeSequenceLatencyTotal.addAndGet(latency);
		if (latency < writeSequenceLatencyMin.get()  
			|| writeSequenceLatencyMin.get() == 0 ){
			writeSequenceLatencyMin.set(latency);
		}
		if (latency > writeSequenceLatencyMax.get()){
			writeSequenceLatencyMax.set(latency);
		}
	}

	@Override
	public void recordSequenceReadLatency(long latency) {
		readSequenceOperations.incrementAndGet();
		readSequenceLatencySample.incrementAndGet();
		readSequenceLatencyTotal.addAndGet(latency);
		if (latency < readSequenceLatencyMin.get()  
			|| readSequenceLatencyMin.get() == 0 ){
			readSequenceLatencyMin.set(latency);
		}
		if (latency > readSequenceLatencyMax.get()){
			readSequenceLatencyMax.set(latency);
		}
		
	}
	
	@Override
	public long getMaxWriteSequenceLatency() {
		long valueToReturn = writeSequenceLatencyMax.get();
		writeSequenceLatencyMax.set(0);
		return valueToReturn;
	}

	@Override
	public long getMinWriteSequenceLatency() {
		long valueToReturn = writeSequenceLatencyMin.get();
		writeSequenceLatencyMin.set(0);
		return valueToReturn;
	}

	@Override
	public void incrementErrorResponses() {
		errorResponses.incrementAndGet();
	}

	@Override
	public void incrementReadErrorResponses() {
		readErrorResponses.incrementAndGet();
	}

	@Override
	public long getMinReadSequenceLatency() {
		long valueToReturn = readSequenceLatencyMin.get();
		readSequenceLatencyMin.set(0);
		return valueToReturn;
	}

	@Override
	public long getMaxReadSequenceLatency() {
		long valueToReturn = readSequenceLatencyMax.get();
		readSequenceLatencyMax.set(0);
		return valueToReturn;
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
