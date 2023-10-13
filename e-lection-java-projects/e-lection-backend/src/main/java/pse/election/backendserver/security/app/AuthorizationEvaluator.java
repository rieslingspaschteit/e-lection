package pse.election.backendserver.security.app;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pse.election.backendserver.core.service.AuthorityService;
import pse.election.backendserver.core.service.ElectionService;
import pse.election.backendserver.core.service.TrusteeService;
import pse.election.backendserver.core.service.VoterService;
import pse.election.backendserver.security.ElectionAuthentication;
import pse.election.backendserver.security.ElectionAuthorizationEvaluator;
import pse.election.backendserver.security.user.UserRole;

/**
 * This class is used for validation of user permission to access any functionality. As some
 * functionalities in the system require certain permissions (e.g. only authorities are permitted to
 * create elections) this class is called by a controller and validates these permissions.
 *
 * @version 1.0
 */
@Service
public class AuthorizationEvaluator implements ElectionAuthorizationEvaluator {

  private final ElectionAuthentication electionAuthentication;

  @Autowired
  private ElectionService electionService;

  @Autowired
  private VoterService voterService;

  @Autowired
  private AuthorityService authorityService;

  @Autowired
  private TrusteeService trusteeService;


  public AuthorizationEvaluator() {
    this.electionAuthentication = new ElectionAuthenticationAdapter();
  }

  @Override
  public boolean hasRole(UserRole userRole) {
    String userEmail = electionAuthentication.getAuthenticatedEmail();
    return switch (userRole) {
      case VOTER -> voterService.existsByEmail(userEmail);
      case TRUSTEE -> trusteeService.existsByEmail(userEmail);
      case AUTHORITY -> authorityService.existsByEmail(userEmail);
    };
  }

  @Override
  public boolean hasRoleInElection(UserRole userRole, Long electionId) {
    String userEmail = electionAuthentication.getAuthenticatedEmail();

    return switch (userRole) {
      case VOTER -> voterService.isVoterInElection(userEmail, electionId);
      case TRUSTEE -> trusteeService.isTrusteeInElection(userEmail, electionId);
      case AUTHORITY -> electionService
          .getElection(electionId)
          .getAuthorityEmail()
          .equals(userEmail);
    };
  }

  @Override
  public boolean isParticipantInElection(Long electionId) {
    for (UserRole userRole : UserRole.values()) {
      if (hasRoleInElection(userRole, electionId)) {
        return true;
      }
    }

    return false;
  }

  @Override
  public boolean isParticipantInElection(String fingerprint) {
    return isParticipantInElection(electionService.getElection(fingerprint).getElectionId());
  }

}

