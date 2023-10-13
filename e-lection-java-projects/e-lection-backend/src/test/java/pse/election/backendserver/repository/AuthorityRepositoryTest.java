package pse.election.backendserver.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import pse.election.backendserver.entity.Authority;

@SpringBootTest(args = "--oidcClients=./oidcClients.toml")
class AuthorityRepositoryTest {

  static String authorityMail;
  static Authority authority;
  @Autowired
  AuthorityRepository authorityRepository;

  @BeforeAll
  static void setup() {
    authorityMail = "authority@mail.com";
    authority = new Authority(authorityMail);
  }

  @BeforeEach
  void saveAuthority() {
    authorityRepository.save(new Authority(authorityMail));
  }

  @Test
  void existsByEmail() {
    assertTrue(authorityRepository.existsById(authorityMail));
  }

  @Test
  void findByEmail() {
    assertEquals(authorityRepository.findByEmail(authorityMail).getEmail(), authority.getEmail());
  }
}
