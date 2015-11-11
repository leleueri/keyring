package io.github.leleueri.keyring;

import io.github.leleueri.keyring.exception.KeyringConfigurationException;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.eventbus.ReplyFailure;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.Json;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.PfxOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static io.github.leleueri.keyring.ConfigConstants.SERVER_SSL_TRUSTSTORE_PWD;
import static io.github.leleueri.keyring.ConfigConstants.SERVER_SSL_TRUSTSTORE_PATH;
import static io.github.leleueri.keyring.KeystoreVerticle.*;

import static io.github.leleueri.keyring.ConfigConstants.*;
/**
 * Created by eric on 07/10/15.
 */
public class KeyringVerticle extends AbstractVerticle {

    private int processingTimeOut;

    @java.lang.Override
    public void start(Future<java.lang.Void> fut) throws Exception {
        // to launch the application taking a configuration file use :
        // java -jar target/my-first-app-1.0-SNAPSHOT-fat.jar -conf src/main/conf/my-application-conf.json

        // Retrieve the port from the configuration,
        // default to 8080.
        Integer port = config().getInteger(SERVER_HTTP_PORT, SERVER_HTTP_DEFAULT_PORT);
        // default processing timeOut
        processingTimeOut = config().getInteger(SERVER_PROCESS_TIMEOUT, SERVER_DEFAULT_PROCESS_TIMEOUT);

         // Create a router object.
        Router router = Router.router(vertx);

        /* // HTTP method may be sepcified (by default, all methods match)
        router.route("/keyring/aliases").method(HttpMethod.GET).handler(this::getAliases);
        router.route("/keyring/secret-keys").method(HttpMethod.GET).handler(this::getAllKeys);
        router.route("/keyring/secret-key/:alias").method(HttpMethod.GET).handler(this::getKey);
        router.route("/keyring/secret-key/:alias").method(HttpMethod.PUT).handler(this::getKey);
        */

        // Http method may also be specified with a well named "route" method ...
        // Routes are matched in the order of they addition into the route...
        router.route().handler(BodyHandler.create());
        router.get("/keyring/aliases").produces("application/json").handler(this::getAliases);
        router.get("/keyring/secret-keys").produces("application/json").handler(this::getAllKeys);
        router.get("/keyring/secret-key/:alias").produces("application/json").handler(this::getKey);
        router.delete("/keyring/secret-key/:alias").produces("application/json").handler(this::deleteKey);
        router.post("/keyring/secret-keys").consumes("application/json").handler(this::putKey);

        // Define keystore verticle
        // Acording to the configuration a worker is never executed concurrently by Vert.x by more than one thread,
        // but can executed by different threads at different times.
        vertx.deployVerticle("io.github.leleueri.keyring.KeystoreVerticle",
                new DeploymentOptions()
                        .setWorker(true) // as worker (like an actor so no concurrent acces on it!)
                        .setConfig(config())// provides the config object to this new verticle
        );

        HttpServerOptions httpOptions = getHttpServerOptions();
        // Create the HTTP server and pass the "accept" method to the request handler.
        vertx
            .createHttpServer(httpOptions)
            .requestHandler(router::accept)
            .listen(port, result -> {
                if (result.succeeded()) {
                    fut.complete();
                } else {
                    fut.fail(result.cause());
                }
            });
    }

    private HttpServerOptions getHttpServerOptions() {
        HttpServerOptions httpOptions = null;
        final boolean useSSL = config().getBoolean(SERVER_USE_SSL, false);
        if (useSSL) {
            httpOptions = new HttpServerOptions().setSsl(true);

            final String keystoreType = config().getString(SERVER_SSL_KEYSTORE_TYPE);
            switch (keystoreType) {
                case SERVER_SSL_TYPE_JKS:
                    httpOptions.setKeyStoreOptions(new JksOptions().
                        setPath(config().getString(SERVER_SSL_KEYSTORE_PATH)).
                            setPassword(config().getString(SERVER_SSL_KEYSTORE_PWD)));
                    break;
                case SERVER_SSL_TYPE_PKCS12:
                    httpOptions.setPfxKeyCertOptions(new PfxOptions().
                            setPath(config().getString(SERVER_SSL_KEYSTORE_PATH)).
                            setPassword(config().getString(SERVER_SSL_KEYSTORE_PWD)));
                    break;
                default:
                    throw new KeyringConfigurationException("SSL Keystore type is invalid, expected JKS or P12");
            }

            final boolean clientAuth = config().getBoolean(SERVER_SSL_CLIENT_AUTH, false);
            if (clientAuth) {
                httpOptions.setClientAuth(ClientAuth.REQUIRED);

                final String truststoreType = config().getString(SERVER_SSL_TRUSTSTORE_TYPE);
                switch (truststoreType) {
                    case SERVER_SSL_TYPE_JKS:
                        httpOptions.setTrustStoreOptions(new JksOptions().
                                setPath(config().getString(SERVER_SSL_TRUSTSTORE_PATH)).
                                setPassword(config().getString(SERVER_SSL_TRUSTSTORE_PWD)));
                        break;
                    case SERVER_SSL_TYPE_PKCS12:
                        httpOptions.setPfxTrustOptions(new PfxOptions().
                                setPath(config().getString(SERVER_SSL_TRUSTSTORE_PATH)).
                                setPassword(config().getString(SERVER_SSL_TRUSTSTORE_PWD)));
                        break;
                    default:
                        throw new KeyringConfigurationException("SSL truststore type is invalid, expected JKS or P12");
                }
            }
        } else {
            httpOptions = new HttpServerOptions();
        }
        return httpOptions;
    }

