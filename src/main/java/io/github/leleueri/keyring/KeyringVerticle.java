package io.github.leleueri.keyring;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;

import java.util.Arrays;
import java.util.List;

/**
 * Created by eric on 07/10/15.
 */
public class KeyringVerticle extends AbstractVerticle{

    private KeystoreProvider provider = new KeystoreProvider();

    @java.lang.Override
    public void start(Future<java.lang.Void> fut) throws Exception {
        // Retrieve the port from the configuration,
        // default to 8080.

        // to launch the application taking a configuration file use :
        // java -jar target/my-first-app-1.0-SNAPSHOT-fat.jar -conf src/main/conf/my-application-conf.json
        Integer port = config().getInteger("http.port", 8080);

        String type = config().getString("keystore.type", "JKS");
        String path = config().getString("keystore.path", "JKS");
        String pwd = config().getString("keystore.password");
        String keypwd = config().getString("secretkey.password");
        provider.loadKeystore(type, pwd, path, keypwd);

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

        router.route("/keyring/secret-keys").handler(this::getAllKeyName);


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

    public void getAllKeyName(RoutingContext routingContext) {
        routingContext.response()
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(provider.listSecretKeys()));
    }
}
