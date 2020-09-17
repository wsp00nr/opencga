package org.opencb.opencga.server.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;

import javax.ws.rs.ext.Provider;

@Provider
public class OpenCgaApplicationEventListener implements ApplicationEventListener {

    private final Logger logger = LogManager.getLogger(getClass());

    @Override
    public void onEvent(ApplicationEvent event) {
        switch (event.getType()) {
            case INITIALIZATION_APP_FINISHED:
                Object opencgaHome = event.getResourceConfig().getProperty("OPENCGA_HOME");
                OpenCGAWSServer.init(String.valueOf(opencgaHome));
                break;
            case INITIALIZATION_START:
            case INITIALIZATION_FINISHED:
                break;
            case DESTROY_FINISHED:
                OpenCGAWSServer.shutdown();
                break;
            case RELOAD_FINISHED:
                break;
        }
    }

    @Override
    public RequestEventListener onRequest(RequestEvent requestEvent) {
        return null;
    }
}
