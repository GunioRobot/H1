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

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.talis.jersey.exceptions.NotFoundException;
import com.talis.jersey.exceptions.ServerErrorException;
import com.talis.platform.TimestampProvider;
import com.talis.platform.sequencing.Clock;
import com.talis.platform.sequencing.NoSuchSequenceException;
import com.talis.platform.sequencing.metrics.SequencingMetrics;

@Path("/seq/{key}")
public class Sequence {
	
	private static final Logger LOG = LoggerFactory.getLogger(Sequence.class);
	
	private final Clock clock;
	private final SequencingMetrics metrics;
	private final TimestampProvider timestampProvider;
	
	@Inject
	public Sequence(Clock clock, TimestampProvider timestampProvider, SequencingMetrics metrics) {
		this.clock = clock;
		this.timestampProvider = timestampProvider;
		this.metrics = metrics;
	}

    @GET
    @Produces(MediaType.TEXT_PLAIN)
	public String getCurrentSequence(@PathParam("key") String key) {
		try {
			key = "/" + key;
			LOG.debug("Getting sequence for key {}", key);
			long start = timestampProvider.getCurrentTimeInMillis();
			Long sequence = clock.getSequence(key);
			long end = timestampProvider.getCurrentTimeInMillis();
			metrics.recordSequenceReadLatency(end - start);
			LOG.debug("Current sequence for key {} is {}", key, sequence);
			return sequence.toString();
		} catch (NoSuchSequenceException e) {
			// Don't add this to error metrics, as it's not really an error.
			LOG.info( String.format("Sequence for key %s not found", key), e);
			throw new NotFoundException(e.getMessage());
		} catch (Exception e) {
			metrics.incrementReadErrorResponses();
			LOG.error(String.format("Clock errored when getting sequence for key {}", key), e);
			throw new ServerErrorException("Internal Error");
		}
	}

	@POST
	@Produces(MediaType.TEXT_PLAIN)
    public String incrementSequence(@PathParam("key") String key) {
		try {
			key = "/" + key;
			LOG.debug("Getting next sequence for key {}", key);
			long start = timestampProvider.getCurrentTimeInMillis();
			Long sequence = clock.getNextSequence(key);
			long end = timestampProvider.getCurrentTimeInMillis();
			metrics.recordSequenceWriteLatency(end - start);
			LOG.debug("Next sequence for key {} is {}", key, sequence);
			return sequence.toString();
		} catch (Exception e) {
			metrics.incrementErrorResponses();
			LOG.error( String.format("Clock errored when incrementing sequence for key %s", key), e);
			throw new ServerErrorException("Internal Error");
		}
    }
}