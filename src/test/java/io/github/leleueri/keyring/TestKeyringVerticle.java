package io.github.leleueri.keyring;

import io.github.leleueri.keyring.bean.SecretKey;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.*;
import org.junit.runner.RunWith;

import javax.crypto.KeyGenerator;
import java.net.ServerSocket;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;

import static io.github.leleueri.keyring.ConfigConstants.*;
/**
 * Created by eric on 07/10/15.
 */
@RunWith(VertxUnitRunner.class)
public class TestKeyringVerticle {

    private Vertx vertx;

    private Integer port = 8081;
    private HttpClient httpClient;

    /**
     * So, the idea is very simple. We open a server socket that would pick a random port
     * (that’s why we put 0 as parameter). We retrieve the used port and close the socket.
     * Be aware that this method is not perfect and may fail if the picked port becomes used
     * between the close method and the start of our HTTP server. However, it would work fine
     * in the very high majority of the case.
     * (source : http://vertx.io/blog/vert-x-application-configuration/ )
     * @throws Exception
     */
    public void reservePort() throws Exception{
        ServerSocket socket = new ServerSocket(0);
        port = socket.getLocalPort();
        socket.close();
    }


    @Before
    public void setUp(TestContext context) throws Exception {
        reservePort();

        // deploy the vertx with a custom configuration
        final String path = "target/KeyringKeystore.jceks";
        Paths.get(path).toFile().delete();

        DeploymentOptions options = new DeploymentOptions()
                .setConfig(new JsonObject().put("http.port", port)
                        .put(APP_KEYSTORE_PATH, path)
                        .put(APP_KEYSTORE_PWD, "simplemotdepasse")
                        .put(APP_KEYSTORE_SECRET_KEY_PWD, "simplemotdepassecle"));

        vertx = Vertx.vertx();
        vertx.deployVerticle(KeyringVerticle.class.getName(), options, context.asyncAssertSuccess());

        httpClient = vertx.createHttpClient();
    }

    @After
    public void tearDown(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void testEmptyAliases(TestContext context) {
        final Async async = context.async();
        httpClient.getNow(port, "localhost", "/keyring/aliases", response -> {
            context.assertEquals(204, response.statusCode());
            async.complete();
        });
    }

    @Test
    public void testEmptyKeys(TestContext context) {
        final Async async = context.async();
        httpClient.getNow(port, "localhost", "/keyring/secret-keys", response -> {
            context.assertEquals(204, response.statusCode());
            async.complete();
        });
    }

    @Test
    public void testUnknownKey(TestContext context) {
        final Async async = context.async();
        httpClient.getNow(port, "localhost", "/keyring/secret-key/invalide", response -> {
            context.assertEquals(404, response.statusCode());
            async.complete();
        });
    }

    @Test
    public void testDeleteUnknownKey(TestContext context) {
        final Async async = context.async();
        httpClient.delete(port, "localhost", "/keyring/secret-key/invalid", response -> {
            context.assertEquals(204, response.statusCode());
            async.complete();
        }).end();
    }

    @Test
    public void testAllInOne(TestContext context) throws Exception {
        final Async asyncPost = context.async();

        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128);
        javax.crypto.SecretKey secretKey = keyGen.generateKey();

        final SecretKey key = new SecretKey();
        key.setAlgorithm("AES");
        key.setAlias("cle1"+System.currentTimeMillis());
        key.setFormat(secretKey.getFormat());
        key.setB64Key(new String(Base64.getEncoder().encode(secretKey.getEncoded())));

        httpClient.post(port, "localhost", "/keyring/secret-keys").putHeader("Content-Type", "application/json")
                .setChunked(true)
                .handler(response -> {
                    context.assertEquals(201, response.statusCode());
                    asyncPost.complete();
                }).end(Json.encodePrettily(key));

        asyncPost.awaitSuccess();

        final Async asyncReadAliases = context.async();
        httpClient.getNow(port, "localhost", "/keyring/aliases", response -> {
            context.assertEquals(200, response.statusCode());
            response.bodyHandler(body -> {
                final ArrayList aliases = Json.decodeValue(body.toString(), ArrayList.class);
                context.assertTrue(aliases.contains(key.getAlias()));
                asyncReadAliases.complete();
            });
        });

        asyncReadAliases.awaitSuccess();

        final Async asyncReadKeys = context.async();
        httpClient.getNow(port, "localhost", "/keyring/secret-keys", response -> {
            context.assertEquals(200, response.statusCode());
            asyncReadKeys.complete();
        });

        asyncReadKeys.awaitSuccess();

        final Async asyncReadKey = context.async();
        httpClient.getNow(port, "localhost", "/keyring/secret-key/"+key.getAlias(), response -> {
            context.assertEquals(200, response.statusCode());
            response.bodyHandler(body -> {
                final SecretKey readKey = Json.decodeValue(body.toString(), SecretKey.class);
                context.assertEquals(key.getAlgorithm(), readKey.getAlgorithm());
                context.assertEquals(key.getAlias(), readKey.getAlias());
                //context.assertEquals(key.getFormat(), readKey.getFormat());
                context.assertTrue(Arrays.equals(Base64.getDecoder().decode(key.getB64Key().getBytes()), Base64.getDecoder().decode(readKey.getB64Key().getBytes())));
                asyncReadKey.complete();
            });
        });

        asyncReadKey.awaitSuccess();

        final Async asyncDeleteAlias = context.async();
        httpClient.delete(port, "localhost", "/keyring/secret-key/" + key.getAlias(), response -> {
            context.assertEquals(204, response.statusCode());
            asyncDeleteAlias.complete();
        }).end();

        asyncDeleteAlias.awaitSuccess();

        final Async asyncReadAliases2 = context.async();
        httpClient.getNow(port, "localhost", "/keyring/secret-key/" + key.getAlias(), response -> {
            context.assertEquals(404, response.statusCode());
            asyncReadAliases2.complete();
        });
    }

}
