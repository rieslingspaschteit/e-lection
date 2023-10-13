import { Encryptor } from '@/app/controller/encryption'
import type { Ballot } from '@/app/model/ballot'
import { ElectionManifest } from '@/app/model/election'
import manifestData from '@/test/resources/manifest-payload.json'

test('generate ballot', () => {
  const manifest = ElectionManifest.fromJSON(manifestData['full-manifest'])
  const plaintext = (manifest.ballot as Ballot).getPlainTextBallot()
  plaintext.toggleSelection(0, 1)
  const encryptor = new Encryptor()
  const encryptedBallot = encryptor.encrypt(plaintext, manifest)
  const seed = encryptor.getSeed()

  console.log(JSON.stringify(encryptedBallot.stringify(), null, 4))
  console.log(JSON.stringify(plaintext.stringify(), null, 4))
  console.log(seed.toHex())
})
export {}
