package pse.election.backendserver.utils;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

class OidcConfigReaderTest {

  @Test
  void readClients() throws IOException {

    final File oidcClientConfigFile = new File("src/test/resources/utils/oidcClientConf.toml");

    assertTrue(oidcClientConfigFile.exists());
    assertTrue(oidcClientConfigFile.canRead());

    Collection<ClientRegistration> clients = new OidcConfigProvider(oidcClientConfigFile.getPath(),
        null)
        .getClientRegistrations();

    assertEquals(1, clients.size());

    ClientRegistration client = clients.iterator().next();

    assertAll(() -> {
      assertEquals("my_client", client.getClientName());
      assertEquals("my_client_id", client.getClientId());
      assertEquals("my_client_secret", client.getClientSecret());
      assertEquals(ClientAuthenticationMethod.CLIENT_SECRET_BASIC,
          client.getClientAuthenticationMethod());
      assertEquals(AuthorizationGrantType.AUTHORIZATION_CODE, client.getAuthorizationGrantType());
      assertEquals("{baseUrl}/my/redirect/uri/{registrationId}", client.getRedirectUri());
      assertEquals("my/authorization/uri", client.getProviderDetails().getAuthorizationUri());
      assertEquals(Set.of("openid"), client.getScopes());
      assertEquals("my/token/uri", client.getProviderDetails().getTokenUri());
      assertEquals("my/user-info/uri", client.getProviderDetails().getUserInfoEndpoint().getUri());
      assertEquals("my/jwk-set/uri", client.getProviderDetails().getJwkSetUri());
      assertEquals("my/issuer/uri", client.getProviderDetails().getIssuerUri());
      assertEquals("sub",
          client.getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName());
    });

  }
}