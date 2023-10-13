/* eslint-disable @typescript-eslint/ban-ts-comment */
import { Election, FinishedElection, OpenElection } from '@/app/model/election'
import type { IElectionHandler } from '@/app/controller/handlers/IElectionHandler'
import { RequestError, ResponseError, ResponseErrorType, RequestErrorTypes } from './error'
import axios, { AxiosError, type AxiosInstance } from 'axios'

import { Ballot } from '@/app/model/ballot'
import { User } from '@/app/model/user'
import { ManifestHashes } from '@/app/model/election/manifest/manifestHashes'

/**
 * ElectionHandler implements the {@link IElectionHandler} interface
 * and uses the Web Endpoints to fetch the required information from a server.
 * After every request the {@link Election} object of the election
 * the handler is concerned with is updated accordingly
 * @version 1.0
 *
 */
export class ElectionHandler implements IElectionHandler {
  protected static readonly OK = 200
  protected static readonly CREATED = 201
  protected static readonly EMPTY_RESPONSE = 204
  private static readonly getResultEndpoint = '/elections/{electionId}/result'
  private static readonly getElectionRecordEndpoint = '/elections/{electionId}/election-record/{type}'
  private static readonly getBallotBoardEndpoint = '/elections/{electionId}/ballot-board'
  private static readonly getBallotEndpoint = '/elections/{electionId}/manifest/ballot'
  private static readonly getHashesEndpoint = '/elections/{electionId}/manifest/hashes'
  private static readonly getTrusteesEndpoint = '/elections/{electionId}/manifest/trustees'
  private static readonly getElectionByIdEndpoint = '/elections'
  private static readonly getElectionEndpoint = '/elections'
  private static readonly getElectionByFingerprintEndpoint = '/elections'
  private static readonly getEligibleToVoteEndpoint = '/elections/{electionId}/vote'
  private static readonly getUserEndpoint = '/user'
  private static readonly baseURL = '/api'
  protected axios: AxiosInstance

  /**
   * The constructor sets the  election that the handler is concerned with
   * @param election the current election
   */
  constructor (
    protected readonly serverUrl: string,
    protected _election?: Election
  ) {
    this.axios = axios.create({
      baseURL: serverUrl.concat(ElectionHandler.baseURL),
      withCredentials: true,
      validateStatus: function (status) {
        return status === ElectionHandler.OK ||
          status === ElectionHandler.CREATED ||
          status === ElectionHandler.EMPTY_RESPONSE
      }

    })
  }

  /**
   * Handles http request errors or returns the error to handle if it is not a http error
   * @param error the error that shall be handled
   * @throws {@link RequestError} with an specific type
   */
  protected handleIfAxiosError (error: any): RequestError | any {
    return !(error.isAxiosError as boolean)
      ? error
      : error.code === AxiosError.ERR_BAD_REQUEST
        ? new RequestError(error.message, RequestErrorTypes.REJECTED)
        : error.code === AxiosError.ECONNABORTED || error.code === AxiosError.ERR_NETWORK
          ? new RequestError(error.message, RequestErrorTypes.CONNECTION_FAILED)
          : new RequestError(error.message, RequestErrorTypes.OTHER)
  }

  /**
   * The constructor fetches the {@link ElectionMeta} for the provided election
   * and creates the {@link Election} object
   * @param electionId the id of the current election
   * @throws {@link RequestError} if the request was unsuccessful
   */
  public async fromId (electionId: number): Promise<void> {
    await this.axios.get(
      ElectionHandler.getElectionByIdEndpoint,
      { params: { electionId } }
    ).then(async (response) => {
      this.election = Election.fromJSON(response.data) // FIXME: handle error
      await Promise.resolve()
    }).catch(async (error) => await Promise.reject(this.handleIfAxiosError(error)))
  }

  public async fetchElectionByFingerprint (fingerprint: string): Promise<void> {
    await this.axios.get(
      ElectionHandler.getElectionByFingerprintEndpoint,
      { params: { fingerprint } }
    )
      .then(async response => {
        this.election = Election.fromJSON(response.data)
        await Promise.resolve()
      })
      .catch(async error => await Promise.reject(this.handleIfAxiosError(error)))
  }

  /**
   * This method calls the Web Endpoint api/elections/{userRole}
   * to get every election the user is connected to in a specific role
   * @param userRole the role of user in the current context
   * @throws {@link RequestError} if the request was unsuccessful
   */
  public async fetchElections (userRole: string): Promise<Election[]> {
    return await this.axios.get(
      ElectionHandler.getElectionEndpoint,
      { params: { userRole } }
    )
      .then(async response => Array.isArray(response.data)
        ? await Promise.resolve(response.data
          .map((electionData: any) => Election.fromJSON(electionData))
        )
        : await Promise.reject(
          new RequestError(response.data, RequestErrorTypes.OTHER)
        )
      )
      .catch(async error => await Promise.reject(this.handleIfAxiosError(error)))
  }

