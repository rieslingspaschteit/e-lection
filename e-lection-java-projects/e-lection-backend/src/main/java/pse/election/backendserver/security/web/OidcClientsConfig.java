package pse.election.backendserver.security.web;

import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pse.election.backendserver.utils.OidcConfigProvider;

/**
 * This class contains the configuration for the open id connect client.
 * */
@Configuration
public class OidcClientsConfig {

  @Value("${oidcClients:#{null}}")
  private String oidcClientConfigFilePath;

  @Value("${proxyServer:#{null}}")
  private String proxyServer;

  @Bean
  public OidcConfigProvider oidcConfigProvider() throws IOException {
    return new OidcConfigProvider(oidcClientConfigFilePath, proxyServer);
  }
}
