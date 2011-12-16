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

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.talis.jersey.exceptions.NotFoundException;
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
	TimestampProvider timestampProvider;
	Clock clock;
	SequencingMetrics metrics;
	
	
	@Before
	public void setup(){
		metrics = new NullSequencingMetrics();
		timestampProvider = new SystemTimestampProvider();
		key = UUID.randomUUID().toString();
		fullKey = "/" + key;
	}
	
	@After
	public void teardown(){
		verify(clock);
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
		clock = createNiceMock(Clock.class);
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
		clock = createNiceMock(Clock.class);
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
    
    @Test (expected=NotFoundException.class)
	public void getReturns404WhenNoSuchSequenceExceptionIsThrown() throws Exception { 
		clock = createStrictMock(Clock.class);
		Exception ex = new NoSuchSequenceException("BOOM!", null);
		expect(clock.getSequence(fullKey)).andThrow(ex);
		replay(clock);
		
		Sequence resource = new Sequence(clock, timestampProvider, metrics);
		resource.getCurrentSequence(key);
	}
    
	@Test (expected=ServerErrorException.class)
	public void incrementClockReadErrorsViaMetricsObject() throws Exception{
		clock = createNiceMock(Clock.class);
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
	public void getSequenceIncrementLatencyViaMetricsObject() throws Exception{
		clock = createNiceMock(Clock.class);
		expect(clock.getSequence(fullKey)).andReturn(999l);
		replay(clock);
		
		TimestampProvider mockProvider = createStrictMock(TimestampProvider.class);
		expect(mockProvider.getCurrentTimeInMillis()).andReturn(100l);
		expect(mockProvider.getCurrentTimeInMillis()).andReturn(150l);
		replay(mockProvider);
		
		SequencingMetrics mockMetrics = createStrictMock(SequencingMetrics.class);
		mockMetrics.recordSequenceReadLatency(50l);
		replay(mockMetrics);
		
		Sequence resource = new Sequence(clock, mockProvider, mockMetrics);
		try{
			resource.getCurrentSequence(key);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
}