package pse.election.backendserver.payload.error.exception;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import pse.election.backendserver.security.web.oidc.OidcUserInfoFactory;

/**
 * Thrown by the {@link OidcUserInfoFactory} if no authentication provider could be found that
 * supports the presented authentication object.
 *
 * @version 1.0
 */
public class ProviderNotFoundException extends OAuth2AuthenticationException {

  /**
   * Constructor of new ProviderNotFoundException.
   */
  public ProviderNotFoundException(String errorCode) {
    super(errorCode);
  }
}
