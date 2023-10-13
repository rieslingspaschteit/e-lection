package pse.election.backendserver.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import pse.election.backendserver.entity.Ballot;

/**
 * Provides the functionality to directly communicate with the database on behalf of the voter
 * entity. It can be used to perform all basic CRUD-Operations and also to perform specialized find
 * operations.
 *
 * @version 1.0
 */
@Repository
public interface BallotRepository extends CrudRepository<Ballot, Long> {

  /**
   * Getter for the last sent ballot.
   *
   * @param latestTrackingCode the tracking code that was used last for computation of the next
   *                           tracking code
   * @param electionId         the id of the election.
   * @return the Ballot that matches the tracking code and the electionId
   */
  public Ballot findByLatestTrackingCodeAndElectionId(String latestTrackingCode, long electionId);

  /**
   * Getter if the tracking code is referencing the latest tracking code.
   *
   * @param latestTrackingCode is the latest tracking code
   * @return true if the referenced tracking code is the latest
   */
  public boolean existsByLatestTrackingCode(String latestTrackingCode);

  /**
   * Getter if the election with the referenced id exists.
   *
   * @param electionId is the id to search for
   * @return true if the election exists
   */
  public boolean existsByElectionId(long electionId);

  /**
   * Getter for the ballot referenced by the id.
   *
   * @param ballotId is the ballot to search for
   * @return the ballot with the referenced id
   */
  public Ballot findByBallotId(long ballotId);

  /**
   * Retrieves a collection of Ballot entites by their election identifiers. If some election
   * identifiers are not to be found, then no Ballot entities are returned for these election
   * identifiers.
   *
   * @param electionId collection of election identifiers
   * @return collection of found Ballot entities
   */
  public Iterable<Ballot> findByElectionIdAndIsSubmitted(long electionId, boolean isSubmitted);

}
