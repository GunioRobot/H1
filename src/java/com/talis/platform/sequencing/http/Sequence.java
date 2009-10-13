package com.talis.platform.sequencing.http;

import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.talis.platform.sequencing.Clock;

public class Sequence extends Resource {
	
	private static final Logger LOG = LoggerFactory.getLogger(Sequence.class);
	public static final Variant VARIANT = new Variant(MediaType.TEXT_PLAIN);
	
	private final String myKey;
	private Clock myClock;
	
	@Inject
	public Sequence(Context context, Request request, Response response) {
		super(context, request, response);
		SequenceServer.getInjector().injectMembers(this);
		getVariants().add(new Variant(MediaType.TEXT_PLAIN));
		myKey = "/" + (String)getRequest().getAttributes().get("key");
	}
	
	@Inject
    public void setClock(Clock clock){
        myClock = clock;
    }

	@Override
    public boolean allowGet() {
        return false;
    }
	
	@Override
    public boolean allowPost() {
        return true;
    }

    @Override
    public void handlePost() {
    	Response response = getResponse();
    	response.getServerInfo()
				.setAgent(SequenceServer.SERVER_IDENTIFIER);
    	Representation rep = represent(VARIANT);
        response.setEntity(rep);
    }
	
	@Override
	public Representation represent(Variant variant) {
		try {
			if (LOG.isDebugEnabled()){
				LOG.debug(String.format("Getting next sequence for key %s",  
										myKey));
			}
			
			Long sequence = myClock.getNextSequence(myKey);
			
			if (LOG.isDebugEnabled()){
				LOG.debug(String.format("Next sequence for key %s is %s",  
										myKey, sequence));
			}
			
			return new StringRepresentation(sequence.toString(), 
											MediaType.TEXT_PLAIN);
		} catch (Exception e) {
			LOG.error("ERROR", e);
			getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, 
									"Internal Error");
			return new StringRepresentation(e.getMessage(), 
									MediaType.TEXT_PLAIN);
		}
	}
}