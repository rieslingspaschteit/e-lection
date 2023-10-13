package pse.election.backendserver.payload.response;

/**
 * This class is the election meta response.
 * */
public record ElectionMetaResponse(long electionId, ElectionMeta electionMeta, String state,
                                   String fingerprint) {

  /**
   * This class is used to wrap election meta information.
   * */
  public record ElectionMeta(String title, String description, String start, String end,
                             String authority,
                             int threshold,
                             String key, String fingerprint) {

  }

}
