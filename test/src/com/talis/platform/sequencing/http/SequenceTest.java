package com.talis.platform.sequencing.http;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.reportMatcher;
import static org.easymock.classextension.EasyMock.createNiceMock;
import static org.easymock.classextension.EasyMock.createStrictMock;
import static org.easymock.classextension.EasyMock.replay;
import static org.easymock.classextension.EasyMock.verify;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.easymock.IArgumentMatcher;
import org.junit.Before;
import org.junit.Test;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.ServerInfo;
import org.restlet.data.Status;
import org.restlet.resource.StringRepresentation;

import com.talis.platform.sequencing.Clock;
import com.talis.platform.sequencing.SequencingException;

public class SequenceTest {

	private Context context;
	private Request request;
	private Response response;
	private ServerInfo serverInfo;
	private final String keyBase = UUID.randomUUID().toString();
	private String key;
	
	@Before
	public void setup(){
		key = "/" + keyBase;
		Map<String,Object> attributes = new HashMap<String, Object>()
										{{ put("key", keyBase); }}; 

		context = createNiceMock(Context.class);
		request = createStrictMock(Request.class);
		expect(request.getAttributes()).andReturn(attributes);
		replay(request);
		
		serverInfo = createStrictMock(ServerInfo.class);
		serverInfo.setAgent(SequenceServer.SERVER_IDENTIFIER);
		replay(serverInfo);
	}
	
	@Test
    public void putIsNotAnAllowedMethod(){
	   Sequence resource = new Sequence(context, request, 
			   								createNiceMock(Response.class));
       assertFalse(resource.allowPut());
    }
        
    @Test
    public void deleteIsNotAnAllowedMethod(){
    	Sequence resource = new Sequence(context, request, 
			   								createNiceMock(Response.class));
        assertFalse(resource.allowDelete());
    }
    
    @Test
    public void getIsNotAnAllowedMethod(){
    	Sequence resource = new Sequence(context, request, 
			   								createNiceMock(Response.class));
        assertFalse(resource.allowGet());
    }

    @Test
    public void postIsAnAllowedMethod(){
    	Sequence resource = new Sequence(context, request, 
			   								createNiceMock(Response.class));
        assertTrue(resource.allowPost());
    }
	
	@Test
	public void postingUsesClockToIncrementSeqAndReturnsNextValue() 
	throws SequencingException{
		Clock clock = createStrictMock(Clock.class);
		expect(clock.getNextSequence(key)).andReturn(999l);
		replay(clock);
		
		response = createStrictMock(Response.class);
		expect(response.getServerInfo()).andReturn(serverInfo);
        response.setEntity( eqStringRepresentation( 
        					new StringRepresentation("999") ) ) ;
        replay(response);

		Sequence resource = new Sequence(context, request, response);
		resource.setClock(clock);
		resource.handlePost();
		
		verify(request);
		verify(response);
	}
	
	@Test
	public void return500IfClockThrowsException() throws Exception{ 
		Clock clock = createStrictMock(Clock.class);
		expect(clock.getNextSequence(key))
				.andThrow(new RuntimeException("BANG!"));
		replay(clock);
		
		response = createStrictMock(Response.class);
		expect(response.getServerInfo()).andReturn(serverInfo);
		response.setStatus(Status.SERVER_ERROR_INTERNAL, "Internal Error");
        response.setEntity( eqStringRepresentation( 
        					new StringRepresentation("BANG!") ) ) ;
        replay(response);

		Sequence resource = new Sequence(context, request, response);
		resource.setClock(clock);
		resource.handlePost();
		
		verify(request);
		verify(response);		
	}
	
	public static StringRepresentation eqStringRepresentation(
									StringRepresentation expected) {
    	reportMatcher(new StringRepresentationEquals(expected));
		return expected;
    }
	
	static class StringRepresentationEquals implements IArgumentMatcher{
		private StringRepresentation expected;
		StringRepresentationEquals(StringRepresentation expected){
			this.expected = expected;
		}
		
		@Override
		public void appendTo(StringBuffer arg0) {
			arg0.append("eqStringRepresentation(");
			arg0.append(expected.getText());
			arg0.append(")");
		}

		@Override
		public boolean matches(Object arg0) {
			if (! (arg0 instanceof StringRepresentation)){
				return false;
			}
			return ((StringRepresentation)arg0).getText().equals(
													expected.getText() );
		}
	}
}
