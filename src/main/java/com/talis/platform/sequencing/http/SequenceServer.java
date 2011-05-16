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

import java.util.ServiceLoader;
import java.util.concurrent.CountDownLatch;

import org.restlet.Component;
import org.restlet.VirtualHost;
import org.restlet.data.Protocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.talis.platform.NullInjector;
import com.talis.platform.sequencing.metrics.SequencingMetrics;

public class SequenceServer {
	private static final Logger LOG = LoggerFactory.getLogger(SequenceServer.class);
	
	public static final int DEFAULT_PORT = 9595;
	public static final String SERVER_IDENTIFIER = "H1 Server";
	
	private static Injector INJECTOR = new NullInjector();
	public static Injector getInjector(){
		return INJECTOR;
	}

	private Component myWebserver;
	
	public SequenceServer(Injector injector){
		INJECTOR = injector;
	}
	
	public void startWebserver(int port) throws Exception{
		INJECTOR.getInstance(SequencingMetrics.class);
		myWebserver = new Component();
	    myWebserver.getLogService().setEnabled(false);
	    myWebserver.getServers().add(Protocol.HTTP, port);
	    VirtualHost defaultHost = myWebserver.getDefaultHost();
	               
	    SequencingApplication sequencingApplication = 
	    	new SequencingApplication();
	    defaultHost.attach(sequencingApplication);
	    myWebserver.start();
	}
	
	
	public static void main(String[] args) {
		int port = DEFAULT_PORT;
		if (args.length > 0){
			port = Integer.parseInt(args[0]);
		}
		LOG.info("Starting Service on port %s port");
		
		Injector injector = 
			Guice.createInjector(ServiceLoader.load(Module.class));
		
	    try{
	    	new SequenceServer(injector).startWebserver(port);
	    }catch(Exception e){
	    	LOG.error("Unable to start webserver", e);
	    }
	    LOG.info("Service Started");
	    
	    try{
	    	new CountDownLatch(1).await();
	    }catch(Exception e){
	    	LOG.error("Server shutdown unexpectedly", e);
	    }
	}

}
