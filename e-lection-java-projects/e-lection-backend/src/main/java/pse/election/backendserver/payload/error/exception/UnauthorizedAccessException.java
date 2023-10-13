package pse.election.backendserver.payload.error.exception;

/**
 * This is thrown in case a user tries to access an election without the assigned permissions to do
 * so.
 *
 * @version 1.0
 */
public class UnauthorizedAccessException extends Exception {

  /**
   * Constructor of new UnauthorizedAccessException.
   *
   * @param message is the error message
   */
  public UnauthorizedAccessException(String message) {
    super(message);
  }
}
