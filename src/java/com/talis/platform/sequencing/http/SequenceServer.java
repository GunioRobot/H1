package com.talis.platform.sequencing.http;

import org.restlet.Component;
import org.restlet.VirtualHost;
import org.restlet.data.Protocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.talis.platform.NullInjector;
import com.talis.platform.sequencing.zookeeper.ZooKeeperModule;

public class SequenceServer {
	private static final Logger LOG = LoggerFactory.getLogger(SequenceServer.class);
	
	public static final String SERVER_IDENTIFIER = 
		String.format("H1/%s.%s", Version.release, Version.revision);
	private static Injector INJECTOR = new NullInjector();
	public static Injector getInjector(){
		return INJECTOR;
	}

	public static Injector initInjector(){
		return Guice.createInjector(
				new ZooKeeperModule());
	}
	
	public static void main(String[] args) {
		LOG.info("Starting Service");
		INJECTOR = initInjector();
		Component myWebserver = new Component();
	    myWebserver.getLogService().setEnabled(false);
	    myWebserver.getServers().add(Protocol.HTTP, 9595);
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
