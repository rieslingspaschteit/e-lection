import { Encryptor } from '@/app/controller/encryption'
import { ElectionManifest } from '@/app/model/election'
import { generateExtendedBaseHash, globalContext, minimumRand } from '@/app/utils/cryptoConstants'
import { ElGamalCiphertext, ElGamalPublicKey, type ElementModP, type ElementModQ } from 'electionguard'
import manifestData from '@/test/resources/manifest-payload.json'
import type { Ballot, EncryptedBallot } from '@/app/model/ballot'
import { ConfigError } from '@/app/model/error'

const electionManifest = ElectionManifest.fromJSON(manifestData['full-manifest'])
const ballot: Ballot = electionManifest.ballot as Ballot
const plaintextBallot = ballot.getPlainTextBallot()
let encryptor: Encryptor
let ciphertextBallot: EncryptedBallot

beforeAll(() => {
  expect(typeof globalContext.randQ(minimumRand)).not.toBe('undefined')
  plaintextBallot.toggleSelection(0, 0)
  plaintextBallot.toggleSelection(0, 1)
  encryptor = new Encryptor()
  ciphertextBallot = encryptor.encrypt(plaintextBallot, electionManifest)
})

test('Encryption has extended ballot regardless of actual number of selections', () => {
  expect(ciphertextBallot.ciphertext.get(0)?.size).toBe(4)
  expect(ciphertextBallot.ciphertext.get(1)?.size).toBe(6)
})

test('Encryption fails on invalid plaintext ballot', () => {
  const electionManifest2 = ElectionManifest.fromJSON(manifestData['full-manifest2'])
  const ballot2: Ballot = electionManifest2.ballot as Ballot
  const plaintextBallot2 = ballot2.getPlainTextBallot()
  plaintextBallot2.toggleSelection(0, 0)
  plaintextBallot2.toggleSelection(0, 1)
  expect(() => { encryptor.encrypt(plaintextBallot2, electionManifest2) }).toThrow(ConfigError)
})

test('Proofs are correct', () => {
  expect(ciphertextBallot.accumulatedProofs.size).toBe(2)
  expect(ciphertextBallot.individualProofs.size).toBe(2)
  expect(ciphertextBallot.individualProofs.get(0)?.size).toBe(4)
  expect(ciphertextBallot.individualProofs.get(1)?.size).toBe(6)
  // const manifestHash = globalContext.createElementModQFromHex(crypto.SHA256(JSON.stringify(electionManifest.stringify())).toString(crypto.enc.Hex)) as ElementModQ
  const publicKey = new ElGamalPublicKey(electionManifest.key as ElementModP)
  const baseHash = generateExtendedBaseHash(electionManifest.trustees?.length as number, electionManifest.threshold,
    electionManifest.hashes?.manifestHash as ElementModQ, electionManifest.hashes?.commitments as ElementModQ)
  const accPad = new Map<number, ElementModP>()
  const accData = new Map<number, ElementModP>()
  for (const entry of ciphertextBallot.ciphertext) {
    accData.set(entry[0], globalContext.ONE_MOD_P)
    accPad.set(entry[0], globalContext.ONE_MOD_P)
    for (const key of entry[1].keys()) {
      const cipher = entry[1].get(key) as ElGamalCiphertext
      expect(ciphertextBallot.individualProofs.get(entry[0])?.get(key)?.isValid(cipher, publicKey, baseHash)).toBe(true)
      accPad.set(entry[0], globalContext.multP(accPad.get(entry[0]) as ElementModP, cipher.pad))
      accData.set(entry[0], globalContext.multP(accData.get(entry[0]) as ElementModP, cipher.data))
    }
  }
  // console.log(accPad.get(0)?.toHex())
  // console.log(accData.get(0)?.toHex())
  expect(ciphertextBallot.accumulatedProofs.get(0)?.isValid(new ElGamalCiphertext(accPad.get(0) as ElementModP, accData.get(0) as ElementModP), publicKey, baseHash, 2)).toBe(true)
  expect(ciphertextBallot.accumulatedProofs.get(1)?.isValid(new ElGamalCiphertext(accPad.get(1) as ElementModP, accData.get(1) as ElementModP), publicKey, baseHash, 3)).toBe(true)
})

export {}
