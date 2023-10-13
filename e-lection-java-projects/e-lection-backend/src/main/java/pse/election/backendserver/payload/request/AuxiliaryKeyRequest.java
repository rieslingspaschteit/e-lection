package pse.election.backendserver.payload.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;

/**
 * This class is used for translating a public auxiliary key upload into an object which can be
 * managed in the system. The auxiliary key is used for encrypting information and thus the type of
 * the key can vary.
 *
 * @version 1.0
 */
public class AuxiliaryKeyRequest {

  @JsonProperty("publicKey")
  @NotNull
  private String publicKey;

  @JsonProperty("keyType")
  @NotNull
  private String keyType;

  /**
   * Getter for the public auxiliary key translated from the request.
   *
   * @return the public auxiliary key from the request
   */
  public String getPublicKey() {
    return publicKey;
  }

  /**
   * Setter for the public auxiliary key from the request.
   *
   * @param publicKey is the public key from the request
   */
  public void setPublicKey(String publicKey) {
    this.publicKey = publicKey;
  }

  /**
   * Getter for the type of the key. This is needed for the encryption and decryption process.
   *
   * @return type of the key
   */
  public String getKeyType() {
    return keyType;
  }

  /**
   * Setter for the type of the key mapped from the request.
   *
   * @param keyType is the type of the key
   */
  public void setKeyType(String keyType) {
    this.keyType = keyType;
  }
}
