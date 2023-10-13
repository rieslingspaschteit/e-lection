package pse.election.backendserver.security;

import pse.election.backendserver.security.user.UserRole;

/**
 * This interface provides methods for validation of authorized access. In case a user wants to
 * access an election, this interface provides the ability to validate if the user has the role in
 * that election for that specific access.
 *
 * @version 1.0
 */
public interface ElectionAuthorizationEvaluator {

  /**
   * Returns if the requesting user has a certain role in the API.
   *
   * @param userRole is the role to be checked for
   * @return true in case the user has been assigned the parsed role, otherwise false
   */
  boolean hasRole(UserRole userRole);

  /**
   * Returns if the requesting user has a certain role assigned to an election referenced by an id.
   *
   * @param electionId is the election to look for
   * @param userRole   is the role of the user to check for in the referenced election
   * @return true in case the user has been assigned the parsed role to the referenced election, otherwise false.
   */
  boolean hasRoleInElection(UserRole userRole, Long electionId);

  /**
   * Returns if the requesting user has any role assigned to an election referenced by an id.
   *
   * @param electionId is the election to be checked for
   * @return true in case the user has been assigned any role to the referenced election, otherwise false
   */
  boolean isParticipantInElection(Long electionId);

  boolean isParticipantInElection(String fingerprint);

}
