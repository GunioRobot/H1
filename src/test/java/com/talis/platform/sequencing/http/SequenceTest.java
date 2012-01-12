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

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.talis.jersey.exceptions.BadRequestException;
import com.talis.jersey.exceptions.ServerErrorException;
import com.talis.platform.SystemTimestampProvider;
import com.talis.platform.TimestampProvider;
import com.talis.platform.sequencing.Clock;
import com.talis.platform.sequencing.NoSuchSequenceException;
import com.talis.platform.sequencing.SequencingException;
import com.talis.platform.sequencing.metrics.NullSequencingMetrics;
import com.talis.platform.sequencing.metrics.SequencingMetrics;

public class SequenceTest {

	String key;
	String fullKey;
	String otherKey;
	String otherFullKey;
	TimestampProvider timestampProvider;
	Clock clock;
	SequencingMetrics metrics;
	
	TimestampProvider mockProvider;
	SequencingMetrics mockMetrics;
	
	@Before
	public void setup(){
		metrics = new NullSequencingMetrics();
		timestampProvider = new SystemTimestampProvider();
		key = UUID.randomUUID().toString();
		fullKey = "/" + key;
		otherKey = UUID.randomUUID().toString();
		otherFullKey = "/" + otherKey;
		
		mockProvider = createStrictMock(TimestampProvider.class);
		replay(mockProvider);
		
		mockMetrics = createStrictMock(SequencingMetrics.class);
		replay(mockMetrics);
	}
	
	@After
	public void teardown(){
		verify(clock);
		verify(mockProvider);
		verify(mockMetrics);
	}
	
	@Test
	public void postingUsesClockToIncrementSeqAndReturnsNextValue() 
	throws SequencingException{
		clock = createStrictMock(Clock.class);
		expect(clock.getNextSequence(fullKey)).andReturn(999l);
		replay(clock);

		Sequence resource = new Sequence(clock, timestampProvider, metrics );
		assertEquals("999", resource.incrementSequence(key));
	}
	
	@Test
	public void recordSequenceIncrementLatencyViaMetricsObject() 
	throws Exception{
		clock = createMock(Clock.class);
		expect(clock.getNextSequence(fullKey)).andReturn(999l);
		replay(clock);
		
		TimestampProvider mockProvider = createStrictMock(TimestampProvider.class);
		expect(mockProvider.getCurrentTimeInMillis()).andReturn(100l);
		expect(mockProvider.getCurrentTimeInMillis()).andReturn(150l);
		replay(mockProvider);
		
		SequencingMetrics mockMetrics = createStrictMock(SequencingMetrics.class);
		mockMetrics.recordSequenceWriteLatency(50l);
		replay(mockMetrics);
		
		Sequence resource = new Sequence(clock, mockProvider, mockMetrics);
		resource.incrementSequence(key);
		verify(mockProvider);
		verify(mockMetrics);
	}
	
	@Test (expected=ServerErrorException.class)
	public void return500IfClockThrowsException() throws Exception { 
		clock = createStrictMock(Clock.class);
		expect(clock.getNextSequence(fullKey)).andThrow(new RuntimeException("BANG!"));
		replay(clock);
		
		Sequence resource = new Sequence(clock, timestampProvider, metrics);
		resource.incrementSequence(key);
	}
	
	@Test (expected=ServerErrorException.class)
	public void return500IfClockThrowsExceptionDuringRead() throws Exception { 
		clock = createStrictMock(Clock.class);
		expect(clock.getSequence(fullKey)).andThrow(new RuntimeException("BANG!"));
		replay(clock);
		
		Sequence resource = new Sequence(clock, timestampProvider, metrics);
		resource.getCurrentSequence(key);
	}
	
