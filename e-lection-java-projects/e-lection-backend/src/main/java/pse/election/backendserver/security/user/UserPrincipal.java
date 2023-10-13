package pse.election.backendserver.security.user;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * This class is used for storing the information from an OpenID provider user information response
 * into a java object that can be stored in Spring's security context. It is used in the application
 * for identification of the user in case of requests. This class is used by the
 * {@link pse.election.backendserver.security.web.oidc.OidcUserInfoFactory} to store the email
 * address of the logged-in user as a name attribute. Without this, the {@link Authentication}
 * interface would not return the email of a user when calling {@link  Authentication#getName()}.
 *
 * @version 1.0
 */
public class UserPrincipal implements OidcUser {

  private final OidcIdToken token;
  private final OidcUserInfo userInfo;
  private final Collection<? extends GrantedAuthority> authorities;
  private final String email;

  /**
   * Constructor of new UserPrincipal.
   * */
  public UserPrincipal(Collection<? extends GrantedAuthority> authorities, OidcIdToken idToken,
      OidcUserInfo oidcUserInfo) {
    this.userInfo = oidcUserInfo;
    this.token = idToken;
    this.authorities = authorities;
    this.email = oidcUserInfo.getEmail();
  }

  @Override
  public Map<String, Object> getClaims() {
    return null; //not relevant for the application
  }

  @Override
  public OidcUserInfo getUserInfo() {
    return this.userInfo;
  }

  @Override
  public OidcIdToken getIdToken() {
    return this.token;
  }

  @Override
  public Map<String, Object> getAttributes() {
    return Collections.emptyMap(); // no avoid null-pointer exceptions
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return this.authorities;
  }

  @Override
  public String getName() {
    return this.email;
  }

  /**
   * Getter for the email of the currently logged-in user.
   *
   * @return email of the currently logged-in user
   */
  public String getEmail() {
    return this.email;
  }

}
