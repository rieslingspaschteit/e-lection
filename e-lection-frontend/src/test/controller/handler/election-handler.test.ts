import { ElectionHandler } from '@/app/controller/handler'
import { Election, ElectionManifest, FinishedElection, OpenElection } from '@/app/model/election'
import { User, UserRole } from '@/app/model/user'
import { Ballot } from '@/app/model/ballot'

import axios, { AxiosError } from 'axios'

import getElectionTestResponse from '../../resources/election-response.json'
import getElectionsTestResponse from '../../resources/elections-response.json'
import getUserInformationTestResponse from '../../resources/user-response.json'
import getTrusteesTestResponse from '../../resources/trustees-response.json'
import getVotersResponse from '../../resources/voters-response.json'
import getBallotResponse from '../../resources/ballot-response.json'
import getBallotBoardResponse from '../../resources/ballot-board-response.json'
import getEligibleToVoteResponse from '../../resources/eligible-to-vote-response.json'
import getResultResponse from '../../resources/result-response.json'
import getRecordResponse from '../../resources/election-record-response.json'
import requestError from '../../resources/request-error.json'
import { RequestError, RequestErrorTypes } from '@/app/controller/handlers/error'

describe('test successful request', () => {
  const axiosInstance = axios.create({
    validateStatus: function (status) {
      return status === 200
    }
  })

  let getSpy: jest.SpyInstance<Promise<unknown>>

  beforeAll(() => {
    jest.spyOn(axios, 'create')
      .mockImplementation(() => axiosInstance)
    jest.spyOn(axios, 'create')
      .mockImplementation(() => axiosInstance)
    getSpy = jest.spyOn(axiosInstance, 'get')
  })

  beforeEach(() => {
    getSpy.mockReset()
  })

  test('election by id', async () => {
    const handler: ElectionHandler = new ElectionHandler('')
    const testResponse = getElectionTestResponse['valid-election-response']
    getSpy.mockResolvedValue(testResponse)

    expect(() => handler.election).toThrow()

    await expect(handler.fromId(1).then(async () => await Promise.resolve(handler.election)))
      .resolves.toStrictEqual(Election.fromJSON(testResponse.data))
  })

  test('elections by role', async () => {
    const handler: ElectionHandler = new ElectionHandler('')
    const testResponse = getElectionsTestResponse['valid-elections-for-voter-role']
    getSpy.mockResolvedValue(testResponse)

    await expect(handler.fetchElections(UserRole.VOTER.toString())).resolves
      .toEqual(testResponse.data.map(election => Election.fromJSON(election)))
  })

  test('user information', async () => {
    const handler: ElectionHandler = new ElectionHandler('')
    const testResponse = getUserInformationTestResponse['valid-user-response']
    getSpy.mockResolvedValue(testResponse)

    await expect(handler.fetchUserInformation().catch(async (error) => await Promise.reject(error)))
      .resolves.toStrictEqual(User.fromJSON(testResponse.data))
  })

  test('trustees', async () => {
    const handler: ElectionHandler = new ElectionHandler('')
    const testElectionResponse = getElectionTestResponse['valid-election-response']
    const testTrusteesResponse = getTrusteesTestResponse['valid-trustees-response']
    getSpy.mockResolvedValueOnce(testElectionResponse)

    await expect(
      handler.fromId(1)
        .then(async () => {
          getSpy.mockResolvedValueOnce(testTrusteesResponse)
          await handler.fetchTrustees()
        })
        .then(async () => {
          return handler.election.electionMeta instanceof ElectionManifest
            ? await Promise.resolve(handler.election.electionMeta.trustees)
            : await Promise.reject(new Error())
        })
    ).resolves.toEqual(testTrusteesResponse.data.trustees)
      .catch()
  })

  test('hashes', async () => {
    const handler: ElectionHandler = new ElectionHandler('')
    const testElectionResponse = getElectionTestResponse['valid-election-response']
    const testVotersResponse = getVotersResponse['valid-hashes-response']

    getSpy.mockResolvedValueOnce(testElectionResponse)

    await expect(
      handler.fromId(1)
        .then(async () => {
          getSpy.mockResolvedValueOnce(testVotersResponse)
          await handler.fetchHashes()
        })
        .then(async () => {
          return handler.election.electionMeta instanceof ElectionManifest
            ? await Promise.resolve(handler.election.electionMeta.hashes?.stringify())
            : await Promise.reject(new Error())
        })
    ).resolves.toStrictEqual(testVotersResponse.data)
  })

  test('ballot', async () => {
    const handler: ElectionHandler = new ElectionHandler('')
    const testElectionResponse = getElectionTestResponse['valid-election-response']
    const testBallotResponse = getBallotResponse['valid-ballot-response']

    getSpy.mockResolvedValueOnce(testElectionResponse)

    await expect(
      handler.fromId(1)
        .then(async () => {
          getSpy.mockResolvedValueOnce(testBallotResponse)
          await handler.fetchBallot()
        })
        .then(async () =>
          handler.election.electionMeta instanceof ElectionManifest
            ? await Promise.resolve(handler.election.electionMeta.ballot)
            : await Promise.reject(new Error())
        )
    ).resolves.toStrictEqual(Ballot.fromJSON(testBallotResponse.data.questions))
  })

  test('ballot-board', async () => {
    const handler: ElectionHandler = new ElectionHandler('')
    const testElectionResponse = getElectionTestResponse['valid-election-response']
    const testBallotBoardResponse = getBallotBoardResponse['valid-ballot-board-response']

    getSpy.mockResolvedValueOnce(testElectionResponse)

    await expect(handler.fromId(1)).resolves.toBeUndefined()

    getSpy.mockResolvedValueOnce(testBallotBoardResponse)

    await expect(handler.fetchBallotBoard()).resolves.toBeUndefined()

    const fetchBallotBoardSpy = jest.spyOn(handler, 'fetchBallotBoard')

    await expect(handler.getOpenElection()).resolves
      .toStrictEqual(new OpenElection(
        Election.fromJSON(testElectionResponse.data),
        testBallotBoardResponse.data.trackingCodes
      )
      )

    expect(fetchBallotBoardSpy).not.toBeCalled()

    fetchBallotBoardSpy.mockReset()
  })

  describe('eligible to vote', () => {
    const testElectionResponse = getElectionTestResponse['valid-election-response']
    const testBallotBoardResponse = getBallotBoardResponse['valid-ballot-board-response']
    const testEligibleToVoteResponse = getEligibleToVoteResponse['valid-response']

    test('with triggered fetchBallotBoard', async () => {
      const handler: ElectionHandler = new ElectionHandler('')
      getSpy
        .mockResolvedValueOnce(testElectionResponse)
        .mockResolvedValueOnce(testBallotBoardResponse)
        .mockResolvedValueOnce(testEligibleToVoteResponse)

      const fetchBallotBoardSpy = jest.spyOn(handler, 'fetchBallotBoard')

      await expect(
        handler.fromId(1)
          .then(async () => { await handler.fetchEligibleToVote() })
          .then(async () => await handler.getOpenElection())
      ).resolves.toStrictEqual(
        new OpenElection(
          Election.fromJSON(testElectionResponse.data),
          testBallotBoardResponse.data.trackingCodes,
          testEligibleToVoteResponse.data.eligibleToVote
        )
      )

      expect(fetchBallotBoardSpy).toBeCalledTimes(1)

      fetchBallotBoardSpy.mockReset()
    })

    test('with already existing OpenElection', async () => {
      const handler: ElectionHandler = new ElectionHandler('')
      getSpy
        .mockResolvedValueOnce(testElectionResponse)
        .mockResolvedValueOnce(testBallotBoardResponse)
        .mockResolvedValueOnce(testEligibleToVoteResponse)

      const fetchBallotBoardSpy = jest.spyOn(handler, 'fetchBallotBoard')

      await expect(handler.fromId(1)
        .then(async () => { await handler.fetchBallotBoard() })
        .then(async () => { await handler.fetchEligibleToVote() })
        .then(async () => await handler.getOpenElection())
      ).resolves.toStrictEqual(
        new OpenElection(
          Election.fromJSON(testElectionResponse.data),
          testBallotBoardResponse.data.trackingCodes,
          testEligibleToVoteResponse.data.eligibleToVote
        )
      )

      expect(fetchBallotBoardSpy).toBeCalledTimes(1)
    })
  })

  test('result', async () => {
    const handler: ElectionHandler = new ElectionHandler('')
    const testElectionResponse = getElectionTestResponse['valid-election-response']
    const testResultResponse = getResultResponse['valid-result-response']

    getSpy
      .mockResolvedValueOnce(testElectionResponse)
      .mockResolvedValueOnce(testResultResponse)

    const fetchResultMock = jest.spyOn(handler, 'fetchResult')

    await expect(
      handler.fromId(1)
        .then(async () => { await handler.fetchResult() })
        .then(async () => await handler.getFinishedElection())
    ).resolves.toStrictEqual(
      new FinishedElection(
        Election.fromJSON(testElectionResponse.data),
        new Map(Object.entries(testResultResponse.data.result)
          .map(([key, val]) => [Number(key), val]))
      )
    )

    expect(fetchResultMock).toBeCalledTimes(1)

    fetchResultMock.mockReset()
  })

  describe('record', () => {
    const testElectionResponse = getElectionTestResponse['valid-election-response']
    const testResultResponse = getResultResponse['valid-result-response']
    const testRecordResponse = getRecordResponse['valid-election-record-response']

    const mock = (): void => {
      getSpy
        .mockResolvedValueOnce(testElectionResponse)
        .mockResolvedValueOnce(testResultResponse)
        .mockResolvedValueOnce(testRecordResponse)
    }

    test('with existing FinishedElection', async () => {
      const handler: ElectionHandler = new ElectionHandler('')

      mock()

      const fetchResultMock = jest.spyOn(handler, 'fetchResult')

      await expect(
        handler.fromId(1)
          .then(async () => { await handler.fetchResult() })
          .then(async () => { await handler.fetchElectionRecord() })
          .then(async () => await handler.getFinishedElection())
      ).resolves.toStrictEqual(
        new FinishedElection(
          Election.fromJSON(testElectionResponse.data),
          new Map(Object.entries(testResultResponse.data.result)
            .map(([key, val]) => [Number(key), val])),
          undefined
        )
      )

      expect(fetchResultMock).toBeCalledTimes(1)
    })

    test('with trigger fetchResult', async () => {
      const handler: ElectionHandler = new ElectionHandler('')

      mock()

      const fetchResultMock = jest.spyOn(handler, 'fetchResult')

      await expect(handler.fromId(1)).resolves.toBeUndefined()

      await expect(handler.fetchElectionRecord()).resolves.toBeUndefined()

      expect(fetchResultMock).toBeCalled()

      await expect(handler.getFinishedElection())
        .resolves.toStrictEqual(
          new FinishedElection(
            Election.fromJSON(testElectionResponse.data),
            new Map(Object.entries(testResultResponse.data.result)
              .map(([key, val]) => [Number(key), val])),
            undefined
          )
        )

      expect(fetchResultMock).toBeCalledTimes(1)
    })
  })
})

