package pse.election.backendserver.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * In case a trustee did not perform its supposed partial decryption, all remaining trustees have to
 * perfom a partial partial decryption on that missing partial decryption. This can be done with the
 * assistance of the {@link Trustee} election partial key backups.
 *
 * @version 1.0
 */
@Entity
public class PartialPartialDecryption {

  /**
   * This is the primary key.
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long partialPartialDecryptionId;

  /**
   * This is the foreign key of a trustee.
   */
  private long trusteeId;

  /**
   * This is the trustee identifier from whom the partial partial decryption was made.
   */
  private long forWhichTrusteeId;

  /**
   * This is the foreign key of a tally.
   */
  private long tallyId;

  /**
   * This is the foreign key of an encrypted option.
   */
  private long optionEncryptedId;

  @Column(columnDefinition = "TEXT")
  private String decryption;

  /**
   * Proof that the partial partial decryption is a correct decryption.
   */
  @Column(columnDefinition = "TEXT")
  private String proof;

  public PartialPartialDecryption() {
  }

  /**
   * Constructor of new PartialPartialDecryption.
   * */
  public PartialPartialDecryption(long trusteeId, long forWhichTrusteeId, long tallyId,
      long optionEncryptedId, String decryption, String proof) {
    this.trusteeId = trusteeId;
    this.tallyId = tallyId;
    this.optionEncryptedId = optionEncryptedId;
    this.decryption = decryption;
    this.proof = proof;
    this.forWhichTrusteeId = forWhichTrusteeId;
  }

  /**
   * Getter for the primary key of a partial partial decryption.
   *
   * @return identifier
   */
  public long getPartialPartialDecryptionId() {
    return this.partialPartialDecryptionId;
  }

  /**
   * Getter for the foreign key of a trustee.
   *
   * @return identifier
   */
  public long getTrusteeId() {
    return this.trusteeId;
  }

  /**
   * Getter for the foreign key of a tallied option.
   *
   * @return identifier
   */
  public long getTallyId() {
    return this.tallyId;
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
   * Getter for the partial partial decryption.
   *
   * @return {@link String} decryption
   */
  public String getDecryption() {
    return this.decryption;
  }

  /**
   * Getter for the proof of the partial partial decryption.
   *
   * @return {@link String} proof
   */
  public String getProof() {
    return this.proof;
  }

  /**
   * Getter for the trustee identifier of the trustee for whom the partial partial decryption was
   * made.
   *
   * @return trustee identifier
   */
  public long getForWhichTrusteeId() {
    return this.forWhichTrusteeId;
  }
}
