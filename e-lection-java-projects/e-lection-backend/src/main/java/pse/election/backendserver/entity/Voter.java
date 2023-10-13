package pse.election.backendserver.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * A voter is a participant in an election, where a voter is determined by the authority of an
 * election. Voters can have their ballot papers verified as often as they like. However, a ballot
 * can only be cast once.
 *
 * @version 1.0
 */

@Entity
public class Voter {

  /**
   * This is the primary key.
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long voterId;

  /**
   * This is the foreign key to the election.
   */
  private long electionId;

  private String email;

  /**
   * True if Voter has submitted a valid ballot.
   */
  private boolean hasVoted;

  public Voter() {
  }

  /**
   * Constructs a voter instance.
   *
   * @param electionId identifier of the election the voter participates
   * @param email      of voter
   */
  public Voter(long electionId, String email) {
    this.electionId = electionId;
    this.email = email;
  }

  /**
   * Getter for the primary key of a voter.
   *
   * @return identifier
   */
  public long getVoterId() {
    return this.voterId;
  }

  /**
   * Getter for the foreign key of an election.
   *
   * @return identifier
   */
  public long getElectionId() {
    return this.electionId;
  }

  /**
   * Getter for the email of a voter.
   *
   * @return email
   */
  public String getEmail() {
    return this.email;
  }

  /**
   * Getter for the value if voter has already voted.
   *
   * @return true, if voter has alread voted
   */
  public boolean isHasVoted() {
    return this.hasVoted;
  }

  /**
   * Setter for the value if voter has already voted. This can only be set once to true.
   *
   * @param hasVoted value whether voter has voted
   */
  public void setHasVoted(boolean hasVoted) {
    this.hasVoted = hasVoted;
  }

  @Override
  public String toString() {
    return email;
  }
}