    public void getAliases(RoutingContext routingContext) {

        vertx.eventBus().send(
                LIST_ALIASES,
                "",
                new DeliveryOptions().setSendTimeout(processingTimeOut),
                r -> {
                    System.out.println("[Main] getAllAliases Receiving reply in " + Thread.currentThread().getName());
                    if (r.succeeded()) {
                        String aliases = (String) r.result().body();
                        if (aliases == null || aliases.isEmpty()) {
                            routingContext.response().setStatusCode(204).end();
                        } else {
                            routingContext.response()
                                    .putHeader("content-type", "application/json; charset=utf-8")
                                    .end(aliases);
                        }
                    } else {
                        // on failure, the resultHander contains a Throwable accessible through "cause" method
                        manageFailedResult(routingContext, r);
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

    private void manageFailedResult(RoutingContext routingContext, AsyncResult<Message<Object>> r) {
        ReplyException replyExc = (ReplyException) r.cause();
        if (replyExc.failureType() == ReplyFailure.TIMEOUT) {
            responseWithError(routingContext, 503, "Server unavailable, retry later");
        } else if (replyExc.failureType() == ReplyFailure.NO_HANDLERS) {
            responseWithError(routingContext, 500, "Internal Server Error");
        }else {
            responseWithError(routingContext, replyExc.failureCode(), replyExc.getMessage());
        }
    }

    private void responseWithError(RoutingContext routingContext, int status, String message) {
        Map<String, String> body = new HashMap<>();
        body.put("message", message);
        routingContext.response().setStatusCode(status)
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(body));
    }

    public void getAllKeys(RoutingContext routingContext) {
        System.out.println("[Main] getAllKeys Received by " + Thread.currentThread().getName());
        vertx.eventBus().send(
                LIST_SECRET_KEYS,
                "",
                new DeliveryOptions().setSendTimeout(processingTimeOut),
                r -> {
                    System.out.println("[Main] getAllKeys Receiving reply in " + Thread.currentThread().getName());
                    if (r.succeeded()) {
                        String secretKeys = (String) r.result().body();
                        if (secretKeys == null || secretKeys.isEmpty()) {
                            routingContext.response().setStatusCode(204).end();
                        } else {
                            routingContext.response()
                                    .putHeader("content-type", "application/json; charset=utf-8")
                                    .end(secretKeys);
                        }
                    } else {
                        // on failure, the resultHander contains a Throwable accessible through "cause" method
                        manageFailedResult(routingContext, r);
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
                    new DeliveryOptions().setSendTimeout(processingTimeOut),
                    r -> {
                        System.out.println("[Main] getKey Receiving reply in " + Thread.currentThread().getName());
                        if (r.succeeded()) {
                            String secretKey = (String) r.result().body();
                            if (secretKey == null || secretKey.isEmpty()) {
                                routingContext.response().setStatusCode(404).end();
                            } else {
                                routingContext.response()
                                        .putHeader("content-type", "application/json; charset=utf-8")
                                        .end(secretKey);
                            }
                        } else {
                            // on failure, the resultHander contains a Throwable accessible through "cause" method
                            manageFailedResult(routingContext, r);
                        }
                    }
            );
        } else {
            routingContext.response().setStatusCode(400).end();
        }
    }

    public void putKey(RoutingContext routingContext) {
        Optional<String> body = Optional.ofNullable(routingContext.getBodyAsString());
        if (body.isPresent()) {
            vertx.eventBus().send(
                    POST_SECRET_KEY,
                    body.get(),
                    new DeliveryOptions().setSendTimeout(processingTimeOut),
                    r -> {
                        System.out.println("[Main] postKey Receiving reply in " + Thread.currentThread().getName());
                        if (r.succeeded()) {
                            String secretKey = (String) r.result().body();
                            routingContext.response().setStatusCode(201)
                                    .putHeader("content-type", "application/json; charset=utf-8")
                                    .putHeader("Location", "/keyring/secret-key/" + secretKey) // TODO how to build location based on a route
                                    .end();
                        } else {
                            // on failure, the resultHander contains a Throwable accessible through "cause" method
                            manageFailedResult(routingContext, r);
                        }
                    }
            );
        } else {
            routingContext.response().setStatusCode(400).end();
        }
    }

    public void deleteKey(RoutingContext routingContext) {
        Optional<String> aliasParam = Optional.ofNullable(routingContext.request().getParam("alias"));
        if (aliasParam.isPresent()) {
            vertx.eventBus().send(
                    DELETE_SECRET_KEY,
                    aliasParam.get(),
                    new DeliveryOptions().setSendTimeout(processingTimeOut),
                    r -> {
                        System.out.println("[Main] deleteKey Receiving reply in " + Thread.currentThread().getName());
                        if (r.succeeded()) {
                            routingContext.response().setStatusCode(204)
                                    .putHeader("content-type", "application/json; charset=utf-8")
                                    .end();
                        } else {
                            // on failure, the resultHander contains a Throwable accessible through "cause" method
                            manageFailedResult(routingContext, r);
                        }
                    }
            );
        } else {
            routingContext.response().setStatusCode(400).end();
        }
    }
}
