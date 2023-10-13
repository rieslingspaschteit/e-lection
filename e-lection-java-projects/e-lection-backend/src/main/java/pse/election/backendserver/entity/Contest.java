package pse.election.backendserver.entity;

import com.sunya.electionguard.Hash;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.util.List;


/**
 * A contest corresponds to a question on a ballot, with each contest having multiple options, which
 * in turn correspond to an answer. Furthermore, the authority determines, while in the process of
 * creating an election, how the individual contests are to be answered. This is represented by the
 * minimum and maximum values.
 *
 * @version 1.0
 */
@Entity
public class Contest implements Comparable<pse.election.backendserver.entity.Contest> {

  /**
   * This is the primary key.
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long contestId;

  /**
   * This is the foreign key to the election.
   */
  private long electionId;

  /**
   * The maximum amount of selected options needed in order to fulfill the correctness of a ballot.
   */
  private int max;

  /**
   * Indicates an order of contests for a specific election.
   */
  private int contestIndex;

  private String contestName;
  private List<String> contestOptions;

  public Contest() {
  }

  /**
   * Constructor of new Contest.
   * */
  public Contest(long electionId, String contestName, int max, int contestIndex,
      List<String> contestOptions) {
    this.electionId = electionId;
    this.contestName = contestName;
    this.max = max;
    this.contestIndex = contestIndex;
    this.contestOptions = contestOptions;
  }

  /**
   * Getter for the primary key.
   *
   * @return {@link long} identifier
   */
  public long getContestId() {
    return this.contestId;
  }

  /**
   * Getter for the foreign key to the election.
   *
   * @return {@link long} identifier
   */
  public long getElectionId() {
    return this.electionId;
  }

  /**
   * Getter for the name of the contest, which is a question to the options.
   *
   * @return name
   */
  public String getName() {
    return this.contestName;
  }

  /**
   * Getter for the maximum amount of needed options.
   *
   * @return {@link int} maximum
   */
  public int getMax() {
    return this.max;
  }

  /**
   * Getter for the index of a contest to a specific election.
   *
   * @return {@link int} index
   */
  public int getIndex() {
    return contestIndex;
  }

  /**
   * Getter for the Collection of options of a contest.
   *
   * @return {@link List} options
   */
  public List<String> getOptions() {
    return this.contestOptions;
  }

  @Override
  public int compareTo(Contest o) {
    return this.contestIndex - o.contestIndex;
  }

  public String getHashString() {
    return Hash.hash_elems(contestName, contestOptions, max).toString();
  }
}