package com.talis.platform.sequencing.metrics;

import java.io.IOException;

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

	private int sequencesGenerated = 0;
	
	@Override
	public String getBeanName() {
		return "com.talis:name=SequencingMetrics";
	}

	@Override
	public void incrementSequencesGenerated() {
		sequencesGenerated++;
	}

	@Override
	public int getSequencesGenerated() {
		int valueToReturn = sequencesGenerated;
		sequencesGenerated = 0;
		return valueToReturn;
	}

}
