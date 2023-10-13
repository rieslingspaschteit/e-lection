package pse.election.backendserver.payload.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigInteger;

/**
 * This class is used to cast a proof from the request into an object that can be managed in the
 * system. It contains information such as the public key and a challenge.
 *
 * @version 1.0
 */
public class SchnorrProofRequest {

  @JsonProperty("publicKey")
  private BigInteger publicKey;

  @JsonProperty("commitment")
  private BigInteger commitment;

  @JsonProperty("challenge")
  private BigInteger challenge;

  @JsonProperty("response")
  private BigInteger response;

  /**
   * Getter for the public key of the proof.
   *
   * @return public key of the proof
   */
  public BigInteger getPublicKey() {
    return publicKey;
  }

  /**
   * Setter of the public key of the proof.
   *
   * @param publicKey is the public key from the request
   */
  public void setPublicKey(String publicKey) {
    this.publicKey = new BigInteger(publicKey, 16);
  }

  /**
   * Getter for the commitment of the proof.
   *
   * @return the commitment of the proof from the request
   */
  public BigInteger getCommitment() {
    return commitment;
  }

  /**
   * Setter for the commitment of the proof.
   *
   * @param commitment is the commitment from the request
   */
  public void setCommitment(String commitment) {
    this.commitment = new BigInteger(commitment, 16);
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
   * @param challenge is the challenge of the proof
   */
  public void setChallenge(String challenge) {
    this.challenge = new BigInteger(challenge, 16);
  }

  /**
   * Getter for the response of the proof.
   *
   * @return response of the proof
   */
  public BigInteger getResponse() {
    return response;
  }

  /**
   * Setter for the response of the proof.
   *
   * @param response is the response from the request
   */
  public void setResponse(String response) {
    this.response = new BigInteger(response, 16);
  }
}

