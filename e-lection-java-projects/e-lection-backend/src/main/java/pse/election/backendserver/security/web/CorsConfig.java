package pse.election.backendserver.security.web;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * This class provides the configuration of cors in the app.
 * */
@Configuration
public class CorsConfig {

  @Value("${proxyServer:#{null}}")
  private String proxyServer;

  /**
   * This method configures the cors.
   * */
  @Bean
  public WebMvcConfigurer corsConfigurer() {
    return new WebMvcConfigurer() {
      @Override
      public void addCorsMappings(@NotNull CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("http://localhost:[*]", proxyServer)
                .allowedMethods("*")
                .allowCredentials(true)
                .allowedHeaders("*");
      }
    };
  }
}
