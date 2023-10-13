package pse.election.backendserver.controller.app;

import java.util.Collection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Controller;
import pse.election.backendserver.controller.ElectionAPI;
import pse.election.backendserver.controller.ElectionAuthorityAPI;
import pse.election.backendserver.controller.ElectionDataAPI;
import pse.election.backendserver.controller.ElectionTrusteeAPI;
import pse.election.backendserver.controller.ElectionUserAPI;
import pse.election.backendserver.controller.ElectionVoterAPI;
import pse.election.backendserver.payload.error.exception.UnauthorizedAccessException;
import pse.election.backendserver.payload.request.AuxiliaryKeyRequest;
import pse.election.backendserver.payload.request.BallotCommitRequest;
import pse.election.backendserver.payload.request.DecryptionRequest;
import pse.election.backendserver.payload.request.ElectionCreationRequest;
import pse.election.backendserver.payload.request.ElgamalKeysAndBackupsRequest;
import pse.election.backendserver.payload.response.AuthorityDecryptionStateResponse;
import pse.election.backendserver.payload.response.AuthorityKeyCeremonyStateResponse;
import pse.election.backendserver.payload.response.BallotResponse;
import pse.election.backendserver.payload.response.BallotSentResponse;
import pse.election.backendserver.payload.response.ElectionAuxiliaryKeysResponse;
import pse.election.backendserver.payload.response.ElectionBallotBoardResponse;
import pse.election.backendserver.payload.response.ElectionCreationResponse;
import pse.election.backendserver.payload.response.ElectionDecryptedResultResponse;
import pse.election.backendserver.payload.response.ElectionEncryptedResultResponse;
import pse.election.backendserver.payload.response.ElectionHashesResponse;
import pse.election.backendserver.payload.response.ElectionMetaResponse;
import pse.election.backendserver.payload.response.ElectionTrusteeResponse;
import pse.election.backendserver.payload.response.ElectionsVoterResponse;
import pse.election.backendserver.payload.response.ElgamalKeysAndBackupsResponse;
import pse.election.backendserver.payload.response.EmptyResponse;
import pse.election.backendserver.payload.response.TrusteeDecryptionStateResponse;
import pse.election.backendserver.payload.response.TrusteeKeyCeremonyStateResponse;
import pse.election.backendserver.payload.response.UserInformationResponse;
import pse.election.backendserver.payload.response.VoterInformationResponse;
import pse.election.backendserver.payload.response.record.ElectionRecord;
import pse.election.backendserver.security.ElectionAuthorizationEvaluator;
import pse.election.backendserver.security.user.UserRole;

/**
 * This class is the front controller of the api. It is responsible for intercepting every request
 * to the api and validating the request's permission. After validation, the front controller
 * delegates the request to the specific api controller responsible for handling the request or
 * throws an {@link UnauthorizedAccessException} exception.
 *
 * @version 1.0
 */

@Qualifier("FrontController")
@Primary
@Controller
public class FrontController implements ElectionAPI {

  private static final String UNAUTHORIZED_PERMISSION_MSG = "User must have the role: %s for the requested operation.";
  private static final String UNAUTHORIZED_PERMISSION_ELECTION_MSG = "User must have the role: %s in the requested "
          + "election.";
  private static final String NOT_ASSIGNED_ELECTION_BY_ID = "User is not a participant in the election with id: %d.";
  private static final String NOT_ASSIGNED_ELECTION_BY_FINGERPRINT = "User is not a participant in the election with "
          + "fingerprint: %s.";


  @Autowired
  private ElectionAuthorizationEvaluator electionAuthorizationEvaluator;
  @Autowired
  @Qualifier("AuthorityController")
  private ElectionAuthorityAPI electionAuthorityAPI;
  @Autowired
  @Qualifier("ElectionDataController")
  private ElectionDataAPI electionDataAPI;
  @Autowired
  @Qualifier("TrusteeController")
  private ElectionTrusteeAPI electionTrusteeAPI;
  @Autowired
  @Qualifier("UserController")
  private ElectionUserAPI electionUserAPI;
  @Autowired
  @Qualifier("VoterController")
  private ElectionVoterAPI electionVoterAPI;

