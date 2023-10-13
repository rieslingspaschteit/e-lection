package pse.election.backendserver.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pse.election.backendserver.entity.Authority;
import pse.election.backendserver.repository.AuthorityRepository;

/**
 * This class is responsible for reading the authority file when starting the application.
 * */
@Configuration
public class AuthorityConfig {

  private final AuthorityRepository authorityRepository;

  @Value("${authorityConfig:#{null}}")
  private String authorityConfigFilePath;

  public AuthorityConfig(AuthorityRepository authorityRepository) {
    this.authorityRepository = authorityRepository;
  }

  /**
   * This method adds the authorities from the files path provided when starting the app.
   * */
  @Bean
  public void initAuthorityRepo() throws IOException {
    if (authorityConfigFilePath == null) {
      return;
    }

    final File authorityConfigFile = new File(authorityConfigFilePath);
    assert authorityConfigFile.exists();
    assert authorityConfigFile.isFile();
    assert authorityConfigFile.canRead();
    Files.readAllLines(authorityConfigFile.toPath()).stream()
        .map(Authority::new)
        .forEach(authorityRepository::save);
  }

}
