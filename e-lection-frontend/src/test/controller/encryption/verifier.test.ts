import { Encryptor, Verifier } from '@/app/controller/encryption'
import { ElectionManifest } from '@/app/model/election'
import { globalContext, minimumRand } from '@/app/utils/cryptoConstants'
import manifestData from '@/test/resources/manifest-payload.json'
import { EncryptedBallot, type Ballot, SpoiledBallot } from '@/app/model/ballot'
import spoiledBallotData from '@/test/resources/spoiled-ballot-payload.json'
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
  const spoiledBallot = new SpoiledBallot(
    // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
    globalContext.createElementModQFromHex(spoiledBallotData['full-ballot'].trackingCode)!,
    // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
    globalContext.createElementModQFromHex(spoiledBallotData['full-ballot'].lastTrackingCode)!,
    encryptor.getSeed(), ciphertextBallot, plaintextBallot.getWithDummies())
  spoiledBallot.manifest = electionManifest
})

test('test invalid ballot', () => {
  const spoiledBallotObj = spoiledBallotData.ballot3
  const verifier = new Verifier(spoiledBallotObj)
  const result = verifier.verify()
  const spoiledBallot = spoiledBallotData.ballot3
  const encryptedBallot = EncryptedBallot.fromJSON(spoiledBallot.encryptedBallot)
  for (const contest of result.reEncryptions) {
    for (const selection of contest[1]) {
      expect(encryptedBallot.ciphertext.get(contest[0])?.get(selection[0])?.pad.toHex()).not.toStrictEqual(selection[1].pad.toHex())
      expect(encryptedBallot.ciphertext.get(contest[0])?.get(selection[0])?.data.toHex()).not.toStrictEqual(selection[1].data.toHex())
    }
  }
  expect(result.recreatedTrackinCode.cryptoHashString).not.toStrictEqual(spoiledBallot.trackingCode.toUpperCase())
  expect(result.valid).toBe(false)
})

// TODO generate valid spoiled ballot
/*
test("verifying directly", () => {
    let verifier = new Verifier(spoiledBallotObj)
    let result = verifier.verify()
    for (let contest of result.reEncryptions) {
        for (let selection of contest[1]) {
            expect(ciphertextBallot.ciphertext.get(contest[0])?.get(selection[0])?.pad.toHex()).toStrictEqual(selection[1].pad.toHex())
            expect(ciphertextBallot.ciphertext.get(contest[0])?.get(selection[0])?.data.toHex()).toStrictEqual(selection[1].data.toHex())
        }
    }
})
*/

test('verifying valid spoiled ballot', () => {
  const spoiledBallotObj = spoiledBallotData.verifyableBallot
  const verifier = new Verifier(spoiledBallotObj)
  const result = verifier.verify()
  const encryptedBallot = EncryptedBallot.fromJSON(spoiledBallotObj.encryptedBallot)
  for (const contest of result.reEncryptions) {
    for (const selection of contest[1]) {
      expect(encryptedBallot.ciphertext.get(contest[0])?.get(selection[0])?.pad.toHex()).toStrictEqual(selection[1].pad.toHex())
      expect(encryptedBallot.ciphertext.get(contest[0])?.get(selection[0])?.data.toHex()).toStrictEqual(selection[1].data.toHex())
    }
  }
  expect(result.recreatedTrackinCode.cryptoHashString).toStrictEqual(spoiledBallotObj.trackingCode.toUpperCase())
  expect(result.valid).toBe(true)
})

export {}
