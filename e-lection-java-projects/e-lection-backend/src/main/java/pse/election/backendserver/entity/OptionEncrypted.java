package pse.election.backendserver.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.math.BigInteger;

/**
 * When one speaks of an encrypted ballot, basically only every option of the ballot is encrypted.
 * This set of encrypted options then make the Encryption of a ballot. An option is either selected,
 * which is marked as a 1, or not selected, which is marked as a 0. So an encrypted option is
 * actually an encryption of either 0 or 1, depending on the selection made for that option.
 *
 * @version 1.0
 */

@Entity
public class OptionEncrypted implements Comparable<OptionEncrypted> {

  private static final int BASE_OF_HEX = 16;

  /**
   * This is the primary key.
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long optionEncryptedId;

  private long electionId;
  private int contestIndex;
  private int optionIndex;

  @Column(columnDefinition = "TEXT")
  private String ciphertextPAD;
  @Column(columnDefinition = "TEXT")
  private String ciphertextDATA;

  /**
   * This is the foreign key to the ballot.
   */
  private long ballotId;

  /**
   * This demonstrates whether the option is selected or not to showcase the decrypted spoiled
   * ballot result.
   */
  private boolean selectionMade;

  /**
   * This proof consists of two proofs: 1) proof that the option is either correctly encrpyted as 0
   * 2) proof that the option is correctly encrypted as 1. Each of those proofs consists of a PAD,
   * DATA, CHALLENGE and RESPONSE part. Those parts are encoded in a single String in the following
   * format: PAD;DATA;CHALLENGE;RESPONSE|PAD;DATA;CHALLENGE;RESPONSE|CHALLENGE The part before | is
   * the proof of 0. The part in between | is the proof of 1. The part after | is the global
   * challenge.
   */
  @Column(columnDefinition = "TEXT")
  private String individualProof;

  /**
   * This is a proof, which states that the added up options are equal to a constant. This proof
   * consists of a PAD, DATA, CHALLENGE, RESPONSE and CONSTANT part. Those parts are encoded in a
   * single String in the following format: PAD;DATA;CHALLENGE;RESPONSE;CONSTANT
   */
  @Column(columnDefinition = "TEXT")
  private String accumulatedProof;

  public OptionEncrypted() {
  }

  /**
   * Constructor of new OptionEncrypted.
   * */
  public OptionEncrypted(long ballotId, long electionId, int optionIndex, int contestIndex,
      String ciphertextPAD,
      String ciphertextDATA, String individualProof, String accumulatedProof) {
    this.electionId = electionId;
    this.contestIndex = contestIndex;
    this.optionIndex = optionIndex;
    this.ciphertextPAD = ciphertextPAD;
    this.ciphertextDATA = ciphertextDATA;
    this.ballotId = ballotId;
    this.individualProof = individualProof;
    this.accumulatedProof = accumulatedProof;
  }

  /**
   * Getter for primary key.
   *
   * @return {@link long} identifier
   */
  public long getOptionEncryptedId() {
    return this.optionEncryptedId;
  }

  public long getElectionId() {
    return electionId;
  }

  public int getOptionIndex() {
    return optionIndex;
  }

  public int getContestIndex() {
    return contestIndex;
  }

  /**
   * Getter for the homomorphic encryption PAD of a encrypted option.
   *
   * @return {@link BigInteger} encryptiom
   */
  public BigInteger getCiphertextPAD() {
    return new BigInteger(this.ciphertextPAD, BASE_OF_HEX);
  }

  /**
   * Getter for the homomorphic encryption DATA of a encrypted option.
   */
  public BigInteger getCiphertextDATA() {
    return new BigInteger(this.ciphertextDATA, BASE_OF_HEX);
  }

  /**
   * Getter for the foreign key to the ballot.
   *
   * @return {@link long} identifier
   */
  public long getBallotId() {
    return this.ballotId;
  }

  /**
   * Getter for the individual proof of a ciphertext. For the encoded format see
   * {@link OptionEncrypted#individualProof}.
   *
   * @return {@link String} individual proof
   */
  public String getIndividualProof() {
    return individualProof;
  }

  /**
   * Getter for the accumulated proof of a ciphertext. For the encoded format see
   * {@link OptionEncrypted#accumulatedProof}.
   *
   * @return {@link String} accumulated proof
   */
  public String getAccumulatedProof() {
    return accumulatedProof;
  }

  /**
   * Getter for the decrypted option being made. This is required due to spoiled ballots being
   * stored as cleartext, if the decrpytion phase has been finished.
   *
   * @return true in case the selection has been made
   */
  public boolean isSelectionMade() {
    return selectionMade;
  }

  /**
   * Setter for the decrypted value of the option.
   *
   * @param selectionMade is the value to set for
   */
  public void setSelectionMade(boolean selectionMade) {
    this.selectionMade = selectionMade;
  }


  @Override
  public int compareTo(OptionEncrypted o) {
    if (this.contestIndex > o.getContestIndex()) {
      return 1;
    } else if (this.contestIndex == o.getContestIndex()) {
      if (this.optionIndex > o.getOptionIndex()) {
        return 1;
      } else if (this.optionIndex == o.getOptionIndex()) {
        return 0;
      }
      return -1;
    }
    return -1;
  }


}