  /**
   * This method calls the Web Endpoint api/user/
   * to get information about the user from the OIDC provider
   * @throws {@link RequestError} if the request was unsuccessful
   */
  public async fetchUserInformation (): Promise<User> {
    return await this.axios.get(ElectionHandler.getUserEndpoint)
      .then(async (response) => {
        return await Promise.resolve(User.fromJSON(response.data)) // FIXME: handle error
      })
      .catch(async error => await Promise.reject(this.handleIfAxiosError(error)))
  }

  /**
   * This method calls the Web Endpoint api/elections/{electionId}
   * to provide the {@link ElectionMeta} and the state of the current election
   * @throws {@ink RequestError} if the request was unsuccessful
   */
  public async fetchElection (): Promise<void> {
    await this.fromId(this.election.electionId)
  }

  /**
   * This method calls the Web Endpoint api/elections/{electionID}/manifest/trustees
   * to get information about the trustees of the election
   * @throws {@link RequestError} if the request was unsuccessful
   */
  public async fetchTrustees (): Promise<void> {
    await this.axios.get(
      ElectionHandler.getTrusteesEndpoint
        .replace('{electionId}', this.election.electionId.toString())
    )
      .then(async response => {
        const trustees = response.data.trustees
        const isBotEnabled = response.data.isBotEnabled

        if (trustees === undefined || isBotEnabled === undefined) {
          return await Promise.reject(new ResponseError(ResponseErrorType.FIELD_MISSING))
        }

        if (
          !Array.isArray(trustees) ||
        !trustees.every(val => typeof val === 'string') ||
        typeof isBotEnabled !== 'boolean'
        ) {
          return await Promise.reject(
            new ResponseError(ResponseErrorType.FIELD_WITH_WRONG_TYPE)
          )
        }

        this.election = this.election.cloneWithMeta(
          this.election.electionMeta.cloneWithTrustees(trustees, isBotEnabled)
        )

        await Promise.resolve()
      })
      .catch(async error => await Promise.reject(this.handleIfAxiosError(error)))
  }

  /**
     * This method calls the Web Endpoint api/elections/{electionID}/manifest/voters
     * to get information about the voters of the election
     * @throws {@link RequestError} if the request was unsuccessful
     */
  public async fetchHashes (): Promise<void> {
    await this.axios.get(ElectionHandler.getHashesEndpoint
      .replace('{electionId}', this.election.electionId.toString())
    )
      .then(async response => {
        this.election = this.election.cloneWithMeta(
          this.election.electionMeta.cloneWithHashes(ManifestHashes.fromJSON(response.data))
        )
        await Promise.resolve()
      })
      .catch(async error => await Promise.reject(this.handleIfAxiosError(error)))
  }

  /**
   * This method calls the Web Endpoint api/elections/{electionID}/manifest/ballot
   * to get the ballot of the election
   * @throws {@link RequestError} if the request was unsuccessful
   */
  public async fetchBallot (): Promise<void> {
    this.axios.defaults.baseURL = this.serverUrl + ElectionHandler.baseURL
    await this.axios.get(
      ElectionHandler.getBallotEndpoint
        .replace('{electionId}', this.election.electionId.toString())
    )
      .then(async response => {
        this.election = this.election.cloneWithMeta(
          this.election.electionMeta
            .cloneWithBallot(Ballot.fromJSON(response.data.questions))
        )
        await Promise.resolve()
      })
      .catch(async error => await Promise.reject(this.handleIfAxiosError(error)))
  }

  /**
   * fetches a boolean indicating wether the user is (still) allowed to vote in the election
eligibleToVote   * from api/user/{electionId}/vote
   * @throws {@link RequestError} if the request was unsuccessful
   */
  public async fetchEligibleToVote (): Promise<void> {
    this.election instanceof OpenElection
      ? await this.updateEligibleToVoteOnOpenElection()
      : await this.fetchBallotBoard()
        .then(async () => { await this.updateEligibleToVoteOnOpenElection() })
  }

  private async updateEligibleToVoteOnOpenElection (): Promise<void> {
    await this.axios.get(
      ElectionHandler.getEligibleToVoteEndpoint
        .replace('{electionId}', this.election.electionId.toString())
    )
      .then(response =>
        typeof response.data.eligibleToVote === 'boolean'
          ? response.data.eligibleToVote
          : Promise.reject(new ResponseError(ResponseErrorType.FIELD_WITH_WRONG_TYPE))
      )
      .then(async eligibleToVote => {
        const old = this.election as OpenElection
        this.election = new OpenElection(old, old.submittedBallots, eligibleToVote)
        console.log('fetched eligibleToVote: ', eligibleToVote)

        await Promise.resolve()
      })
      .catch(async error => await Promise.reject(this.handleIfAxiosError(error)))
  }