describe('test correct error handling', () => {
  const axiosInstance = axios.create({
    validateStatus: function (status) {
      return status === 200
    }
  })

  let getSpy: jest.SpyInstance<Promise<unknown>>

  let handler: ElectionHandler

  beforeAll(() => {
    jest.spyOn(axios, 'create')
      .mockImplementation(() => axiosInstance)
    getSpy = jest.spyOn(axiosInstance, 'get')
  })

  beforeEach(() => {
    handler = new ElectionHandler('')
  })

  describe('for ERR_BAD_REQUEST when fetching', () => {
    const initHandler = async (handler: ElectionHandler): Promise<void> => {
      getSpy.mockResolvedValueOnce(getElectionTestResponse['valid-election-response'])
      await handler.fromId(1)
    }

    const mockRequestError = (): void => {
      getSpy.mockRejectedValue({
        response: requestError['error-bad-request'],
        code: AxiosError.ERR_BAD_REQUEST,
        isAxiosError: true
      })
    }

    const genericErrorTest = async (promise: Promise<any>): Promise<any> => {
      return await promise.catch(async error =>
        error instanceof RequestError
          ? await Promise.reject(error.getType())
          : Promise.resolve
      )
    }

    test('by id', async () => {
      await initHandler(handler)
      mockRequestError()
      await expect(genericErrorTest(handler.fromId(1)))
        .rejects.toBe(RequestErrorTypes.REJECTED)
    })

    test('elections', async () => {
      await initHandler(handler)
      mockRequestError()
      await expect(genericErrorTest(handler.fetchElections('userRole')))
        .rejects.toBe(RequestErrorTypes.REJECTED)
    })

    test('user information', async () => {
      await initHandler(handler)
      mockRequestError()
      await expect(genericErrorTest(handler.fetchUserInformation()))
        .rejects.toBe(RequestErrorTypes.REJECTED)
    })

    test('election', async () => {
      await initHandler(handler)
      mockRequestError()
      await expect(genericErrorTest(handler.fetchElection()))
        .rejects.toBe(RequestErrorTypes.REJECTED)
    })

    test('voters', async () => {
      await initHandler(handler)
      mockRequestError()
      await expect(genericErrorTest(handler.fetchHashes()))
        .rejects.toBe(RequestErrorTypes.REJECTED)
    })

    test('ballot', async () => {
      await initHandler(handler)
      mockRequestError()
      await expect(genericErrorTest(handler.fetchBallot()))
        .rejects.toBe(RequestErrorTypes.REJECTED)
    })

    test('eligible to vote', async () => {
      await initHandler(handler)
      mockRequestError()
      await expect(genericErrorTest(handler.fetchEligibleToVote()))
        .rejects.toBe(RequestErrorTypes.REJECTED)
    })

    test('ballot board', async () => {
      await initHandler(handler)
      mockRequestError()
      await expect(genericErrorTest(handler.fetchBallotBoard()))
        .rejects.toBe(RequestErrorTypes.REJECTED)
    })

    test('election record', async () => {
      await initHandler(handler)
      mockRequestError()
      await expect(genericErrorTest(handler.fetchElectionRecord()))
        .rejects.toBe(RequestErrorTypes.REJECTED)
    })

    test('result', async () => {
      await initHandler(handler)
      mockRequestError()
      await expect(genericErrorTest(handler.fetchResult()))
        .rejects.toBe(RequestErrorTypes.REJECTED)
    })
  })
})
