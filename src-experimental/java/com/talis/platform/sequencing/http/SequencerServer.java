package com.talis.platform.sequencing.http;

import org.restlet.Component;
import org.restlet.VirtualHost;
import org.restlet.data.Protocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.talis.platform.sequencing.zookeeper.ZooKeeperModule;

public class SequencerServer {
	private static final Logger LOG = LoggerFactory.getLogger(SequencerServer.class);
	
	private static Injector INJECTOR;// = new NullInjector();
	public static Injector getInjector(){
		return INJECTOR;
	}

	public static Injector initInjector(){
		return Guice.createInjector(
				new ZooKeeperModule());
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
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
	            String message = "Unable to start webserver"; 
	            LOG.error(message, e);
	        }
	        System.out.println("Started");
	}

}
