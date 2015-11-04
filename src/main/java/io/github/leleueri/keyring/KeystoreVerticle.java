package io.github.leleueri.keyring;

import io.github.leleueri.keyring.bean.SecretKey;
import io.github.leleueri.keyring.provider.KeystoreProvider;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.logging.LoggerFactory;
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
public class KeystoreVerticle extends AbstractVerticle {

    private KeystoreProvider provider;

    public static final String LIST_ALIASES = "keystore.list.aliases";
    public static final String LIST_SECRET_KEYS = "keystore.list.keys";
    public static final String GET_SECRET_KEY = "keystore.get.key";

    @Override
    public void start(Future<Void> fut) throws Exception {

        String type = config().getString("keystore.type", "JKS");
        String path = config().getString("keystore.path", "keyring.jks");
        String pwd = config().getString("keystore.password");
        String keypwd = config().getString("secretkey.password");

        provider = new KeystoreProvider(type, pwd, path, keypwd);

        // register this Verticle as consumer of keystore events
        // - list all secret key aliases
        vertx.eventBus().consumer(LIST_ALIASES, message -> {
            System.out.println("[Worker] list all aliases " + Thread.currentThread().getName());
            Set<String> aliases = provider.listAlias();
            if (aliases.isEmpty()) {
                message.reply(null);
            } else {
                message.reply(Json.encodePrettily(aliases)); // TODO to reply with an object we must have a message codec
            }
        });
        // - list all secret keys
        vertx.eventBus().consumer(LIST_SECRET_KEYS, message -> {
            System.out.println("[Worker] list all keys " + Thread.currentThread().getName());
            Map<String, SecretKey> keys = provider.listSecretKeys();
            if (keys.isEmpty()) {
                message.reply(null);
            } else {
                message.reply(Json.encodePrettily(keys)); // TODO to reply with an object we must have a message codec
            }
        });
        // - Get a secret key description
        vertx.eventBus().consumer(GET_SECRET_KEY, message -> {
            System.out.println("[Worker] get a key " + Thread.currentThread().getName());
            Optional<SecretKey> key = provider.getSecretKey((String) message.body());
            if (key.isPresent()) {
                message.reply(Json.encodePrettily(key.get())); // TODO to reply with an object we must have a message codec
            } else {
                message.reply(null);
            }
        });
    }
}
