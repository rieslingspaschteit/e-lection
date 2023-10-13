package pse.election.backendserver.controller;

import java.util.Collection;
import pse.election.backendserver.payload.error.exception.UnauthorizedAccessException;
import pse.election.backendserver.payload.response.BallotResponse;
import pse.election.backendserver.payload.response.ElectionBallotBoardResponse;
import pse.election.backendserver.payload.response.ElectionDecryptedResultResponse;
import pse.election.backendserver.payload.response.ElectionHashesResponse;
import pse.election.backendserver.payload.response.ElectionMetaResponse;
import pse.election.backendserver.payload.response.ElectionTrusteeResponse;
import pse.election.backendserver.payload.response.ElectionsVoterResponse;
import pse.election.backendserver.payload.response.record.ElectionRecord;

/**
 * This interface provides election information functionalities. It can be used to access
 * information and content of an election referenced by an id.
 *
 * @version 1.0
 */
public interface ElectionDataAPI {

  /**
   * Getter for the election metadata from an election referenced by an id. This is needed for
   * describing an election.
   *
   * @param electionId is the id referencing the election
   * @return api response containing election meta data
   */
  ElectionMetaResponse getElectionMetaById(Long electionId) throws UnauthorizedAccessException;

  /**
   * Getter for the election metadata from an election referenced by a fingerprint. This is needed
   * for describing an election.
   *
   * @param fingerprint is the fingerprint of an election
   * @return api response containing election meta data
   */
  ElectionMetaResponse getElectionMetaByFingerprint(String fingerprint)
      throws UnauthorizedAccessException;

  /**
   * Getter for multiple election meta information of elections, the user has been assigned a
   * certain role to.
   *
   * @param userRole  is the role the user in the returned elections
   * @param userEmail is the email of the user
   * @return collection of election meta information of elections, the user has been assigned the parsed role to.
   */
  Collection<ElectionMetaResponse> getElectionsFromUserByRole(String userEmail, String userRole);

  /**
   * Getter for the ballot of an election referenced by an id.
   *
   * @param electionId the id of the election
   * @return the ballot of the election
   */
  BallotResponse getElectionBallot(Long electionId) throws UnauthorizedAccessException;

  /**
   * Getter for all the trustees associated to an election referenced by an electionId.
   *
   * @param electionId the id of the election
   * @return collection of trustee emails from the referenced election
   */
  ElectionTrusteeResponse getElectionTrustees(Long electionId) throws UnauthorizedAccessException;

  /**
   * Getter for all the voters associated to an election referenced by an electionId.
   *
   * @param electionId the id of the election
   * @return collection of voter emails from the referenced election
   */
  ElectionsVoterResponse getElectionVoters(Long electionId) throws UnauthorizedAccessException;

  /**
   * Getter for the ballot board of an election referenced by an electionId.
   *
   * @param electionId the id of the election
   * @return ballot board contain all tracking codes of submitted ballots from the referenced election
   * */
  ElectionBallotBoardResponse getElectionBallotBoard(Long electionId)
      throws UnauthorizedAccessException;

  /**
   * Getter for the election record of an election referenced by an electionId.
   *
   * @param electionId   the id of the election
   * @param recordFormat is the format of the record
   * @return an election record hat can be used to verify an election using ElectionGuard
   */
  ElectionRecord getElectionRecord(Long electionId, String recordFormat)
      throws UnauthorizedAccessException;

  /**
   * Getter for result of an election referenced by an electionId.
   *
   * @param electionId the id of the election
   * @return the result of the election referenced by the electionId
   */
  ElectionDecryptedResultResponse getElectionResult(Long electionId)
      throws UnauthorizedAccessException;

  /**
   * Getter for the election hashes of the election. Read more about this in the api specification.
   *
   * @param electionId is the id of the election
   * @return hashes of the election
   */
  ElectionHashesResponse getElectionHashes(Long electionId) throws UnauthorizedAccessException;
}
