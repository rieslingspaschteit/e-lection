package pse.election.backendserver.payload.response.state;

import pse.election.backendserver.core.state.ElectionState;

/**
 * This class implements the key ceremony state formatter.
 * */
public class KeyCeremonyStateFormatter implements StateFormatter {

  @Override
  public String formatElectionState(ElectionState electionState) {
    return switch (electionState) {
      case AUX_KEYS -> "AUX_KEYS";
      case EPKB -> "EPKB";
      default -> "FINISHED";
    };
  }
}