	@Test (expected=ServerErrorException.class)
	public void incrementClockErrorsViaMetricsObject() throws Exception {
		clock = createMock(Clock.class);
		expect(clock.getNextSequence(fullKey)).andThrow(new RuntimeException("KABOOM!"));
		replay(clock);
		
		SequencingMetrics mockMetrics = createStrictMock(SequencingMetrics.class);
		mockMetrics.incrementErrorResponses();
		replay(mockMetrics);
		
		Sequence resource = new Sequence(clock, timestampProvider, mockMetrics);
		try{
			resource.incrementSequence(key);
		}finally{
			verify(mockMetrics);	
		}
	}
	
    @Test
	public void getUsesClockToGetValue() throws SequencingException{
		clock = createStrictMock(Clock.class);
		expect(clock.getSequence(fullKey)).andReturn(999l);
		replay(clock);
		
		Sequence resource = new Sequence(clock, timestampProvider, metrics);
		assertEquals("999", resource.getCurrentSequence(key));
		verify(clock);
	}
    
    @Test
	public void getReturnsNegativeOneWhenNoSuchSequenceExceptionIsThrown() throws Exception { 
		clock = createStrictMock(Clock.class);
		Exception ex = new NoSuchSequenceException("BOOM!", null);
		expect(clock.getSequence(fullKey)).andThrow(ex);
		replay(clock);
		
		Sequence resource = new Sequence(clock, timestampProvider, metrics);
		assertEquals("-1", resource.getCurrentSequence(key));
	}
    
	@Test (expected=ServerErrorException.class)
	public void incrementClockReadErrorsViaMetricsObject() throws Exception{
		clock = createMock(Clock.class);
		expect(clock.getSequence(fullKey)).andThrow(new RuntimeException("KABOOM!"));
		replay(clock);
		
		SequencingMetrics mockMetrics = createStrictMock(SequencingMetrics.class);
		mockMetrics.incrementReadErrorResponses();
		replay(mockMetrics);
        
		Sequence resource = new Sequence(clock, timestampProvider, mockMetrics);
		try{
			resource.getCurrentSequence(key);
		}finally{
			verify(mockMetrics);
		}
	}
	
	@Test
	public void getCurrentSequenceIncrementsLatencyViaMetricsObject() throws Exception{
		clock = createMock(Clock.class);
		expect(clock.getSequence(fullKey)).andReturn(999l);
		replay(clock);
		
		mockProvider = createStrictMock(TimestampProvider.class);
		expect(mockProvider.getCurrentTimeInMillis()).andReturn(100l);
		expect(mockProvider.getCurrentTimeInMillis()).andReturn(150l);
		replay(mockProvider);
		
		mockMetrics = createStrictMock(SequencingMetrics.class);
		mockMetrics.recordSequenceReadLatency(50l);
		replay(mockMetrics);
		
		Sequence resource = new Sequence(clock, mockProvider, mockMetrics);
		String sequence = resource.getCurrentSequence(key);
		assertEquals("999", sequence);
	}
	
	@Test
	public void getCurrentSequencesIncrementsLatencyViaMetricsObject() throws Exception{
		Long expectedKeySeq = 1066l;
		
		clock = createMock(Clock.class);
		expect(clock.getSequence(fullKey)).andReturn(expectedKeySeq);
		replay(clock);
		
		mockProvider = createStrictMock(TimestampProvider.class);
		expect(mockProvider.getCurrentTimeInMillis()).andReturn(100l);
		expect(mockProvider.getCurrentTimeInMillis()).andReturn(150l);
		replay(mockProvider);
		
		mockMetrics = createStrictMock(SequencingMetrics.class);
		mockMetrics.recordSequenceReadLatency(50l);
		replay(mockMetrics);
		Sequence resource = new Sequence(clock, mockProvider, mockMetrics);
		SortedSet<String> keys = new TreeSet<String>();
		keys.add(key);
		Map<String, Long> currentSequences = resource.getCurrentSequences(keys);
		assertEquals(expectedKeySeq, currentSequences.get(key));
	}
	
