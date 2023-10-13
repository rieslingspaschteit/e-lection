package pse.election.backendserver.payload.response;

import java.util.List;
import java.util.Map;

/**
 * This class is the ballot response.
 * */
public record BallotResponse(Map<Integer, Question> questions) {

  /**
   * This class is the question response.
   * */
  public record Question(String questionText, List<String> options, int max) {

  }
}
