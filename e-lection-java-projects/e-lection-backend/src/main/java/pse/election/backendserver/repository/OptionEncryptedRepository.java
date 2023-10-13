package pse.election.backendserver.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import pse.election.backendserver.entity.OptionEncrypted;

/**
 * Provides the functionality to directly communicate with the database on behalf of the
 * OptionEncrypted entity. It can be used to perform all basic CRUD-Operations and also to perform a
 * specialized find operation.
 */
@Repository
public interface OptionEncryptedRepository extends CrudRepository<OptionEncrypted, Long> {

  /**
   * Retrieves a collection of OptionEncrypted entites by their ballot identifiers. If some ballot
   * identifiers are not to be found, then no OptionEncrypted entities are returned for these ballot
   * identifiers.
   *
   * @param ballotId collection of ballot identifiers
   * @return collection of found OptionEncrypted entities
   */
  public Iterable<OptionEncrypted> findByBallotId(long ballotId);

  /**
   * Getter for the encrypted options to an election.
   */
  public Iterable<OptionEncrypted> findByElectionId(long electionId);

  /**
   * Getter for the encrypted option to a ballot.
   */
  public OptionEncrypted findByBallotIdAndContestIndexAndOptionIndex(long ballotId,
      int contestIndex, int optionIndex);

  /**
   * Getter for the encrypted option to an election and contest.
   * */
  public Iterable<OptionEncrypted> findByElectionIdAndContestIndexAndOptionIndex(long electionId,
      int contestIndex, int optionIndex);

  public OptionEncrypted findByOptionEncryptedId(long optionEncryptedId);

  public boolean existsByElectionId(long electionId);

}
