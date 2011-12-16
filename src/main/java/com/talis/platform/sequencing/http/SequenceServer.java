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

public class SequenceServer {
	private static final Logger LOG = LoggerFactory.getLogger(SequenceServer.class);
	
	public static final String SERVER_IDENTIFIER = "H1 Server";
	private static final int DEFAULT_HTTP_PORT =9595;

	@SuppressWarnings("PMD")
	public static void main(String[] args) throws Exception {
		int httpPort = DEFAULT_HTTP_PORT;
		if (args.length > 0){
			httpPort = Integer.parseInt(args[0]);
		}

		Injector injector = Guice.createInjector(
									new ZooKeeperModule(),
									new BaseModule(),
									new JerseyServletModule("com.talis.platform.sequencing"));
				 
		LOG.info("Starting webserver on port %s ", httpPort);
		HttpServer webserver = new HttpServer();
        webserver.start(httpPort, injector);    
	    LOG.info("Service Started");
	}
}