	@Test
	public void getCurrentSequencesIncrementsLatencyViaMetricsObjectForOtherKey() throws Exception{
		Long expectedKeySeq = 42l;
		Long expectedOtherKeySeq = 1066l;
		clock = createMock(Clock.class);
		expect(clock.getSequence(fullKey)).andReturn(expectedKeySeq);
		expect(clock.getSequence(otherFullKey)).andReturn(expectedOtherKeySeq);
		replay(clock);
		
		mockProvider = createStrictMock(TimestampProvider.class);
		expect(mockProvider.getCurrentTimeInMillis()).andReturn(100l);
		expect(mockProvider.getCurrentTimeInMillis()).andReturn(150l);
		expect(mockProvider.getCurrentTimeInMillis()).andReturn(200l);
		expect(mockProvider.getCurrentTimeInMillis()).andReturn(270l);
		replay(mockProvider);
		
		mockMetrics = createStrictMock(SequencingMetrics.class);
		mockMetrics.recordSequenceReadLatency(50l);
		mockMetrics.recordSequenceReadLatency(70l);
		replay(mockMetrics);
		
		Sequence resource = new Sequence(clock, mockProvider, mockMetrics);
		SortedSet<String> keys = new TreeSet<String>();
		keys.add(key);
		keys.add(otherKey);
		Map<String, Long> currentSequences = resource.getCurrentSequences(keys);
		assertEquals(expectedKeySeq, currentSequences.get(key));
		assertEquals(expectedOtherKeySeq, currentSequences.get(otherKey));
	}
		
	@Test(expected = ServerErrorException.class)
	public void getCurrentSequencesWhenFirstErrors() throws Exception{
		clock = createMock(Clock.class);
		expect(clock.getSequence(anyObject(String.class))).andThrow(new SequencingException("BOOM!", null));
		replay(clock);
		
		mockProvider = createStrictMock(TimestampProvider.class);
		expect(mockProvider.getCurrentTimeInMillis()).andReturn(100l);
		replay(mockProvider);
		
		SequencingMetrics mockMetrics = createStrictMock(SequencingMetrics.class);
		mockMetrics.incrementReadErrorResponses();
		replay(mockMetrics);
		
		Sequence resource = new Sequence(clock, mockProvider, mockMetrics);
		SortedSet<String> keys = new TreeSet<String>();
		keys.add(otherKey);
		keys.add(key);
		
		assertQuerySequencesFailsWithCorrectErrorMessage(resource, keys);
	}
	
	@Test(expected = ServerErrorException.class)
	public void getCurrentSequencesWhenSecondErrors() throws Exception{
		clock = createMock(Clock.class);
		expect(clock.getSequence(anyObject(String.class))).andReturn(42l);
		expect(clock.getSequence(anyObject(String.class))).andThrow(new SequencingException("BOOM!", null));
		replay(clock);
		
		mockProvider = createStrictMock(TimestampProvider.class);
		expect(mockProvider.getCurrentTimeInMillis()).andReturn(100l);
		expect(mockProvider.getCurrentTimeInMillis()).andReturn(150l);
		expect(mockProvider.getCurrentTimeInMillis()).andReturn(200l);
		replay(mockProvider);
		
		mockMetrics = createStrictMock(SequencingMetrics.class);
		mockMetrics.recordSequenceReadLatency(50l);
		mockMetrics.incrementReadErrorResponses();
		replay(mockMetrics);
		
		Sequence resource = new Sequence(clock, mockProvider, mockMetrics);
		SortedSet<String> keys = new TreeSet<String>();
		keys.add(otherKey);
		keys.add(key);
		
		assertQuerySequencesFailsWithCorrectErrorMessage(resource, keys);
	}
	
