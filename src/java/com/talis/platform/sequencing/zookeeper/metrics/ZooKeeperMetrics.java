package com.talis.platform.sequencing.zookeeper.metrics;

public interface ZooKeeperMetrics {

	public void incrementKeyCollisions();
	public void incrementKeyCreations();
	public void incrementKeeperExceptions();
	public void incrementSessionExpiredEvents();
	public void incrementConnectionLossEvents();
	public void incrementInterruptedExceptions();
	
}
