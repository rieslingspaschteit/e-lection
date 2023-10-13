package pse.election.backendserver.controller;

import pse.election.backendserver.payload.error.exception.UnauthorizedAccessException;
import pse.election.backendserver.payload.request.ElectionCreationRequest;
import pse.election.backendserver.payload.response.AuthorityDecryptionStateResponse;
import pse.election.backendserver.payload.response.AuthorityKeyCeremonyStateResponse;
import pse.election.backendserver.payload.response.ElectionCreationResponse;
import pse.election.backendserver.payload.response.EmptyResponse;

/**
 * This interface provides methods for the authority functionalities of the api. It is implemented
 * by the {@link pse.election.backendserver.controller.app.AuthorityController}.
 *
 * @version 1.0
 */
public interface ElectionAuthorityAPI {

  /**
   * Creation of a new election with the user defined by authorityEmail as election authority.
   *
   * @param authorityEmail  email of the election authority
   * @param creationRequest election request the authority wants to create
   * @return api response containing the election id and state
   */
  ElectionCreationResponse createElection(String authorityEmail,
      ElectionCreationRequest creationRequest) throws UnauthorizedAccessException;

  /**
   * Setter for the next state of the election referenced by the electionId.
   *
   * @param electionId id that references a certain election
   * @param state      is the state to set the election to
   * @return empty body in case of success
   */
  EmptyResponse nextState(Long electionId, String state) throws UnauthorizedAccessException;

  /**
   * Getter for the current key ceremony state of the election referenced by the electionId.
   *
   * @param electionId id that references a certain election
   * @return api response containing the current election state and counts showing the progress of the state
   * */
  AuthorityKeyCeremonyStateResponse getElectionKeyCeremonyState(Long electionId)
      throws UnauthorizedAccessException;

  /**
   * Getter for the current decryption state of the election referenced by the electionId.
   *
   * @param electionId id that references a certain election
   * @return api response containing the current election state and counts showing the progress of the state
   */
  AuthorityDecryptionStateResponse getElectionDecryptionState(Long electionId)
      throws UnauthorizedAccessException;

}

