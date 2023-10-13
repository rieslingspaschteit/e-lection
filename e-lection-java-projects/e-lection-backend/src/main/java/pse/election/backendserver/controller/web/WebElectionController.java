package pse.election.backendserver.controller.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.zip.ZipOutputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pse.election.backendserver.controller.ElectionAPI;
import pse.election.backendserver.payload.error.exception.UnauthorizedAccessException;
import pse.election.backendserver.payload.response.BallotResponse;
import pse.election.backendserver.payload.response.ElectionBallotBoardResponse;
import pse.election.backendserver.payload.response.ElectionDecryptedResultResponse;
import pse.election.backendserver.payload.response.ElectionHashesResponse;
import pse.election.backendserver.payload.response.ElectionMetaResponse;
import pse.election.backendserver.payload.response.ElectionTrusteeResponse;
import pse.election.backendserver.payload.response.ElectionsVoterResponse;
import pse.election.backendserver.payload.response.VoterInformationResponse;
import pse.election.backendserver.payload.response.record.ElectionRecord;
import pse.election.backendserver.security.user.UserPrincipal;

/**
 * This class is used for managing the /api/elections endpoints and is responsible for handling
 * requests on election information that are public for all participants.
 *
 * @version 1.0
 */
@RestController
@RequestMapping("/api/elections")
public class WebElectionController {

  @Autowired
  private ElectionAPI electionAPI;

  /**
   * This method is called by a GET-Request on /api/election/{electionId} and handles election
   * metadata requests. *
   *
   * @param electionId is the id of the election to get the metadata from
   * @return response containing public information about the election
   */

  @RequestMapping(params = "electionId", method = RequestMethod.GET)
  public ResponseEntity<ElectionMetaResponse> getElectionMetaDataById(@RequestParam Long electionId)
      throws UnauthorizedAccessException {
    return new ResponseEntity<>(electionAPI.getElectionMetaById(electionId), HttpStatus.OK);
  }

  /**
   * This method is called by a GET-Request on /api/election/{fingerprint} and handles election
   * metadata requests.
   *
   * @param fingerprint is the fingerprint of the election to get the metadata from
   * @return response containing public information about the election
   */
  @RequestMapping(params = "fingerprint", method = RequestMethod.GET)
  public ResponseEntity<ElectionMetaResponse> getElectionMetaDataByFingerprint(
      @RequestParam String fingerprint) throws UnauthorizedAccessException {
    return new ResponseEntity<>(electionAPI.getElectionMetaByFingerprint(fingerprint),
        HttpStatus.OK);
  }


  /**
   * This method is called by a GET-Request on /api/election/{userRole} and handles election
   * metadata requests of elections, the currently logged-in user is assigned a certain role to.
   *
   * @param user     is the currently logged-in user loaded from the security context
   * @param userRole is the role of the user to search for in the database
   * @return a collection of metadata to every election the parsed user has been assigned the parsed role
   */
  @RequestMapping(params = "userRole", method = RequestMethod.GET)
  public ResponseEntity<Collection<ElectionMetaResponse>> getElectionsFromUserByRole(
      @AuthenticationPrincipal UserPrincipal user,
      @RequestParam String userRole) {
    return new ResponseEntity<>(electionAPI.getElectionsFromUserByRole(user.getEmail(), userRole),
        HttpStatus.OK);
  }

  /**
   * This method is called by a GET-Request on /api/voter/{electionId}/ and handles voter details
   * request.
   *
   * @param electionId is the id referencing the voter's election
   * @param user       is the currently logged-in voter
   * @return information about the voter in context of the election
   */
  @GetMapping("/{electionId}/vote")
  public ResponseEntity<VoterInformationResponse> getVoterDetails(
      @PathVariable("electionId") Long electionId,
      @AuthenticationPrincipal UserPrincipal user) throws UnauthorizedAccessException {
    return new ResponseEntity<>(electionAPI.getVoterDetails(electionId, user.getEmail()),
        HttpStatus.OK);
  }

