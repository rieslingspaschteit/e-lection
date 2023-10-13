package pse.election.backendserver.controller.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pse.election.backendserver.controller.ElectionAPI;
import pse.election.backendserver.payload.response.UserInformationResponse;
import pse.election.backendserver.security.user.UserPrincipal;

/**
 * This class is used for managing the "/api/user" endpoint. It is used for user information
 * requests.
 *
 * @version 1.0
 */
@RestController
@RequestMapping(path = "/api")
public class WebUserController {

  private final ElectionAPI electionAPI;

  public WebUserController(ElectionAPI electionAPI) {
    this.electionAPI = electionAPI;
  }

  /**
   * This method is called by a GET-Request on /api/user/ and handles the user roles request.
   *
   * @param user is the currently logged-in user
   * @return response containing the roles of the user in the system.
   */
  @GetMapping("/user")
  public ResponseEntity<UserInformationResponse> getUserRoles(
      @AuthenticationPrincipal UserPrincipal user) {
    return new ResponseEntity<>(electionAPI.getUserRoles(user.getEmail()), HttpStatus.OK);
  }

}