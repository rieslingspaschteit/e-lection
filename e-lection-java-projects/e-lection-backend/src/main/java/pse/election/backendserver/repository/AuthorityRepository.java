package pse.election.backendserver.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import pse.election.backendserver.entity.Authority;

/**
 * Provides the functionality to directly communicate with the database on behalf of the authority
 * entity. It can be used to perform all basic CRUD-Operations and also to check whether an email
 * exists in the authority table.
 *
 * @version 1.0
 */

@Repository
public interface AuthorityRepository extends CrudRepository<Authority, String> {

  /**
   * Checks if the database contains an entry with that email in the authority table.
   *
   * @param email the email of authority
   * @return true, if database contains that email
   */
  boolean existsByEmail(String email);


  /**
   * Retrieves authority entity by its identifier.
   *
   * @param email the email of the authority
   * @return authority entity
   */
  Authority findByEmail(String email);

}
