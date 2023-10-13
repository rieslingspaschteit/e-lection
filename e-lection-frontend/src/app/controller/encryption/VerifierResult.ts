import type { SpoiledBallot } from '@/app/model/ballot'
import type { ElementModQ, ElGamalCiphertext } from 'electionguard'

/**
 * The class VerifierResult represents the result of verifying a {@link SpoiledBallot}.
 * It contains the recreations of the ballots encryption and tracking code as well as the fingerprint of the election
 */
export class VerifierResult {
  /**
     * @param valid whether the provided {@link Spoiledballot is valid}
     * @param recreatedElectionFingerprint fingerprint of the election calculated from the data in the {@link SpoiledBallot}
     * @param recreatedTrackingCode tracking code of the ballot calculated from the data in the {@link SpoiledBallot}
     * @param reEncryptions encryption of the ballot calculated from the data in the {@link SpoiledBallot} using the provided nonce
     * @param verifiedSpoiledBallot the {@link SpoiledBallot} that was verified
     */
  public constructor (
    private readonly _valid: boolean,
    private readonly _recreatedElectionFingerprint: string,
    private readonly _recreatedTrackingCode: ElementModQ,
    private readonly _reEncryptions: ReadonlyMap<number, ReadonlyMap<number, ElGamalCiphertext>>,
    private readonly _verifiedSpoiledBallot: SpoiledBallot) {}

  /**
     * @returns whether the {@link SpoiledBallot} is valid
     */
  public get valid (): boolean {
    return this._valid
  }

  /**
     * @returns The fingerprint of the election the ballot was created for recalculated from the data in the {@link SpoiledBallot}
     */
  public get recreatedElectionFingerprint (): string {
    return this._recreatedElectionFingerprint
  }

  /**
     * @returns The tracking code of the ballot recalculated from the data in the {@link SpoiledBallot}
     */
  public get recreatedTrackinCode (): ElementModQ {
    return this._recreatedTrackingCode
  }

  /**
     * @returns The encryption of the ballot recalculated from the data in the {@link SpoiledBallot}
     */
  public get reEncryptions (): ReadonlyMap<number, ReadonlyMap<number, ElGamalCiphertext>> {
    return this._reEncryptions
  }

  /**
     * @returns The {@link SpoiledBallot} that was verified
     */
  public get verifiedSpoiledBallot (): SpoiledBallot {
    return this._verifiedSpoiledBallot
  }
}
