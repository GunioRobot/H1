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

import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.talis.jersey.exceptions.BadRequestException;
import com.talis.jersey.exceptions.ServerErrorException;
import com.talis.platform.TimestampProvider;
import com.talis.platform.sequencing.Clock;
import com.talis.platform.sequencing.NoSuchSequenceException;
import com.talis.platform.sequencing.metrics.SequencingMetrics;

@Path("/seq/")
public class Sequence {

	private static final Long DEFAULT_SEQUENCE = -1l;

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
    @Produces(MediaType.APPLICATION_JSON)
	public Map<String, Long> getCurrentSequences(@QueryParam("key") SortedSet<String> keys) {
		if (null == keys || keys.isEmpty()) {
			LOG.error("Sequence list {} was null or empty", keys);
			throw new BadRequestException("Cannot query an empty list of sequences");
		}
		LOG.debug("Starting query for keys {}", keys);
		Map<String, Long> results = new HashMap<String, Long>();
		for (String key : keys) {
			results.put(key, getSequence(key));
		}
		return results;
	}

    @POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, Long> getCurrentSequencesByFormPost(@FormParam("key") SortedSet<String> keys) {
    	return getCurrentSequences(keys);
	}

    @GET
    @Path("{key}")
    @Produces(MediaType.TEXT_PLAIN)
	public String getCurrentSequence(@PathParam("key") String key) {
		Long sequence = getSequence(key);
		return sequence.toString();
	}

	@POST
    @Path("{key}")
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

	private Long getSequence(String key) {
		try {
			key = "/" + key;
			LOG.debug("Getting sequence for key {}", key);
			long start = timestampProvider.getCurrentTimeInMillis();
			Long sequence = clock.getSequence(key);
			long end = timestampProvider.getCurrentTimeInMillis();
			metrics.recordSequenceReadLatency(end - start);
			LOG.debug("Current sequence for key {} is {}", key, sequence);
			return sequence;
		} catch (NoSuchSequenceException e) {
			// Don't add this to error metrics, as it's not really an error.
			LOG.info( String.format("Sequence for key %s not found. Returning -1", key), e);
			return DEFAULT_SEQUENCE;
		} catch (Exception e) {
			metrics.incrementReadErrorResponses();
			LOG.error(String.format("Clock errored when getting sequence for key {}", key), e);
			throw new ServerErrorException(String.format("Internal Error while accessing sequence %s", key));
		}
	}

}