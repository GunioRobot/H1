package com.talis.platform.sequencing.zookeeper.metrics;

public interface ZooKeeperMetrics {

	public void incrementKeyCollisions();
	public void incrementKeeperExceptions();
	public void incrementExpiredSessionEvents();
	public void incrementConnectionLossEvents();
	public void incrementInterruptedExceptions();
	public void incrementKeyCreations();
}
