import type { IElectionHandler } from './IElectionHandler'
import type { EncryptedBallot } from '@/app/model/ballot'
import type { VoterElection } from '@/app/model/election'

/**
 * The interface IVoterHandler defines the properties of an ElectionHandler that is concerned with the requests required by users in the role voter
 * @version 1.0
 */
export interface IVoterHandler extends IElectionHandler {

  /**
     * This method posts an {@link EncryptedBallot}.
     * @param ballot the {@link EncryptedBallot} to be posted to the server
     * @returns the tracking code of the ballot as well as the previous tracking code
     */
  postBallot: (ballot: EncryptedBallot) => Promise<{ trackingCode: string, lastTrackingCode: string }>

  /*
    /**
     * This method fetches the list of voters and list of trustees and adds them to the {@link ElectionManifest} of the provided {@link SpoiledBallot}.
     * @param spoiledBallot the {@link SpoiledBallot} the information are to be added to
     * @returns the spoiledBallot with added information

    spoilBallot(spoiledBallot: SpoiledBallot): Promise<SpoiledBallot>;
    */

  /**
     * This method sets the status of the ballot with the given tracking code to submitted
     */
  submitBallot: (trackingCode: string) => Promise<void>

  /**
     * @returns the current election as {@link VoterElection}
     * If the current election is not a {@link VoterElection}, it fetches the missing information and creates a new {@link VoterElection}
     */
  getVoterElection: () => Promise<VoterElection>
}
