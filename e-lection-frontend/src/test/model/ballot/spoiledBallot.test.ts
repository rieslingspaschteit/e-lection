import { ConfigError } from '@/app/model/error'
import { type EncryptedBallot, SpoiledBallot } from '@/app/model/ballot'
import { globalContext } from '@/app/utils/cryptoConstants'
import type { ElementModQ } from 'electionguard'
import { getEnc } from './encryptedBallot.setup'

let ballot: EncryptedBallot
let selections: Map<number, Map<number, number>>
const trackingCode: ElementModQ = globalContext.createElementModQFromHex('123345') as ElementModQ
const previousTrackingCode = globalContext.createElementModQFromHex('765432') as ElementModQ
const nonce: ElementModQ = globalContext.createElementModQSafe(100)

beforeAll(() => {
  const encryption = getEnc()
  ballot = encryption.ballot
  selections = new Map<number, Map<number, number>>()
  selections.set(0, new Map<number, number>())
  selections.set(1, new Map<number, number>())
  selections.get(0)?.set(0, 1)
  selections.get(0)?.set(1, 0)
  selections.get(1)?.set(0, 0)
  selections.get(1)?.set(1, 0)
})

describe('testing constructor', () => {
  test('Constructor should throw on missing trackingCode', () => {
    let trackingCodeUndefined: ElementModQ
    expect(() => new SpoiledBallot(trackingCodeUndefined, previousTrackingCode, nonce, ballot, selections)).toThrow(ConfigError)
  })

  test('Constructor should throw on missing lastTrackingCode', () => {
    let trackingCodeUndefined: ElementModQ
    expect(() => new SpoiledBallot(trackingCode, trackingCodeUndefined, nonce, ballot, selections)).toThrow(ConfigError)
  })

  test('Constructor should throw on missing nonce', () => {
    let nonceUndefined: ElementModQ
    expect(() => new SpoiledBallot(trackingCode, previousTrackingCode, nonceUndefined, ballot, selections)).toThrow(ConfigError)
  })

  test('Constructor should throw on missing encryptedBallot', () => {
    let ballotUndefined: EncryptedBallot
    expect(() => new SpoiledBallot(trackingCode, previousTrackingCode, nonce, ballotUndefined, selections)).toThrow(ConfigError)
  })

  test('Constructor should throw on missing spoiledBallot', () => {
    let selectionsUndefined: Map<number, Map<number, number>>
    expect(() => new SpoiledBallot(trackingCode, previousTrackingCode, nonce, ballot, selectionsUndefined)).toThrow(ConfigError)
  })

  test('Constructor should not throw on everything correct', () => {
    expect(() => new SpoiledBallot(trackingCode, previousTrackingCode, nonce, ballot, selections)).not.toThrow()
  })

  test('Constructor should throw on empty spoiledBallot', () => {
    const selectionsUndefined = new Map<number, Map<number, number>>()
    expect(() => new SpoiledBallot(trackingCode, previousTrackingCode, nonce, ballot, selectionsUndefined)).toThrow(ConfigError)
  })
})

describe('Testing json serialization', () => {
  test('stringify should throw on missing manifest', () => {
    const spoiled = new SpoiledBallot(trackingCode, previousTrackingCode, nonce, ballot, selections)
    expect(() => spoiled.stringify()).toThrow(ConfigError)
  })

  test('fromJSON should throw on missing manifest', () => {
    const plaintextMap = new Map<number, number[]>()
    plaintextMap.set(0, [1, 0])
    plaintextMap.set(1, [0, 0])
    expect(() => SpoiledBallot.fromJSON({
      plaintextBallot: plaintextMap,
      encryptedBallot: ballot,
      trackingCode: '123345',
      lastTrackingCode: '765432',
      node: 100
    })
    ).toThrow(ConfigError)
  })

  // TODO test throwing with other missing attributes, stringify and fromJSON with manifest
})

export {}
