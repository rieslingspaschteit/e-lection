package pse.election.backendserver.payload.error.exception;

/**
 * This is thrown then a request tries to operate an election that is not in state for such
 * operations.
 *
 * @version 1.0
 */
public class IllegalStateOperationException extends IllegalArgumentException {


  /**
   * Constructor of new IllegalStateOperationException.
   *
   * @param message is the error message
   */
  public IllegalStateOperationException(String message) {
    super(message);
  }
}
