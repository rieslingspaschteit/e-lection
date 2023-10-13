import type { EncryptedBallot, PlainTextBallot, SpoiledBallot } from '../ballot'
import { Election } from './election'
import { ElectionManifest, type ElectionMeta } from '@/app/model/election'

/**
 * This model class is used for holding data that a voter requests in the process of voting for one specific election.
 * It additionally holds the {@link PlainTextBallot} which the voter can fill in and also stores the encrypted result
 * @see {EncryptedBallot}
 *
 * @version 1.0
 */
export class VoterElection extends Election {
  private _encryptedBallot?: EncryptedBallot
  private _spoiledBallot?: SpoiledBallot

  /**
     * Constructs a new instance and should only be constructed from data fetched from the backend server.
     * @param election the election instance this VoterElection shall extend.
     * @param _plaintextBallot the plaintextBallot constructed obtained the {@link Ballot} of the election.
     */
  constructor (
    election: Election,
    private readonly _plaintextBallot: PlainTextBallot
  ) {
    super(
      election.electionId,
      election.electionMeta,
      election.electionState,
      election.fingerprint
    )
    // FIXME: figure out constraints to check
  }

  public override cloneWithMeta (meta: ElectionMeta): Election {
    return new VoterElection(
      super.cloneWithMeta(meta),
      this._plaintextBallot
    )
  }

  /**
     * Casts this.electionMeta into an {@link "election-manifest".ElectionManifest} if possible.
     *
     * @throws TypeError if the cast fails.
     */
  public get electionManifest (): ElectionManifest {
    if (!(this.electionMeta instanceof ElectionManifest)) {
      throw new TypeError()
    }
    return this.electionMeta
  }

  public get plaintextBallot (): PlainTextBallot { return this._plaintextBallot }
  public get encryptedBallot (): EncryptedBallot | undefined { return this._encryptedBallot }
  public set encryptedBallot (encryptedBallot: EncryptedBallot | undefined) { this._encryptedBallot = encryptedBallot }
  public get spoiledBallot (): SpoiledBallot | undefined { return this._spoiledBallot }
  public set spoiledBallot (spoiledBallot: SpoiledBallot | undefined) { this._spoiledBallot = spoiledBallot }
}
