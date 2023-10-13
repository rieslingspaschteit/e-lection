package pse.election.backendserver.core.service;

import org.springframework.stereotype.Service;
import pse.election.backendserver.repository.AuthorityRepository;

/**
 * This class processes all the authority service functionalities. It is responsible for managing
 * authorities in the backend system.
 *
 * @version 1.0
 */
@Service
public class AuthorityService {

  private final AuthorityRepository authorityRepository;

  public AuthorityService(AuthorityRepository authorityRepository) {
    this.authorityRepository = authorityRepository;
  }

  /**
   * Returns if an authority with the parsed email exists in the database.
   *
   * @param email of authority
   * @return true if the email exists in the database
   */
  public boolean existsByEmail(String email) {
    return authorityRepository.existsByEmail(email);
  }

}
