package pse.election.backendserver.payload.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * This class is needed for ballot commit request. A ballot commit is a filled out ballot that gets
 * send to the api. The system handles every sent ballot as a spoiled ballot until the user sends a
 * submit request.
 *
 * @version 1.0
 */
public class BallotCommitRequest {

  @JsonProperty("cipherText")
  @NotNull
  private Map<String, Collection<EncryptedOptionRequest>> cipherText;

  @JsonProperty("individualProofs")
  @NotNull
  private Map<String, Collection<DisjunctiveChaumPedersenRequest>> individualProofs;

  @JsonProperty("accumulatedProofs")
  @NotNull
  private Map<String, ConstantChaumPedersenRequest> accumulatedProofs;

  @JsonProperty("deviceInformation")
  @NotNull
  private String deviceInformation;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  @JsonProperty("date")
  @NotNull
  private Date date;

  @JsonProperty("ballotId")
  @NotNull
  private String ballotId;

  public String getBallotId() {
    return ballotId;
  }

  public void setBallotId(String ballotId) {
    this.ballotId = ballotId;
  }

  /**
   * Getter for the ciphertext of the ballot. This contains the encrypted options.
   *
   * @return map mapping the question index to the encrypted options.
   */
  public Map<String, Collection<EncryptedOptionRequest>> getCipherText() {
    return cipherText;
  }

  /**
   * Setter for the ciphertext of the ballot from the request.
   *
   * @param cipherText is map mapping the question index to the encrypted options of the ballot
   */
  public void setCipherText(Map<String, Collection<EncryptedOptionRequest>> cipherText) {
    this.cipherText = cipherText;
  }

  /**
   * Getter for the individual proofs of the question.
   *
   * @return map mapping the question indexes to the disjunctive chaum pederson proofs
   */
  public Map<String, Collection<DisjunctiveChaumPedersenRequest>> getIndividualProofs() {
    return individualProofs;
  }

  /**
   * Setter for the individual proofs of the ballot from the request.
   *
   * @param individualProofs is a map mapping the question index to the individual proofs
   */
  public void setIndividualProofs(
      Map<String, Collection<DisjunctiveChaumPedersenRequest>> individualProofs) {
    this.individualProofs = individualProofs;
  }

  /**
   * Getter for the accumulated proofs of the ballot from the request.
   *
   * @return map mapping the question index to the constant chaum pederson proof
   */
  public Map<String, ConstantChaumPedersenRequest> getAccumulatedProofs() {
    return accumulatedProofs;
  }

  /**
   * Setter for the accumulated proofs of the ballot from the request.
   *
   * @param accumulatedProofs is a map mapping the question index to the constant chaum pederson
   *                          proof
   */
  public void setAccumulatedProofs(Map<String, ConstantChaumPedersenRequest> accumulatedProofs) {
    this.accumulatedProofs = accumulatedProofs;
  }

  /**
   * Getter for the device information of the user committing the ballot.
   *
   * @return device information of the ballot committer
   */
  public String getDeviceInformation() {
    return deviceInformation;
  }

  /**
   * Setter for the device information of the user committing the ballot.
   *
   * @param deviceInformation is the device information of the committor
   */
  public void setDeviceInformation(String deviceInformation) {
    this.deviceInformation = deviceInformation;
  }

  /**
   * Getter for the date of the ballot commitment from the request.
   *
   * @return date of the ballot commitment
   */
  public Date getDate() {
    return date;
  }

  /**
   * Setter for the date of the ballot commitment from the request.
   *
   * @param date is the date of the ballot commitment
   */
  public void setDate(Date date) {
    this.date = date;
  }


}
