package com.talis.platform.sequencing.redis;

import org.apache.commons.pool.impl.GenericObjectPool.Config;

import redis.clients.jedis.JedisPool;

import com.google.inject.Provider;

public class JedisPoolProvider implements Provider<JedisPool> {

	private final JedisPool pool;
	
	public JedisPoolProvider(){
		Config poolConfig = new Config();
		poolConfig.maxActive = 100;
		this.pool = new JedisPool(poolConfig, "127.0.0.1");
	}
	
	@Override
	public JedisPool get() {
		return pool;
	}

}
