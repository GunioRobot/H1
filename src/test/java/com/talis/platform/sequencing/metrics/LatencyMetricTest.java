package com.talis.platform.sequencing.metrics;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class LatencyMetricTest {
	
	LatencyMetric metric;
	
	@Before
	public void setup() {
		metric = new LatencyMetric();
	}
	
	@Test
	public void testCounterInitialisesAtZero() {
		assertEquals(0, metric.getCount());
	}
	
	@Test
	public void testCountIncremented() {
		metric.recordLatency(1);
		assertEquals(1, metric.getCount());
	}
	
	@Test
	public void testCountIncremented_2() {
		metric.recordLatency(1);
		metric.recordLatency(1);
		metric.recordLatency(1);
		assertEquals(3, metric.getCount());
	}

	@Test
	public void testGetMinLatencyIsZeroWhenNoCalls() {
		assertEquals(0, metric.getMinLatency());
	}

	@Test
	public void testGetMaxLatencyIsZeroWhenNoCalls() {
		assertEquals(0, metric.getMaxLatency());
	}

	@Test
	public void testGetAverageLatencyIsZeroWhenNoCalls() {
		assertEquals(0, metric.getAverageLatency());
	}

	@Test
	public void testGetMinLatencyForSingleDataPoint() {
		metric.recordLatency(11);
		assertEquals(11, metric.getMinLatency());
	}

	@Test
	public void testGetMaxLatencyForSingleDataPoint() {
		metric.recordLatency(11);
		assertEquals(11, metric.getMaxLatency());
	}

	@Test
	public void testGetAverageLatencyForSingleDataPoint() {
		metric.recordLatency(11);
		assertEquals(11, metric.getAverageLatency());
	}

	@Test
	public void testGetMinLatency() {
		metric.recordLatency(1);
		metric.recordLatency(2);
		metric.recordLatency(3);
		metric.recordLatency(4);
		metric.recordLatency(5);
		assertEquals(1, metric.getMinLatency());
	}

	@Test
	public void testGetMaxLatency() {
		metric.recordLatency(1);
		metric.recordLatency(2);
		metric.recordLatency(3);
		metric.recordLatency(4);
		metric.recordLatency(5);
		assertEquals(5, metric.getMaxLatency());
	}

	@Test
	public void testGetAverageLatency() {
		metric.recordLatency(1);
		metric.recordLatency(2);
		metric.recordLatency(3);
		metric.recordLatency(4);
		metric.recordLatency(5);
		assertEquals(3, metric.getAverageLatency());
		
	}

}
