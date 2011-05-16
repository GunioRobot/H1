package com.talis.platform.sequencing.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.google.inject.Inject;
import com.talis.platform.sequencing.Clock;
import com.talis.platform.sequencing.SequencingException;

public class RedisClock implements Clock {

	static final Logger LOG = LoggerFactory.getLogger(RedisClock.class);
			
	private final JedisPool connectionPool;
	
	@Inject 
	public RedisClock(JedisPool connectionPool){
		this.connectionPool = connectionPool;
	}
		
	@Override
	public long getNextSequence(String key) throws SequencingException {
		Jedis client = connectionPool.getResource();
		try{
			return (long)client.incr(key);
		}catch(Exception e){
			throw new SequencingException("FAIL", e);
		}finally{
			connectionPool.returnResource(client);
		}
	}
	
}
