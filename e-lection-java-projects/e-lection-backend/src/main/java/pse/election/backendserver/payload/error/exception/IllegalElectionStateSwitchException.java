package pse.election.backendserver.payload.error.exception;


/**
 * This exception gets thrown if state switch has been ordered but the state is not ready yet. This
 * can also be thrown in case the new state can not be the next state.
 *
 * @version 1.0
 */
public class IllegalElectionStateSwitchException extends IllegalArgumentException {

  /**
   * Constructor of new IllegalElectionStateSwitchException.
   *
   * @param message is the error message
   */
  public IllegalElectionStateSwitchException(String message) {
    super(message);
  }
}
