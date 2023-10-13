# e-lection Backend Server

## Build Jar from source

Assuming you are in the root of the projects, `è-lection-java-projects`, run
```shell
./gradlew bootJar
```

## Starting the Jar

```shell
java -jar path/to/e-lection-backend-jar.jar \ 
  --oidcClients=path/to/clientsConfig.toml \
  --authorityConfig=path/to/authorities.txt \
  --dataSource=path/to/dataSource.toml \
  --frontendOrigin=http://url.of.the.frontend \
  --proxyServer=http://url.of.the.proxyServer
```

## Configuration

### Oidc-Clients

For configuring the OpenID-Connect provider a .toml configuration file must be provided.
The following configuration schema is expected by the application:

```toml
[client_name]
client-id = "the client id"
client-secret = "the client secret"
client-authentication-method = "client_secret_basic"
authorization-grant-type = "authorization_code"
redirect-uri = "login/oauth2/code"
scope = ["openid", "another scope"]
authorization-uri = "https://example.provider.org/auth/uri"
token-uri = "https://example.provider.org/token/uri"
user-info-uri = "https://example.provider.org/user-info/uri"
jwk-set-uri = "https://example.provider.org/jwk-set/uri"
issuer-uri = "https://example.provider.org"
user-name-attribute-name = "the username attribute name"
```

The configuration can contain multiple Oidc Providers.

**Addition Configuration and default Values:**

+ _email-Claim_: if the OpenId-Connect provider does not use the OpenId Connect standard claim-name
  for the email-claim
  the custom email-claim should be provided with the key ``email-claim``
+ _scope_: Some Oidc Providers like Google do not serve the required email attribute within their '
  openid' scope.
  In this case the scope must contain an addition 'email' entry
+ _authentication-method and authorization-grant-type_: The example values are recommended,
  for other possible values the behaviour of the application is not tested
+ _redirect-uri_: the redirect-uri should start with '/login'
  since the ui clients expect this url prefix for authentication

### Authorities

For configuring the authorities for the e-lection Application a .txt file can be provided containing
the email for each authority in a new line.

**Example authorities.txt**

```text
some.authority@mail.org
some.other.authority@mail.org
```

### Database

This configuration is optional. The Server defaults to using an in-memory H2 Database that holds data while the server is running. :warning: When the Server stops all data will get lost :warning:

This application supports the Oracle MySql Database or Databases with an equivalent interface. In order to use your own Database you must provide a configuration `.toml`-file that provides at least a username and password for the Database. The Server expects the Database to be called `e_lection_db`. You can specify your own name by overwriting the `url` attribute. The port on which the database listens can also be specified there. The default is 3306.

**Example dataSource.toml**

```toml
username = "user123"
password = "password"
url = "jdbc:mysql://localhost:1234/my_database"
```

### Frontend Origin
If the Frontend-Application is served by another web-server the origin of the application needs to be specified via the flag `--frontendOrigin=<the_origin>`

### Proxy Server
If the Backend-Server is installed behind a Proxy Web-Server the origin of the Web-Server needs to be specified via the flat `--proxyServer=<the_origin>`

## Development

// TODO: edit section

### Running and Testing

#### With Gradle

#### With Intellij

