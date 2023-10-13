package pse.election.backendserver.payload.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigInteger;

/**
 * This class is used to translate chaum pederson proofs from a request into a java object.
 *
 * @version 1.0
 */
public class ChaumPedersenProofRequest {

  @JsonProperty("pad")
  private BigInteger pad;

  @JsonProperty("data")
  private BigInteger data;

  @JsonProperty("challenge")
  private BigInteger challenge;

  @JsonProperty("response")
  private BigInteger response;

  /**
   * Getter for the padding of the encryption. The pad is g^b for generator g and random number b.
   *
   * @return padding of the encryption
   */
  public BigInteger getPad() {
    return pad;
  }

  /**
   * Setter for the padding of the encryption. The pad is g^b for generator g and random number b.
   *
   * @param pad is the padding of the encryption
   */
  public void setPad(String pad) {
    this.pad = new BigInteger(pad, 16);
  }

  /**
   * Getter for the DATA of the encrypted option. The DATA is m * x^b with message m and public key
   * x.
   *
   * @return data of the encrypted option
   */
  public BigInteger getData() {
    return data;
  }

  /**
   * Setter for the DATA of the encrypted option. The DATA is m * x^b with message m and public key
   * x.
   */
  public void setData(String data) {
    this.data = new BigInteger(data, 16);
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
   * @param response of the proof
   */
  public void setResponse(String response) {
    this.response = new BigInteger(response, 16);
  }
}