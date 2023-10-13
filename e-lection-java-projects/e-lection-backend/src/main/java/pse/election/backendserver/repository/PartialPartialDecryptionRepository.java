package pse.election.backendserver.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import pse.election.backendserver.entity.PartialPartialDecryption;

/**
 * Provides the functionality to directly communicate with the database on behalf of the
 * PartialPartialDecryption entity. It can be used to perform all basic CRUD-Operations and also to
 * perform specialized find operations.
 *
 * @version 1.0
 */
@Repository
public interface PartialPartialDecryptionRepository extends
    CrudRepository<PartialPartialDecryption, Long> {

  /**
   * Retrieves a collection of PartialPartialDecryption entites by their tally identities. If some
   * tally identities are not to be found, then no PartialPartialDecryption entities are returned
   * for these tally identities.
   *
   * @param tallyId collection of tally identifiers
   * @return collection of found PartialPartialDecryption entities
   */
  public Iterable<PartialPartialDecryption> findByTallyId(long tallyId);

  /**
   * Retrieves a collection of PartialPartialDecryption entites by their trustee identities. If some
   * trustee identities are not to be found, then no PartialPartialDecryption entities are returned
   * for these trustee identities.
   *
   * @param trusteeId collection of trustee identifiers
   * @return collection of found PartialPartialDecryption entities
   */
  public Iterable<PartialPartialDecryption> findByTrusteeId(long trusteeId);

  /**
   * Retrieves a collection of partial partial decryptions of a trustee to a tallied option.
   */
  public Iterable<PartialPartialDecryption> findByTrusteeIdAndTallyId(long trusteeId, long tallyId);

  /**
   * Checks whether there are partial partial decryptions of a tallied option.
   */
  public boolean existsByTallyIdAndTrusteeId(long tallyId, long trusteeId);

  /**
   * Checks whether there are partial partial decrpytions of an trustee.
   *
   * @param trusteeId trustee identifier
   * @return true, if partial partial decryptions exists
   */
  public boolean existsByTrusteeId(long trusteeId);

  /**
   * Retrieves a collection of PartialPartialDecryption entites by their ballot identities. If some
   * ballot identities are not to be found, then no PartialPartialDecryption entities are returned
   * for these ballot identities.
   *
   * @param optionEncryptedId collection of ballot identifiers
   * @return collection of found PartialPartialDecryption entities
   */
  public Iterable<PartialPartialDecryption> findByOptionEncryptedId(long optionEncryptedId);

}
