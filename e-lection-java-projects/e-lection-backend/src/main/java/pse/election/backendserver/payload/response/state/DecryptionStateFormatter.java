package pse.election.backendserver.payload.response.state;

import pse.election.backendserver.core.state.ElectionState;

/**
 * This class implements the state formatter for the decryption.
 * */
public class DecryptionStateFormatter implements StateFormatter {

  @Override
  public String formatElectionState(ElectionState electionState) {
    return switch (electionState) {
      case EPKB, AUX_KEYS, OPEN -> "NOT STARTED";
      case P_DECRYPTION -> "P_DECRYPTION";
      case PP_DECRYPTION -> "PP_DECRYPTION";
      default -> "FINISHED";
    };
  }
}
