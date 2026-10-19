package com.uce.programacions.clients;


import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.uce.programacions.dtos.UserDto;

@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
//@RegisterRestClient(baseUri = "https://jsonplaceholder.typicode.com")
@RegisterRestClient(baseUri = "stork://users-api")
public interface UsersRestClient {

    @GET
    @Path("/{id}")
    @Retry(maxRetries = 2, delay = 1000)
    @Fallback(fallbackMethod = "getUserByIdFallback")
    UserDto getUserById(@PathParam("id") int id);

    default UserDto getUserByIdFallback(int id) {
        return UserDto.builder()
                .id(id)
                .name("unknown")
                .username("unknown")
                .email("unknown")
                .build();
    }
}