  @Autowired
  private AuthorityController authorityController;


  @Override
  public ElectionCreationResponse createElection(String authorityEmail,
      ElectionCreationRequest creationRequest) throws UnauthorizedAccessException {

    if (!electionAuthorizationEvaluator.hasRole(UserRole.AUTHORITY)) {
      throw new UnauthorizedAccessException(
          String.format(UNAUTHORIZED_PERMISSION_MSG, UserRole.AUTHORITY));
    }

    return authorityController.createElection(authorityEmail, creationRequest);
  }

  @Override
  public EmptyResponse nextState(Long electionId, String state) throws UnauthorizedAccessException {

    if (!electionAuthorizationEvaluator.hasRoleInElection(UserRole.AUTHORITY, electionId)) {
      throw new UnauthorizedAccessException(
          String.format(UNAUTHORIZED_PERMISSION_ELECTION_MSG, UserRole.AUTHORITY));
    }

    return electionAuthorityAPI.nextState(electionId, state);
  }

  @Override
  public AuthorityDecryptionStateResponse getElectionDecryptionState(Long electionId)
      throws UnauthorizedAccessException {
    if (!electionAuthorizationEvaluator.hasRoleInElection(UserRole.AUTHORITY, electionId)) {
      throw new UnauthorizedAccessException(
          String.format(UNAUTHORIZED_PERMISSION_ELECTION_MSG, UserRole.AUTHORITY));
    }

    return electionAuthorityAPI.getElectionDecryptionState(electionId);
  }

  @Override
  public ElectionMetaResponse getElectionMetaById(Long electionId)
      throws UnauthorizedAccessException {
    if (!electionAuthorizationEvaluator.isParticipantInElection(electionId)) {
      throw new UnauthorizedAccessException(String.format(NOT_ASSIGNED_ELECTION_BY_ID, electionId));
    }

    return this.electionDataAPI.getElectionMetaById(electionId);
  }

  @Override
  public ElectionMetaResponse getElectionMetaByFingerprint(String fingerprint)
      throws UnauthorizedAccessException {
    if (!electionAuthorizationEvaluator.isParticipantInElection(fingerprint)) {
      throw new UnauthorizedAccessException(
          String.format(NOT_ASSIGNED_ELECTION_BY_FINGERPRINT, fingerprint));
    }
    return this.electionDataAPI.getElectionMetaByFingerprint(fingerprint);
  }

  @Override
  public Collection<ElectionMetaResponse> getElectionsFromUserByRole(String userEmail,
      String userRole) {
    return electionDataAPI.getElectionsFromUserByRole(userEmail, userRole);
  }

  @Override
  public BallotResponse getElectionBallot(Long electionId) throws UnauthorizedAccessException {
    if (!electionAuthorizationEvaluator.isParticipantInElection(electionId)) {
      throw new UnauthorizedAccessException(String.format(NOT_ASSIGNED_ELECTION_BY_ID, electionId));
    }

    return electionDataAPI.getElectionBallot(electionId);
  }

  @Override
  public ElectionTrusteeResponse getElectionTrustees(Long electionId)
      throws UnauthorizedAccessException {
    if (!electionAuthorizationEvaluator.isParticipantInElection(electionId)) {
      throw new UnauthorizedAccessException(String.format(NOT_ASSIGNED_ELECTION_BY_ID, electionId));
    }

    return electionDataAPI.getElectionTrustees(electionId);
  }

  @Override
  public ElectionsVoterResponse getElectionVoters(Long electionId)
      throws UnauthorizedAccessException {
    if (!electionAuthorizationEvaluator.isParticipantInElection(electionId)) {
      throw new UnauthorizedAccessException(String.format(NOT_ASSIGNED_ELECTION_BY_ID, electionId));
    }

    return electionDataAPI.getElectionVoters(electionId);
  }