  /**
   * Fetches the submitted tracking-codes for this election
   * from api/elections/{electionID}/ballot-board
   * and constructs a new OpenElection of updates the OpenElection
   * @throws {@link RequestError} if the request was unsuccessful
   */
  public async fetchBallotBoard (): Promise<void> {
    await this.axios.get(
      ElectionHandler.getBallotBoardEndpoint
        .replace('{electionId}', this.election.electionId.toString())
    )
      .then(async response =>
        Array.isArray(response.data.trackingCodes)
          ? await Promise.resolve(response.data.trackingCodes as any[])
          : await Promise.reject(new ResponseError(ResponseErrorType.FIELD_WITH_WRONG_TYPE))
      )
      .then(async trackingCodes =>
        trackingCodes.every(code => typeof code === 'string')
          ? await Promise.resolve(trackingCodes as string[])
          : await Promise.reject(new ResponseError(ResponseErrorType.FIELD_WITH_WRONG_TYPE))
      )
      .then(async trackingCodes => {
        this.election = new OpenElection(this.election, trackingCodes)
        await Promise.resolve()
      })
      .catch(async error => await Promise.reject(this.handleIfAxiosError(error)))
  }

  /**
   * This method calls the Web Endpoint api/elections/{electionId}/election-record
   * to provide an ElectionGuard Election Record
   * @throws {@link RequestError} if the request was unsuccessful
   */
  public async fetchElectionRecord (): Promise<void> {
    this.election instanceof FinishedElection
      ? await this.updateElectionRecordOnFinishedElection()
      : await this.fetchResult()
        .then(async () => { await this.updateElectionRecordOnFinishedElection() })
  }

  private async updateElectionRecordOnFinishedElection (): Promise<void> {
    await this.axios.get(
      ElectionHandler.getElectionRecordEndpoint
        .replace('{electionId}', this.election.electionId.toString())
        .replace('{type}', 'electionguard'),
      {
        responseType: 'blob'
      } // FIXME remove magic string
    )
      .then(async record => {
        this.election = (this.election as FinishedElection).cloneWithElectionRecord(record.data)
        await Promise.resolve()
      })
      .catch(async error => await Promise.reject(this.handleIfAxiosError(error)))
  }

  /**
   * This method calls the Web Endpoint api/elections/{electionId}/result
   * to get the fully decrypted result of the current elections
   * @throws {@link RequestError} if the request was unsuccessful
   */
  public async fetchResult (): Promise<void> {
    await this.axios.get(
      ElectionHandler.getResultEndpoint
        .replace('{electionId}', this.election.electionId.toString())
    )
      .then(async response => response.data.result !== undefined
        ? await Promise.resolve(response.data.result)
        : await Promise.reject(new ResponseError(ResponseErrorType.FIELD_MISSING))
      )
      .then(async result => await Promise.resolve(new Map(Object.entries(result)
        // @ts-expect-error
        .map(([key, val]) => [Number(key), val.map(i => Number(i))])))
      )
      .then(async result => {
        this.election = this.election instanceof FinishedElection
          ? new FinishedElection(this.election, result, this.election.electionRecord)
          : new FinishedElection(this.election, result)
        await Promise.resolve()
      })
      .catch(async error => await Promise.reject(this.handleIfAxiosError(error)))
  }

  /**
   * @returns the current election as {@link OpenElection}
   * If the current election is not an {@link OpenElection},
   * it fetches the missing information and creates a new {@link OpenElection}
   */
  public async getOpenElection (): Promise<OpenElection> {
    return this.election instanceof OpenElection
      ? await Promise.resolve(this.election)
      : await this.fetchBallotBoard()
        .then(async () => await Promise.resolve(this.election as OpenElection))
  }

  /**
   * @returns the current election as {@link FinishedElection}
   * If the current election is not a {@link FinishedElection},
   * it fetches the missing information and creates a new {@link FinishedElection}
   */
  public async getFinishedElection (): Promise<FinishedElection> {
    return this.election instanceof FinishedElection
      ? await Promise.resolve(this.election)
      : await this.fetchResult()
        .then(async () => await Promise.resolve(this.election as FinishedElection))
  }

  /**
   * Returns all fetched information about the current election
   * as an {@link Election} object
   * @returns the current election
   * @throws error if the election is not defined
   */
  public get election (): Election {
    if (this._election == null) {
      throw new Error('handler not correct initialized') // FIXME: maybe a specific error for this case?
    }
    return this._election
  }

  /**
   * sets the new election of this handler
   */
  protected set election (election: Election) {
    this._election = election
  }
}
