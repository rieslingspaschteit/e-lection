import type { Election, FinishedElection, OpenElection } from '@/app/model/election'
import type { User } from '@/app/model/user'

/**
 * IElection defines the properties of a handler class that is concerned with the requests required by e-lection users in any role.
 * Classes implementing the IElectionHandler interface must temporarily save the state of the election the handler is concerned with.
 * This election is referred to as the 'current election' in the following documentation
 * @version 1.0
 */
export interface IElectionHandler {

  /**
     * This method fetches every election the user is connected to in a specific role
     * @param userRole the role of user in the current context
     * @returns the elections the user is concerned with in the specified role
     */
  fetchElections: (userRole: string) => Promise<Election[]>

  /**
     * @returns the roles the user has in different elections
     */
  fetchUserInformation: () => Promise<User>

  /**
     * This method calls fetchElection() to create an {@link Election} object for the election with the provided id
     * @param electionId the id of the election the handler is concerned with
     */
  fromId: (electionId: number) => Promise<void>

  /**
     * This method fetches the {@link ElectionMeta} and the state of the current election
     */
  fetchElection: () => Promise<void>

  /**
     * This method fetches information about the voters of the current election
     */
  fetchHashes: () => Promise<void>

  /**
     * This method fetches information about the trustees of the current election
     */
  fetchTrustees: () => Promise<void>

  /**
     * This method fetches the ballot of the current election
     */
  fetchBallot: () => Promise<void>

  /**
     * fetches a boolean indicating wether the user is (still) allowed to vote in the election
     * from api/user/{electionId}/vote
     * @throws {@link RequestError} if the request was unsuccessful
     */
  fetchEligibleToVote: () => Promise<void>

  /**
     * Fetches the submitted tracking-codes for this election from api/user/{electionID}/ballot-board
     * and constructs a new OpenElection of updates the OpenElection
     * @throws {@link RequestError} if the request was unsuccessful
     */
  fetchBallotBoard: () => Promise<void>

  /**
     * This method fetches the decrypted result of the current election
     */
  fetchResult: () => Promise<void>

  /**
     * This method fetches an ElectionGuard Election Record of the current election
     * @returns the ElectionGuard Election Record
     */
  fetchElectionRecord: () => Promise<void>

  /**
     * Returns all fetched information about the current election as an {@link Election} object
     * @returns the current election
     */
  get election(): Election | undefined

  /**
     * @returns the current election as {@link OpenElection}
     * If the current election is not an {@link OpenElection}, it fetches the missing information and creates a new {@link OpenElection}
     */
  getOpenElection: () => Promise<OpenElection>

  /**
     * @returns the current election as {@link FinishedElection}
     * If the current election is not a {@link FinishedElection}, it fetches the missing information and creates a new {@link FinishedElection}
     */
  getFinishedElection: () => Promise<FinishedElection>
}
