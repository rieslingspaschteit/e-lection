import { ConfigError } from '@/app/model/error'
import { EncryptedBallot } from '@/app/model/ballot'
import type { ConstantChaumPedersenProofKnownNonce, DisjunctiveChaumPedersenProofKnownNonce, ElGamalCiphertext } from 'electionguard'
import { setupAccProof, setupIndProof, setupCiphertext, getEnc } from './encryptedBallot.setup'

import encryptedBallot from '@/test/resources/encrypted-ballot-payload.json'
let ballot: EncryptedBallot

let json: object

let ciphertextMap: Map<number, Map<number, ElGamalCiphertext>>
let individualMap: Map<number, Map<number, DisjunctiveChaumPedersenProofKnownNonce>>
let accumulatedMap: Map<number, ConstantChaumPedersenProofKnownNonce>
let ciphertextArray: object
let individualArray: object
let accumulatedArray: object
const date = new Date(2000)
const device = 'empty'
const ballotId = '0'

beforeAll(() => {
  const cipher = setupCiphertext()
  ciphertextMap = cipher[0] as Map<number, Map<number, ElGamalCiphertext>>
  ciphertextArray = cipher[1] as object
  const indproof = setupIndProof()
  individualMap = indproof[0] as Map<number, Map<number, DisjunctiveChaumPedersenProofKnownNonce>>
  individualArray = indproof[1] as object
  const accProof = setupAccProof()
  accumulatedMap = accProof[0] as Map<number, ConstantChaumPedersenProofKnownNonce>
  accumulatedArray = accProof[1] as object
  const encryption = getEnc()
  ballot = encryption.ballot
  json = encryption.json
})

describe('json conversion works', () => {
  test('stringify works', () => {
    expect(() => ballot.stringify()).not.toThrow()
    expect(ballot.stringify()).toStrictEqual(json)
  })

  test('fromJSON works', () => {
    expect(() => EncryptedBallot.fromJSON(json)).not.toThrow()
    const secondBallot = EncryptedBallot.fromJSON(json)
    // expect(ballot).toEqual(secondBallot); //For some reason, this takes forever or is an infinity loop //FIXME Figure out why
    expect(() => EncryptedBallot.fromJSON(ballot.stringify())).not.toThrow()
    expect(secondBallot.stringify()).toStrictEqual(json) // At least same deserialization as ballot
    expect(secondBallot).toStrictEqual(ballot)
  })

  test('fromJSON throws correctly on missing date', () => {
    const lackingJSON = {
      cipherText: ciphertextArray,
      individualProofs: individualArray,
      accumulatedProofs: accumulatedArray,
      deviceInformation: device,
      ballotId
    }
    expect(() => EncryptedBallot.fromJSON(lackingJSON)).toThrow(ConfigError)
  })

  test('fromJSON throws correctly on missing ciphertext', () => {
    const lackingJSON = {
      individualProofs: individualArray,
      accumulatedProofs: accumulatedArray,
      deviceInformation: device,
      date,
      ballotId
    }
    expect(() => EncryptedBallot.fromJSON(lackingJSON)).toThrow(ConfigError)
  })

  test('fromJSON throws correctly on missing ind. proofs', () => {
    const lackingJSON = {
      cipherText: ciphertextArray,
      accumulatedProofs: accumulatedArray,
      deviceInformation: device,
      date,
      ballotId
    }
    expect(() => EncryptedBallot.fromJSON(lackingJSON)).toThrow(ConfigError)
  })

  test('fromJSON throws correctly on missing acc. proofs', () => {
    const lackingJSON = {
      cipherText: ciphertextArray,
      individualProofs: individualArray,
      deviceInformation: device,
      date,
      ballotId
    }
    expect(() => EncryptedBallot.fromJSON(lackingJSON)).toThrow(ConfigError)
  })

  test('fromJSON works on missing device', () => {
    const lackingJSON = {
      cipherText: ciphertextArray,
      individualProofs: individualArray,
      accumulatedProofs: accumulatedArray,
      date,
      ballotId
    }
    // expect(accumulatedArray.get(0)).toBe(0)
    expect(() => EncryptedBallot.fromJSON(lackingJSON)).not.toThrow()
  })

  test('payload', () => {
    expect(() => EncryptedBallot.fromJSON(encryptedBallot['encrypted-ballot'])).not.toThrow()
  })
})

describe('Test constructor', () => {
  test('constructor throws on missing ciphertext', () => {
    let empty: ReadonlyMap<number, ReadonlyMap<number, ElGamalCiphertext>>
    expect(() => new EncryptedBallot(empty, individualMap, accumulatedMap, date, ballotId, device)).toThrow(ConfigError)
  })

  test('constructor throws on missing ind. proof', () => {
    let empty: ReadonlyMap<number, ReadonlyMap<number, DisjunctiveChaumPedersenProofKnownNonce>>
    expect(() => new EncryptedBallot(ciphertextMap, empty, accumulatedMap, date, ballotId, device)).toThrow(ConfigError)
  })

  test('constructor throws on missing acc. proofs', () => {
    let empty: ReadonlyMap<number, ConstantChaumPedersenProofKnownNonce>
    expect(() => new EncryptedBallot(ciphertextMap, individualMap, empty, date, ballotId, device)).toThrow(ConfigError)
  })

  test('constructor throws on missing date', () => {
    let empty: Date
    expect(() => new EncryptedBallot(ciphertextMap, individualMap, accumulatedMap, empty, ballotId, device)).toThrow(ConfigError)
  })

  test('constructor throws on empty ballotId', () => {
    const empty = ''
    expect(() => new EncryptedBallot(ciphertextMap, individualMap, accumulatedMap, date, empty, device)).toThrow(ConfigError)
  })

  test('constructor throws on missing ballotId', () => {
    let empty: string
    expect(() => new EncryptedBallot(ciphertextMap, individualMap, accumulatedMap, date, empty, device)).toThrow(ConfigError)
  })

  test('constructor does not throw on missing device', () => {
    let empty: string
    expect(() => new EncryptedBallot(ciphertextMap, individualMap, accumulatedMap, date, ballotId, empty)).not.toThrow()
  })

  test('constructor throws on size missmatch', () => {
    accumulatedMap.set(3, accumulatedMap.get(0) as ConstantChaumPedersenProofKnownNonce)
    expect(() => new EncryptedBallot(ciphertextMap, individualMap, accumulatedMap, date, ballotId, device)).toThrow(ConfigError)
  })

  test('constructor throws on nested size missmatch', () => {
    individualMap.get(0)?.set(7, individualMap.get(0)?.get(0) as DisjunctiveChaumPedersenProofKnownNonce)
    expect(() => new EncryptedBallot(ciphertextMap, individualMap, accumulatedMap, date, ballotId, device)).toThrow(ConfigError)
  })
})

export {}
