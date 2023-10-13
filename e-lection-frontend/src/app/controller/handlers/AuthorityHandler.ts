import { ElectionHandler } from './ElectionHandler'
import type { ElectionManifest } from '@/app/model/election'
import type { IAuthorityHandler } from './IAuthorityHandler'
import { AuthorityElection, Election } from '@/app/model/election'
import { ResponseError, ResponseErrorType } from './error'
import { DecryptionState, ElectionState } from '@/app/model/election/states'

/**
 * The class AuthorityHandler handles requests to the server over the Web Endpoints that are required by users in the role authority.
 * After every request the {@link Election} object of the election the handler is concerned with is updated accordingly
 * @version 1.0
 */
export class AuthorityHandler extends ElectionHandler implements IAuthorityHandler {
  private static readonly baseUrl = '/authority/elections'
  private static readonly patch = AuthorityHandler.baseUrl + '/{electionId}'
  private static readonly getDecryption = AuthorityHandler.baseUrl + '/{electionId}/decryption'
  private static readonly getKeyCer = AuthorityHandler.baseUrl + '/{electionId}/key-cer'
  private static readonly create = AuthorityHandler.baseUrl + '/create'

  /**
     * Creates a new AuthorityHandler
     * @param election the election the AuthorityHandler is concerned with
     */
  public constructor (
    serverUrl: string,
    election?: Election
  ) {
    super(serverUrl, election)
  }

  /**
     * This method calls the Web Endpoint api/authority/create to post the {@link ElectionManifest} to the server as a newly created election
     * The election is afterwards set as the election the handler is concerned with
     * @throws {@link RequestError} if the request was unsuccessful
     */
  public async postElection (manifest: ElectionManifest): Promise<void> {
    await this.axios.post(
      AuthorityHandler.create, manifest.stringify()
    )
      .then(async response =>
        response.data.electionId !== undefined && response.data.state !== undefined
          ? await Promise.resolve([response.data.electionId, response.data.state])
          : await Promise.reject(new ResponseError(ResponseErrorType.FIELD_MISSING))
      )
      .then(async ([id, state]) => {
        this.election = Election.fromJSON({
          electionId: id,
          fingerprint: null,
          electionMeta: manifest.toElectionMeta(),
          state
        })
        await Promise.resolve()
      })
      .catch(async error => await Promise.reject(this.handleIfAxiosError(error)))
  }

  /**
     * This method calls the Web Endpoint api/authority/elections/{electionId}/key-cer to provide the state and the attendance count of the key ceremony
     * of the current election
     * @throws {@link RequestError} if the request was unsuccessful
     */
  public async fetchKeyCeremonyAttendance (): Promise<void> {
    await this.axios.get(
      AuthorityHandler.getKeyCer
        .replace('{electionId}', this.election.electionId.toString())
    )
      .then(async response =>
        Number.isInteger(Number(response.data.keyCerCount)) && response.data.keyCerState !== undefined
          ? await Promise.resolve([response.data.keyCerCount, response.data.keyCerState])
          : await Promise.reject(new ResponseError(ResponseErrorType.FIELD_MISSING))
      )
      .then(async response => {
        if (!(this.election instanceof AuthorityElection)) {
          this.election = new AuthorityElection(this.election)
        }
        return await Promise.resolve(response)
      })
      .then(async ([count, state]) => {
        const old = this.getAuthorityElection()
        this.election = new AuthorityElection(
          this.election, state, count,
          old.decryptionState, old.decryptionCount
        )
        await Promise.resolve()
      })
      .catch(async error => await Promise.reject(this.handleIfAxiosError(error)))
  }

  /**
     * This method calls the Web Endpoint api/authority/elections/{electionId}/decryption to provide the state and the attendance count of the decryption phase
     * of the current election
     * @throws {@link RequestError} if the request was unsuccessful
     */
  public async fetchDecryptionAttendance (): Promise<void> {
    await this.axios.get(
      AuthorityHandler.getDecryption
        .replace('{electionId}', this.election.electionId.toString())
    )
      .then(async response => response.data.decState !== undefined && Number.isInteger(response.data.decCount)
        ? await Promise.resolve([response.data.decCount, response.data.decState])
        : await Promise.reject(new ResponseError(ResponseErrorType.FIELD_MISSING))
      )
      .then(async response => {
        if (!(this.election instanceof AuthorityElection)) {
          this.election = new AuthorityElection(this.election)
        }
        return await Promise.resolve(response)
      })
      .then(async ([count, state]) => {
        const old = this.getAuthorityElection()
        this.election = new AuthorityElection(
          this.election, old.keyCerState, old.keyCerCount, state, count
        )
        await Promise.resolve()
      })
      .catch(async error => await Promise.reject(this.handleIfAxiosError(error)))
  }

  /**
     * This method calls the Web Endpoint api/authority/elections/{electionId} to open the current election
     * @throws {@link RequestError} if the request was unsuccessful
     */
  public async openElection (): Promise<void> {
    await this.patchElectionState(ElectionState.OPEN.name)
  }

  /**
     * This method calls the Web Endpoint api/authority/elections/{electionId} to set the decryption to the next phase for the trustees
     * @throws {@link RequestError} if the request was unsuccessful
     */
  public async updateDecryption (): Promise<void> {
    await this.patchElectionState(DecryptionState.PP_DECRYPTION.toString())
  }

  private async patchElectionState (state: string): Promise<void> {
    await this.axios.patch(
      AuthorityHandler.patch.replace('{electionId}', this.election.electionId.toString()),
      { state }
    )
      .then(async () => { await Promise.resolve() })
      .catch(async error => await Promise.reject(this.handleIfAxiosError(error)))
  }

  /**
     * @returns the current election as AuthorityElection
     * @throws {@link RequestError} if current election is not an AuthorityElection
     */
  public getAuthorityElection (): AuthorityElection {
    return this.election instanceof AuthorityElection
      ? this.election
      : new AuthorityElection(this.election)
  }
}
