package io.github.leleueri.keyring;

/**
 * Created by eric on 11/11/15.
 */
public interface ConfigConstants {

    String SERVER_HTTP_PORT = "http.port";
    int SERVER_HTTP_DEFAULT_PORT = 8080;
    String SERVER_PROCESS_TIMEOUT = "process.timeout";
    int SERVER_DEFAULT_PROCESS_TIMEOUT = 10_000;

    String SERVER_USE_SSL = "ssl";

    String SERVER_SSL_KEYSTORE_TYPE = "ssl.keystore.type";
    String SERVER_SSL_KEYSTORE_PATH = "ssl.keystore.path";
    String SERVER_SSL_KEYSTORE_PWD = "ssl.keystore.password";

    String SERVER_SSL_CLIENT_AUTH = "ssl.client.authentication";

    String SERVER_SSL_TRUSTSTORE_TYPE = "ssl.truststore.type";
    String SERVER_SSL_TRUSTSTORE_PATH = "ssl.truststore.path";
    String SERVER_SSL_TRUSTSTORE_PWD = "ssl.truststore.password";

    String SERVER_SSL_TYPE_JKS = "JKS";
    String SERVER_SSL_TYPE_PKCS12 = "P12";

    String APP_KEYSTORE_TYPE = "app.keystore.type";
    String APP_KEYSTORE_DEFAULT_TYPE = "JCEKS";
    String APP_KEYSTORE_PATH = "app.keystore.path";
    String APP_KEYSTORE_DEFAULT_PATH = "keyring.jks";
    String APP_KEYSTORE_PWD = "app.keystore.password";
    String APP_KEYSTORE_SECRET_KEY_PWD = "app.keystore.secretkey.password";

}
