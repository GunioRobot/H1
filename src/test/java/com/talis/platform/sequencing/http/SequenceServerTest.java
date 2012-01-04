package com.talis.platform.sequencing.http;

import static org.junit.Assert.*;

import org.junit.Test;

import com.talis.platform.sequencing.test.NetworkUtils;

public class SequenceServerTest {

	@Test
	public void testStartAndStop() throws Exception {
		int httpPort = NetworkUtils.findFreePort();
		SequenceServer sequenceServer = new SequenceServer(httpPort);
		assertFalse(sequenceServer.isRunning());
		sequenceServer.start();
		assertTrue(sequenceServer.isRunning());
		sequenceServer.stop();
		assertFalse(sequenceServer.isRunning());
	}

	@Test(expected = IllegalStateException.class)
	public void stopErrorsIfNotStarted() throws Exception {
		SequenceServer sequenceServer = new SequenceServer(8080);
		sequenceServer.stop();
	}
}
