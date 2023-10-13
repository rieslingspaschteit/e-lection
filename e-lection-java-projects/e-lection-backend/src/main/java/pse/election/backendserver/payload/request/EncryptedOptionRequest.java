package pse.election.backendserver.payload.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigInteger;

/**
 * This nested class is needed to translate the encrypted options from the request into a java
 * object. It is only needed for the ballot committing process and therefore nested.
 *
 * @version 1.0
 */
public class EncryptedOptionRequest {

  @JsonProperty("pad")
  private BigInteger pad;

  @JsonProperty("data")
  private BigInteger data;

  /**
   * Getter for the padding of the encrypted option. The padding is g^b for generator b and random
   * number b.
   *
   * @return padding of the encrypted option
   */
  public BigInteger getPad() {
    return pad;
  }

  /**
   * Setter for the padding of the encrypted option. The padding is g^b for generator b and random
   * number b.
   *
   * @param pad is the padding of the encrypted option
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
   *
   * @param data of the encrypted option from the request
   */
  public void setData(String data) {
    this.data = new BigInteger(data, 16);
  }

}
