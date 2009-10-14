package com.talis.platform.sequencing.zookeeper.metrics;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.talis.platform.sequencing.metrics.SequencingMetricsJmxTest;

public class ZooKeeperMetricsJmxTest extends SequencingMetricsJmxTest {

	@Override
	public ZooKeeperMetricsJmx getReporter() throws Exception{
		return new ZooKeeperMetricsJmx();
	}
	
	@Test
	public void incrementKeyCollisions() throws Exception{
		ZooKeeperMetricsJmx reporter = getReporter();
		reporter.incrementKeyCollisions();
		reporter.incrementKeyCollisions();
		reporter.incrementKeyCollisions();
		assertEquals(3, reporter.getKeyCollisions());
	}
	
	@Test
	public void retrievingKeyCollisionsResetsCounts()
	throws Exception{
		ZooKeeperMetricsJmx reporter = getReporter();
		reporter.incrementKeyCollisions();
		assertEquals(1, reporter.getKeyCollisions());
		assertEquals(0, reporter.getKeyCollisions());
	}
	
	@Test
	public void keyCollisionsCountIsZeroIfNoOperationsRecorded()
	throws Exception{
		ZooKeeperMetricsJmx reporter = getReporter();
		assertEquals(0, reporter.getKeyCollisions());
	}
	
}
