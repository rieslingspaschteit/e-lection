import type { EncryptedBallot, PlainTextBallot } from '@/app/model/ballot'
import type { ElectionManifest } from '@/app/model/election'
import type { ManifestHashes } from '@/app/model/election/manifest/manifestHashes'

/**
 * The interface IEncryptor defines the properties required for an encryptor that can be used to encrypt {@link PlainTextBallot}s.
 *
 * @version 1.0
 */
export interface IEncryptor {

  /**
     * This method encrypts the provided options of the {@link plaintextBallot} with expotential ElGamal
     * using the provided public key.
     * It also generates ChaumPedersenProofs that the encryptions are valid
     * @param plaintextBallot the {@link PlainTextBallot} that has to be encrypted
     * @param key the election public key
     * @param electionFingerprint the fingerprint of the election, used as Election Base Hash
     * @returns A fully instantiated {@link EncryptedBallot} with the encryption of the ballot using a random nonce
     * and the ChaumPedersenProofs
     */
  encrypt: (plaintextBallot: PlainTextBallot, electionManifest: ElectionManifest, hashes: ManifestHashes) => EncryptedBallot

}
