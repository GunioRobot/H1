package com.talis.platform.sequencing.http;

import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.talis.platform.sequencing.Clock;

public class Sequencer extends Resource {
	
	private static final Logger LOG = LoggerFactory.getLogger(Sequencer.class);
	private Clock myClock;
	private String myKey;
	
	@Inject
	public Sequencer(Context context, Request request, Response response) {
		super(context, request, response);
		SequencerServer.getInjector().injectMembers(this);
		getVariants().add(new Variant(MediaType.TEXT_PLAIN));
		myKey = (String)getRequest().getAttributes().get("key");
	}
	
	@Inject
    public void setClock(Clock clock){
        myClock = clock;
    }

	@Override
    public boolean allowPost() {
        return true;
    }

    @Override
    public void handlePost() {
        Variant preferredVariant = getPreferredVariant();
        getResponse().setEntity(represent(preferredVariant));
    }
	
	@Override
	public Representation represent(Variant variant) {
//		if (LOG.isDebugEnabled()){
//			LOG.debug(String.format(
//							"Requested representation with mediatype %s", 
//							variant.getMediaType() ) );
//		}
//		getResponse().getServerInfo().setAgent(Version.identifier);
		getResponse().getServerInfo().setAgent("Spike-0");
		
		try {
			Long sequence = myClock.getNextSequence(myKey);
//			Long sequence = 1l;
			return new StringRepresentation(sequence.toString(), 
											MediaType.TEXT_PLAIN);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			LOG.error("ERROR", e);
			return null;
		}

	}
	
}