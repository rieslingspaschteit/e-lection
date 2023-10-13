package pse.election.backendserver.controller;

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

/**
 * This interface provides methods for all trustee functionalities in the api. This contains methods
 * for executing a key ceremony and election decryption.
 *
 * @version 1.0
 */
public interface ElectionTrusteeAPI {

  /**
   * Getter for the state of the Key Ceremony of an election referenced by an electionId.
   *
   * @param electionId the id of the election
   * @param email      the email of the trustee
   * @return the current state of the Key Ceremony
   */
  TrusteeKeyCeremonyStateResponse getElectionKeyCeremonyState(Long electionId, String email)
      throws UnauthorizedAccessException;

  /**
   * Getter for the auxiliary keys of an election referenced by an electionId.
   *
   * @param electionId the id of the election
   * @return api response containing the auxiliary keys of the election
   */
  ElectionAuxiliaryKeysResponse getElectionAuxKeys(Long electionId, String email)
      throws UnauthorizedAccessException;

  /**
   * Setter for the auxiliary key of a certain trustee of an election referenced by an electionId.
   *
   * @param electionId          the id of the election
   * @param auxiliaryKeyRequest is the request containing the auxiliary key information
   * @param email               is the email of the trustee
   * @return empty api response in case of success
   */
  EmptyResponse setTrusteeAuxKey(Long electionId, AuxiliaryKeyRequest auxiliaryKeyRequest,
      String email) throws UnauthorizedAccessException;

  /**
   * Getter for all ElGamal keys and the election partial key backups of an election referenced by
   * an electionId.
   *
   * @param electionId   the id of the election
   * @param trusteeEmail is the email of the trustee to get the keys from
   * @return api response containing the ElGamal keys and the election partial key backups
   */
  ElgamalKeysAndBackupsResponse getElgamalKeysAndBackups(Long electionId, String trusteeEmail)
      throws UnauthorizedAccessException;

  /**
   * Setter for the ElGamal keys and the election partial key backups of a trustee for an election
   * referenced by an electionId.
   *
   * @param electionId            the id of the election
   * @param keysAndBackupsRequest the request body containing the ElGamal keys and election partial
   *                              key backups
   * @param email                 is the email of the trustee
   * @return empty api response in case of success
   */
  EmptyResponse setElgamalKeysAndBackups(Long electionId,
      ElgamalKeysAndBackupsRequest keysAndBackupsRequest, String email)
      throws UnauthorizedAccessException;

  /**
   * Getter for the decryption state of an election referenced by an electionId.
   *
   * @param electionId the id of the election
   * @param email      is the email of the trustee
   * @return api response with the current decryption state
   */
  TrusteeDecryptionStateResponse getDecryptionState(Long electionId, String email)
      throws UnauthorizedAccessException;

  /**
   * Getter for the encryption result of an election referenced by an electionId.
   *
   * @param electionId the id of the election
   * @return empty api response in case of success
   */
  ElectionEncryptedResultResponse getEncryptedResult(Long electionId, String email)
      throws UnauthorizedAccessException;

  /**
   * Setter for the partial decryption results of a trustee for an election referenced by an
   * electionId.
   *
   * @param electionId        the id of the election
   * @param decryptionRequest containing the partial decryption
   * @param email             is the email of the trustee
   * @return empty api response on case of success
   */
  EmptyResponse setPartialDecryptionResult(Long electionId, DecryptionRequest decryptionRequest,
      String email) throws UnauthorizedAccessException;

}
