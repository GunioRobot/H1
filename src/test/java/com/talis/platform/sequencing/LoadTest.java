package com.talis.platform.sequencing;

import static org.junit.Assert.assertEquals;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

public abstract class LoadTest {

	public static final Logger LOG = LoggerFactory.getLogger(LoadTest.class);
	
	public abstract Clock getClock() throws Exception;
	
	public String randomKey = "/" + UUID.randomUUID().toString().substring(0,8);
	
	@Test 
	public void testManyIterations() throws Exception{
		Clock clock = getClock();
		int iterations = 1000000;
//		int iterations = 1;
		
		long seq = 0;
		long start = System.currentTimeMillis();
		for (int i = 0 ; i < iterations ; i++){
			seq = clock.getNextSequence(randomKey);
			if (i % 10000 == 0) {
				System.out.print(".");
			}
		}
		long end = System.currentTimeMillis();
		System.out.println(String.format("Made %s increments in %s ms", iterations, (end - start)));
		assertEquals(iterations, seq + 1);
	}
	
	@Test 
	public void testConcurrentClients() throws Exception{
		int iterations = 10000;
//		int iterations = 1;
		Clock clock = getClock();

		CountDownLatch startGate = new CountDownLatch(1);
		CountDownLatch endGate = new CountDownLatch(5);
		new Thread(new Driver("/first-key", clock, iterations, startGate, endGate)).start();
		new Thread(new Driver("/second-key", clock, iterations, startGate, endGate)).start();
		new Thread(new Driver("/third-key", clock, iterations, startGate, endGate)).start();
		new Thread(new Driver("/fourth-key", clock, iterations, startGate, endGate)).start();
		new Thread(new Driver("/first-key", clock, iterations, startGate, endGate)).start();
		
		LOG.info("Waiting before starting 5 threads each doing " 
							+ iterations + " iterations (2 contending)");
		Thread.sleep(1000);
		long start = System.currentTimeMillis();
		startGate.countDown();
		endGate.await();
		long end = System.currentTimeMillis();
		LOG.info("Done in :" + (end - start) + " ms");
		
		assertEquals((2 * iterations) , clock.getNextSequence("/first-key") );
		assertEquals(iterations, clock.getNextSequence("/second-key") );
		assertEquals(iterations, clock.getNextSequence("/third-key") );
		assertEquals(iterations, clock.getNextSequence("/fourth-key") );
		
	}
	
	private class Driver implements Runnable{
		Clock clock;
		int iterations;
		String key;
		CountDownLatch startGate;
		CountDownLatch endGate;
		
		Driver(String key, Clock clock, int iterations, 
				CountDownLatch startGate, CountDownLatch endGate){
			this.key = key;
			this.clock = clock;
			this.iterations = iterations;
			this.startGate = startGate;
			this.endGate = endGate;
		}
		
		@Override
		public void run() {
			try{
				startGate.await();
				LOG.info("Running test");
				for (int i = 0; i < iterations; i++){
					clock.getNextSequence(key);
				}
				LOG.info("Test finished");
				endGate.countDown();
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}

	
}
