package com.talis.platform.sequencing.redis;

import org.apache.commons.pool.impl.GenericObjectPool.Config;
import org.junit.Before;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.talis.platform.sequencing.Clock;
import com.talis.platform.sequencing.LoadTest;

public class RedisClockLoadTest extends LoadTest{

	@Before
	public void setup() throws Exception{
		Jedis client = new Jedis("localhost");
		client.set(randomKey.getBytes(), "-1".getBytes());
		client.set("/first-key".getBytes(), "-1".getBytes());
		client.set("/second-key".getBytes(), "-1".getBytes());
		client.set("/third-key".getBytes(), "-1".getBytes());
		client.set("/fourth-key".getBytes(), "-1".getBytes());
		
	}
	
	@Override
	public Clock getClock() throws Exception {
		Config poolConfig = new Config();
		poolConfig.maxActive = 100;
		JedisPool pool = new JedisPool(poolConfig, "127.0.0.1");
		return new RedisClock(pool);
	}
	
}
