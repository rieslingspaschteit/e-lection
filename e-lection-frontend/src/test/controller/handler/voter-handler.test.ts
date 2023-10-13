/* eslint-disable @typescript-eslint/no-non-null-assertion */
import { VoterHandler } from '@/app/controller/handler'
import { Ballot, EncryptedBallot } from '@/app/model/ballot'
import { Election, VoterElection } from '@/app/model/election'
import { globalContext } from '@/app/utils/cryptoConstants'
import axios from 'axios'
import { expect } from '@jest/globals'
import {
  ConstantChaumPedersenProofKnownNonce,
  DisjunctiveChaumPedersenProofKnownNonce,
  ElGamalCiphertext,
  ExpandedGenericChaumPedersenProof
} from 'electionguard'

import electionJson from '../../resources/election-payload.json'
import voterHandlerData from '../../resources/voter-handler-data.json'
import ballotResponse from '../../resources/ballot-response.json'

const axiosInstance = axios.create()
let getSpy: jest.SpyInstance
let postSpy: jest.SpyInstance
let patchSpy: jest.SpyInstance

let handler: VoterHandler
const election = Election.fromJSON(electionJson['valid-no-optionals'])

beforeAll(() => {
  jest.spyOn(axios, 'create')
    .mockReturnValue(axiosInstance)
  getSpy = jest.spyOn(axiosInstance, 'get')
  postSpy = jest.spyOn(axiosInstance, 'post')
  patchSpy = jest.spyOn(axiosInstance, 'patch')
})

beforeEach(() => {
  getSpy.mockReset()
  postSpy.mockReset()
  patchSpy.mockReset()
  handler = new VoterHandler('', election)
})

describe('test successful request', () => {
  test('post ballot', async () => {
    postSpy.mockResolvedValue({
      data: { trackingCode: 'abc', lastTrackingCode: 'cba' },
      status: 200
    })

    getSpy.mockResolvedValue({ data: { ballotId: 1 }, status: 200 })

    const encBallot = voterHandlerData['encrypted-ballot']
    jest.spyOn(EncryptedBallot, 'fromJSON')
      .mockReturnValue(
        new EncryptedBallot(
          new Map(
            Object.entries(encBallot.cipherText)
              .map(([qi, encs]) => [
                Number(qi),
                new Map(
                  encs.map((enc, i) => [
                    i, new ElGamalCiphertext(
                      globalContext.createElementModP(enc.pad)!,
                      globalContext.createElementModP(enc.data)!
                    )
                  ])
                )
              ])
          ),
          new Map(
            Object.entries(encBallot.individualProofs)
              .map(([qi, p]) => [
                Number(qi),
                new Map(p.map((proof, i) => [
                  i,
                  new DisjunctiveChaumPedersenProofKnownNonce(
                    new ExpandedGenericChaumPedersenProof(
                      globalContext.createElementModP(proof.proof0.data)!,
                      globalContext.createElementModP(proof.proof0.pad)!,
                      globalContext.createElementModQ(proof.proof0.challenge)!,
                      globalContext.createElementModQ(proof.proof0.response)!
                    ),
                    new ExpandedGenericChaumPedersenProof(
                      globalContext.createElementModP(proof.proof1.data)!,
                      globalContext.createElementModP(proof.proof1.pad)!,
                      globalContext.createElementModQ(proof.proof1.challenge)!,
                      globalContext.createElementModQ(proof.proof1.response)!
                    ),
                    globalContext.createElementModQ(proof.challenge)!
                  )
                ]))
              ]
              )
          ),
          new Map(
            Object.entries(encBallot.accumulatedProofs)
              .map(([qi, p]) => [
                Number(qi),
                new ConstantChaumPedersenProofKnownNonce(
                  new ExpandedGenericChaumPedersenProof(
                    globalContext.createElementModP(p.data)!,
                    globalContext.createElementModP(p.pad)!,
                    globalContext.createElementModQ(p.challenge)!,
                    globalContext.createElementModQ(p.response)!
                  ),
                  p.constant
                )
              ])
          ),
          new Date(encBallot.date),
          '0'
        )
      )

    const ballot = EncryptedBallot.fromJSON(voterHandlerData['encrypted-ballot'])

    await expect(handler.postBallot(ballot))
      .resolves.toStrictEqual({
        trackingCode: 'abc',
        lastTrackingCode: 'cba'
      })

    expect(postSpy).toBeCalledWith('/voter/1', ballot.stringify())
  })

  test('submit ballot', async () => {
    patchSpy.mockResolvedValue({ data: {}, status: 200 })
    await expect(handler.submitBallot('1234'))
      .resolves.toBeUndefined()

    expect(patchSpy).toBeCalledWith(
      '/voter/1',
      undefined,
      { params: { trackingCode: '1234' } }
    )
  })

  test('get VoterElection', async () => {
    const ballotJson = ballotResponse['valid-ballot-response']
    getSpy.mockResolvedValueOnce(ballotJson)

    const ballot = Ballot.fromJSON(ballotJson.data.questions)
    await expect(handler.getVoterElection())
      .resolves.toStrictEqual(
        new VoterElection(
          election.cloneWithMeta(election.electionMeta.cloneWithBallot(ballot)),
          ballot.getPlainTextBallot()
        )
      )
  })
})
