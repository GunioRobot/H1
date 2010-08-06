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

import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.Router;
import org.restlet.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SequencingApplication extends Application{

    private static final Logger LOG = LoggerFactory.getLogger(SequencingApplication.class);

    public static enum ROUTE{
        SEQUENCE ("/seq/{key}", Sequence.class);

        public final String uriPattern;
        public final Class< ? extends Resource> resource;
        ROUTE(final String uriPattern, final Class < ? extends Resource> resource){
            this.uriPattern = uriPattern;
            this.resource = resource;
        }
    }
    
    public SequencingApplication(){
        super();
        LOG.info("Created Sequencing Application");
    }
    
    @Override
    public Restlet createRoot() {
        final Router router = new Router(getContext());
        router.attach(ROUTE.SEQUENCE.uriPattern, ROUTE.SEQUENCE.resource);
        return router;
    }

}