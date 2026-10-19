package com.uce.programacions;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.vertx.core.Vertx;
import io.vertx.ext.consul.CheckOptions;
import io.vertx.ext.consul.ConsulClient;
import io.vertx.ext.consul.ConsulClientOptions;
import io.vertx.ext.consul.ServiceOptions;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.InetAddress;
import java.util.List;

@ApplicationScoped
public class TodosLifecycle {

    @Inject
    @ConfigProperty(name = "consul.host", defaultValue = "127.0.0.1")
    String consulHost;

    @Inject
    @ConfigProperty(name = "consul.port", defaultValue = "8500")
    Integer consulPort;

    @Inject
    @ConfigProperty(name = "quarkus.http.port", defaultValue = "8080")
    Integer appPort;

    @Inject
    @ConfigProperty(name = "app.name", defaultValue = "app-todos")
    String appName;

    @Inject
    @ConfigProperty(name = "app.instance-id", defaultValue = "app-todos")
    String instanceId;

    String serviceId;

    public void init(@Observes StartupEvent event, Vertx vertx) {
        System.out.println(appName + "-lifecycle: init (instance=" + instanceId + ")");

        try {
            ConsulClientOptions options = new ConsulClientOptions()
                    .setHost(consulHost)
                    .setPort(consulPort);

            ConsulClient client = ConsulClient.create(vertx, options);

            String ipAddress = InetAddress.getLocalHost().getHostAddress();
            serviceId = "%s-%s".formatted(appName, instanceId);

            var urlCheck = "http://%s:%d/q/health/live".formatted(ipAddress, appPort);

            CheckOptions checkOptions = new CheckOptions()
                    .setHttp(urlCheck)
                    .setInterval("10s")
                    .setDeregisterAfter("20s");

            var tags = List.of(
                    "traefik.enable=true",
                    "traefik.http.routers.router-app-todos.rule=PathPrefix(`/app-todos`)",
                    "traefik.http.routers.router-app-todos.middlewares=middleware-todos",
                    "traefik.http.middlewares.middleware-todos.stripprefix.prefixes=/app-todos",
                    "traefik.http.services.service-app-todos.loadbalancer.server.port=" + appPort,
                    "instance=" + instanceId
            );

            ServiceOptions serviceOptions = new ServiceOptions()
                    .setName(appName)
                    .setId(serviceId)
                    .setAddress(ipAddress)
                    .setPort(appPort)
                    .setCheckOptions(checkOptions)
                    .setTags(tags);

            client.registerService(serviceOptions)
                    .onSuccess(it -> System.out.printf("%s registrado en Consul con ID %s%n", appName, serviceId))
                    .onFailure(err -> System.out.printf("Error registrando %s en Consul: %s%n", appName, err.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void destroy(@Observes ShutdownEvent event, Vertx vertx) {
        System.out.println(appName + "-lifecycle: destroy (instance=" + instanceId + ")");

        try {
            ConsulClientOptions options = new ConsulClientOptions()
                    .setHost(consulHost)
                    .setPort(consulPort);

            ConsulClient client = ConsulClient.create(vertx, options);

            client.deregisterService(serviceId)
                    .onSuccess(it -> System.out.printf("%s des-registrado de Consul con ID %s%n", appName, serviceId))
                    .onFailure(err -> System.out.printf("Error des-registrando %s de Consul: %s%n", appName, err.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
