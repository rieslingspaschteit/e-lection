package pse.election.backendserver.payload.response;

import java.util.List;
import pse.election.backendserver.security.user.UserRole;

/**
 * This class contains the user information response.
 * */
public record UserInformationResponse(List<UserRole> userRoles, String email) {

}
