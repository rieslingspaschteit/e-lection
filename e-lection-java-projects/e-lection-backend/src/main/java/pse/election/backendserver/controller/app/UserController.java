package pse.election.backendserver.controller.app;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import pse.election.backendserver.controller.ElectionUserAPI;
import pse.election.backendserver.payload.response.ResponseBuilder;
import pse.election.backendserver.payload.response.UserInformationResponse;

/**
 * This class is used as an api controller and handles user information requests. It implements the
 * {@link ElectionUserAPI} and gets controlled by teh {@link FrontController}. This includes user
 * roles information.
 *
 * @version 1.0
 */
@Component
@Qualifier("UserController")
public class UserController implements ElectionUserAPI {

  @Autowired
  private ResponseBuilder responseBuilder;

  @Override
  public UserInformationResponse getUserRoles(String email) {
    return this.responseBuilder.buildUserInformationResponse(email);
  }
}
