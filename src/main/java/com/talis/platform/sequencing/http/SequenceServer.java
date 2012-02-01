/*
 *    Copyright 2010 Talis Systems Ltd
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.talis.platform.sequencing.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.talis.jersey.HttpServer;
import com.talis.jersey.guice.JerseyServletModule;
import com.talis.platform.sequencing.BaseModule;
import com.talis.platform.sequencing.zookeeper.ZooKeeperModule;

@SuppressWarnings("PMD")
public class SequenceServer {
	private static final Logger LOG = LoggerFactory.getLogger(SequenceServer.class);

	private static final int DEFAULT_HTTP_PORT =9595;

	private final int httpPort;
	private HttpServer webserver;

	public SequenceServer(int httpPort) {
		this.httpPort = httpPort;
	}

	public void start() throws Exception {
		Injector injector = Guice.createInjector(
				new ZooKeeperModule(),
				new BaseModule(),
				new JerseyServletModule("com.talis.platform.sequencing"));

		LOG.info("Starting webserver on port %s ", httpPort);
		webserver = new HttpServer();
		webserver.start(httpPort, injector);
		LOG.info("Service Started");
	}

	public void stop() throws Exception {
		if (null == webserver) {
			throw new IllegalStateException("Cannot stop server, as it has not been started");
		}
		webserver.stop();
	}

	public boolean isRunning() {
		if (webserver != null) {
			return webserver.isRunning();
		} else {
			return false;
		}
	}

	public static void main(String[] args) throws Exception {
		int httpPort = DEFAULT_HTTP_PORT;
		if (args.length > 0){
			httpPort = Integer.parseInt(args[0]);
		}
		SequenceServer server = new SequenceServer(httpPort);
		server.start();
	}
}
