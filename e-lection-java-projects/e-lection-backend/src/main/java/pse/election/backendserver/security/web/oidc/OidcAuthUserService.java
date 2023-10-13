package pse.election.backendserver.security.web.oidc;

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import pse.election.backendserver.security.user.UserPrincipal;
import pse.election.backendserver.utils.OidcConfigProvider;

/**
 * This class is used to create a UserPrincipal {@link UserPrincipal} object, that has the email of
 * the currently logged-in user as the name attribute. The UserPrincipal is than stored in the
 * security context and can be used to authorize access to protected resources such as elections.
 *
 * @version 1.0
 */
@Service
public class OidcAuthUserService extends OidcUserService {

  private final OidcConfigProvider oidcConfigProvider;

  public OidcAuthUserService(OidcConfigProvider oidcConfigProvider) {
    this.oidcConfigProvider = oidcConfigProvider;
  }

  @Override
  public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
    OidcUser user = super.loadUser(userRequest);

    OidcUserInfo oidcUserInfo = new OidcUserInfoFactory(oidcConfigProvider).getOidcUserInfo(
        userRequest.getClientRegistration().getRegistrationId(),
        user.getAttributes()
    );

    return new UserPrincipal(user.getAuthorities(), userRequest.getIdToken(), oidcUserInfo);
  }
}
