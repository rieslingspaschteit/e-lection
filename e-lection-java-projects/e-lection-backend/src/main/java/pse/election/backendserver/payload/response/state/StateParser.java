package pse.election.backendserver.payload.response.state;

import pse.election.backendserver.core.state.ElectionState;

/**
 * This class is used to parse strings to state enums.
 * */
public class StateParser {

  /**
   * Parsing a string to a state.
   * */
  public ElectionState parse(String state) {

    if (state == null) {
      return null;
    }

    return switch (state) {
      case "AUX_KEYS" -> ElectionState.AUX_KEYS;
      case "EPKB" -> ElectionState.EPKB;
      case "KEYCEREMONY_FINISHED" -> ElectionState.KEYCEREMONY_FINISHED;
      case "OPEN" -> ElectionState.OPEN;
      case "P_DECRYPTION" -> ElectionState.P_DECRYPTION;
      case "PP_DECRYPTION" -> ElectionState.PP_DECRYPTION;
      case "FINISHED" -> ElectionState.DONE;
      default -> null;
    };
  }
}
