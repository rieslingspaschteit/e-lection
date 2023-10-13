package pse.election.backendserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(args = {"--oidcClients=./fakeOIDC.toml",
    "--authorityConfig=./fakeAuthorities.txt"})
class ELectionBackendApplicationTests {

  @Test
  void contextLoads() {
  }

}
