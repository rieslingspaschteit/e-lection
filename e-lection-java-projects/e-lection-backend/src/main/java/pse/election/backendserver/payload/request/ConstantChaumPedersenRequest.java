package pse.election.backendserver.payload.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigInteger;

/**
 * This class is needed to translate the constant chaum pederson from the request into a java
 * object.
 *
 * @version 1.0
 */
public class ConstantChaumPedersenRequest {

  @JsonProperty("pad")
  private BigInteger pad;

  @JsonProperty("data")
  private BigInteger data;

  @JsonProperty("challenge")
  private BigInteger challenge;

  @JsonProperty("response")
  private BigInteger response;

  @JsonProperty("constant")
  private int constant;

  /**
   * Getter for the constant of the proof.
   *
   * @return constant of the proof
   */
  public int getConstant() {
    return constant;
  }

  /**
   * Setter for the constant of the proof.
   *
   * @param constant of the proof
   */
  public void setConstant(int constant) {
    this.constant = constant;
  }

  public BigInteger getPad() {
    return pad;
  }

  public void setPad(String pad) {
    this.pad = new BigInteger(pad, 16);
  }

  public BigInteger getData() {
    return data;
  }

  public void setData(String data) {
    this.data = new BigInteger(data, 16);
  }

  public BigInteger getChallenge() {
    return challenge;
  }

  public void setChallenge(String challenge) {
    this.challenge = new BigInteger(challenge, 16);
  }

  public BigInteger getResponse() {
    return response;
  }

  public void setResponse(String response) {
    this.response = new BigInteger(response, 16);
  }
}