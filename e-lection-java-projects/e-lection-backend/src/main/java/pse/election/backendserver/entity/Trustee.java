package pse.election.backendserver.entity;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A trustee is a person who is determined by the authority of the election. Trustees play an
 * important and central role in the correct conduct of the election. A trustee must perform a
 * partial decryption of the encrypted election result, so that finally, by merging all partial
 * decryptions of all other trustees of the same election, the election result is fully decrypted
 * and can be finally displayed to the voter.
 *
 * @version 1.0
 */

@Entity
public class Trustee implements Comparable<Trustee> {

  /**
   * This is the primary key.
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long trusteeId;

  /**
   * This is the foreign key to the election.
   */
  private long electionId;

  /**
   * Containing the public elgamal keys and its corresponding proofs.
   * Format: order;publicKey;commitment;challenge;response
   */
  @Column(columnDefinition = "LONGTEXT")
  @ElementCollection
  private List<String> publicElgamalKeyAndProof;

  /**
   * This is a collection of election partial key backups, which are required if a trustee decides
   * to not perform a partial decryption.
   * In that case, each of the remaining trustees of the election have to perform a partial
   * decrpytion of the missing partial decryption of the trustee who did not perform his part of the
   * decryption.
   * Therefore each trustee generates for each other trustee a backup, so that the remaining
   * trustees can perform the missing partial decryption.
   * Format: trusteeIndex;backup      trusteeIndex of whom the backup was generated
   */
  @Column(columnDefinition = "LONGTEXT")
  @ElementCollection
  private List<String> backups;
  private String email;

  /**
   * This index represents the order of trustees in a specific election, which is required due to
   * the fact that trustees have to share information with other trustees. It is important to keep
   * track which trustee has shared information with which trustee.
   * This index cannot be 0.
   */
  private int trusteeIndex;

  /**
   * This is the public key whereby trustees have a secure communication among one another.
   */
  @Column(columnDefinition = "TEXT")
  private String auxkey;
  private String auxkeyType;

  /**
   * Is true, if the trustee has done its required work during a specific state of the election.
   */
  private boolean isWaiting;

  /**
   * This is true if the trustee uploaded decryptions and is counted as available trustee.
   */
  private boolean isAvailable = false;

  @Column(columnDefinition = "TEXT")
  private String lagrangeCoefficient;

  public Trustee() {
  }

  public Trustee(String elGamalKey) {

  }

  /**
   * Constructor of new Trustee.
   * */
  public Trustee(long electionId, String email, int trusteeIndex) {
    this.electionId = electionId;
    this.email = email;
    this.trusteeIndex = trusteeIndex;
  }

  /**
   * Getter for the primary key of a trustee.
   *
   * @return {@link long} identifier of trustee
   */
  public long getTrusteeId() {
    return this.trusteeId;
  }

  /**
   * Getter for the foreign key of an election.
   *
   * @return {@link long} identifier of election
   */
  public long getElectionId() {
    return this.electionId;
  }

  /**
   * Getter for the email of a trustee.
   *
   * @return {@link String} of email
   */
  public String getEmail() {
    return this.email;
  }

  /**
   * Getter for the index of a trustee. For more information about an index of a trustee, see
   * {@link Trustee#trusteeIndex}.
   *
   * @return {@link int} of index
   */
  public int getIndex() {
    return this.trusteeIndex;
  }

  /**
   * Getter for the auxiliary public key of a trustee.
   *
   * @return {@link String} of auxiliary public key
   */
  public String getAuxkey() {
    return this.auxkey;
  }

  /**
   * Setter for the auxiliary public key of a trustee.
   *
   * @param auxkey auxiliary public key of trustee
   */
  public void setAuxkey(String auxkey) {
    this.auxkey = auxkey;
  }

  /**
   * Getter for the type of auxiliary public key a trustee has used.
   *
   * @return {@link String} of type of auxiliary public key
   */
  public String getAuxkeyType() {
    return this.auxkeyType;
  }

  /**
   * Setter for the type of auxiliary public key of a trustee.
   *
   * @param auxkeyType type of auxiliay public key
   */
  public void setAuxkeyType(String auxkeyType) {
    this.auxkeyType = auxkeyType;
  }

  /**
   * Return is Trustee has already done all its duties corresponding to the election state.
   *
   * @return true, if is waiting
   */
  public boolean isWaiting() {
    return this.isWaiting;
  }

  public void setWaiting(boolean isWaiting) {
    this.isWaiting = isWaiting;
  }


  /**
   * Getter for the public elgamal keys and proofs.
   * */
  public List<String> getPublicElgamalKeyAndProof() {
    return Objects.requireNonNullElseGet(publicElgamalKeyAndProof, ArrayList::new);
  }

  /**
   * Setter for the public elgamal keys and proofs.
   * */
  public void addPublicElgamalKeyAndProof(List<String> keysAndProofs) {
    if (publicElgamalKeyAndProof == null) {
      publicElgamalKeyAndProof = new ArrayList<>();
    }
    publicElgamalKeyAndProof.addAll(keysAndProofs);
  }

  public List<String> getBackups() {
    return Objects.requireNonNullElseGet(backups, ArrayList::new);
  }

  /**
   * Setter for the backups.
   * */
  public void addBackup(String backup) {
    if (backups == null) {
      backups = new ArrayList<>();
    }
    backups.add(backup);
  }

  public BigInteger getLagrangeCoefficient() {
    return new BigInteger(this.lagrangeCoefficient, 16);
  }

  public void setLagrangeCoefficient(String coefficient) {
    this.lagrangeCoefficient = coefficient;
  }

  /**
   * Getting the public elgamal key to the trustee.
   *
   * @return public elgamal key
   */
  public BigInteger getPrimaryKey() {
    for (String keyAndProof : publicElgamalKeyAndProof) {
      String[] tempKeyAndProof = keyAndProof.split(";");
      if (tempKeyAndProof[0].equals("0")) {
        return new BigInteger(tempKeyAndProof[1], 16);
      }
    }
    return null;
  }

  public boolean isAvailable() {
    return this.isAvailable;
  }

  public void setAvailable() {
    this.isAvailable = true;
  }

  @Override
  public int compareTo(Trustee o) {
    return Integer.compare(this.trusteeIndex, o.trusteeIndex);
  }
}
