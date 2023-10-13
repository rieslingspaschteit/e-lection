package pse.election.backendserver.controller;

import pse.election.backendserver.payload.error.exception.UnauthorizedAccessException;
import pse.election.backendserver.payload.request.BallotCommitRequest;
import pse.election.backendserver.payload.response.BallotSentResponse;
import pse.election.backendserver.payload.response.EmptyResponse;
import pse.election.backendserver.payload.response.VoterInformationResponse;

/**
 * This interface provides methods for the voter functionalities of the api. This includes voter
 * information (e.g. if he already submitted a ballot to an election) and voting abilities.
 *
 * @version 1.0
 */
public interface ElectionVoterAPI {

  /**
   * Getter for voter information in context of an election.
   *
   * @param electionId id of an election
   * @param email      email of the user
   * @return api response containing the information if the voter has already submitted a ballot to an election
   */
  VoterInformationResponse getVoterDetails(Long electionId, String email)
      throws UnauthorizedAccessException;

  /**
   * Setter for an encrypted ballot of a user for a certain election referenced by an electionId.
   *
   * @param electionId          id of the election
   * @param ballotCommitRequest contains the cipher of the ballot, the individual proofs, the
   *                            accumulated proofs, the device Information and the date
   * @return api response containing the trackingCode of the last encrypted ballot and the user's sent ballot.
   */
  BallotSentResponse setVotersBallotSent(Long electionId, BallotCommitRequest ballotCommitRequest,
      String email) throws UnauthorizedAccessException;

  /**
   * Setter for the final submit of a ballot of a voter to an election referenced by an electionId.
   *
   * @param electionId   id of the election
   * @param trackingCode trackingCode of the voter's ballot
   * @return empty response in case of success
   */
  EmptyResponse setVotersBallotSubmitted(Long electionId, String trackingCode, String email)
      throws UnauthorizedAccessException;
}
