package pse.election.backendserver.payload.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigInteger;

/**
 * This nested class is used to translate the disjunctive chaum pederson proof from the request.
 *
 * @version 1.0
 */
public class DisjunctiveChaumPedersenRequest {

  @JsonProperty("proof0")
  private ChaumPedersenProofRequest proof0;

  @JsonProperty("proof1")
  private ChaumPedersenProofRequest proof1;

  @JsonProperty("challenge")
  private BigInteger challenge;

  /**
   * Getter for the zero proofs of the ballot from the request.
   *
   * @return proofs of the zeros in the ballot
   */
  public ChaumPedersenProofRequest getProof0() {
    return proof0;
  }

  /**
   * Setter for the zero proofs of the ballot from the request.
   *
   * @param proof0 is the zero proof of the ballot
   */
  public void setProof0(ChaumPedersenProofRequest proof0) {
    this.proof0 = proof0;
  }

  /**
   * Getter for the ones' proofs of the proof from the request.
   *
   * @return proofs of the zeros in the proof
   */
  public ChaumPedersenProofRequest getProof1() {
    return proof1;
  }

  /**
   * Setter for the ones' proofs of the proof from the request.
   *
   * @param proof1 is the proofs of ones in the proof
   */
  public void setProof1(ChaumPedersenProofRequest proof1) {
    this.proof1 = proof1;
  }

  /**
   * Getter for the challenge of the proof.
   *
   * @return challenge of the proof
   */
  public BigInteger getChallenge() {
    return challenge;
  }

  /**
   * Setter for the challenge of the proof.
   *
   * @param challenge of the proof
   */
  public void setChallenge(String challenge) {
    this.challenge = new BigInteger(challenge, 16);
  }
}
