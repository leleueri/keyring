package io.github.leleueri.keyring;

import io.github.leleueri.keyring.bean.SecretKey;
import io.github.leleueri.keyring.provider.KeystoreProvider;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;

import javax.swing.text.html.Option;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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

        String type = config().getString("keystore.type", "JKS");
        String path = config().getString("keystore.path", "keyring.jks");
        String pwd = config().getString("keystore.password");
        String keypwd = config().getString("secretkey.password");

        provider = new KeystoreProvider(type, pwd, path, keypwd); // TODO check if the Provider is unique (may have several instance if the Keyring verticle is loaded more than once)

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

        router.route("/keyring/aliases").handler(this::getAllKeyNames);
        router.route("/keyring/secret-keys").handler(this::getAllKeys);
        router.route("/keyring/secret-key/:alias").handler(this::getAllKeys);


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

    public void getAllKeyNames(RoutingContext routingContext) {
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

        /*

        // blocking way (because Keystore action may be blocking)
        routingContext.response()
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(provider.listAlias()));
         */
    }

    public void getAllKeys(RoutingContext routingContext) {
        // TODO try worker verticle to handle blocking action
        vertx.executeBlocking(future -> {
            future.complete(provider.listSecretKeys());
        }, res -> {
            final Map<String, SecretKey> result = (Map<String, SecretKey> )res.result();
            if (result.isEmpty()) {
                routingContext.response().setStatusCode(204).end();
            } else {
                routingContext.response()
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .end(Json.encodePrettily(result));
            }
        });

    }

    public void getKey(RoutingContext routingContext) {
        Optional<String> aliasParam = Optional.of(routingContext.request().getParam("id"));
        if (aliasParam.isPresent()) {
            vertx.executeBlocking(future -> {
                future.complete(provider.getSecretKey(aliasParam.get()));
            }, res -> {
                Optional<SecretKey> result = (Optional<SecretKey>)res.result();
                if (result.isPresent()) {
                    routingContext.response()
                            .putHeader("content-type", "application/json; charset=utf-8")
                            .end(Json.encodePrettily(res.result()));
                } else {
                    routingContext.response().setStatusCode(404).end();
                }
            });
        } else {
            routingContext.response().setStatusCode(400).end();
        }
    }
}
