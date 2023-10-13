package pse.election.backendserver.controller.web;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;
import pse.election.backendserver.payload.dto.OidcProvider;
import pse.election.backendserver.utils.OidcConfigProvider;

/**
 * Controller that handles requests related to the registered Oidc providers.
 *
 * @version 1.0
 */
@RestController
@RequestMapping("/api")
public class OidcController {

  private static final String AUTH_URL_TEMPLATE = "%s/oauth2/authorization/%s";
  private final OidcConfigProvider oidcConfigProvider;
  @Value("${frontendOrigin:#{null}}")
  private String frontendOrigin;
  @Value("${proxyServer:#{null}}")
  private String proxyServer;

  public OidcController(OidcConfigProvider oidcConfigProvider) {
    this.oidcConfigProvider = oidcConfigProvider;
  }

  /**
   * Endpoint that provides information about supported OpenId Connect Identity Provider.
   *
   * @return a list of all registered OpenId Connect provider.
   */
  @GetMapping("/auth")
  public List<OidcProvider> getAvailableOidcClients(HttpServletRequest request) {
    return oidcConfigProvider.getClientRegistrations().stream()
        .map(clientRegistration -> {
          final String clientName = clientRegistration.getClientName();
          final String registrationId = clientRegistration.getRegistrationId();
          final String baseUrl =
              this.proxyServer != null ? this.proxyServer : this.getBaseUrl(request);

          return new OidcProvider(clientName,
              String.format(AUTH_URL_TEMPLATE, baseUrl, registrationId));
        })
        .toList();
  }

  @GetMapping("/auth/success")
  public RedirectView afterLogin(HttpServletRequest request) {
    return new RedirectView(
        this.frontendOrigin != null ? this.frontendOrigin : this.getBaseUrl(request));
  }

  @GetMapping("/am-i-logged-in")
  public ResponseEntity<Object> amIAuthorized() {
    return ResponseEntity.ok().build();
  }

  @GetMapping("/logout-success")
  public RedirectView afterLogout(HttpServletRequest request) {
    return new RedirectView(
        this.frontendOrigin != null ? this.frontendOrigin : this.getBaseUrl(request));
  }

  private String getBaseUrl(HttpServletRequest request) {
    final String scheme = request.getScheme();
    final String serverName = request.getServerName();
    final int serverPort = request.getServerPort();

    final StringBuilder baseUrl = new StringBuilder();
    baseUrl.append(scheme).append("://").append(serverName);

    if (serverPort != 80 && serverPort != 443) {
      baseUrl.append(":").append(serverPort);
    }

    return baseUrl.toString();
  }

}
