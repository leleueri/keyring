package io.github.leleueri.keyring;

import io.github.leleueri.keyring.bean.SecretKey;
import io.github.leleueri.keyring.exception.KeyringApplicativeException;
import io.github.leleueri.keyring.exception.KeyringConfigurationException;
import io.github.leleueri.keyring.provider.KeystoreProvider;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.Json;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

import static io.github.leleueri.keyring.ConfigConstants.*;
/**
 * Created by eric on 07/10/15.
 */
public class KeystoreVerticle extends AbstractVerticle {

    private KeystoreProvider provider;

    private final Logger LOGGER = Logger.getLogger(getClass().getName());

    public static final String LIST_ALIASES = "keystore.list.aliases";
    public static final String LIST_SECRET_KEYS = "keystore.list.keys";
    public static final String GET_SECRET_KEY = "keystore.get.key";
    public static final String DELETE_SECRET_KEY = "keystore.delete.key";
    public static final String POST_SECRET_KEY = "keystore.post.key";

    @Override
    public void start(Future<Void> fut) throws Exception {
        String type = config().getString(APP_KEYSTORE_TYPE, APP_KEYSTORE_DEFAULT_TYPE);
        if (!type.equals(APP_KEYSTORE_DEFAULT_TYPE)) {
            throw new KeyringConfigurationException("Only the "+APP_KEYSTORE_DEFAULT_TYPE+" type is authorized for the keystore '"  + APP_KEYSTORE_TYPE +"'");
        }
        String path = config().getString(APP_KEYSTORE_PATH, APP_KEYSTORE_DEFAULT_PATH);
        String pwd = config().getString(APP_KEYSTORE_PWD);
        String keypwd = config().getString(APP_KEYSTORE_SECRET_KEY_PWD);

        provider = new KeystoreProvider(type, pwd, path, keypwd);

        // register this Verticle as consumer of keystore events
        // - list all secret key aliases
        vertx.eventBus().consumer(LIST_ALIASES, message -> {
            LOGGER.fine("[Worker] list all aliases " + Thread.currentThread().getName());
            try {
                Set<String> aliases = provider.listAlias();
                if (aliases.isEmpty()) {
                    message.reply(null);
                } else {
                    message.reply(Json.encodePrettily(aliases)); // to reply with an object we must have a message codec
                }
            } catch (KeyringApplicativeException e) {
                LOGGER.throwing(getClass().getName(), "Exception on listAllAliases", e);
                message.fail(500, e.getMessage()); // create an error object to return in JSON format??
            }
        });

        // - list all secret keys
        vertx.eventBus().consumer(LIST_SECRET_KEYS, message -> {
            LOGGER.fine("[Worker] list all keys " + Thread.currentThread().getName());
            try {
                Map<String, SecretKey> keys = provider.listSecretKeys();
                if (keys == null || keys.isEmpty()) {
                    message.reply(null);
                } else {
                    message.reply(Json.encodePrettily(keys)); // to reply with an object we must have a message codec
                }
            } catch (KeyringApplicativeException e) {
                LOGGER.throwing(getClass().getName(), "Exception on listAllSecretKeys", e);
                message.fail(500, e.getMessage()); // create an error object to return in JSON format??
            }
        });

        // - Get a secret key description
        vertx.eventBus().consumer(GET_SECRET_KEY, message -> {
            LOGGER.fine("[Worker] get a key " + Thread.currentThread().getName());
            try {
                Optional<SecretKey> key = provider.getSecretKey((String) message.body());
                if (key.isPresent()) {
                    message.reply(Json.encodePrettily(key.get())); // to reply with an object we must have a message codec
                } else {
                    message.reply(null);
                }
            } catch (KeyringApplicativeException e) {
                LOGGER.throwing(getClass().getName(), "Exception on getSecretKey", e);
                message.fail(500, e.getMessage()); // create an error object to return in JSON format??
            }
        });

        // - create a secret key description
        vertx.eventBus().consumer(POST_SECRET_KEY, message -> {
            LOGGER.fine("[Worker] post a key " + Thread.currentThread().getName());
            try {
                Optional<SecretKey> key = Optional.ofNullable((String) message.body()).map(str -> Json.decodeValue(str, SecretKey.class));
                if (key.isPresent()) {

                    final SecretKey secretKey = key.get();
                    if (secretKey.getAlias() == null || secretKey.getAlias().isEmpty()) {
                        message.fail(400, "Alias is missing");
                    }
                    if (secretKey.getB64Key() == null || secretKey.getB64Key().isEmpty()) {
                        message.fail(400, "Key is missing");
                    }
                    if (secretKey.getAlgorithm() == null || secretKey.getAlgorithm().isEmpty()) {
                        message.fail(400, "Algorithm is missing");
                    }

                    if(provider.getSecretKey(secretKey.getAlias()).isPresent()) {
                        message.fail(409, "SecretKey already exists");
                    } else {
                        provider.addSecretKey(secretKey);
                        message.reply(secretKey.getAlias());
                    }
                } else {
                    message.fail(400, "SecretKey is missing from the request body");
                }
            } catch (KeyringApplicativeException e) {
                LOGGER.throwing(getClass().getName(), "Exception on postSecretKey", e);
                message.fail(500, e.getMessage()); // create an error object to return in JSON format??
            }
        });

        // - Delete a secret key description
        vertx.eventBus().consumer(DELETE_SECRET_KEY, message -> {
            LOGGER.fine("[Worker] delete a key " + Thread.currentThread().getName());
            try {
                provider.deleteSecretKey((String) message.body());
                message.reply(null);
            } catch (KeyringApplicativeException e) {
                LOGGER.throwing(getClass().getName(), "Exception on deleteSecretKey", e);
                message.fail(500, e.getMessage()); // create an error object to return in JSON format??
            }
        });

    }
}
