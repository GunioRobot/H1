package com.talis.platform.sequencing.apitest;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.talis.platform.sequencing.http.SequenceServer;
import com.talis.platform.sequencing.test.NetworkUtils;
import com.talis.platform.sequencing.zookeeper.EmbeddedZookeeper;
import com.talis.platform.sequencing.zookeeper.ZooKeeperProvider;

public class SequenceServerAcceptanceITCase {
	
	private String key1 = "key1";
	private String key2 = "key2";
	
	private int httpPort;
	private SequenceServer sequenceServer;
	private HttpClient httpClient = new DefaultHttpClient();
	
	@Rule
	public final EmbeddedZookeeper embeddedZookeeper = new EmbeddedZookeeper();
		
	@Before
	public void setUp() throws Exception {
		httpPort = NetworkUtils.findFreePort();
		sequenceServer = new SequenceServer(httpPort);
		sequenceServer.start();
		
		System.setProperty(ZooKeeperProvider.SERVER_LIST_LOCATION_PROPERTY, embeddedZookeeper.getZkServersFileLocation());
		System.setProperty(ZooKeeperProvider.SESSION_TIMEOUT_PROPERTY, "100");
		System.setProperty(ZooKeeperProvider.CONNECTION_TIMEOUT_PROPERTY, "100");
	}

	@After
	public void tearDown() throws Exception {
		try {
			sequenceServer.stop();
		} finally {
			System.clearProperty(ZooKeeperProvider.SERVER_LIST_LOCATION_PROPERTY);
			System.clearProperty(ZooKeeperProvider.SESSION_TIMEOUT_PROPERTY);
			System.clearProperty(ZooKeeperProvider.CONNECTION_TIMEOUT_PROPERTY);
		}
	}
	
	@Test
	public void getUnknownSequenceReturns404() throws Exception {
		assertNotFound("unknownSequence");
	}
	
	@Test
	public void getSequenceWhenZookeeperUnavailable() throws Exception {
		embeddedZookeeper.stopServer();
		assertInternalError("unknownSequence");
	}
	
	@Test
	public void postToUnknownCreatesSequence() throws Exception {
		String key = "newKey";
		assertNotFound(key);
		long newSequence = incrementKey(key);
		assertEquals(0, newSequence);
		assertSequenceValue(newSequence, key);
	}
	
	@Test
	public void postIncrementsExistingSequence() throws Exception {
		String key = "anotherKey";
		assertNotFound(key);
		assertEquals(0, incrementKey(key));
		assertEquals(1, incrementKey(key));
		assertEquals(2, incrementKey(key));
		assertSequenceValue(2, key);
	}
	
	@Test
	public void queryKeysWithSingleKey() throws Exception {
		incrementKeysForQuery();
		// Test
		HttpGet get = new HttpGet(buildQueryUri(key1));
		HttpResponse response = httpClient.execute(get);
		try {
			assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
			String returnedValue = IOUtils.toString(response.getEntity().getContent());
			JSONObject jsonObject = new JSONObject(returnedValue);
			assertEquals(0, jsonObject.getLong(key1));
		} finally {
			EntityUtils.consume(response.getEntity());
		}
	}
	
	@Test
	public void queryKeys() throws Exception {
		incrementKeysForQuery();
		// Test
		HttpGet get = new HttpGet(buildQueryUri(key1, key2));
		HttpResponse response = httpClient.execute(get);
		try {
			assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
			String returnedValue = IOUtils.toString(response.getEntity().getContent());
			JSONObject jsonObject = new JSONObject(returnedValue);
			assertEquals(0, jsonObject.getLong(key1));
			assertEquals(1, jsonObject.getLong(key2));
		} finally {
			EntityUtils.consume(response.getEntity());
		}
	}
	
	@Test
	public void queryKeysWithNoKeys() throws Exception {
		HttpGet get = new HttpGet(buildQueryUri());
		HttpResponse response = httpClient.execute(get);
		try {
			assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
			IOUtils.toString(response.getEntity().getContent());
		} finally {
			EntityUtils.consume(response.getEntity());
		}
	}
	
	@Test
	public void queryWithMissingKey() throws Exception {
		// Setup
		incrementKey(key1);
		String missingKey = "missing";
		// Test
		HttpGet get = new HttpGet(buildQueryUri(key1, missingKey));
		assertQueryWithMissingKeyIsNegative(get, missingKey);
	}
	
	private void incrementKeysForQuery() throws Exception {
		incrementKey(key1);
		incrementKey(key2);
		incrementKey(key2);
	}
	
	private void assertNotFound(String key)
			throws Exception {
		assertExpectedStatusForGet(HttpStatus.SC_NOT_FOUND, key);
	}

	private void assertQueryWithMissingKeyIsNegative(HttpUriRequest req, String missingKey)
			throws Exception {
		HttpResponse response = httpClient.execute(req);
		try {
			assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
			String returnedValue = IOUtils.toString(response.getEntity().getContent());
			JSONObject json = new JSONObject(returnedValue);
			assertEquals(-1, json.getLong(missingKey));
		} finally {
			EntityUtils.consume(response.getEntity());
		}
	}
	
	private void assertInternalError(String key)
			throws Exception {
		assertExpectedStatusForGet(HttpStatus.SC_INTERNAL_SERVER_ERROR, key);
	}

	private void assertExpectedStatusForGet(int expectedStatus, String key)
			throws IOException, ClientProtocolException {
		HttpResponse response = httpGet(key);
		try {
			assertEquals(expectedStatus, response.getStatusLine().getStatusCode());
		} finally {
			EntityUtils.consume(response.getEntity());
		}
	}

	private void assertSequenceValue(long expectedSequence, String key) 
			throws Exception {
		HttpResponse response = httpGet(key);
		try {
			assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
			String returnedValue = IOUtils.toString(response.getEntity().getContent());
			long sequence = Long.parseLong(returnedValue);
			assertEquals(expectedSequence, sequence);
		} finally {
			EntityUtils.consume(response.getEntity());
		}		
	}
	
	private long incrementKey(String key)
			throws Exception {
		HttpResponse response = httpPost(key);
		try {
			String returnedValue = IOUtils.toString(response.getEntity().getContent());
			return Long.parseLong(returnedValue);
		} finally {
			EntityUtils.consume(response.getEntity());
		}
	}
	
	private HttpResponse httpGet(String key) throws IOException,
			ClientProtocolException {
		HttpGet get = new HttpGet(buildUri(key));
		HttpResponse response = httpClient.execute(get);
		return response;
	}
	
	private HttpResponse httpPost(String key) throws IOException,
			ClientProtocolException {
		HttpPost post = new HttpPost(buildUri(key));
		HttpResponse response = httpClient.execute(post);
		return response;
	}

	private String buildUri(String key) {
		return String.format("http://localhost:%d/seq/%s", httpPort, key);
	}

	private String buildQueryUri(String... keys) {
		StringBuilder uriBuilder = new StringBuilder();
		uriBuilder.append(String.format("http://localhost:%d/seq/?", httpPort));
		for (String key : keys) {
			uriBuilder.append("key=");
			uriBuilder.append(key);
			uriBuilder.append("&");
		}
		return uriBuilder.toString();
	}
}
