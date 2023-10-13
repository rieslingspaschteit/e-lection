package pse.election.backendserver.controller.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pse.election.backendserver.controller.ElectionAPI;
import pse.election.backendserver.payload.error.exception.UnauthorizedAccessException;
import pse.election.backendserver.payload.request.AuxiliaryKeyRequest;
import pse.election.backendserver.payload.request.DecryptionRequest;
import pse.election.backendserver.payload.request.ElgamalKeysAndBackupsRequest;
import pse.election.backendserver.payload.response.ElectionAuxiliaryKeysResponse;
import pse.election.backendserver.payload.response.ElectionEncryptedResultResponse;
import pse.election.backendserver.payload.response.ElgamalKeysAndBackupsResponse;
import pse.election.backendserver.payload.response.EmptyResponse;
import pse.election.backendserver.payload.response.TrusteeDecryptionStateResponse;
import pse.election.backendserver.payload.response.TrusteeKeyCeremonyStateResponse;
import pse.election.backendserver.security.user.UserPrincipal;

/**
 * This class is used for managing the /api/trustee/elections/{electionId} endpoints. It is
 * responsible for managing the trustee functionalities, including the key ceremony and the
 * decryption of an election.
 *
 * @version 1.0
 **/
@RestController
@RequestMapping("api/trustee/elections/{electionId}")
public class WebTrusteeController {

  private final ElectionAPI electionAPI;

  public WebTrusteeController(ElectionAPI electionAPI) {
    this.electionAPI = electionAPI;
  }

  /**
   * This method is called by a GET-Request on /api/trustee/elections/{electionsId}/key-ceremony and
   * handles state requests in the key ceremony.
   *
   * @param electionId is the id of the election to get the state from
   * @return information about the current key ceremony state
   */
  @GetMapping("/key-ceremony")
  public ResponseEntity<TrusteeKeyCeremonyStateResponse> getElectionKeyCeremonyState(
      @PathVariable("electionId") Long electionId,
      @AuthenticationPrincipal UserPrincipal user)
      throws UnauthorizedAccessException {
    return new ResponseEntity<>(
        electionAPI.getElectionKeyCeremonyState(electionId, user.getEmail()), HttpStatus.OK);
  }

  /**
   * This method is called by a GET-Request on /api/trustee/elections/{electionsId}/aux-keys and
   * handles auxiliary keys requests in the key ceremony.
   *
   * @param electionId is the id of the election to get the trustees' auxiliary keys
   * @return collection of the auxiliary keys including their type and the id of the trustee that possesses the key
   */
  @GetMapping("/auxkeys")
  public ResponseEntity<ElectionAuxiliaryKeysResponse> getElectionAuxKeys(
      @PathVariable("electionId") Long electionId,
      @AuthenticationPrincipal UserPrincipal userPrincipal)
      throws UnauthorizedAccessException {
    return new ResponseEntity<>(
        electionAPI.getElectionAuxKeys(electionId, userPrincipal.getEmail()), HttpStatus.OK);
  }

