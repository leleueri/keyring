package io.github.leleueri.keyring;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.ServerSocket;

/**
 * Created by eric on 07/10/15.
 */
@RunWith(VertxUnitRunner.class)
public class TestKeyringVerticle {

    private Vertx vertx;

    private Integer port = 8081;

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
        DeploymentOptions options = new DeploymentOptions().setConfig(new JsonObject().put("http.port", port)
                    .put("keystore.path", "KeyringKeystore")
                .put("keystore.type", "JKS")
                .put("keystore.password", "simplemotdepasse")
                .put("secretkey.password", "simplemotdepassecle"));

        vertx = Vertx.vertx();
        vertx.deployVerticle(KeyringVerticle.class.getName(), options, context.asyncAssertSuccess());
    }

    @After
    public void tearDown(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void testMyApplication(TestContext context) {
        final Async async = context.async();

        vertx.createHttpClient().getNow(port, "localhost", "/",
                response -> {
                    response.handler(body -> {
                        context.assertTrue(body.toString().contains("Hello"));
                        async.complete();
                    });
                });
    }

}
