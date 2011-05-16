package com.talis.platform.sequencing.redis;

import redis.clients.jedis.JedisPool;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.talis.platform.sequencing.Clock;
import com.talis.platform.sequencing.metrics.NullSequencingMetrics;
import com.talis.platform.sequencing.metrics.SequencingMetrics;

public class RedisModule extends AbstractModule{

	@Override
	protected void configure() {
		bind(JedisPool.class).toProvider(JedisPoolProvider.class).in(Scopes.SINGLETON);
		bind(Clock.class).to(RedisClock.class);
		bind(SequencingMetrics.class).to(NullSequencingMetrics.class);
	}
	
}
