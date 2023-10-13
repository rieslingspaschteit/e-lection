package pse.election.backendserver.controller;

import pse.election.backendserver.payload.response.UserInformationResponse;

/**
 * This interface provides methods for all user information requests. The user is the currently
 * logged-in and authenticated user.
 *
 * @version 1.0
 */
public interface ElectionUserAPI {

  /**
   * Getter all roles a user has been assigned in the system. The user is referenced by his email
   *
   * @param email email of this user to get the roles
   * @return api response containing all the user's roles
   */
  UserInformationResponse getUserRoles(String email);
}
