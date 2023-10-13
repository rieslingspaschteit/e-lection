import type { VerifierResult } from './VerifierResult'

/**
 * This interface proviedes the properties of a verifier that can verify if a given {@link SpoiledBallot} contains a valid encryption of the provided options
 * for the correct election with a correct tracking code for the encryption
 * @version 1.0
 */
export interface IVerifier {
  /**
     * This method verifies the {@link SpoiledBallot} by recreating the encryption, the tracking code and
     * the election fingerprint.
     * @return A {@link VerifierResult} containing the recreated values, the {@link SpoiledBallot} and whether the reacreated and provided values match
     */
  verify: () => VerifierResult
}
