package pse.election.backendserver.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.math.BigInteger;
import java.util.Date;
import pse.election.backendserver.core.state.ElectionState;

/**
 * An election is the central part of our application, because with no election there is no use for
 * our system. An election is initiated by one authority. Each election contains a collection of
 * contest and each election has to have a public elgamal key, which is required by the voters. All
 * the ballots will be encrypted by the public elgamal key of the corresponding election.
 *
 * @version 1.0
 */
@Entity
public class Election {

  private static final int BASE_OF_HEX = 16;

  /**
   * This is the primary key.
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long electionId;

  /**
   * The fingerprint will be hashed with the following parts: title of election, description of
   * election, authority email of election, starting date of election, ending date of election,
   * trustee threshold, public elgamal key of election, list of voter emails of election, list of
   * trustee emails of election, value if election has bot, list of contest of election.
   */
  private String fingerprint;

  /**
   * Decrypted result of the election. Result will be encoded in the following format:
   * <p>Format: opt1;opt2;...optN|opt1;...optM|... Each |...| corresponds to a contest with the tallied
   * options, eg.: |12,4,2| would mean that the tally of the first option of the contest is 12, and
   * so on...</p>
   */
  private String cleartextResult;

  /**
   * Represents the latest tracking code. This is needed because each newly calculated tracking code
   * has to be hashed with the previous tracking code. This ultimately builds up a chain of hashes.
   */
  private String trackingCodeLatest;
  private Date startTime;
  private Date endTime;
  private String title;
  private String description;

  /**
   * This is the public elgamal key to the election, which is needed so that voters can encrypt
   * their ballot.
   */
  @Column(columnDefinition = "TEXT")
  private String publicKey;
  private String authorityEmail;

  /**
   * Set to true, if the election should have a bot, which is actually our system that participates
   * as a trustee on the election.
   */
  private boolean hasBot;

  /**
   * Minimum amout of trustees needed for the decryption.
   */
  private int trusteeThreshold;
  private ElectionState state;

  public Election() {
  }

  public Election(String publicKey) {
    this.publicKey = publicKey;
  }

  /**
   * Constructor of new Election.
   * */
  public Election(Date endTime, String title, String description, String authorityEmail,
      boolean hasBot, int threshold) {
    this.endTime = endTime;
    this.title = title;
    this.description = description;
    this.authorityEmail = authorityEmail;
    this.hasBot = hasBot;
    this.trusteeThreshold = threshold;
    this.state = ElectionState.AUX_KEYS;
  }

  /**
   * Getter for the primary key of an election.
   *
   * @return identifier
   */
  public long getElectionId() {
    return this.electionId;
  }

  /**
   * Getter for the fingerprint of an election.
   *
   * @return {@link String} fingerprint
   */
  public String getFingerprint() {
    return this.fingerprint;
  }

  /**
   * Setter for the fingerprint of an election.
   *
   */
  public void setFingerprint(String fingerprint) {
    this.fingerprint = fingerprint;
  }

  /**
   * Getter for the decrypted result of an election. For format, see
   * {@link Election#cleartextResult}.
   *
   * @return {@link String} decrypted result
   */
  public String getCleartextResult() {
    return this.cleartextResult;
  }

  /**
   * Setter for the decrypted result of the election. For format, see
   * {@link Election#cleartextResult}.
   *
   * @param cleartextResult is the clear text representation of the election result
   */
  public void setCleartextResult(String cleartextResult) {
    this.cleartextResult = cleartextResult;
  }

  /**
   * Getter for the latest known tracking code corresponding to an election.
   *
   * @return {@link String} tracking code
   */
  public String getTrackingCodeLatest() {
    return this.trackingCodeLatest;
  }

  /**
   * Setter for the latest tracking code.
   *
   * @param trackingcode is tracking code to set
   */
  public void setTrackingCodeLatest(String trackingcode) {
    this.trackingCodeLatest = trackingcode;
  }

  /**
   * Getter for the election creation date.
   *
   * @return {@link Date} creation date
   */
  public Date getStartTime() {
    return this.startTime;
  }

  public void setStartTime(Date startTime) {
    this.startTime = startTime;
  }

  /**
   * Getter for the election ending date.
   *
   * @return {@link Date} ending date
   */
  public Date getEndTime() {
    return this.endTime;
  }

  /**
   * Getter for the election title.
   *
   * @return {@link String} title
   */
  public String getTitle() {
    return this.title;
  }

  /**
   * Getter for the election description.
   *
   * @return {@link String} description
   */
  public String getDescription() {
    return this.description;
  }

  /**
   * Getter for the election public elgamal key.
   *
   * @return {@link BigInteger} public elgamal key
   */
  public BigInteger getPublicKey() {
    return new BigInteger(publicKey, BASE_OF_HEX);
  }

  /**
   * Setter for the election public elgamal key.
   *
   * @param publicKey is the public key to set
   */
  public void setPublicKey(String publicKey) {
    this.publicKey = publicKey;
  }

  /**
   * Getter for the authority email.
   *
   * @return email of authority
   */
  public String getAuthorityEmail() {
    return this.authorityEmail;
  }

  /**
   * Getter for the value if an election should have a trustee bot.
   *
   * @return true, if bot should be used
   */
  public boolean hasBot() {
    return this.hasBot;
  }

  /**
   * Getter for the trustee threshold.
   *
   * @return threshold
   */
  public int getTrusteeThreshold() {
    return this.trusteeThreshold;
  }

  /**
   * Getter for the state of the election. See {@link ElectionState} for all possible states.
   *
   * @return {@link ElectionState} election state
   */
  public ElectionState getState() {
    return this.state;
  }

  /**
   * Setter for the election state.
   *
   * @param state see {@link ElectionState}
   */
  public void setState(ElectionState state) {
    this.state = state;
  }

}
