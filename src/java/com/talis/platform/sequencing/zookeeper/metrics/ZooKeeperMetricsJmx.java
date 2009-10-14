package com.talis.platform.sequencing.zookeeper.metrics;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;

import com.talis.platform.sequencing.metrics.SequencingMetricsJmx;

public class ZooKeeperMetricsJmx extends SequencingMetricsJmx 
implements ZooKeeperMetrics, ZooKeeperMetricsJmxMBean {

	private final AtomicInteger keyCollisions = new AtomicInteger(0);
	
	public ZooKeeperMetricsJmx() throws MalformedObjectNameException,
			InstanceAlreadyExistsException, MBeanRegistrationException,
			NotCompliantMBeanException, NullPointerException, IOException {
		super();
	}

	@Override
	public String getBeanName() {
		return "com.talis:name=ZkSequencingMetrics";
	};
	
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

	@Override
	public void incrementConnectionLossEvents() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void incrementExpiredSessionEvents() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void incrementInterruptedExceptions() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void incrementKeeperExceptions() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void incrementKeyCreations() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getConnectionLossEvents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getInterruptedExceptions() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getKeeperExceptions() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getKeyCreations() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getSessionExpiredEvents() {
		// TODO Auto-generated method stub
		return 0;
	}
	
}
