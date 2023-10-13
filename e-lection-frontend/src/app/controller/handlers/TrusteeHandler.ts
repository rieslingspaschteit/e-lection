import { DecryptionElection, KeyCeremonyElection, type Election } from '@/app/model/election'
import { KeyCeremonyState } from '@/app/model/election/states'
import { ElectionHandler } from './ElectionHandler'
import { RequestError, RequestErrorTypes } from './error'

/**
 * The class TrusteeHandler handles requests to the server over the Web Endpoints that are required by users in the role trustee.
 * After every request the {@link Election} object of the election the handler is concerned with is updated accordingly
 * @version 1.0
*/
export class TrusteeHandler extends ElectionHandler {
  private static readonly baseUrl = '/trustee/elections'
  private static readonly resultEndpoint = TrusteeHandler.baseUrl + '/{electionId}/result'
  private static readonly EPKBEndpoint = TrusteeHandler.baseUrl + '/{electionId}/keys-and-backups'
  private static readonly AuxKeysEndpoint = TrusteeHandler.baseUrl + '/{electionId}/auxkeys'
  private static readonly getDecryptionState = TrusteeHandler.baseUrl + '/{electionId}/decryption'
  private static readonly getKeyCeremonyState = TrusteeHandler.baseUrl + '/{electionId}/key-ceremony'

  /**
     * Creates a new TrusteeHandler
     * @param election the election the TrusteeHandler is concerned with
     */
  public constructor (
    serverUrl: string,
    election?: Election
  ) {
    super(serverUrl, election)
  }

  /**
     * This method calls the Web Endpoint api/trustee/{electionId}/keyceremony to fetch the phase of the key ceremony
     * If the key ceremony is in the state EPKB it calls the Web Endpoint api/trusteee/{electionId}/auxkeys to fetch the auxiliary keys
     * of the other trustees
     * If the key ceremony is in the state FINISHED it calls the Web Endpoint api/trusteee/{electionId}/epkb to fetch the EPKBs
     * of the other trustees
     * @throws {@link RequestError} if the request was unsuccessful
     */
  public async fetchKeyCeremony (): Promise<void> {
    await this.axios.get(this.urlWithId(TrusteeHandler.getKeyCeremonyState))
      .then(async response =>
        await Promise.resolve([response.data.state, response.data.waiting])
      )
      .then(async ([state, waiting]) => { // FIXME: check before
        console.log(state)
        if (this.election instanceof KeyCeremonyElection) {
          this.election.waiting = waiting
          this.election = this.election.cloneWithState(state)
        } else {
          this.election = new KeyCeremonyElection(this.election, state, waiting)
        }
        await Promise.resolve()
      })
      .catch(async error => await Promise.reject(this.handleIfAxiosError(error)))
  }

  /**
     * fetches the aux-keys provided by the other trustees
     * @returns a resolved void promise, or a rejected promise with an error
     */
  public async fetchAuxKeys (): Promise<void> {
    await this.axios.get(this.urlWithId(TrusteeHandler.AuxKeysEndpoint))
      .then(async response => this.election instanceof KeyCeremonyElection
        ? await Promise.resolve(response)
        : await this.fetchKeyCeremony()
          .then(async () => await Promise.resolve(response))
      )
      .then(async response => {
        this.election = (this.election as KeyCeremonyElection).cloneWithAuxKeys(response.data)
        await Promise.resolve()
      })
      .catch(async error => await Promise.reject(this.handleIfAxiosError(error)))
  }

  /**
     * fetches the keys and backups provided by the other trustees
     */
  public async fetchEBKB (): Promise<void> {
    return await this.axios.get(this.urlWithId(TrusteeHandler.EPKBEndpoint))
      .then(async response => this.election instanceof KeyCeremonyElection
        ? await Promise.resolve(response)
        : await this.fetchKeyCeremony()
          .then(async () => await Promise.resolve(response)))
      .then(response => {
        this.election = (this.election as KeyCeremonyElection).cloneWithEPKB(response.data)
      })
      .catch(error => this.handleIfAxiosError(error))
  }

