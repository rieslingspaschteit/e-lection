package pse.election.backendserver.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

/**
 * This class is the open id config provider.
 * */
public class OidcConfigProvider {

  private static final String CLIENT_ID = "client-id";
  private static final String CLIENT_SECRET = "client-secret";
  private static final String CLIENT_AUTHENTICATION_METHOD = "client-authentication-method";
  private static final String AUTHORIZATION_GRANT_TYPE = "authorization-grant-type";
  private static final String AUTHORIZATION_URI = "authorization-uri";
  private static final String REDIRECT_URI = "redirect-uri";
  private static final String SCOPE = "scope";
  private static final String TOKEN_URI = "token-uri";
  private static final String USER_INFO_URI = "user-info-uri";
  private static final String JWK_SET_URI = "jwk-set-uri";
  private static final String USER_NAME_ATTRIBUTE_NAME = "user-name-attribute-name";
  private static final String ISSUER_URI = "issuer-uri";
  private static final String REDIRECT_URI_TEMPLATE = "{baseUrl}/%s/{registrationId}";
  private static final String EXPECTED_FILE_TYPE = ".toml";
  private static final String EMAIL_CLAIM = "email-claim";

  private final List<ClientRegistration> clientRegistrations;

  private final Map<String, String> customEmailClaims;

  private final TomlMapper mapper;

  private final String oidcClientConfigFilePath;

  private final String proxyServer;

  /**
   * Constructor of new OidcConfigProvider.
   * */
  public OidcConfigProvider(String path, String proxyServer) throws IOException {
    this.proxyServer = proxyServer;
    this.oidcClientConfigFilePath = path;
    this.mapper = new TomlMapper();
    final Map<String, Object> fileContent = parseFile();

    this.clientRegistrations = fileContent.entrySet().stream()
        .map(this::parseClient)
        .toList();

    this.customEmailClaims = new HashMap<>();
    fileContent.forEach((key, value) -> {
      final String customEmailClaim = this.parseCustomEmailClaim(value);
      if (customEmailClaim != null) {
        this.customEmailClaims.put(key, customEmailClaim);
      }
    });
  }

  private Map<String, Object> parseFile() throws IOException {
    if (oidcClientConfigFilePath == null) {
      return Collections.emptyMap();
    }
    final File oidcClientsConfig = new File(oidcClientConfigFilePath);
    assert oidcClientsConfig.exists();
    assert oidcClientsConfig.isFile();
    assert oidcClientsConfig.canRead();
    assert oidcClientsConfig.getName().endsWith(EXPECTED_FILE_TYPE);
    return mapper.readValue(oidcClientsConfig, new TypeReference<>() {
    });
  }


  // content of file is specified by the readme, if the content is not as expected, the application might fail.
  @SuppressWarnings("unchecked")
  private ClientRegistration parseClient(Entry<String, Object> client) {
    final Map<String, Object> clientConfig = (Map<String, Object>) client.getValue();
    String redirectUri = String.format(REDIRECT_URI_TEMPLATE, clientConfig.get(REDIRECT_URI));
    if (proxyServer != null) {
      redirectUri = redirectUri.replace("{baseUrl}", proxyServer);
    }
    return ClientRegistration.withRegistrationId(client.getKey())
        .clientId(clientConfig.get(CLIENT_ID).toString())
        .clientSecret(clientConfig.get(CLIENT_SECRET).toString())
        .clientAuthenticationMethod(
            new ClientAuthenticationMethod(
                clientConfig.get(CLIENT_AUTHENTICATION_METHOD).toString()))
        .authorizationGrantType(
            new AuthorizationGrantType(clientConfig.get(AUTHORIZATION_GRANT_TYPE).toString()))
        .authorizationUri(clientConfig.get(AUTHORIZATION_URI).toString())
        .redirectUri(redirectUri)
        .scope((List<String>) clientConfig.get(SCOPE))
        .tokenUri(clientConfig.get(TOKEN_URI).toString())
        .userInfoUri(clientConfig.get(USER_INFO_URI).toString())
        .jwkSetUri(clientConfig.get(JWK_SET_URI).toString())
        .issuerUri(clientConfig.get(ISSUER_URI).toString())
        .userNameAttributeName(clientConfig.get(USER_NAME_ATTRIBUTE_NAME).toString())
        .build();
  }

  @SuppressWarnings("unchecked")
  private String parseCustomEmailClaim(Object client) {
    final Map<String, Object> clientConfig = (Map<String, Object>) client;
    final Object emailClaim = clientConfig.get(EMAIL_CLAIM);
    return emailClaim != null ? emailClaim.toString() : null;
  }

  public List<ClientRegistration> getClientRegistrations() {
    return this.clientRegistrations;
  }

  public Map<String, String> getCustomEmailClaims() {
    return this.customEmailClaims;
  }
}