	@Test
	public void getCurrentSequencesTranslatesMissingSequenceIntoMinusOne() throws Exception{
		Long expectedSequence = 42l;
		Long expectedOtherSequence = -1l;
		
		clock = createMock(Clock.class);
		expect(clock.getSequence(fullKey)).andReturn(expectedSequence);
		expect(clock.getSequence(otherFullKey)).andThrow(new NoSuchSequenceException("BOOM!", null));
		replay(clock);
		
		mockProvider = createStrictMock(TimestampProvider.class);
		expect(mockProvider.getCurrentTimeInMillis()).andReturn(100l);
		expect(mockProvider.getCurrentTimeInMillis()).andReturn(150l);
		expect(mockProvider.getCurrentTimeInMillis()).andReturn(200l);
		replay(mockProvider);
		
		mockMetrics = createStrictMock(SequencingMetrics.class);
		mockMetrics.recordSequenceReadLatency(50l);
		replay(mockMetrics);
		
		Sequence resource = new Sequence(clock, mockProvider, mockMetrics);
		SortedSet<String> keys = new TreeSet<String>();
		keys.add(otherKey);
		keys.add(key);
		
		Map<String, Long> currentSequences = resource.getCurrentSequences(keys);
		
		assertEquals(expectedSequence, currentSequences.get(key));
		assertEquals(expectedOtherSequence, currentSequences.get(otherKey));
	}

	@Test
	public void getCurrentSequencesByFormPostIncrementsLatencyViaMetricsObject() throws Exception{
		Long expectedKeySeq = 1066l;
		
		clock = createMock(Clock.class);
		expect(clock.getSequence(fullKey)).andReturn(expectedKeySeq);
		replay(clock);
		
		mockProvider = createStrictMock(TimestampProvider.class);
		expect(mockProvider.getCurrentTimeInMillis()).andReturn(100l);
		expect(mockProvider.getCurrentTimeInMillis()).andReturn(150l);
		replay(mockProvider);
		
		mockMetrics = createStrictMock(SequencingMetrics.class);
		mockMetrics.recordSequenceReadLatency(50l);
		replay(mockMetrics);
		Sequence resource = new Sequence(clock, mockProvider, mockMetrics);
		SortedSet<String> keys = new TreeSet<String>();
		keys.add(key);
		Map<String, Long> currentSequences = resource.getCurrentSequencesByFormPost(keys);
		assertEquals(expectedKeySeq, currentSequences.get(key));
	}
		
	@Test(expected = ServerErrorException.class)
	public void getCurrentSequencesByFormPostWhenFirstErrors() throws Exception{
		clock = createMock(Clock.class);
		expect(clock.getSequence(anyObject(String.class))).andThrow(new SequencingException("BOOM!", null));
		replay(clock);
		
		mockProvider = createStrictMock(TimestampProvider.class);
		expect(mockProvider.getCurrentTimeInMillis()).andReturn(100l);
		replay(mockProvider);
		
		SequencingMetrics mockMetrics = createStrictMock(SequencingMetrics.class);
		mockMetrics.incrementReadErrorResponses();
		replay(mockMetrics);
		
		Sequence resource = new Sequence(clock, mockProvider, mockMetrics);
		SortedSet<String> keys = new TreeSet<String>();
		keys.add(otherKey);
		keys.add(key);
		
		assertQuerySequencesByFormPostFailsWithCorrectErrorMessage(resource, keys);
	}
	
	@Test(expected = ServerErrorException.class)
	public void getCurrentSequencesByFormPostWhenSecondErrors() throws Exception{
		clock = createMock(Clock.class);
		expect(clock.getSequence(anyObject(String.class))).andReturn(42l);
		expect(clock.getSequence(anyObject(String.class))).andThrow(new SequencingException("BOOM!", null));
		replay(clock);
		
		mockProvider = createStrictMock(TimestampProvider.class);
		expect(mockProvider.getCurrentTimeInMillis()).andReturn(100l);
		expect(mockProvider.getCurrentTimeInMillis()).andReturn(150l);
		expect(mockProvider.getCurrentTimeInMillis()).andReturn(200l);
		replay(mockProvider);
		
		mockMetrics = createStrictMock(SequencingMetrics.class);
		mockMetrics.recordSequenceReadLatency(50l);
		mockMetrics.incrementReadErrorResponses();
		replay(mockMetrics);
		
		Sequence resource = new Sequence(clock, mockProvider, mockMetrics);
		SortedSet<String> keys = new TreeSet<String>();
		keys.add(otherKey);
		keys.add(key);
		
		assertQuerySequencesByFormPostFailsWithCorrectErrorMessage(resource, keys);
	}
	
