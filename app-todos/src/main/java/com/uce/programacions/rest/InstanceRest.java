package com.uce.programacions.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Map;

@Path("/instance")
@Produces(MediaType.APPLICATION_JSON)
public class InstanceRest {

    @ConfigProperty(name = "app.instance-id", defaultValue = "app-todos")
    String instanceId;

    @ConfigProperty(name = "quarkus.http.port", defaultValue = "8080")
    Integer port;

    @GET
    public Map<String, Object> info() {
        return Map.of(
                "instanceId", instanceId,
                "port", port
        );
    }
}
