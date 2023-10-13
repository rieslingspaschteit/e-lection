package pse.election.backendserver.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.math.BigInteger;

/**
 * Each trustee uploads a partial decryption to each spoiled ballot and to each tallied option.
 *
 * @version 1.0
 */
@Entity
public class PartialDecryption {

  /**
   * This is the primary key.
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long partialDecryptionId;

  /**
   * This is the foreign key to the tally.
   */
  private long tallyId;

  /**
   * This is the foreign key to the trustee.
   */
  private long trusteeId;

  /**
   * This is the foreign key to the ballot.
   */
  private long optionEncryptedId;

  @Column(columnDefinition = "TEXT")
  private String decryption;

  /**
   * Format: pad;data;challenge;response.
   */
  @Column(columnDefinition = "TEXT")
  private String proof;

  public PartialDecryption() {
  }

  public PartialDecryption(String decryption) {
    this.decryption = decryption;
  }

  /**
   * Constructor of new PartialDecryption.
   * */
  public PartialDecryption(long tallyId, long trusteeId, long optionEncryptedId, String decryption,
      String proof) {
    this.tallyId = tallyId;
    this.trusteeId = trusteeId;
    this.decryption = decryption;
    this.proof = proof;
    this.optionEncryptedId = optionEncryptedId;
  }

  public PartialDecryption(String decryption, long trusteeId) {
    this.trusteeId = trusteeId;
    this.decryption = decryption;
  }

  /**
   * Getter for the primary key of a partial decryption.
   *
   * @return identifier
   */
  public long getPartialDecryptionId() {
    return this.partialDecryptionId;
  }

  /**
   * Getter for the foreign key of a tally.
   *
   * @return identifier
   */
  public long getTallyId() {
    return this.tallyId;
  }

  /**
   * Getter for the foreign key of a trustee.
   *
   * @return identifier
   */
  public long getTrusteeId() {
    return this.trusteeId;
  }

  public void setTrusteeId(long trusteeId) {
    this.trusteeId = trusteeId;
  }

  /**
   * Getter for the partial decryption.
   *
   * @return {@link String} decryption
   */
  public BigInteger getDecryption() {
    return new BigInteger(decryption, 16);
  }

  public void setDecryption(String decryption) {
    this.decryption = decryption;
  }

  /**
   * Getter for the foreign key of a ballot.
   *
   * @return identifier
   */
  public long getOptionEncryptedId() {
    return this.optionEncryptedId;
  }

  /**
   * Getter for the proof of the partial decryption.
   *
   * @return {@link String} proof
   */
  public String getProof() {
    return this.proof;
  }
}
