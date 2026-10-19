package com.uce.programacions.rest;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import com.uce.programacions.clients.UsersRestClient;
import com.uce.programacions.dtos.TodoDto;
import com.uce.programacions.modelo.Todo;
import com.uce.programacions.repo.TodoRepository;

import java.util.List;

@Path("/todos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Transactional
@ApplicationScoped
public class TodoRest {

    final TodoRepository todoRepository;
    final UsersRestClient usersClient;

    @Inject
    public TodoRest(TodoRepository todoRepository, @RestClient UsersRestClient usersClient) {
        this.todoRepository = todoRepository;
        this.usersClient = usersClient;
    }

    @GET
    public List<TodoDto> findAll() {
        return todoRepository.streamAll()
                .map(this::toDto)
                .toList();
    }

    @GET
    @Path("/{id}")
    public Response findById(@PathParam("id") Integer id) {
        return todoRepository.findByIdOptional(id)
                .map(this::toDto)
                .map(Response::ok)
                .orElse(Response.status(Response.Status.NOT_FOUND))
                .build();
    }

    @POST
    public Response create(Todo todo) {
        todo.setId(null);
        if (todo.getCompleted() == null) {
            todo.setCompleted(Boolean.FALSE);
        }
        todoRepository.persist(todo);

        var uri = UriBuilder.fromUri("/todos/{id}")
                .build(todo.getId());

        return Response.created(uri).entity(toDto(todo)).build();
    }

    @PUT
    @Path("/{id}")
    @Transactional
    public Response update(@PathParam("id") Integer id, Todo todo) {
        return todoRepository.findByIdOptional(id)
                .map(existing -> {
                    existing.setUserId(todo.getUserId());
                    existing.setTitle(todo.getTitle());
                    existing.setCompleted(todo.getCompleted() != null ? todo.getCompleted() : existing.getCompleted());
                    todoRepository.persist(existing);
                    return Response.ok(toDto(existing)).build();
                })
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public Response delete(@PathParam("id") Integer id) {
        boolean deleted = todoRepository.deleteById(id);
        if (deleted) {
            return Response.noContent().build();
        }
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    private TodoDto toDto(Todo todo) {
        String userName = "unknown";
        try {
            var user = usersClient.getUserById(todo.getUserId());
            if (user != null && user.getName() != null) {
                userName = user.getName();
            }
        } catch (Exception ignored) {
        }

        return TodoDto.builder()
                .id(todo.getId())
                .userId(todo.getUserId())
                .title(todo.getTitle())
                .completed(todo.getCompleted())
                .userName(userName)
                .build();
    }
}
