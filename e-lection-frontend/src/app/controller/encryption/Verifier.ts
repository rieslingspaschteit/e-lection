import { hashElements } from '@/app/utils/cryptoConstants'
import { SpoiledBallot } from '@/app/model/ballot'
import type { ElementModP, ElementModQ, ElGamalCiphertext } from 'electionguard'
import { Encryptor } from './Encryptor'
import type { IVerifier } from './IVerifier'
import { VerifierResult } from './VerifierResult'
import { ConfigError, Messages } from '@/app/model/error'
import type { ElectionManifest } from '@/app/model/election'

// TODO add hashes to manfest

/**
 * The class Verifier implements the interface {@link IVerifier} and extends the cass {@link Encryptor} to be able
 * to reconstruct the necessary information to verify the provided {@link SpoiledBallot}
 */
export class Verifier extends Encryptor implements IVerifier {
  private readonly spoiledBallot: SpoiledBallot

  /**
     * This constructor deserializes the provided JSON to initialize the spoiledBallot attributes
     * @param spoiledBallot the provieded spoiled ballot as a JSON
     */
  public constructor (spoiledBallot: any) {
    super()
    this.spoiledBallot = SpoiledBallot.fromJSON(spoiledBallot)
    this.seed = this.spoiledBallot.nonce
    this.ballotId = this.spoiledBallot.encryptedBallot.ballotId
  }

  /**
     * This method uses the encryption functions of the super class {@link Encryptor} to recreate the required information
     * to verify the spoiled ballot
     * @returns a {@link VerifierResult} containing the recreated values for the verification
     */
  public verify (): VerifierResult {
    const reEncryption: Map<number, Map<number, ElGamalCiphertext>> = this.reEncrypt()
    const reCreatedFingerprint = this.recreateElectionFingerprint()
    const reCreatedTrackingCode = this.recreateTrackingCode(reEncryption)
    let valid: boolean = reCreatedTrackingCode.equals(this.spoiledBallot.trackingCode)
    for (const contest of reEncryption) {
      for (const selection of contest[1]) {
        if (!selection[1].equals(this.spoiledBallot.encryptedBallot.ciphertext.get(contest[0])?.get(selection[0]) as ElGamalCiphertext)) {
          valid = false
          break
        }
      }
    }
    return new VerifierResult(valid, reCreatedFingerprint, reCreatedTrackingCode, reEncryption, this.spoiledBallot)
  }

  /**
     * reencrypts the ballot
     */
  private reEncrypt (): Map<number, Map<number, ElGamalCiphertext>> {
    // This should not be possible
    if ((this.spoiledBallot.manifest?.hashes) == null) {
      throw new ConfigError(Messages.MISSING_ARGUMENT_CONTENT)
    }
    this.seed = this.spoiledBallot.nonce
    this.ballotId = this.spoiledBallot.encryptedBallot.ballotId
    return this.encryptToCiphertext(
      this.spoiledBallot.plaintextBallot,
      this.spoiledBallot.manifest?.key as ElementModP,
      this.spoiledBallot.manifest.hashes
    )
  }

  /**
     * recreates the tracking code of the ballot
     */
  private recreateTrackingCode (reEncryption: Map<number, Map<number, ElGamalCiphertext>>): ElementModQ {
    const contestArray: ElementModQ[] = new Array<ElementModQ>()
    for (const contest of reEncryption) {
      const selectionArray: ElementModQ[] = new Array<ElementModQ>()
      for (const selection of contest[1]) {
        selectionArray[selection[0]] = hashElements(
          this.spoiledBallot.manifest?.hashes?.selectionIds.get(contest[0])?.get(selection[0]),
          this.spoiledBallot.manifest?.hashes?.selectionHashes.get(contest[0])?.get(selection[0]),
          contest[1].get(selection[0])?.cryptoHashElement.cryptoHashString
        )
      }
      contestArray[contest[0]] = hashElements(
        this.spoiledBallot.manifest?.hashes?.contestIds.get(contest[0]),
        this.spoiledBallot.manifest?.hashes?.contestHashes.get(contest[0]),
        ...selectionArray
      )
    }
    const cryptoHash = hashElements(
      this.spoiledBallot.encryptedBallot.ballotId,
      this.spoiledBallot.manifest?.hashes?.manifestHash,
      ...contestArray
    )
    const ballotCode = hashElements(
      this.spoiledBallot.lastTrackingCode,
      this.spoiledBallot.encryptedBallot.encryptionDate.getTime(),
      cryptoHash
    )
    return ballotCode
  }

  /**
     * recreates the fingerprint of the election the ballot was prepared for
     */
  private recreateElectionFingerprint (): string {
    const manifest = this.spoiledBallot.manifest as ElectionManifest
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
}
