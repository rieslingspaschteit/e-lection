package pse.election.backendserver.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import pse.election.backendserver.entity.Voter;

/**
 * Provides the functionality to directly communicate with the database on behalf of the voter
 * entity. It can be used to perform all basic CRUD-Operations, but furthermore it can also be used
 * to perform specialized find operations and to check whether an email exists in the voter table.
 *
 * @version 1.0
 */

@Repository
public interface VoterRepository extends CrudRepository<Voter, Long> {

  /**
   * Retrieves a voter entity by its identifier.
   *
   * @param voterId voter identifier
   * @return voter entity
   */
  public Voter findByVoterId(long voterId);

  /**
   * Checks if the database contains an entry with the given email in the voter table.
   *
   * @param email the email of voter
   * @return true, if database contains that email in voter table
   */
  public boolean existsByEmail(String email);

  /**
   * Checks the database if there is an entry in the voter table with the given email and election
   * identifier.
   *
   * @param email      the email of voter
   * @param electionId the identifier of the election
   * @return true, if voter participates in the election
   */
  public boolean existsByEmailAndElectionId(String email, long electionId);

  /**
   * Retrieves a voter entity by its email and election identifier.
   *
   * @param email      the email of voter
   * @param electionId the identifier of the election
   * @return the voter entity if found or {@literal Optional#empty()} if none found.
   */
  public Voter findByEmailAndElectionId(String email, long electionId);

  /**
   * Retrieves a collection of voter entites by their emails. If some emails are not to be found,
   * then no voter entities are returned for these emails.
   *
   * @param email collection of emails
   * @return collection of found voter entities
   */
  public Iterable<Voter> findByEmail(String email);

  /**
   * Retrieves a voter entity by its election identifier.
   *
   * @param electionId the identifier of the election
   * @return the voter entity if found or {@literal Optional#empty()} if none found.
   */
  public Iterable<Voter> findByElectionId(long electionId);

}
