package io.github.leleueri.keyring;

import io.github.leleueri.keyring.bean.SecretKey;
import io.github.leleueri.keyring.provider.KeystoreProvider;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static io.github.leleueri.keyring.KeystoreVerticle.*;

/**
 * Created by eric on 07/10/15.
 */
public class KeyringVerticle extends AbstractVerticle {

    private KeystoreProvider provider;

    @java.lang.Override
    public void start(Future<java.lang.Void> fut) throws Exception {
        // Retrieve the port from the configuration,
        // default to 8080.

        // to launch the application taking a configuration file use :
        // java -jar target/my-first-app-1.0-SNAPSHOT-fat.jar -conf src/main/conf/my-application-conf.json
        Integer port = config().getInteger("http.port", 8080);

         // Create a router object.
        Router router = Router.router(vertx);

        // Bind "/" to our hello message - so we are still compatible.
        router.route("/").handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            response
                    .putHeader("content-type", "text/html")
                    .end("<h1>Hello from my first Vert.x 3 application</h1>");
        });

        // Serve static resources from the /assets directory
        router.route("/assets/*").handler(StaticHandler.create("assets"));

        router.route("/keyring/aliases").handler(this::getAliases);
        router.route("/keyring/secret-keys").handler(this::getAllKeys);
        router.route("/keyring/secret-key/:alias").handler(this::getKey);


        // Define keystore verticle
        // Acording to the configuration a worker is never executed concurrently by Vert.x by more than one thread,
        // but can executed by different threads at different times.
        vertx.deployVerticle("io.github.leleueri.keyring.KeystoreVerticle",
                new DeploymentOptions()
                        .setWorker(true) // as worker (like an actor so no concurrent acces on it!)
                        .setConfig(config())// provides the config object to this new verticle
        );

        // Create the HTTP server and pass the "accept" method to the request handler.
        vertx
                .createHttpServer()
                .requestHandler(router::accept)
                .listen(port, result -> {
                    if (result.succeeded()) {
                        fut.complete();
                    } else {
                        fut.fail(result.cause());
                    }
                });
    }

    public void getAliases(RoutingContext routingContext) {

        vertx.eventBus().send(
                LIST_ALIASES,
                "",
                new DeliveryOptions().setSendTimeout(60000), // TODO configure timeout and manage failures
                r -> {
                    System.out.println("[Main] getAllAliases Receiving reply in " + Thread.currentThread().getName());
                    String aliases = (String)r.result().body();
                    if (aliases == null || aliases.isEmpty()) {
                        routingContext.response().setStatusCode(204).end();
                    } else {
                        routingContext.response()
                                .putHeader("content-type", "application/json; charset=utf-8")
                                .end(aliases);
                    }
                }
        );

        /*
        vertx.executeBlocking(future -> {
            // Call some blocking API that takes a significant amount of time to return
            future.complete(provider.listAlias());
        }, res -> {
            final Set<String> result = (Set<String>)res.result();
            if (result.isEmpty()) {
                routingContext.response().setStatusCode(204).end();
            } else {
                routingContext.response()
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .end(Json.encodePrettily(result));
            }
        });
        */

        /*
        // blocking way (because Keystore action may be blocking)
        routingContext.response()
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(provider.listAlias()));
         */
    }

    public void getAllKeys(RoutingContext routingContext) {

        vertx.eventBus().send(
                LIST_SECRET_KEYS,
                "",
                new DeliveryOptions().setSendTimeout(60000), // TODO configure timeout and manage failures
                r -> {
                    System.out.println("[Main] getAllKeys Receiving reply in " + Thread.currentThread().getName());
                    String secretKeys = (String)r.result().body();
                    if (secretKeys == null || secretKeys.isEmpty()) {
                        routingContext.response().setStatusCode(204).end();
                    } else {
                        routingContext.response()
                                .putHeader("content-type", "application/json; charset=utf-8")
                                .end(secretKeys);
                    }
                }
        );
    }

    public void getKey(RoutingContext routingContext) {
        Optional<String> aliasParam = Optional.ofNullable(routingContext.request().getParam("alias"));
        if (aliasParam.isPresent()) {
            vertx.eventBus().send(
                    GET_SECRET_KEY,
                    aliasParam.get(),
                    new DeliveryOptions().setSendTimeout(60000), // TODO configure timeout and manage failures
                    r -> {
                        System.out.println("[Main] getKey Receiving reply in " + Thread.currentThread().getName());
                        String secretKey = (String) r.result().body();
                        if (secretKey == null || secretKey.isEmpty()) {

                            routingContext.response().setStatusCode(404).end();
                            } else {
                            routingContext.response()
                                .putHeader("content-type", "application/json; charset=utf-8")
                                .end(secretKey);
                            }
                        }
            );
        }else {
            routingContext.response().setStatusCode(400).end();
        }
    }
}