  /**
     * If the key ceremony is in the state AUX_KEYS, this method calls the Web Endpoint api/trustee/{electionId}/auxkeys to post the auxiliary key
     * of the current election to the server.
     * If the key ceremony is in the state EPKB, it calls the Web Endpoint api/trustees/{electionId}/epkb to post the EPKBs of the current election
     * to the server
     * @throws {@link RequestError} if the request was unsuccessful
     */
  public async postKeyCeremony (): Promise<void> {
    !(this.election instanceof KeyCeremonyElection)
      ? await Promise.reject(new RequestError(RequestErrorTypes.OTHER))
      : this.election.state === KeyCeremonyState.AUX_KEYS
        ? await this.postKeyCeremonyGeneric(
          this.urlWithId(TrusteeHandler.AuxKeysEndpoint),
          this.election.committedAuxKeys // FIXME: check!
        )
        : this.election.state === KeyCeremonyState.EPKB
          ? await this.postKeyCeremonyGeneric(
            this.urlWithId(TrusteeHandler.EPKBEndpoint),
            this.election.committedEPKB // FIXME: check
          )
          : await Promise.reject(new RequestError(RequestErrorTypes.OTHER))
  }

  private async postKeyCeremonyGeneric (url: string, data?: object): Promise<void> {
    return await this.axios.post(url, data)
      .then(async () => { await Promise.resolve() })
      .catch(error => this.handleIfAxiosError(error))
  }

  /**
     * This method calls the Web Endpoint api/trustee/{electionId}/decryption to get the phase of the decrpytion of the current election
     * @throws {@link RequestError} if the request was unsuccessful
     */
  public async fetchDecryptionState (): Promise<void> {
    await this.axios.get(this.urlWithId(TrusteeHandler.getDecryptionState))
      .then(async response => {
        if (this.election instanceof DecryptionElection) {
          this.election.cloneWithState(response.data.state, response.data.waiting) // FIXME: check
        } else {
          console.log('state', response.data.state)
          this.election = new DecryptionElection(
            this.election,
            response.data.state,
            response.data.waiting
          )
        }
        await Promise.resolve()
      })
      .catch(async error => await Promise.reject(this.handleIfAxiosError(error)))
  }

  /**
     * It calls the Web Endpoint api/trustee/{electionId}/result to get the encrypted result of the election
     * @throws {@link RequestError} if the request was unsuccessful
     */
  public async fetchEncryptions (): Promise<void> {
    await this.axios.get(this.urlWithId(TrusteeHandler.resultEndpoint))
      .then(async response => this.election instanceof DecryptionElection
        ? await Promise.resolve(response)
        : await this.fetchDecryptionState()
          .then(async () => await Promise.resolve(response)))
      .then(response => {
        this.election = (this.election as DecryptionElection).cloneWithEncryptions(response.data)
      })
      .catch(async error => await Promise.reject(this.handleIfAxiosError(error)))
  }

  /**
     * This method calls the Web Endpoint api/trustee/{electionId}/result to post the partial decryptions or partial partial decryptions
     *  of the result of the current election to the server
     * @throws {@link RequestError} if the request was unsuccessful
     */
  public async postDecryptions (): Promise<void> {
    this.election instanceof DecryptionElection
      ? await this.axios.post(
        this.urlWithId(TrusteeHandler.resultEndpoint),
        this.election.committedDecryptions
      )
        .then(async () => { await Promise.resolve() })
        .catch(async error => await Promise.reject(this.handleIfAxiosError(error)))
      : await Promise.reject(new RequestError(RequestErrorTypes.OTHER))
  }

  /**
     * @returns the current election as {@link KeyCeremonyElection}
     * If the current election is not a {@link KeyCeremonyElection}, it fetches the missing information and creates a new {@link KeyCeremonyElection}
     */
  public async getKeyCeremonyElection (): Promise<KeyCeremonyElection> {
    return this.election instanceof KeyCeremonyElection
      ? this.election
      : await this.fetchKeyCeremony()
        .then(async () => await Promise.resolve(this.election as KeyCeremonyElection))
  }

  /**
     * @returns the current election as {@link DecryptionElection}
     * If the current election is not a {@link DecryptionElection}, it fetches the missing information and creates a new {@link DecryptionElection}
     */
  public async getDecryptionElection (): Promise<DecryptionElection> {
    return this.election instanceof DecryptionElection
      ? this.election
      : await this.fetchDecryptionState()
        .then(async () => await Promise.resolve(this.election as DecryptionElection))
  }

  private urlWithId (url: string): string {
    return url.replace('{electionId}', this.election.electionId.toString())
  }
}
