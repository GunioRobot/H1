package com.talis.platform.sequencing.metrics;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.talis.platform.metrics.JmxMetricsReporterBase;

public class SequencingMetricsJmx extends JmxMetricsReporterBase 
implements SequencingMetrics, SequencingMetricsJmxMBean {

	private static final Log LOG = LogFactory.getLog(SequencingMetricsJmx.class);
	
	public SequencingMetricsJmx() throws MalformedObjectNameException,
			InstanceAlreadyExistsException, MBeanRegistrationException,
			NotCompliantMBeanException, NullPointerException, IOException {
		super();
	}
	
	@Override
	public String getBeanName() {
		return "com.talis:name=SequencingMetrics";
	}

	private AtomicInteger writeSequenceOperations = new AtomicInteger(0);
	public void incrementWriteSequenceOperations() {
		writeSequenceOperations.incrementAndGet();
	}

	@Override
	public int getWriteSequenceOperations() {
		int valueToReturn = writeSequenceOperations.get();
		writeSequenceOperations.set(0);
		return valueToReturn;
	}

	private AtomicLong writeSequenceLatencyTotal = new AtomicLong(0);
	private AtomicInteger writeSequenceLatencySample = new AtomicInteger(0);
	
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
	public void recordSequenceWriteLatency(long latency) {
		incrementWriteSequenceOperations();
		writeSequenceLatencySample.incrementAndGet();
		writeSequenceLatencyTotal.addAndGet(latency);
	}

}
