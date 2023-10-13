package pse.election.backendserver.controller.app;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import pse.election.backendserver.controller.ElectionDataAPI;
import pse.election.backendserver.core.service.ElectionService;
import pse.election.backendserver.core.service.TrusteeService;
import pse.election.backendserver.core.service.VoterService;
import pse.election.backendserver.entity.Election;
import pse.election.backendserver.payload.response.BallotResponse;
import pse.election.backendserver.payload.response.ElectionBallotBoardResponse;
import pse.election.backendserver.payload.response.ElectionDecryptedResultResponse;
import pse.election.backendserver.payload.response.ElectionHashesResponse;
import pse.election.backendserver.payload.response.ElectionMetaResponse;
import pse.election.backendserver.payload.response.ElectionTrusteeResponse;
import pse.election.backendserver.payload.response.ElectionsVoterResponse;
import pse.election.backendserver.payload.response.ResponseBuilder;
import pse.election.backendserver.payload.response.record.ElectionRecord;
import pse.election.backendserver.security.user.UserRole;

/**
 * This class is used as an api controller and handles election information requests. It implements
 * the {@link ElectionDataAPI} interface and is controlled by the {@link FrontController}. This
 * includes meta election information, such as voter lists, or ballot information.
 *
 * @version 1.0
 */
@Component
@Qualifier("ElectionDataController")
public class ElectionDataController implements ElectionDataAPI {

  private static final String ROLE_NOT_FOUND_ERROR_MSG = "The requested role is not supported";

  @Autowired
  private ElectionService electionService;
  @Autowired
  private VoterService voterService;
  @Autowired
  private TrusteeService trusteeService;

  @Autowired
  private ResponseBuilder responseBuilder;

  @Override
  public ElectionMetaResponse getElectionMetaById(Long electionId) {
    return this.responseBuilder.buildElectionMetaResponse(electionId);
  }

  @Override
  public ElectionMetaResponse getElectionMetaByFingerprint(String fingerprint) {
    return this.responseBuilder.buildElectionMetaResponse(
        electionService.getElection(fingerprint).getElectionId()
    );
  }

  @Override
  public Collection<ElectionMetaResponse> getElectionsFromUserByRole(String userEmail,
      String userRole) {
    UserRole role;

    if ((role = parseFromString(userRole)) == null) {
      throw new IllegalArgumentException(ROLE_NOT_FOUND_ERROR_MSG);
    }

    List<Election> requestedElections = switch (role) {
      case VOTER -> voterService.getAllElectionsOfVoter(userEmail);
      case TRUSTEE -> trusteeService.getAllElectionsOfTrustee(userEmail);
      case AUTHORITY -> electionService.getAllElectionsOfAuthority(userEmail);
    };

    return this.responseBuilder.buildMultipleElectionMetaResponse(
        requestedElections.stream().map(Election::getElectionId).toList());
  }

  @Override
  public BallotResponse getElectionBallot(Long electionId) {
    return this.responseBuilder.buildElectionBallotResponse(electionId);
  }

  @Override
  public ElectionTrusteeResponse getElectionTrustees(Long electionId) {
    return this.responseBuilder.buildElectionTrusteeResponse(electionId);
  }

  @Override
  public ElectionsVoterResponse getElectionVoters(Long electionId) {
    return this.responseBuilder.buildElectionVoterInformationResponse(electionId);
  }

  @Override
  public ElectionBallotBoardResponse getElectionBallotBoard(Long electionId) {
    return this.responseBuilder.buildElectionBallotBoardResponse(electionId);
  }

  @Override
  public ElectionRecord getElectionRecord(Long electionId, String recordFormat) {
    try {
      return this.responseBuilder.buildElectionGuardRecord(electionId);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public ElectionDecryptedResultResponse getElectionResult(Long electionId) {
    return this.responseBuilder.buildElectionDecryptedResultResponse(electionId);
  }

  @Override
  public ElectionHashesResponse getElectionHashes(Long electionId) {
    return this.responseBuilder.buildElectionHashesResponse(electionId);
  }

  private UserRole parseFromString(String userRoleString) {
    for (UserRole userRole : UserRole.values()) {
      if (userRole.toString().equalsIgnoreCase(userRoleString)) {
        return userRole;
      }
    }
    return null;
  }
}
