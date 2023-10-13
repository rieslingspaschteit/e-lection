package pse.election.backendserver.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.util.Date;

/**
 * A ballot consists of several contests, which in turn consist of several options. A ballot can be
 * verified by the voter as often as desired, thereby the ballot is marked as "spoiled". However, as
 * soon as the voter decides to cast his ballot, the ballot is marked as "submitted" and can no
 * longer be verified. Also, the voter cannot participate in the election anymore.
 *
 * @version 1.0
 */
@Entity
public class Ballot {

  /**
   * This is the primary key.
   */
  @Id
  @GeneratedValue (strategy = GenerationType.IDENTITY)
  private long ballotId;

  /**
   * This is the foreign key to the election.
   */
  private long electionId;

  /**
   * This is the unique identifier for a ballot which is needed by the frontend to encrypt a
   * ballot.
   */
  private String ballotIdForEncryption;

  /**
   * Is set to false if it is a spoiled ballot, otherwise true.
   */
  private boolean isSubmitted;
  private String latestTrackingCode;
  private String previousTrackingCode;
  private String deviceInfo;
  private Date encryptionDate;

  public Ballot() {
  }

  /**
   * Constructor of new Ballot.
   * */
  public Ballot(long electionId, String ballotIdForEncryption, String deviceInfo,
      Date encryptionDate, boolean isSubmitted) {
    this.electionId = electionId;
    this.ballotIdForEncryption = ballotIdForEncryption;
    this.deviceInfo = deviceInfo;
    this.encryptionDate = encryptionDate;
    this.isSubmitted = isSubmitted;
  }

  /**
   * Getter for the primary key.
   *
   * @return {@link long} identifier
   */
  public long getBallotId() {
    return this.ballotId;
  }

  public String getBallotIdForEncryption() {
    return this.ballotIdForEncryption;
  }

  /**
   * Getter for the foreign key.
   *
   * @return {@link long} identifier
   */
  public long getElectionId() {
    return this.electionId;
  }

  /**
   * Getter for the value if a ballot has been submitted.
   *
   * @return {@link boolean} state of submission
   */
  public boolean isSubmitted() {
    return this.isSubmitted;
  }

  /**
   * Setter if Ballot has been officially submitted by the voter.
   *
   * @param isSubmitted true, if submitted
   */
  public void setSubmitted(boolean isSubmitted) {
    this.isSubmitted = isSubmitted;
  }

  /**
   * Getter for the tracking code of a ballot.
   *
   * @return {@link String} tracking code
   */
  public String getLatestTrackingCode() {
    return this.latestTrackingCode;
  }

  public void setLatestTrackingCode(String string) {
    this.latestTrackingCode = string;
  }

  public String getPreviousTrackingCode() {
    return this.previousTrackingCode;
  }

  public void setPreviousTrackingCode(String string) {
    this.previousTrackingCode = string;
  }

  /**
   * Getter for the device information on which device the encryption of the ballot has been done.
   *
   * @return {@link String} device information
   */
  public String getDeviceInfo() {
    return this.deviceInfo;
  }

  /**
   * Getter for the encrpytion date of the ballot.
   *
   * @return {@link Date} date of encryption
   */
  public Date getEncryptionDate() {
    return this.encryptionDate;
  }
}
