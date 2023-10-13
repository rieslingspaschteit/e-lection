package pse.election.backendserver.payload.error.exception;

/**
 * This exception is thrown in case a proof is illegal.
 *
 * @version 1.0
 */
public class IllegalProofException extends IllegalArgumentException {

  /**
   * Constructor of new IllegalProofException.
   *
   * @param errorMessage is the error message
   */
  public IllegalProofException(String errorMessage) {
    super(errorMessage);
  }
}