  @Override
  public ElectionBallotBoardResponse getElectionBallotBoard(Long electionId)
      throws UnauthorizedAccessException {
    if (!electionAuthorizationEvaluator.isParticipantInElection(electionId)) {
      throw new UnauthorizedAccessException(String.format(NOT_ASSIGNED_ELECTION_BY_ID, electionId));
    }

    return electionDataAPI.getElectionBallotBoard(electionId);
  }

  @Override
  public ElectionRecord getElectionRecord(Long electionId, String recordFormat)
      throws UnauthorizedAccessException {
    if (!electionAuthorizationEvaluator.isParticipantInElection(electionId)) {
      throw new UnauthorizedAccessException(String.format(NOT_ASSIGNED_ELECTION_BY_ID, electionId));
    }

    return electionDataAPI.getElectionRecord(electionId, recordFormat);
  }

  @Override
  public ElectionDecryptedResultResponse getElectionResult(Long electionId)
      throws UnauthorizedAccessException {
    if (!electionAuthorizationEvaluator.isParticipantInElection(electionId)) {
      throw new UnauthorizedAccessException(String.format(NOT_ASSIGNED_ELECTION_BY_ID, electionId));
    }

    return electionDataAPI.getElectionResult(electionId);
  }

  @Override
  public ElectionHashesResponse getElectionHashes(Long electionId)
      throws UnauthorizedAccessException {
    if (!electionAuthorizationEvaluator.isParticipantInElection(electionId)) {
      throw new UnauthorizedAccessException(String.format(NOT_ASSIGNED_ELECTION_BY_ID, electionId));
    }
    return electionDataAPI.getElectionHashes(electionId);
  }

  @Override
  public TrusteeKeyCeremonyStateResponse getElectionKeyCeremonyState(Long electionId, String email)
      throws UnauthorizedAccessException {
    if (!electionAuthorizationEvaluator.hasRoleInElection(UserRole.TRUSTEE, electionId)) {
      throw new UnauthorizedAccessException(
          String.format(UNAUTHORIZED_PERMISSION_ELECTION_MSG, UserRole.TRUSTEE));
    }
    return electionTrusteeAPI.getElectionKeyCeremonyState(electionId, email);
  }

  @Override
  public AuthorityKeyCeremonyStateResponse getElectionKeyCeremonyState(Long electionId)
          throws UnauthorizedAccessException {
    if (!electionAuthorizationEvaluator.hasRoleInElection(UserRole.AUTHORITY, electionId)) {
      throw new UnauthorizedAccessException(
              String.format(UNAUTHORIZED_PERMISSION_ELECTION_MSG, UserRole.AUTHORITY));
    }

    return electionAuthorityAPI.getElectionKeyCeremonyState(electionId);
  }

  @Override
  public ElectionAuxiliaryKeysResponse getElectionAuxKeys(Long electionId, String email)
      throws UnauthorizedAccessException {
    if (!electionAuthorizationEvaluator.hasRoleInElection(UserRole.TRUSTEE, electionId)) {
      throw new UnauthorizedAccessException(
          String.format(UNAUTHORIZED_PERMISSION_ELECTION_MSG, UserRole.TRUSTEE));
    }
    return electionTrusteeAPI.getElectionAuxKeys(electionId, email);
  }

  @Override
  public EmptyResponse setTrusteeAuxKey(Long electionId, AuxiliaryKeyRequest auxiliaryKeyRequest,
      String email) throws UnauthorizedAccessException {
    if (!electionAuthorizationEvaluator.hasRoleInElection(UserRole.TRUSTEE, electionId)) {
      throw new UnauthorizedAccessException(
          String.format(UNAUTHORIZED_PERMISSION_ELECTION_MSG, UserRole.TRUSTEE));
    }
    return electionTrusteeAPI.setTrusteeAuxKey(electionId, auxiliaryKeyRequest, email);
  }

