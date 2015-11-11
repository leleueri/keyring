# keyring
REST service to access Symmetric Keys in a Keystore.

Usage :

  java -jar target/keyring-0.1.0-SNAPSHOT-fat.jar -conf src/main/resources/application.conf

Configuration :

  "http.port" : default 8080,
  "process.timeout" : default 10000, the maximum duration of a request
  "ssl" : default false, the server must activate the TLS/SSL
  "ssl.keystore.type" : default "JKS", the type of Keystore managed for the SSL/TLS (JKS or P12)
  "ssl.keystore.path" : required if the ssl is set to true
  "ssl.keystore.password" : required if the ssl is set to true
  "ssl.client.authentication" : default false, true if the client must be authenticated
  "ssl.truststore.type" : default "JKS", the type of Truststore managed for the client authentication (JKS or P12)
  "ssl.truststore.path" : required if the client authentication is set to true
  "ssl.truststore.password" :  required if the client authentication is set to true
  "app.keystore.path" : default "keyring.jceks", the path to the keystore that contains keys received through the REST interface
  "app.keystore.type" : default "JCEKS", the type of Keystore managed to store keys received through the REST interface (ONLY JCEKS is managed)
  "app.keystore.password" : the password of the keystore
  "app.keystore.secretkey.password" : the password used for each symmetric key
