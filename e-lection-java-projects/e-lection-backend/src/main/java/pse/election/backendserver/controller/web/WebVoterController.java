package pse.election.backendserver.controller.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pse.election.backendserver.controller.ElectionAPI;
import pse.election.backendserver.payload.error.exception.UnauthorizedAccessException;
import pse.election.backendserver.payload.request.BallotCommitRequest;
import pse.election.backendserver.payload.response.BallotSentResponse;
import pse.election.backendserver.security.user.UserPrincipal;

/**
 * This class is used for managing the /api/voter/{electionId} endpoints. It is responsible for
 * managing the voter functionalities, including the submission of ballots.
 *
 * @version 1.0
 */
@RestController
@RequestMapping("/api/voter/{electionId}")
public class WebVoterController {

  @Autowired
  private ElectionAPI electionAPI;

  /**
   * This method is called by a POST-Request on /api/voter/{electionId}/ and handles voter ballot
   * commitments. This means that the ballot is stored in the system as a spoiled ballot, until the
   * {@link #submitBallot(Long, String, UserPrincipal)} method is called.
   *
   * @param electionId          is the id referencing the voter's election
   * @param ballotCommitRequest is the submitted ballot request
   * @return tracking-code of the last submitted ballot as well as the tracking-code of the currently submitted ballot
   */
  @PostMapping()
  public ResponseEntity<BallotSentResponse> setBallotSent(
      @PathVariable("electionId") Long electionId,
      @RequestBody BallotCommitRequest ballotCommitRequest,
      @AuthenticationPrincipal UserPrincipal userPrincipal)
      throws UnauthorizedAccessException {
    return new ResponseEntity<>(
        electionAPI.setVotersBallotSent(electionId, ballotCommitRequest, userPrincipal.getEmail()),
        HttpStatus.OK
    );
  }

  /**
   * This method is called by a PATCH-Request on /api/voter/{electionId}/{trackingcode} and handles
   * voter ballot submissions.
   *
   * @param electionId   is the id referencing the voter's election
   * @param trackingCode is a ssh256 hash referencing a committed ballot
   * @param user         is the currently logged-in voter
   * @return information about the voter in context of the election
   */
  @PatchMapping(params = "trackingCode")
  public ResponseEntity<?> submitBallot(@PathVariable("electionId") Long electionId,
      @RequestParam String trackingCode,
      @AuthenticationPrincipal UserPrincipal user)
      throws UnauthorizedAccessException {
    electionAPI.setVotersBallotSubmitted(electionId, trackingCode, user.getEmail());
    return new ResponseEntity<>(
        HttpStatus.NO_CONTENT
    );
  }

}
