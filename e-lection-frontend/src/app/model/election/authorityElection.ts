import type { ElectionMeta } from '../election'
import { Election } from './election'
import type { DecryptionState, KeyCeremonyState } from './states'

/**
 * This specific election model class additionally holds data that are important for an authority to manage an election.
 * It primarily stores data indicating in which state the election currently is.
 *
 * @version 1.0
 */
export class AuthorityElection extends Election {
  /**
     * Constructs a new instance and should only be constructed from data fetched from the backend server.
     * @param election the election instance this AuthorityElection shall extend
     * @param _keyCerState the state of the key-ceremony, must be FINISHED if the decryptionState is not provided
     * @param _keyCerCount the number of trustees that participated at the key-ceremony of this election
     * @param _decryptionState the state of the decryption, if provided _keyCerState must be FINISHED
     * @param _decryptionCount the number of partial decryptions,
     * must be undefined if the _decryptionState is not provided
     */
  public constructor (
    election: Election,
    private readonly _keyCerState?: KeyCeremonyState,
    private readonly _keyCerCount?: number,
    private readonly _decryptionState?: DecryptionState,
    private readonly _decryptionCount?: number
  ) {
    super(
      election.electionId,
      election.electionMeta,
      election.electionState,
      election.fingerprint
    )
    // FIXME: figure out constraints
  }

  public override cloneWithMeta (meta: ElectionMeta): Election {
    return new AuthorityElection(
      super.cloneWithMeta(meta),
      this.keyCerState,
      this.keyCerCount,
      this.decryptionState,
      this.decryptionCount
    )
  }

  public get keyCerState (): KeyCeremonyState | undefined { return this._keyCerState }
  public get keyCerCount (): number | undefined { return this._keyCerCount }
  public get decryptionState (): DecryptionState | undefined { return this._decryptionState }
  public get decryptionCount (): number | undefined { return this._decryptionCount }
}
