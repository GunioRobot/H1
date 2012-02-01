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

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;

import com.talis.jmx.JmxSupport;

public class ZooKeeperMetricsJmx extends JmxSupport
implements ZooKeeperMetrics, ZooKeeperMetricsJmxMBean {


	public ZooKeeperMetricsJmx() throws MalformedObjectNameException,
			InstanceAlreadyExistsException, MBeanRegistrationException,
			NotCompliantMBeanException, NullPointerException, IOException {
		super();
	}

	@Override
	public String getBeanName() {
		return "com.talis:name=ZkSequencingMetrics";
	};

	private final AtomicInteger keyCollisions = new AtomicInteger(0);
	@Override
	public void incrementKeyCollisions() {
		keyCollisions.incrementAndGet();
	}

	@Override
	public int getKeyCollisions() {
		int valueToReturn = keyCollisions.get();
		keyCollisions.set(0);
		return valueToReturn;
	}

	private final AtomicInteger keyCreations = new AtomicInteger(0);
	@Override
	public void incrementKeyCreations() {
		keyCreations.incrementAndGet();
	}

	@Override
	public int getKeyCreations() {
		int valueToReturn = keyCreations.get();
		keyCreations.set(0);
		return valueToReturn;
	}


	private final AtomicInteger connectionLossEvents = new AtomicInteger(0);
	@Override
	public void incrementConnectionLossEvents() {
		connectionLossEvents.incrementAndGet();
		keeperExceptions.incrementAndGet();
	}

	@Override
	public int getConnectionLossEvents() {
		int valueToReturn = connectionLossEvents.get();
		connectionLossEvents.set(0);
		return valueToReturn;
	}

	private final AtomicInteger sessionExpiredEvents = new AtomicInteger(0);
	@Override
	public void incrementSessionExpiredEvents() {
		sessionExpiredEvents.incrementAndGet();
		keeperExceptions.incrementAndGet();
	}

	@Override
	public int getSessionExpiredEvents() {
		int valueToReturn = sessionExpiredEvents.get();
		sessionExpiredEvents.set(0);
		return valueToReturn;
	}

	private final AtomicInteger interruptedExceptions = new AtomicInteger(0);
	@Override
	public void incrementInterruptedExceptions() {
		interruptedExceptions.incrementAndGet();
	}

	@Override
	public int getInterruptedExceptions() {
		int valueToReturn = interruptedExceptions.get();
		interruptedExceptions.set(0);
		return valueToReturn;
	}

	private final AtomicInteger keeperExceptions = new AtomicInteger(0);
	@Override
	public void incrementKeeperExceptions() {
		keeperExceptions.incrementAndGet();
	}

	@Override
	public int getKeeperExceptions() {
		int valueToReturn = keeperExceptions.get();
		keeperExceptions.set(0);
		return valueToReturn;
	}

}
