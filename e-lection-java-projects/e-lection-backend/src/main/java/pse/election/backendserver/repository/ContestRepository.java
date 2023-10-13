package pse.election.backendserver.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import pse.election.backendserver.entity.Contest;

/**
 * Provides the functionality to directly communicate with the database on behalf of the Contest
 * entity. It can be used to perform all basic CRUD-Operations and also to perform a specialized
 * find operation.
 *
 * @version 1.0
 */
@Repository
public interface ContestRepository extends CrudRepository<Contest, Long> {

  /**
   * Retrieves a collection of Contest entites by their election identifiers. If some election
   * identifiers are not to be found, then no Contest entities are returned for these election
   * identifiers.
   *
   * @param electionId collection of election identifiers
   * @return collection of found contest entities
   */
  public Iterable<Contest> findByElectionId(long electionId);

}
