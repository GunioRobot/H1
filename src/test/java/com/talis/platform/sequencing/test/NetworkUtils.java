package com.talis.platform.sequencing.test;

import java.io.IOException;
import java.net.ServerSocket;

public class NetworkUtils {
	public static int findFreePort() {
		ServerSocket server;
		try {
			server = new ServerSocket(0);
			int port = server.getLocalPort();
			server.close();
			return port;
		} catch (IOException e) {
			throw new RuntimeException("IOException while trying to find a free port", e);
		}
	}
}
