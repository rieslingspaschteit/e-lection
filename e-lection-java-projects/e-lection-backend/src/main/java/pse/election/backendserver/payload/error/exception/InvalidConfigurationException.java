package pse.election.backendserver.payload.error.exception;

/**
 * This is thrown in case an election has been created with an invalid configuration. The validity
 * of an election is defined in the
 * {@link pse.election.backendserver.core.service.ElectionService}.
 *
 * @version 1.0
 */
public class InvalidConfigurationException extends IllegalArgumentException {

  /**
   * Constructor of new InvalidConfigurationException.
   *
   * @param message is the error message
   */
  public InvalidConfigurationException(String message) {
    super(message);
  }
}
