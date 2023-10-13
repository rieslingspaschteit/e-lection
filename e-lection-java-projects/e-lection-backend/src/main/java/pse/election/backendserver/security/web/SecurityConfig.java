package pse.election.backendserver.security.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import pse.election.backendserver.security.web.oidc.OidcAuthUserService;
import pse.election.backendserver.utils.OidcConfigProvider;

/**
 * This class is used to configure the web application security by creating a servlet filter. This
 * filter is responsible for all the security of the web controllers (protection the application
 * URLs, validating access tokens, redirecting to the login form, ...). These security
 * configurations are defined in the {@link #filterChain(HttpSecurity)} method.
 *
 * @version 1.0
 */
@EnableWebSecurity
@Configuration
public class SecurityConfig {

  private final OidcAuthUserService userService;
  private final OidcConfigProvider oidcConfigProvider;

  public SecurityConfig(OidcAuthUserService userService, OidcConfigProvider oidcConfigProvider) {
    this.userService = userService;
    this.oidcConfigProvider = oidcConfigProvider;
  }

  /**
   * This method is used to define filters that secure the web controllers against unauthorized
   * accesses. These filters ensure that any request to our application requires the user to be
   * authenticated or redirects them to the login page in case they aren't.
   *
   * @param httpSecurity is the file to configure the filter chain on
   * @return a filter chain that every request parses threw before reaching a web endpoint
   */
  @Bean
  public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
    return httpSecurity
        .cors().and()
        .csrf().disable() //only disabled for testing purpose
        .authorizeHttpRequests()
        .requestMatchers("/api/auth", "/api/logout-success", "/api/logout")
        .permitAll()
        .anyRequest()
        .authenticated()
        .and()
        .oauth2Login()
        .userInfoEndpoint()
        .oidcUserService(userService)
        .and()
        .defaultSuccessUrl("/api/auth/success", true)
        .and()
        .exceptionHandling()
        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
        .and()
        .logout()
        .logoutUrl("/api/logout")
        .logoutSuccessUrl("/api/logout-success")
        .and()
        .build();
  }

  @Bean
  public ClientRegistrationRepository clientRegistrationRepository() {
    return new InMemoryClientRegistrationRepository(oidcConfigProvider.getClientRegistrations());
  }

}
