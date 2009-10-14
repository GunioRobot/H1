package com.talis.platform.sequencing.zookeeper.metrics;

import com.talis.platform.sequencing.metrics.SequencingMetricsJmxMBean;

public interface ZooKeeperMetricsJmxMBean extends SequencingMetricsJmxMBean {

	public int getKeyCollisions();
	public int getKeyCreations();
	public int getConnectionLossEvents();
	public int getSessionExpiredEvents();
	public int getInterruptedExceptions();
	public int getKeeperExceptions();
}
