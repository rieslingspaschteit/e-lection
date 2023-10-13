package pse.election.backendserver.security.app;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import pse.election.backendserver.security.ElectionAuthentication;

/**
 * This class implements the {@link ElectionAuthentication} interface and is used as an adapter for
 * the API-own authentication. With this, the API can access authentication information
 * independently of the Web-API and also from other authentication resources. It also filters the
 * required user information needed for the functionalities of user authorization, as only the email
 * of a user is needed, instead of tokens, ids, etc...
 *
 * @version 1.0
 */
@Component
public class ElectionAuthenticationAdapter implements ElectionAuthentication {

  @Override
  public String getAuthenticatedEmail() {
    return SecurityContextHolder.getContext().getAuthentication().getName();
  }
}
