package pse.election.backendserver.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import pse.election.backendserver.entity.Tally;

/**
 * Provides the functionality to directly communicate with the database on behalf of the tally
 * entity. It can be used to perform all basic CRUD-Operations and also to perform specialized find
 * operations.
 *
 * @version 1.0
 */
@Repository
public interface TallyRepository extends CrudRepository<Tally, Long> {

  /**
   * Retrieves a tally entity by its contest identifier. If no contest identifier is found, then no
   * tally entity is returned for this contest identifier.
   *
   * @return found tally entity
   */
  public Iterable<Tally> findByElectionId(long electionId);

  public Tally findByElectionIdAndContestIndexAndOptionIndex(long electionId, int contestIndex,
      int optionIndex);

  public Iterable<Tally> findByElectionIdAndContestIndex(long electionId, int contestIndex);

}
