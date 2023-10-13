package pse.election.backendserver.payload.error.exception;

/**
 * This is thrown in case a new created election has the same title as another election that already
 * exists. As the titles of an election are unique, this exception will be thrown.
 *
 * @version 1.0
 */
public class IdentityConflictException extends InvalidConfigurationException {

  /**
   * Constructor of new IdentityConflictException.
   *
   * @param message is the error message
   */
  public IdentityConflictException(String message) {
    super(message);
  }
}
