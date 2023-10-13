import type { ElectionMeta } from '../election'
import { Election } from './election'
import type { DecryptionState } from './states'

/**
 * This specific election model class holds data required for a trustee to participate at the decryption of an election.
 *
 * @version 1.0
 */
export class DecryptionElection extends Election {
  /**
     * Constructs a new instance and should only be constructed from data fetched from the backend server.
     * @param election the election instance this DecryptionElection shall extend
     * @param _state: the state of the decryption phase.
     * @param _waiting: wether the requesting trustee has already uploaded his decryptions for the current state.
     * @param _providedEncryptions the encrypted data the trustee needs to partial decrypt
     */
  public constructor (
    election: Election,
    private readonly _state: DecryptionState,
    private readonly _waiting: boolean,
    private readonly _providedEncryptions?: object,
    private _committedDecryptions?: object
  ) {
    super(
      election.electionId,
      election.electionMeta,
      election.electionState,
      election.fingerprint
    )
    // FIXME: figure out constraints to check
  }

  public cloneWithState (state: DecryptionState, waiting: boolean): DecryptionElection {
    return new DecryptionElection(
      this.cloneWithMeta(this.electionMeta),
      state,
      waiting,
      this.providedEncryptions,
      this.committedDecryptions
    )
  }

  public cloneWithEncryptions (encryptions: object): DecryptionElection {
    return new DecryptionElection(
      this.cloneWithMeta(this.electionMeta),
      this.state,
      this.waiting,
      encryptions,
      this.committedDecryptions
    )
  }

  public override cloneWithMeta (meta: ElectionMeta): Election {
    return new DecryptionElection(
      super.cloneWithMeta(meta),
      this.state,
      this.waiting,
      this.providedEncryptions,
      this.cloneWithEncryptions
    )
  }

  public get providedEncryptions (): object | undefined { return this._providedEncryptions }
  public get committedDecryptions (): object | undefined { return this._committedDecryptions }
  public set committedDecryptions (committedDecryptions: object | undefined) { this._committedDecryptions = committedDecryptions }
  public get waiting (): boolean { return this._waiting }
  public get state (): DecryptionState { return this._state }
}
