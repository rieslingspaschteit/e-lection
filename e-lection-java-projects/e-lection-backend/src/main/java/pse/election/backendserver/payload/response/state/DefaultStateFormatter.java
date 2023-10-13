package pse.election.backendserver.payload.response.state;

import pse.election.backendserver.core.state.ElectionState;

/**
 * This class implements the basic state formatter.
 * */
public class DefaultStateFormatter implements StateFormatter {

  @Override
  public String formatElectionState(ElectionState electionState) {
    return switch (electionState) {
      case AUX_KEYS, EPKB, KEYCEREMONY_FINISHED -> "KEY_CEREMONY";
      case P_DECRYPTION, PP_DECRYPTION -> "DECRYPTION";
      case DONE -> "FINISHED";
      default -> electionState.toString().toUpperCase();
    };
  }

}
