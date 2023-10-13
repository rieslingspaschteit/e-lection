package pse.election.backendserver.security.user;

/**
 * This enum contains all user roles in the API. User roles define permissions to certain
 * functionalities and are granted by the HOST or an election creation.
 *
 * @version 1.0
 */
public enum UserRole {
  /**
   * User Role Trustee provides permission to participate in key ceremony and election decryption.
   */
  TRUSTEE,
  /**
   * User Role Voter provides permission to participate in assigned elections.
   */
  VOTER,

  /**
   * User Role Authority provides permission to election creation and management.
   */
  AUTHORITY
}
