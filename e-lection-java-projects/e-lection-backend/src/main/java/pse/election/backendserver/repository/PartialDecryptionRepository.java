package pse.election.backendserver.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import pse.election.backendserver.entity.PartialDecryption;

/**
 * Provides the functionality to directly communicate with the database on behalf of the
 * PartialDecryption entity. It can be used to perform all basic CRUD-Operations and also to perform
 * specialized find operations.
 *
 * @version 1.0
 */
@Repository
public interface PartialDecryptionRepository extends CrudRepository<PartialDecryption, Long> {

  /**
   * Retrieves a collection of PartialDecryption entites by their tally identities. If some tally
   * identities are not to be found, then no PartialDecryption entities are returned for these tally
   * identities.
   *
   * @param tallyId collection of tally identifiers
   * @return collection of found PartialDecryption entities
   */
  public Iterable<PartialDecryption> findByTallyId(long tallyId);

  /**
   * Retrieves a collection of PartialDecryption entites by their trustee identities. If some
   * trustee identities are not to be found, then no PartialDecryption entities are returned for
   * these trustee identities.
   *
   * @param trusteeId collection of trustee identifiers
   * @return collection of found PartialDecryption entities
   */
  public Iterable<PartialDecryption> findByTrusteeId(long trusteeId);

  /**
   * Retrieves a partial decryption entity by its trustee identifier and tally identifier. There can
   * always only be one partial decryption of a trustee to one tallied option.
   */
  public PartialDecryption findByTrusteeIdAndTallyId(long trusteeId, long tallyId);

  /**
   * Retrieves a collection of PartialDecryption entites by their ballot identities. If some ballot
   * identities are not to be found, then no PartialDecryption entities are returned for these
   * ballot identities.
   *
   * @param optionEncryptedId collection of ballot identifiers
   * @return collection of found PartialDecryption entities
   */
  public Iterable<PartialDecryption> findByOptionEncryptedId(long optionEncryptedId);

  /**
   * Checks whether there are partial decryptions of a tallied option.
   */
  public boolean existsByTallyIdAndTrusteeId(long tallyId, long trusteeId);
}