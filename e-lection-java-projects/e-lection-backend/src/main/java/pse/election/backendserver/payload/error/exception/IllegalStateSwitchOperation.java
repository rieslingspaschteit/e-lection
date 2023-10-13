package pse.election.backendserver.payload.error.exception;

/**
 * This class is an exception thrown in case a requested state change is not allowed.
 * */
public class IllegalStateSwitchOperation extends IllegalArgumentException {

  /**
   * Constructor of new IllegalStateSwitchOperation.
   * */
  public IllegalStateSwitchOperation(String message) {
    super(message);
  }
}
