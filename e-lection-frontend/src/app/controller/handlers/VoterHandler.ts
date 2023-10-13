import { ElectionHandler } from './ElectionHandler'
import type { EncryptedBallot } from '@/app/model/ballot'
import { ElectionManifest, VoterElection, type Election } from '@/app/model/election'
import type { IVoterHandler } from './IVoterHandler'
import { ResponseError, RequestError, ResponseErrorType } from './error'

/**
 * The class VoterHandler handles requests to the server over the Web Endpoints that are required by users in the role voter.
 * After every request the {@link Election} object of the election the handler is concerned with is updated accordingly
 * @version 1.0
*/
export class VoterHandler extends ElectionHandler implements IVoterHandler {
  private static readonly baseUrl = '/voter'

  /**
     * Creates a new VoterHandler
     * @param election the election the VoterHandler is concerned with
     */
  public constructor (
    serverUrl: string,
    election?: Election
  ) {
    super(serverUrl, election)
  }

  /**
     * This method calls the Web Endpoint /api/voter/{electionId}/ to post the provided {@link EncryptedBallot} to the server.
     * @param ballot The {@link EncryptedBallot} to be posted to the server
     * @returns the tracking code of the ballot as well as the previous tracking code
     */
  public async postBallot (ballot: EncryptedBallot): Promise<{ trackingCode: string, lastTrackingCode: string }> {
    return await this.axios.post(
      VoterHandler.baseUrl + '/'.concat(this.election.electionId.toString()),
      ballot.stringify()
    )
      .then(async response => response.data.trackingCode !== undefined && response.data.lastTrackingCode !== undefined
        ? await Promise.resolve([response.data.trackingCode, response.data.lastTrackingCode])
        : await Promise.reject(new ResponseError(ResponseErrorType.FIELD_MISSING))
      )
      .then(async ([code, lastCode]) => typeof code === 'string' && typeof lastCode === 'string'
        ? await Promise.resolve({ trackingCode: code, lastTrackingCode: lastCode })
        : await Promise.reject(new ResponseError(ResponseErrorType.FIELD_WITH_WRONG_TYPE))
      )
      .catch(async error => await Promise.reject(this.handleIfAxiosError(error)))
  }

  /**
     * This method calls the Endpoint api/voter/{electionId}/{trackingcode} to set the status of the ballot with the given tracking code to submitted
     * @throws {@link RequestError} if the request was unsuccessful
     */
  public async submitBallot (trackingCode: string): Promise<void> {
    return await this.axios.patch(
      VoterHandler.baseUrl + '/'.concat(this.election.electionId.toString()),
      undefined,
      { params: { trackingCode } }
    )
      .then(async () => { await Promise.resolve() })
      .catch(error => this.handleIfAxiosError(error))
  }

  /**
     * @returns the current election as {@link VoterElection}
     * If the current election is not a {@link VoterElection}, it fetches the missing information and creates a new {@link VoterElection}
     */
  public async getVoterElection (): Promise<VoterElection> { // FIXME: baah das ist mega hässlich so...
    if (this.election instanceof VoterElection) return await Promise.resolve(this.election)
    if (!(this.election instanceof ElectionManifest)) {
      return await this.fetchBallot()
        .then(async () => {
          const plaintextBallot = (this.election.electionMeta as ElectionManifest).ballot?.getPlainTextBallot()
          return plaintextBallot === undefined
            ? await Promise.reject(new RequestError('could not create plaintextBallot'))
            : await Promise.resolve(new VoterElection(this.election, plaintextBallot))
        })
    }
    const ballot = (this.election.electionMeta as ElectionManifest).ballot
    return (ballot !== undefined)
      ? new VoterElection(this.election, ballot.getPlainTextBallot())
      : await this.fetchBallot()
        .then(async () => {
          const plaintextBallot = (this.election.electionMeta as ElectionManifest).ballot?.getPlainTextBallot()
          return plaintextBallot === undefined
            ? await Promise.reject(new RequestError('could not create plaintextBallot'))
            : await Promise.resolve(new VoterElection(this.election, plaintextBallot))
        })
  }
}
