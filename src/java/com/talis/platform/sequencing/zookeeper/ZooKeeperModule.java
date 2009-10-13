package com.talis.platform.sequencing.zookeeper;

import org.apache.zookeeper.ZooKeeper;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.talis.platform.sequencing.Clock;

public class ZooKeeperModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(ZooKeeper.class)
			.toProvider(ZooKeeperProvider.class)
			.in(Scopes.SINGLETON);
		bind(Clock.class).to(ZkClock.class);
	}

}
	