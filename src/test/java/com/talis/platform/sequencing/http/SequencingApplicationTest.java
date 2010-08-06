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
