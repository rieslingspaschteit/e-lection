package pse.election.backendserver.payload.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is used for translation of the partial decryption requests into an object that can be
 * managed in the system. It takes in the partial decryption of the spoiled and submitted ballots.
 *
 * @version 1.0
 */
public class DecryptionRequest {

  @JsonProperty("partialDecryptedSpoiledBallots")
  private Map<Integer, PartialSpoiledBallotDecryptionRequest[]> partialDecryptedSpoiledBallots;

  @JsonProperty("partialDecryptedTally")
  private Map<Integer, PartialTallyDecryptionRequest> partialDecryptedTalliedBallots;

  /**
   * Getter for the partial decrypted spoiled ballots.
   *
   * @return partial decrypted spoiled ballots from the request
   */
  public Map<Integer, PartialSpoiledBallotDecryptionRequest[]> getPartialDecryptedSpoiledBallots() {
    return partialDecryptedSpoiledBallots;
  }

  /**
   * Setter for the partial decrypted spoiled ballots from the request.
   *
   * @param partialDecryptedSpoiledBallots are the partial decrypted spoiled ballots from the
   *                                       request
   */
  public void setPartialDecryptedSpoiledBallots(
      Map<Integer, PartialSpoiledBallotDecryptionRequest[]> partialDecryptedSpoiledBallots) {
    this.partialDecryptedSpoiledBallots = partialDecryptedSpoiledBallots;
  }

  /**
   * Getter for the partial decrypted ballots that have been submitted.
   *
   * @return the partial decrypted submitted ballots
   */
  public Map<Integer, PartialTallyDecryptionRequest> getPartialDecryptedTalliedBallots() {
    return partialDecryptedTalliedBallots;
  }

  /**
   * Setter for the partial decrypted submitted ballots from teh request.
   *
   * @param partialDecryptedTalliedBallots are the partial decrypted submitted ballots
   */
  public void setPartialDecryptedTalliedBallots(
      Map<Integer, PartialTallyDecryptionRequest> partialDecryptedTalliedBallots) {
    this.partialDecryptedTalliedBallots = partialDecryptedTalliedBallots;
  }

  /**
   * This nested class is used to translate partial decryptions from the request into a java
   * object.
   *
   * @version 1.0
   */
  public static class PartialSpoiledBallotDecryptionRequest {

    private final Converter requestConverter = new Converter();

    @JsonProperty("partialDecryption")
    private Map<Integer, BigInteger[]> partialDecryptedOptions;
    private Long ballotId;

    @JsonProperty("proofs")
    private Map<Integer, ChaumPedersenProofRequest[]> chaumPedersonProofs;

    /**
     * Getter for the partial decrypted options. Each question has an index referencing his
     * options.
     *
     * @return partial decrypted options of the questions
     */
    public Map<Integer, BigInteger[]> getPartialDecryptedOptions() {
      return partialDecryptedOptions;
    }

    /**
     * Setter for the partial decrypted options.
     *
     * @param partialDecryptedOptions are the partial decrypted options of a ballot.
     */
    public void setPartialDecryptedOptions(Map<Integer, String[]> partialDecryptedOptions) {
      this.partialDecryptedOptions = requestConverter.convertStringsToBigInteger(
          partialDecryptedOptions);
    }

    /**
     * Getter for the chaum pederson proofs.
     *
     * @return a map that maps indexes of questions to the chaum pederson proofs of its options.
     */
    public Map<Integer, ChaumPedersenProofRequest[]> getChaumPedersonProofs() {
      return chaumPedersonProofs;
    }

    /**
     * Setter for the chaum pederson proofs from the request.
     *
     * @param chaumPedersonProofs are the chaum pederson proofs.
     */
    public void setChaumPedersonProofs(
        Map<Integer, ChaumPedersenProofRequest[]> chaumPedersonProofs) {
      this.chaumPedersonProofs = chaumPedersonProofs;
    }

    public Long getBallotId() {
      return ballotId;
    }

    public void setBallotId(Long ballotId) {
      this.ballotId = ballotId;
    }
  }


  /**
   * This nested class is used to translate partial decryptions from the request into a java
   * object.
   *
   * @version 1.0
   */
  public static class PartialTallyDecryptionRequest {

    private final Converter requestConverter = new Converter();
    @JsonProperty("partialDecryption")
    private Map<Integer, BigInteger[]> partialDecryptedOptions;

    @JsonProperty("proofs")
    private Map<Integer, ChaumPedersenProofRequest[]> chaumPedersonProofs;

    /**
     * Getter for the partial decrypted options. Each question has an index referencing his
     * options.
     *
     * @return partial decrypted options of the questions
     */
    public Map<Integer, BigInteger[]> getPartialDecryptedOptions() {
      return partialDecryptedOptions;
    }

    /**
     * Setter for the partial decrypted options.
     *
     * @param partialDecryptedOptions are the partial decrypted options of a ballot.
     */
    public void setPartialDecryptedOptions(Map<Integer, String[]> partialDecryptedOptions) {
      this.partialDecryptedOptions = requestConverter.convertStringsToBigInteger(
          partialDecryptedOptions);
    }

    /**
     * Getter for the chaum pederson proofs.
     *
     * @return a map that maps indexes of questions to the chaum pederson proofs of its options.
     */
    public Map<Integer, ChaumPedersenProofRequest[]> getChaumPedersonProofs() {
      return chaumPedersonProofs;
    }

    /**
     * Setter for the chaum pederson proofs from the request.
     *
     * @param chaumPedersonProofs are the chaum pederson proofs.
     */
    public void setChaumPedersonProofs(
        Map<Integer, ChaumPedersenProofRequest[]> chaumPedersonProofs) {
      this.chaumPedersonProofs = chaumPedersonProofs;
    }
  }

  private static class Converter {

    private static final int BASE_OF_HEX = 16;

    public Map<Integer, BigInteger[]> convertStringsToBigInteger(
        Map<Integer, String[]> partialDecryptedOptions) {
      Map<Integer, BigInteger[]> parsedFromRequest = new LinkedHashMap<>();
      for (Integer key : partialDecryptedOptions.keySet()) {
        List<BigInteger> list = new ArrayList<>();
        for (String string : partialDecryptedOptions.get(key)) {
          list.add(new BigInteger(string, BASE_OF_HEX));
        }
        parsedFromRequest.put(key, list.toArray(BigInteger[]::new));
      }

      return parsedFromRequest;
    }
  }


}
