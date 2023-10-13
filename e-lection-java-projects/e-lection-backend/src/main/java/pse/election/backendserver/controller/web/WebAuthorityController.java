package pse.election.backendserver.controller.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pse.election.backendserver.controller.ElectionAPI;
import pse.election.backendserver.payload.error.exception.UnauthorizedAccessException;
import pse.election.backendserver.payload.request.ElectionCreationRequest;
import pse.election.backendserver.payload.request.StateChangeRequest;
import pse.election.backendserver.payload.response.AuthorityDecryptionStateResponse;
import pse.election.backendserver.payload.response.AuthorityKeyCeremonyStateResponse;
import pse.election.backendserver.payload.response.ElectionCreationResponse;
import pse.election.backendserver.security.user.UserPrincipal;

/**
 * This class is used for managing the "/api/authority/elections" endpoints. This controller is
 * responsible for managing the authority functionalities, including creation requests and state
 * changes.
 *
 * @version 1.0
 */
@RestController
@RequestMapping(path = "/api/authority/elections")
public class WebAuthorityController {

  @Qualifier("FrontController")
  @Autowired
  private ElectionAPI electionAPI;

  /**
   * This method is called by a POST-Request on /api/authority/elections/create and handles election
   * creations.
   *
   * @param electionCreationRequest is the election translated by Spring from the JSON in the
   *                                request
   * @param user                    is the current logged-in user from the security context
   * @return response containing electionId and current state
   */
  @PostMapping(path = "/create", consumes = {"application/json"})
  public ResponseEntity<ElectionCreationResponse> createElection(
      @RequestBody ElectionCreationRequest electionCreationRequest,
      @AuthenticationPrincipal UserPrincipal user) throws UnauthorizedAccessException {
    return new ResponseEntity<>(
        electionAPI.createElection(user.getEmail(), electionCreationRequest), HttpStatus.CREATED);
  }

  /**
   * This method is called by a PATCH-Request on /api/authority/elections/{electionId} and handles
   * election state changes.
   *
   * @param electionId         is the id of the election to be changed
   * @param stateChangeRequest is the state to set the election mapped to the id
   * @return response containing the new election state
   */
  @PatchMapping("/{electionId}")
  public ResponseEntity<?> setElectionState(@PathVariable("electionId") Long electionId,
      @RequestBody StateChangeRequest stateChangeRequest)
      throws UnauthorizedAccessException {
    electionAPI.nextState(electionId, stateChangeRequest.getState());
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  /**
   * This method is called by a GET-Request on /api/authority/elections/{electionId}/key-cer and
   * handles election state requests.
   *
   * @param electionId is the id of the election to get the state from
   * @return response containing the current election state
   */
  @GetMapping("/{electionId}/key-cer")
  public ResponseEntity<AuthorityKeyCeremonyStateResponse> getElectionKeyCeremonyState(
      @PathVariable("electionId") Long electionId)
      throws UnauthorizedAccessException {
    return new ResponseEntity<>(electionAPI.getElectionKeyCeremonyState(electionId), HttpStatus.OK);
  }

  /**
   * This method is called by a GET-Request on /api/authority/elections/{electionId}/decryption and
   * handles election state requests.
   *
   * @param electionId is the id of the election to get the state from
   * @return response containing the current election state
   */
  @GetMapping("/{electionId}/decryption")
  public ResponseEntity<AuthorityDecryptionStateResponse> getElectionDecryptionState(
      @PathVariable("electionId") Long electionId)
      throws UnauthorizedAccessException {
    return new ResponseEntity<>(electionAPI.getElectionDecryptionState(electionId), HttpStatus.OK);
  }


}