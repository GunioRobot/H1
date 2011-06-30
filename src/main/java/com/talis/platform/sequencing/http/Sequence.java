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
import com.talis.platform.SystemTimestampProvider;
import com.talis.platform.TimestampProvider;
import com.talis.platform.sequencing.Clock;
import com.talis.platform.sequencing.NoSuchSequenceException;
import com.talis.platform.sequencing.metrics.NullSequencingMetrics;
import com.talis.platform.sequencing.metrics.SequencingMetrics;

public class Sequence extends Resource {
	
	private static final Logger LOG = LoggerFactory.getLogger(Sequence.class);
	public static final Variant VARIANT = new Variant(MediaType.TEXT_PLAIN);
	
	private final String myKey;
	private Clock myClock;
	private SequencingMetrics myMetrics;
	private TimestampProvider myTimestampProvider;
	
	@Inject
	public Sequence(Context context, Request request, Response response) {
		super(context, request, response);
		myMetrics = new NullSequencingMetrics();
		myTimestampProvider = new SystemTimestampProvider();
		
		SequenceServer.getInjector().injectMembers(this);
		getVariants().add(new Variant(MediaType.TEXT_PLAIN));
		myKey = "/" + (String)getRequest().getAttributes().get("key");
	}
	
	@Inject
    public void setClock(Clock clock){
        myClock = clock;
    }
	
	@Inject
	public void setTimestampProvider(TimestampProvider provider){
		myTimestampProvider = provider;
	}
	
	@Inject
	public void setMetrics(SequencingMetrics metrics){
		myMetrics = metrics;
	}

	@Override
    public boolean allowGet() {
        return true;
    }
	
	@Override
    public boolean allowPost() {
        return true;
    }

    @Override
	public void handleGet() {
    	Response response = getResponse();
    	response.getServerInfo()
				.setAgent(SequenceServer.SERVER_IDENTIFIER);
    	Representation rep = representGet(VARIANT);
        response.setEntity(rep);
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
			
			long start = myTimestampProvider.getCurrentTimeInMillis();
			Long sequence = myClock.getNextSequence(myKey);
			long end = myTimestampProvider.getCurrentTimeInMillis();
			myMetrics.recordSequenceWriteLatency(end - start);
			
			if (LOG.isDebugEnabled()){
				LOG.debug(String.format("Next sequence for key %s is %s",  
										myKey, sequence));
			}
			
			return new StringRepresentation(sequence.toString(), 
											MediaType.TEXT_PLAIN);
		} catch (Exception e) {
			myMetrics.incrementErrorResponses();
			LOG.error(
				String.format("Clock errored when incrementing sequence for key %s", 
								myKey), e);
			getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, 
									"Internal Error");
			return new StringRepresentation(e.getMessage(), 
									MediaType.TEXT_PLAIN);
		}
	}
	
	private Representation representGet(Variant variant) {
		try {
			if (LOG.isDebugEnabled()){
				LOG.debug(String.format("Getting sequence for key %s",  
										myKey));
			}

			// TODO: should we have read metrics?
			Long sequence = myClock.getSequence(myKey);
			
			if (LOG.isDebugEnabled()){
				LOG.debug(String.format("Current sequence for key %s is %s",  
										myKey, sequence));
			}
			
			return new StringRepresentation(sequence.toString(), 
											MediaType.TEXT_PLAIN);
		} catch (NoSuchSequenceException e) {
			// Don't add this to error metrics, as it's not really an error.
			LOG.info(
				String.format("Sequence for key %s not found", 
								myKey), e);
			getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
			return new StringRepresentation(e.getMessage(), 
					MediaType.TEXT_PLAIN);
		} catch (Exception e) {
			myMetrics.incrementErrorResponses();
			LOG.error(
				String.format("Clock errored when getting sequence for key %s", 
								myKey), e);
			getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, 
									"Internal Error");
			return new StringRepresentation(e.getMessage(), 
									MediaType.TEXT_PLAIN);
		}
	}
}