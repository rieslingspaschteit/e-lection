package pse.election.backendserver.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.math.BigInteger;

/**
 * A Tally describes the basic elements of an encrypted option: the ciphertxt PAD and DATA as well
 * as aforeign key to the election the tally belongs to. A Tally can either be the tallied result of
 * an election (see below) or part of a ballot (see @link {@link OptionEncrypted})
 * If an election has the {@link pse.election.backendserver.core.state.ElectionState} DONE, then no
 * new ballots can be sent to the server. Each encrypted option of each submitted ballot of the
 * corresponding election will be tallied, eventhough a ballot being encrypted, thus resulting in a
 * homomorphic encryption of a tallied option.
 * In order to calculate the election results, it is required that each trustee of the election does
 * it's partial decryption on each tally of that exact same election.
 *
 * @version 1.0
 */
@Entity
public class Tally implements Comparable<Tally> {

  private static final int BASE_OF_HEX = 16;

  /**
   * This is the primary key.
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long tallyId;

  private long electionId;
  private int contestIndex;
  private int optionIndex;

  @Column(columnDefinition = "TEXT")
  private String ciphertextPAD;
  @Column(columnDefinition = "TEXT")
  private String ciphertextDATA;

  public Tally() {
    super();
  }

  /**
   * Constructor of new Tally.
   * */
  public Tally(long electionId, int contestIndex, int optionIndex) {
    this.electionId = electionId;
    this.contestIndex = contestIndex;
    this.optionIndex = optionIndex;
  }

  /**
   * Constructor of new Tally.
   * */
  public Tally(long electionId, int contestIndex, int optionIndex, String homomorphicEncryptionPAD,
      String homomorphicEncryptionDATA) {
    this.electionId = electionId;
    this.contestIndex = contestIndex;
    this.optionIndex = optionIndex;
    this.ciphertextPAD = homomorphicEncryptionPAD;
    this.ciphertextDATA = homomorphicEncryptionDATA;
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
   * Getter for the primary key of a tallied option.
   *
   * @return {@link long} identifier
   */
  public long getTallyId() {
    return this.tallyId;
  }

  /**
   * Getter for the homomorphic encryption of a tallied option.
   *
   * @return {@link BigInteger} encryptiom
   */
  public BigInteger getCiphertextPAD() {
    return new BigInteger(this.ciphertextPAD, BASE_OF_HEX);
  }

  /**
   * Setter for the homomorphic encryption of a tallied option.
   */
  public void setCiphertextPAD(String encryption) {
    this.ciphertextPAD = encryption;
  }

  /**
   * Getter for the homomorphic calculated encryption data.
   *
   * @return encryption data
   */
  public BigInteger getCiphertextDATA() {
    return new BigInteger(this.ciphertextDATA, BASE_OF_HEX);
  }

  /**
   * Setter for the homomorphic calculated encryption data.
   *
   * @param enrcryption data
   */
  public void setCiphertextDATA(String enrcryption) {
    this.ciphertextDATA = enrcryption;
  }

  @Override
  public int compareTo(Tally o) {
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
