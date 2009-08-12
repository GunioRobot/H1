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
        SEQUENCE ("/seq/{key}", Sequencer.class);

        public final String uriPattern;
        public final Class< ? extends Resource> resource;
        ROUTE(String uriPattern, Class < ? extends Resource> resource){
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
        Router router = new Router(getContext());
        router.attach(ROUTE.SEQUENCE.uriPattern, ROUTE.SEQUENCE.resource);
        return router;
    }

}