  /**
   * This method is called by a GET-Request on /api/election/{electionId}/manifest/ballot and
   * handles election ballot request.
   *
   * @param electionId is the id of the election to get the ballot from
   * @return a collection of questions forming the ballot
   */
  @GetMapping("/{electionId}/manifest/ballot")
  public ResponseEntity<BallotResponse> getElectionBallot(
      @PathVariable("electionId") Long electionId)
      throws UnauthorizedAccessException {
    return new ResponseEntity<>(electionAPI.getElectionBallot(electionId), HttpStatus.OK);
  }

  /**
   * This method is called by a GET-Request on /api/election/{electionId}/manifest/trustees and
   * handles participating trustees requests.
   *
   * @param electionId is the id of the election to get the trustee emails from
   * @return a collection containing the email of the election's trustees
   */
  @GetMapping("/{electionId}/manifest/trustees")
  public ResponseEntity<ElectionTrusteeResponse> getElectionTrustees(
      @PathVariable("electionId") Long electionId)
      throws UnauthorizedAccessException {
    return new ResponseEntity<>(electionAPI.getElectionTrustees(electionId), HttpStatus.OK);
  }

  /**
   * This method is called by a GET-Request on /api/election/{electionId}/manifest/voters and
   * handles request about the participating voters.
   *
   * @param electionId is the id of the election to get the trustee emails from
   * @return a collection containing the email of the election's trustees
   */
  @GetMapping("/{electionId}/manifest/voters")
  public ResponseEntity<ElectionsVoterResponse> getElectionVoters(
      @PathVariable("electionId") Long electionId)
      throws UnauthorizedAccessException {
    return new ResponseEntity<>(electionAPI.getElectionVoters(electionId), HttpStatus.OK);
  }

  /**
   * This method is called by a GET-Request on /api/election/{electionId}/manifest/ballot-board and
   * handles request about the submitted ballots.
   *
   * @param electionId is the id of the election to get the trustee emails from
   * @return a collection containing the email of the election's trustees
   */
  @GetMapping("/{electionId}/ballot-board")
  public ResponseEntity<ElectionBallotBoardResponse> getElectionBallotBoard(
      @PathVariable("electionId") Long electionId)
      throws UnauthorizedAccessException {
    return new ResponseEntity<>(electionAPI.getElectionBallotBoard(electionId), HttpStatus.OK);
  }

  /**
   * This method is called by a GET-Request on /api/election/{electionId}/manifest/election-record
   * and handles requests on a record of an election.
   *
   * @param electionId   is the id of the election to get the trustee emails from
   * @param recordFormat is the format of the record
   * @return a collection containing the email of the election's trustees
   */
  @RequestMapping(path = "/{electionId}/election-record/{type}", method = RequestMethod.GET,
          produces = "application/zip")
  public ResponseEntity<InputStreamResource> getElectionRecord(
      @PathVariable("electionId") Long electionId,
      @PathVariable("type") String recordFormat) throws UnauthorizedAccessException, IOException {

    ElectionRecord electionRecord = electionAPI.getElectionRecord(electionId, recordFormat);

    ZipOutputStream zipOutputStream = electionRecord.zipOutputStream();
    File file = electionRecord.file();
    zipOutputStream.flush();

    InputStreamResource resource = new InputStreamResource(new FileInputStream(file));

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName())
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .contentLength(file.length())
        .body(resource);
  }

  /**
   * This method is called by a GET-Request on /api/election/{electionId}/manifest/result and
   * handles request about the election result.
   *
   * @param electionId is the id of the election to get the trustee emails from
   * @return a collection containing the email of the election's trustees
   */
  @GetMapping("/{electionId}/result")
  public ResponseEntity<ElectionDecryptedResultResponse> getElectionResult(
      @PathVariable("electionId") Long electionId)
      throws UnauthorizedAccessException {
    return new ResponseEntity<>(electionAPI.getElectionResult(electionId), HttpStatus.OK);
  }

  @GetMapping("/{electionId}/manifest/hashes")
  public ResponseEntity<ElectionHashesResponse> getElectionHashes(
      @PathVariable("electionId") Long electionId) throws UnauthorizedAccessException {
    return new ResponseEntity<>(electionAPI.getElectionHashes(electionId), HttpStatus.OK);
  }

}
