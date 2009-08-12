package com.talis.platform.sequencing.zookeeper;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.talis.platform.sequencing.Clock;

public class ZooKeeperModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(Clock.class).to(ZkClock.class).in(Scopes.SINGLETON);
	}

}