	@Test
	public void getCurrentSequencesByFormPostTranslatesMissingSequenceIntoMinusOne() throws Exception{
		Long expectedSequence = 42l;
		Long expectedOtherSequence = -1l;
		
		clock = createMock(Clock.class);
		expect(clock.getSequence(fullKey)).andReturn(expectedSequence);
		expect(clock.getSequence(otherFullKey)).andThrow(new NoSuchSequenceException("BOOM!", null));
		replay(clock);
		
		mockProvider = createStrictMock(TimestampProvider.class);
		expect(mockProvider.getCurrentTimeInMillis()).andReturn(100l);
		expect(mockProvider.getCurrentTimeInMillis()).andReturn(150l);
		expect(mockProvider.getCurrentTimeInMillis()).andReturn(200l);
		replay(mockProvider);
		
		mockMetrics = createStrictMock(SequencingMetrics.class);
		mockMetrics.recordSequenceReadLatency(50l);
		replay(mockMetrics);
		
		Sequence resource = new Sequence(clock, mockProvider, mockMetrics);
		SortedSet<String> keys = new TreeSet<String>();
		keys.add(otherKey);
		keys.add(key);
		
		Map<String, Long> currentSequences = resource.getCurrentSequencesByFormPost(keys);
		
		assertEquals(expectedSequence, currentSequences.get(key));
		assertEquals(expectedOtherSequence, currentSequences.get(otherKey));
	}

	private void assertQuerySequencesFailsWithCorrectErrorMessage(
			Sequence resource, SortedSet<String> keys) throws Exception {
		try {
			resource.getCurrentSequences(keys);
		} catch (Exception e) {
			assertTrue(e.getMessage().contains("Internal Error while accessing sequence"));
			assertTrue(e.getMessage().contains(otherKey) ^ e.getMessage().contains(key));
			throw e;
		}
	}

	private void assertQuerySequencesByFormPostFailsWithCorrectErrorMessage(
			Sequence resource, SortedSet<String> keys) throws Exception {
		try {
			resource.getCurrentSequences(keys);
		} catch (Exception e) {
			assertTrue(e.getMessage().contains("Internal Error while accessing sequence"));
			assertTrue(e.getMessage().contains(otherKey) ^ e.getMessage().contains(key));
			throw e;
		}
	}
	
	@Test(expected = BadRequestException.class)
	public void getCurrentSequencesForEmpty() throws Exception{
		clock = createMock(Clock.class);
		replay(clock);
		
		Sequence resource = new Sequence(clock, mockProvider, mockMetrics);
		SortedSet<String> keys = new TreeSet<String>();
		resource.getCurrentSequences(keys);	
	}
	
	@Test(expected = BadRequestException.class)
	public void getCurrentSequencesForNull() throws Exception{
		clock = createMock(Clock.class);
		replay(clock);
		
		Sequence resource = new Sequence(clock, mockProvider, mockMetrics);
		resource.getCurrentSequences(null);	
	}
	
	@Test(expected = BadRequestException.class)
	public void getCurrentSequencesByFormPostForEmpty() throws Exception{
		clock = createMock(Clock.class);
		replay(clock);
		
		Sequence resource = new Sequence(clock, mockProvider, mockMetrics);
		SortedSet<String> keys = new TreeSet<String>();
		resource.getCurrentSequencesByFormPost(keys);	
	}
	
	@Test(expected = BadRequestException.class)
	public void getCurrentSequencesByFormPostForNull() throws Exception{
		clock = createMock(Clock.class);
		replay(clock);
		
		Sequence resource = new Sequence(clock, mockProvider, mockMetrics);
		resource.getCurrentSequencesByFormPost(null);	
	}
}