  @Override
  public ElgamalKeysAndBackupsResponse getElgamalKeysAndBackups(Long electionId,
      String trusteeEmail) throws UnauthorizedAccessException {
    if (!electionAuthorizationEvaluator.hasRoleInElection(UserRole.TRUSTEE, electionId)) {
      throw new UnauthorizedAccessException(
          String.format(UNAUTHORIZED_PERMISSION_ELECTION_MSG, UserRole.TRUSTEE));
    }
    return electionTrusteeAPI.getElgamalKeysAndBackups(electionId, trusteeEmail);
  }

  @Override
  public EmptyResponse setElgamalKeysAndBackups(Long electionId,
      ElgamalKeysAndBackupsRequest keysAndBackupsRequest, String email)
      throws UnauthorizedAccessException {
    if (!electionAuthorizationEvaluator.hasRoleInElection(UserRole.TRUSTEE, electionId)) {
      throw new UnauthorizedAccessException(
          String.format(UNAUTHORIZED_PERMISSION_ELECTION_MSG, UserRole.TRUSTEE));
    }

    return electionTrusteeAPI.setElgamalKeysAndBackups(electionId, keysAndBackupsRequest, email);
  }

  @Override
  public TrusteeDecryptionStateResponse getDecryptionState(Long electionId, String email)
      throws UnauthorizedAccessException {
    if (!electionAuthorizationEvaluator.hasRoleInElection(UserRole.TRUSTEE, electionId)) {
      throw new UnauthorizedAccessException(
          String.format(UNAUTHORIZED_PERMISSION_ELECTION_MSG, UserRole.TRUSTEE));
    }
    return electionTrusteeAPI.getDecryptionState(electionId, email);
  }

  @Override
  public ElectionEncryptedResultResponse getEncryptedResult(Long electionId, String email)
      throws UnauthorizedAccessException {
    if (!electionAuthorizationEvaluator.hasRoleInElection(UserRole.TRUSTEE, electionId)) {
      throw new UnauthorizedAccessException(
          String.format(UNAUTHORIZED_PERMISSION_ELECTION_MSG, UserRole.TRUSTEE));
    }
    return electionTrusteeAPI.getEncryptedResult(electionId, email);
  }

  @Override
  public EmptyResponse setPartialDecryptionResult(Long electionId,
      DecryptionRequest decryptionRequest, String email) throws UnauthorizedAccessException {
    if (!electionAuthorizationEvaluator.hasRoleInElection(UserRole.TRUSTEE, electionId)) {
      throw new UnauthorizedAccessException(
          String.format(UNAUTHORIZED_PERMISSION_ELECTION_MSG, UserRole.TRUSTEE));
    }
    return electionTrusteeAPI.setPartialDecryptionResult(electionId, decryptionRequest, email);
  }

  @Override
  public UserInformationResponse getUserRoles(String email) {
    return electionUserAPI.getUserRoles(email);
  }

  @Override
  public VoterInformationResponse getVoterDetails(Long electionId, String email)
      throws UnauthorizedAccessException {
    if (!electionAuthorizationEvaluator.isParticipantInElection(electionId)) {
      throw new UnauthorizedAccessException(String.format(NOT_ASSIGNED_ELECTION_BY_ID, electionId));
    }
    return electionVoterAPI.getVoterDetails(electionId, email);
  }

  @Override
  public BallotSentResponse setVotersBallotSent(Long electionId,
      BallotCommitRequest ballotCommitRequest, String voterEmail)
      throws UnauthorizedAccessException {
    if (!electionAuthorizationEvaluator.isParticipantInElection(electionId)) {
      throw new UnauthorizedAccessException(String.format(NOT_ASSIGNED_ELECTION_BY_ID, electionId));
    }
    return electionVoterAPI.setVotersBallotSent(electionId, ballotCommitRequest, voterEmail);
  }

  @Override
  public EmptyResponse setVotersBallotSubmitted(Long electionId, String trackingCode, String email)
      throws UnauthorizedAccessException {
    return electionVoterAPI.setVotersBallotSubmitted(electionId, trackingCode, email);
  }
}
