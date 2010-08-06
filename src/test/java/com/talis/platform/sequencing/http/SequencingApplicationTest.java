package com.talis.platform.sequencing.http;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.restlet.Finder;
import org.restlet.Router;
import org.restlet.util.RouteList;


public class SequencingApplicationTest {

	@Test
	public void uriPatternForSequenceBoundToCorrectRoute(){
		SequencingApplication app = new SequencingApplication();
		Router router = (Router)app.createRoot();
		RouteList routeList = router.getRoutes();
		assertEquals(1, routeList.size());
		assertEquals(SequencingApplication.ROUTE.SEQUENCE.uriPattern,
							routeList.get(0).getTemplate().getPattern());
		Finder finder = (Finder)routeList.get(0).getNext();
		assertEquals(SequencingApplication.ROUTE.SEQUENCE.resource,
						finder.getTargetClass());
	}
}