  /**
   * This method is called by a POST-Request on /api/trustee/elections/{electionsId}/aux-keys and
   * handles auxiliary keys submissions in the key ceremony.
   *
   * @param electionId          is the id of the election to save the trustee's auxiliary key in
   * @param auxiliaryKeyRequest is the submitted auxiliary key request
   * @param user                is the trustee submitting the key
   * @return empty response in case of success
   */
  @PostMapping(value = "/auxkeys", consumes = {"application/json"})
  public ResponseEntity<?> setTrusteeAuxKey(@PathVariable("electionId") Long electionId,
      @RequestBody AuxiliaryKeyRequest auxiliaryKeyRequest,
      @AuthenticationPrincipal UserPrincipal user) throws UnauthorizedAccessException {
    electionAPI.setTrusteeAuxKey(electionId, auxiliaryKeyRequest, user.getEmail());
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  /**
   * This method is called by a GET-Request on /api/trustee/elections/{electionsId}/keys-and-backups
   * and handles elgamal key requests in the key ceremony and decryption.
   *
   * @param electionId is the id of the election to get the elgamal keys and backups from
   * @param user       is the currently logged-in user
   * @return collection of election backup keys and public elgamal keys
   */
  @GetMapping("/keys-and-backups")
  public ResponseEntity<ElgamalKeysAndBackupsResponse> getElgamalKeysAndBackups(
      @PathVariable("electionId") Long electionId,
      @AuthenticationPrincipal UserPrincipal user)
      throws UnauthorizedAccessException {
    return new ResponseEntity<>(electionAPI.getElgamalKeysAndBackups(electionId, user.getEmail()),
        HttpStatus.OK);
  }

  /**
   * This method is called by a POST-Request on
   * /api/trustee/elections/{electionsId}/keys-and-backups and handles elgamal keys and election
   * partial key backup submissions in the key ceremony.
   *
   * @param electionId                   is the id of the election to get the elgamal keys and
   *                                     backups from
   * @param user                         is the currently logged-in user
   * @param elgamalKeysAndBackupsRequest is the request object containing the elgamal keys and
   *                                     backups
   * @return collection of election backup keys and public elgamal keys
   */
  @PostMapping("/keys-and-backups")
  public ResponseEntity<?> setElgamalKeysAndBackups(@PathVariable("electionId") Long electionId,
      @RequestBody ElgamalKeysAndBackupsRequest elgamalKeysAndBackupsRequest,
      @AuthenticationPrincipal UserPrincipal user) throws UnauthorizedAccessException {
    electionAPI.setElgamalKeysAndBackups(electionId, elgamalKeysAndBackupsRequest, user.getEmail());
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  /**
   * This method is called by a GET-Request on /api/trustee/elections/{electionsId}/decryption and
   * handles election state requests in the decryption state.
   *
   * @param electionId is the id of the election to get the state from
   * @param user       is the currenty logged in user
   * @return information about the current state of the election's decryption
   */
  @GetMapping("/decryption")
  public ResponseEntity<TrusteeDecryptionStateResponse> getDecryptionState(
      @PathVariable("electionId") Long electionId,
      @AuthenticationPrincipal UserPrincipal user)
      throws UnauthorizedAccessException {
    return new ResponseEntity<>(electionAPI.getDecryptionState(electionId, user.getEmail()),
        HttpStatus.OK);
  }

  /**
   * This method is called by a GET-Request on /api/trustee/elections/{electionsId}/result and
   * handles partial decryption requests in the decryption state.
   *
   * @param electionId    is the id of the election to be decrypted
   * @param userPrincipal is the currently logged in user
   * @return collection of the encrypted options of the tallied and spoiled ballots
   */
  @GetMapping("/result")
  public ResponseEntity<ElectionEncryptedResultResponse> getEncryptedResult(
      @PathVariable("electionId") Long electionId,
      @AuthenticationPrincipal UserPrincipal userPrincipal)
      throws UnauthorizedAccessException {
    return new ResponseEntity<>(
        electionAPI.getEncryptedResult(electionId, userPrincipal.getEmail()), HttpStatus.OK);
  }

  /**
   * This method is called by a POST-Request on /api/trustee/elections/{electionsId}/result and
   * handles partial decryption submission requests in the decryption state.
   *
   * @param electionId        is the id of the election to be decrypted
   * @param decryptionRequest is the partial decryption translated from the request
   * @param user              is the trustee submitting the partial decryption.
   * @return collection of the encrypted options of the tallied and spoiled ballots
   */
  @PostMapping("/result")
  public ResponseEntity<EmptyResponse> setPartialDecryptedResult(
      @PathVariable("electionId") Long electionId,
      @RequestBody DecryptionRequest decryptionRequest,
      @AuthenticationPrincipal UserPrincipal user) throws UnauthorizedAccessException {
    electionAPI.setPartialDecryptionResult(electionId, decryptionRequest, user.getEmail());
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }
}
