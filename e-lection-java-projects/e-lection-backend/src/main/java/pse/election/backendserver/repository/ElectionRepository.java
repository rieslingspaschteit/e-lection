package pse.election.backendserver.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import pse.election.backendserver.entity.Election;

/**
 * Provides the functionality to directly communicate with the database on behalf of the Election
 * entity. It can be used to perform all basic CRUD-Operations and also to perform specialized find
 * operations.
 *
 * @version 1.0
 */
@Repository
public interface ElectionRepository extends CrudRepository<Election, Long> {

  /**
   * Retrieves an election entity by its identifier.
   *
   * @param electionId election identifier
   * @return election entity if found
   */
  public Election findByElectionId(long electionId);

  /**
   * Retrieves an election entity by its unique fingerprint.
   *
   * @param fingerprint is the fingerprint
   * @return election entity if found
   */
  public Election findByFingerprint(String fingerprint);

  /**
   * Retrieves an election entity by its title.
   *
   * @param title the title of the election
   * @return the Election entity if found or {@literal Optional#empty()} if none found
   */
  public Election findByTitle(String title);

  /**
   * Retrieves an election entity by its authority email.
   *
   * @param authorityEmail the authority email
   * @return the Election entity if found or {@literal Optional#empty()} if none found
   */
  public Iterable<Election> findByAuthorityEmail(String authorityEmail);

  /**
   * Checks whether there exists an election with a given title.
   *
   * @param title title of election
   * @return true, if there exists an election
   */
  public boolean existsByTitle(String title);

  /**
   * Getter if the election the the id exists.
   *
   * @param electionId is the election id
   * @return true if election could be found
   */
  public boolean existsByElectionId(long electionId);

  /**
   * Checks whether there exists an election with a given fingerprint.
   *
   * @param fingerprint is the fingerprint
   * @return true, if there exists an election
   */
  public boolean existsByFingerprint(String fingerprint);

}
