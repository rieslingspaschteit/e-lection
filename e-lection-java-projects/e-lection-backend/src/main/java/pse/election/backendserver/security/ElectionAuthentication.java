package pse.election.backendserver.security;

/**
 * This interface provides the ability to get information of the currently logged-in and
 * authenticated principal. Especially, this determines which user information is needed for the
 * APIs functionality.
 *
 * @version 1.0
 */
public interface ElectionAuthentication {

  /**
   * Getter for the email of the authenticated principal.
   *
   * @return email address of the currently logged-in and authenticated principal. Cannot be empty or null.
   */
  String getAuthenticatedEmail();
}
