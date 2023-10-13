package pse.election.backendserver.security.web.oidc;

import java.util.Map;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;

/**
 * This class is used to extract the email from the OpenID Provider response. It generally stores
 * all information from the response but the default extraction fails if the email claim key is not
 * as specified by the OpenID-Connect standard.
 *
 * @version 1.0
 */
public class CustomOidcUserInfo extends OidcUserInfo {

  private final String customEmailClaim;

  /**
   * Constructs a {@code OidcUserInfo} using the provided parameters.
   *
   * @param claims           the claims about the authentication of the End-User
   * @param customEmailClaim the email claim key for the identity provider
   */
  public CustomOidcUserInfo(Map<String, Object> claims, String customEmailClaim) {
    super(claims);
    this.customEmailClaim = customEmailClaim;
  }

  @Override
  public String getEmail() {
    return (String) getClaims().get(customEmailClaim);
  }
}
