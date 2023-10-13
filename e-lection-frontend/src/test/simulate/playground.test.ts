import { Encryptor } from '@/app/controller/encryption'
import type { Ballot, PlainTextBallot } from '@/app/model/ballot'
import manifestData from '@/test/resources/simulate/manifest-payload.json'
import { ElectionManifest } from '@/app/model/election'
import { ElectionContext, ElGamalPublicKey, type ElementModP, type ElementModQ } from 'electionguard'
import { hashElements } from '@/app/utils/cryptoConstants'
// const fs = require("fs")
/*
test("generateBallot", () => {
const encryptor = new Encryptor();

    const electionManifest = ElectionManifest.fromJSON(manifestData["full-manifest1"])
    const ballot: Ballot = electionManifest.ballot as Ballot
    const plaintextBallot1: PlainTextBallot = ballot.getPlainTextBallot();
    plaintextBallot1.toggleSelection(0, 0);
    plaintextBallot1.toggleSelection(1, 1);
    const plaintextBallot2: PlainTextBallot = ballot.getPlainTextBallot();
    plaintextBallot2.toggleSelection(0, 1);
    const plaintextBallot3: PlainTextBallot = ballot.getPlainTextBallot();
    plaintextBallot3.toggleSelection(0, 1);
    plaintextBallot3.toggleSelection(1, 0);
    const plaintextBallot4: PlainTextBallot = ballot.getPlainTextBallot();
    plaintextBallot4.toggleSelection(0, 0);
    plaintextBallot4.toggleSelection(0, 0);
    const ballot1: EncryptedBallot = encryptor.encrypt(plaintextBallot1, electionManifest);
    const ballot2: EncryptedBallot = encryptor.encrypt(plaintextBallot2, electionManifest);
    const ballot3: EncryptedBallot = encryptor.encrypt(plaintextBallot3, electionManifest);
    const ballot4: EncryptedBallot = encryptor.encrypt(plaintextBallot4, electionManifest);
    console.log(JSON.stringify(ballot1.stringify(), null, 4))
    console.log(JSON.stringify(ballot2.stringify(), null, 4))
    console.log(JSON.stringify(ballot3.stringify(), null, 4))
    console.log(JSON.stringify(ballot4.stringify(), null, 4))
})
*/
/*
function write(data: object, name:number) {
    const json: string = JSON.stringify(data, null, 4)
    try {
        fs.writeFile('ballot'+name, json)
    } catch(e) {
        console.log("Failed")
    }
}
*/

/**
     * recreates the fingerprint of the election the ballot was prepared for
     */
function recreateElectionFingerprint (manifest: ElectionManifest): string {
  console.log('time', manifest.start?.getTime())
  console.log('ballot', manifest.ballot?.getHashString())
  console.log('hashes', manifest.hashes?.getHashString().cryptoHashString)
  console.log(manifest.trustees)
  return hashElements(
    manifest.title,
    manifest.authority,
    manifest.start?.getTime(),
    manifest.end.getTime(),
    manifest.trustees,
    (manifest.isBotEnabled ?? false) ? 1 : 0,
    manifest.threshold,
    manifest.key?.toHex(),
    manifest.ballot?.getHashString(),
    manifest.hashes?.getHashString()
  ).toHex()
}

test('generateTrackingCode', () => {
  const manifest = ElectionManifest.fromJSON(manifestData.manifest4)
  console.log('fingerprint', recreateElectionFingerprint(manifest))
})

test('generateBallot', () => {
  const encryptor = new Encryptor()

  const electionManifest = ElectionManifest.fromJSON(manifestData.manifest3)
  const ballot: Ballot = electionManifest.ballot as Ballot
  const plaintextBallot1: PlainTextBallot = ballot.getPlainTextBallot()
  plaintextBallot1.toggleSelection(0, 0)
  plaintextBallot1.toggleSelection(1, 1)
  plaintextBallot1.toggleSelection(2, 0)
  encryptor.encrypt(plaintextBallot1, electionManifest)

  // console.log(JSON.stringify(ballot1.stringify(), null, 4))
  const publicKey = electionManifest.key as ElementModP
  const acceleratedKey: ElGamalPublicKey = new ElGamalPublicKey(publicKey)
  const context: ElectionContext = ElectionContext.create(1, 1, acceleratedKey,
    electionManifest.hashes?.commitments as ElementModQ, electionManifest.hashes?.manifestHash as ElementModQ)
  console.log('commitmentHash', electionManifest.hashes?.commitments?.toHex())
  console.log('qbar', context.cryptoExtendedBaseHash.toHex())
  console.log('key', acceleratedKey.cryptoHashElement.toHex())
  console.log((electionManifest.hashes?.manifestHash as ElementModQ).toHex())
  console.log(context.cryptoBaseHash.toHex())
})

export {}
