package com.talis.platform.sequencing.zookeeper;

import org.apache.zookeeper.ZooKeeper;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.talis.platform.sequencing.Clock;
import com.talis.platform.sequencing.metrics.SequencingMetrics;
import com.talis.platform.sequencing.zookeeper.metrics.ZooKeeperMetrics;
import com.talis.platform.sequencing.zookeeper.metrics.ZooKeeperMetricsJmx;

public class ZooKeeperModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(ZooKeeper.class)
			.toProvider(ZooKeeperProvider.class)
			.in(Scopes.SINGLETON);
		bind(Clock.class).to(ZkClock.class);
		bind(ZooKeeperMetricsJmx.class).in(Scopes.SINGLETON);
		bind(ZooKeeperMetrics.class).to(ZooKeeperMetricsJmx.class);
		bind(SequencingMetrics.class).to(ZooKeeperMetricsJmx.class);
	}

}
	