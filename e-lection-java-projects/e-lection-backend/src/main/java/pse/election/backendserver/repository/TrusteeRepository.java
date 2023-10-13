package pse.election.backendserver.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import pse.election.backendserver.entity.Trustee;

/**
 * Provides the functionality to directly communicate with the database on behalf of the trustee
 * entity. It can be used to perform all basic CRUD-Operations, check whether an email exists in the
 * trustee table and to perform specialized find operations.
 *
 * @version 1.0
 */
@Repository
public interface TrusteeRepository extends CrudRepository<Trustee, Long> {

  /**
   * Checks if the database contains an entry with the given email in the trustee table.
   *
   * @param email the email of trustee
   * @return true, if database contains that email in trustee table
   */
  public boolean existsByEmail(String email);

  /**
   * Checks the database if there is an entry in the trustee table with the given email and election
   * identifier.
   *
   * @param email      the email of trustee
   * @param electionId the identifier of the election
   * @return true, if the trustee participates in the election
   */
  public boolean existsByEmailAndElectionId(String email, long electionId);

  /**
   * Retrieves a trustee entity by its email and election identifier.
   *
   * @param email      the email of trustee
   * @param electionId the identifier of the election
   * @return the trustee entity if found or {@literal Optional#empty()} if none found.
   */
  public Trustee findByEmailAndElectionId(String email, long electionId);

  public Trustee findByTrusteeIndexAndElectionId(int trusteeIndex, long electionId);

  /**
   * Retrieves a collection of trustee entites by their emails. If some emails are not to be found,
   * then no trustee entities are returned for these emails.
   *
   * @param email collection of emails
   * @return collection of found trustee entities
   */
  public Iterable<Trustee> findByEmail(String email);

  /**
   * Retrieves a trustee entity by its election identifier.
   *
   * @param electionId the identifier of the election
   * @return the trustee entity if found or {@literal Optional#empty()} if none found.
   */
  public Iterable<Trustee> findByElectionId(long electionId);

  public Iterable<Trustee> findByElectionIdAndIsWaiting(long electionId, boolean isWaiting);

}
