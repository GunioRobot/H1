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

import org.restlet.Component;
import org.restlet.VirtualHost;
import org.restlet.data.Protocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Scopes;
import com.talis.platform.NullInjector;
import com.talis.platform.SystemTimestampProvider;
import com.talis.platform.TimestampProvider;
import com.talis.platform.sequencing.metrics.SequencingMetrics;
import com.talis.platform.sequencing.zookeeper.ZooKeeperModule;

public class SequenceServer {
	private static final Logger LOG = LoggerFactory.getLogger(SequenceServer.class);
	
	public static final String SERVER_IDENTIFIER = "H1 Server";
	private static Injector INJECTOR = new NullInjector();
	public static Injector getInjector(){
		return INJECTOR;
	}

	public static Injector initInjector(){
		return Guice.createInjector(
				new ZooKeeperModule(),
				new AbstractModule(){
					@Override
					protected void configure() {
						bind(TimestampProvider.class)
							.to(SystemTimestampProvider.class)
							.in(Scopes.SINGLETON);
					}
				});
	}
	
	public static void main(String[] args) {
		int port = 9595;
		if (args.length > 0){
			port = Integer.parseInt(args[0]);
		}
		LOG.info("Starting Service on port %s port");
		
		INJECTOR = initInjector();
		INJECTOR.getInstance(SequencingMetrics.class);
		Component myWebserver = new Component();
	    myWebserver.getLogService().setEnabled(false);
	    myWebserver.getServers().add(Protocol.HTTP, port);
	    VirtualHost defaultHost = myWebserver.getDefaultHost();
	               
	    SequencingApplication sequencingApplication = 
	    	new SequencingApplication();
	    defaultHost.attach(sequencingApplication);
	        
	    try{
	    	myWebserver.start();    
	    }catch(Exception e){
	    	LOG.error("Unable to start webserver", e);
	    }
	    LOG.info("Service Started");
	}

}
