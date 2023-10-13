package pse.election.backendserver.security.web.oidc;

import java.util.Map;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import pse.election.backendserver.utils.OidcConfigProvider;

/**
 * This class is used to extract the user information from the OpenID Connect provider user
 * information response. As different providers have a different response-format, this factory is
 * needed for filtering the information depending on the provider.
 *
 * @version 1.0
 */
public class OidcUserInfoFactory {

  private final OidcConfigProvider oidcConfigProvider;

  public OidcUserInfoFactory(OidcConfigProvider oidcConfigProvider) {
    this.oidcConfigProvider = oidcConfigProvider;
  }

  /**
   * This method is used to extract the user information from the OpenID provider user information
   * response and cast it into Spring's {@link OidcUserInfo} user information container.
   *
   * @param registrationId is the id referencing the provider
   * @param attributes     are the attributes from response containing the user information
   * @return user information from the provider response needed in the application
   */
  public OidcUserInfo getOidcUserInfo(String registrationId, Map<String, Object> attributes) {
    final String customEmailClaim = oidcConfigProvider.getCustomEmailClaims().get(registrationId);
    if (customEmailClaim != null) {
      return new CustomOidcUserInfo(attributes, customEmailClaim);
    }
    return new OidcUserInfo(attributes);
  }

}
