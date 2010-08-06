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
	
	@Test
	public void incrementConnectionLossEvents() throws Exception{
		ZooKeeperMetricsJmx reporter = getReporter();
		reporter.incrementConnectionLossEvents();
		reporter.incrementConnectionLossEvents();
		reporter.incrementConnectionLossEvents();
		reporter.incrementConnectionLossEvents();
		assertEquals(4, reporter.getConnectionLossEvents());
	}
	
	@Test
	public void connectionLossEventsAreASubsetOfKeeperExceptions()
	throws Exception{
		ZooKeeperMetricsJmx reporter = getReporter();
		assertEquals(0, reporter.getKeeperExceptions());
		reporter.incrementConnectionLossEvents();
		assertEquals(1, reporter.getKeeperExceptions());
	}
	
	@Test
	public void retrievingConnectionLostEventsResetsCounts()
	throws Exception{
		ZooKeeperMetricsJmx reporter = getReporter();
		reporter.incrementConnectionLossEvents();
		assertEquals(1, reporter.getConnectionLossEvents());
		assertEquals(0, reporter.getConnectionLossEvents());
	}
	
	@Test
	public void connectionLossEventsCountIsZeroIfNoOperationsRecorded()
	throws Exception{
		ZooKeeperMetricsJmx reporter = getReporter();
		assertEquals(0, reporter.getConnectionLossEvents());
	}

	@Test
	public void incrementSessionExpiredEvents() throws Exception{
		ZooKeeperMetricsJmx reporter = getReporter();
		reporter.incrementSessionExpiredEvents();
		reporter.incrementSessionExpiredEvents();
		reporter.incrementSessionExpiredEvents();
		reporter.incrementSessionExpiredEvents();
		assertEquals(4, reporter.getSessionExpiredEvents());
	}
	
	@Test
	public void sessionExpiredEventsAreASubsetOfKeeperExceptions()
	throws Exception{
		ZooKeeperMetricsJmx reporter = getReporter();
		assertEquals(0, reporter.getKeeperExceptions());
		reporter.incrementSessionExpiredEvents();
		assertEquals(1, reporter.getKeeperExceptions());
	}
	
	@Test
	public void retrievingSessionExpiredEventsResetsCounts()
	throws Exception{
		ZooKeeperMetricsJmx reporter = getReporter();
		reporter.incrementSessionExpiredEvents();
		assertEquals(1, reporter.getSessionExpiredEvents());
		assertEquals(0, reporter.getSessionExpiredEvents());
	}
	
	@Test
	public void sessionExpiredEventCountIsZeroIfNoOperationsRecorded()
	throws Exception{
		ZooKeeperMetricsJmx reporter = getReporter();
		assertEquals(0, reporter.getSessionExpiredEvents());
	}
	
		@Test
	public void incrementInterruptedExceptions() throws Exception{
		ZooKeeperMetricsJmx reporter = getReporter();
		reporter.incrementInterruptedExceptions();
		reporter.incrementInterruptedExceptions();
		reporter.incrementInterruptedExceptions();
		reporter.incrementInterruptedExceptions();
		assertEquals(4, reporter.getInterruptedExceptions());
	}
	
	@Test
	public void retrievingInterruptedExceptionsResetsCounts()
	throws Exception{
		ZooKeeperMetricsJmx reporter = getReporter();
		reporter.incrementInterruptedExceptions();
		assertEquals(1, reporter.getInterruptedExceptions());
		assertEquals(0, reporter.getInterruptedExceptions());
	}
	
	@Test
	public void interruptedExceptionCountIsZeroIfNoOperationsRecorded()
	throws Exception{
		ZooKeeperMetricsJmx reporter = getReporter();
		assertEquals(0, reporter.getInterruptedExceptions());
	}
	
		@Test
	public void incrementKeeperExceptions() throws Exception{
		ZooKeeperMetricsJmx reporter = getReporter();
		reporter.incrementKeeperExceptions();
		reporter.incrementKeeperExceptions();
		reporter.incrementKeeperExceptions();
		reporter.incrementKeeperExceptions();
		assertEquals(4, reporter.getKeeperExceptions());
	}
	
	@Test
	public void retrievingKeeperExceptionsResetsCounts()
	throws Exception{
		ZooKeeperMetricsJmx reporter = getReporter();
		reporter.incrementKeeperExceptions();
		assertEquals(1, reporter.getKeeperExceptions());
		assertEquals(0, reporter.getKeeperExceptions());
	}
	
	@Test
	public void keeperExceptionCountIsZeroIfNoOperationsRecorded()
	throws Exception{
		ZooKeeperMetricsJmx reporter = getReporter();
		assertEquals(0, reporter.getKeeperExceptions());
	}
	
	@Test
	public void incrementKeyCreations() throws Exception{
		ZooKeeperMetricsJmx reporter = getReporter();
		reporter.incrementKeyCreations();
		reporter.incrementKeyCreations();
		reporter.incrementKeyCreations();
		reporter.incrementKeyCreations();
		assertEquals(4, reporter.getKeyCreations());
	}
	
	@Test
	public void retrievingKeyCreationsResetsCounts()
	throws Exception{
		ZooKeeperMetricsJmx reporter = getReporter();
		reporter.incrementKeyCreations();
		assertEquals(1, reporter.getKeyCreations());
		assertEquals(0, reporter.getKeyCreations());
	}
	
	@Test
	public void keyCreationCountIsZeroIfNoOperationsRecorded()
	throws Exception{
		ZooKeeperMetricsJmx reporter = getReporter();
		assertEquals(0, reporter.getKeyCreations());
	}
	
}
