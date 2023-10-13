package pse.election.backendserver.payload.error.exception;

/**
 * This exception is thrown in case a record format could not be found.
 *
 * @version 1.0
 */
public class RecordFormatNotFoundException extends IllegalArgumentException {

  /**
   * Constructor of new RecordNotFoundException.
   *
   * @param message is the error message
   */
  public RecordFormatNotFoundException(String message) {
    super(message);
  }
}
