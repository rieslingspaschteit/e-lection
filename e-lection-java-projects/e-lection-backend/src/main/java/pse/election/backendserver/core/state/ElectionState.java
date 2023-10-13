package pse.election.backendserver.core.state;

/**
 * This enum contains the states of an election.
 *
 * @version 1.0
 */
public enum ElectionState {

  /**
   * election is in the first key ceremony state. Every trustee has to upload an auxiliary key.
   */
  AUX_KEYS,

  /**
   * election is in the second key ceremony state. Every Trustee has to upload enough elgamal keys
   * and partial key backups.
   */
  EPKB,

  /**
   * election has finished with key ceremony and can begin with collecting votes.
   */
  KEYCEREMONY_FINISHED,

  /**
   * election is in state open. Ballots can be sent and submitted.
   */
  OPEN,

  /**
   * election is in the partial decryption state. A threshold of trustees has to upload a partial
   * decryption
   */
  P_DECRYPTION,

  /**
   * election is in the partial-partial decryption state.
   */
  PP_DECRYPTION,

  /**
   * election has finished. Result has been decrypted
   */
  DONE
}
