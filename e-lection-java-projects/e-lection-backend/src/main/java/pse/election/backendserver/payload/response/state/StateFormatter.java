package pse.election.backendserver.payload.response.state;

import pse.election.backendserver.core.state.ElectionState;

/**
 * This provides methods for string state formatting.
 * */
public interface StateFormatter {

  /**
   * Formatter of the state to a string.
   * */
  String formatElectionState(ElectionState electionState);
}
