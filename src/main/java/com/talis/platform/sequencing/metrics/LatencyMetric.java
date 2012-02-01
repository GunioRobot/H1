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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class LatencyMetric {

	private final AtomicInteger count = new AtomicInteger(0);
	private final AtomicLong totalLatency = new AtomicLong(0);
	private final AtomicLong minLatency = new AtomicLong(0);
	private final AtomicLong maxLatency = new AtomicLong(0);
	private final AtomicInteger sampleLatency = new AtomicInteger(0);

	public int getCount() {
		int valueToReturn = count.get();
		count.set(0);
		return valueToReturn;
	}

	public long getAverageLatency() {
		long averageWriteLatency = 0;
		if (sampleLatency.get() > 0){
			averageWriteLatency =
				totalLatency.get() / sampleLatency.get();
			sampleLatency.set(0);
			totalLatency.set(0);
		}
		return averageWriteLatency;
	}

	public long getMaxLatency() {
		long valueToReturn = maxLatency.get();
		maxLatency.set(0);
		return valueToReturn;
	}

	public long getMinLatency() {
		long valueToReturn = minLatency.get();
		minLatency.set(0);
		return valueToReturn;
	}

	public void recordLatency(long latency) {
		count.incrementAndGet();
		sampleLatency.incrementAndGet();
		totalLatency.addAndGet(latency);
		if (latency < minLatency.get()
			|| minLatency.get() == 0 ){
			minLatency.set(latency);
		}
		if (latency > maxLatency.get()){
			maxLatency.set(latency);
		}
	}
}
