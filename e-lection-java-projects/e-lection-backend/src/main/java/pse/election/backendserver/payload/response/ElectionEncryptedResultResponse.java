package pse.election.backendserver.payload.response;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is the encrypted result response.
 * */
public record ElectionEncryptedResultResponse(
    Map<Long, Map<Integer, List<EncryptedOptionResponse>>> encryptedSpoiledBallotQuestions,
    Map<Integer, List<EncryptedOptionResponse>> encryptedTally,
    Map<Integer, String> keyBackups,
    String baseHash
) {


  public ElectionEncryptedResultResponse(
      Map<Long, Map<Integer, List<EncryptedOptionResponse>>> encryptedSpoiledBallotQuestions,
      Map<Integer, List<EncryptedOptionResponse>> encryptedTally, String baseHash) {
    this(encryptedSpoiledBallotQuestions, encryptedTally, new LinkedHashMap<>(), baseHash);
  }

}


