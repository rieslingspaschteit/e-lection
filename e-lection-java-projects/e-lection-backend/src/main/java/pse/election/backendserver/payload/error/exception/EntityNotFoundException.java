package pse.election.backendserver.payload.error.exception;

/**
 * Thrown by a service if no referenced entity could be found.
 *
 * @version 1.0
 */
public class EntityNotFoundException extends IllegalArgumentException {

  /**
   * Constructor of new EntityNotFoundException.
   *
   * @param message is the error message
   */
  public EntityNotFoundException(String message) {
    super(message);
  }
}
