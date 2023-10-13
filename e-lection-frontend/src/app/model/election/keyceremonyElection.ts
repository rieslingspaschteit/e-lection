import type { ElectionMeta } from '../election'
import { Election } from './election'
import type { KeyCeremonyState } from './states'

/**
 * This specific election model class holds data necessary for a trustee to participate at the key-ceremony of an election.
 *
 * @version 1.0
 */
export class KeyCeremonyElection extends Election {
  private _committedAuxKeys?: object
  private _committedEPKB?: object

  /**
     * Constructs a new instance and should only be constructed from data fetched from the backend server.
     * @param election the election instance this KeyCeremonyElection shall extend
     * @param _keyCeremonyState the state of the key-ceremony
     * @param _waiting wether the requesting trustee has already uploaded his keys for the current state.
     * @param _providedAuxKeys the aux keys from the other trustees that can be used for encrypting their backups before sharing
     * @param _providedEPKB the key backups for this trustee
     */
  constructor (
    election: Election,
    private readonly _keyCeremonyState: KeyCeremonyState,
    private _waiting: boolean,
    private readonly _providedAuxKeys?: object,
    private readonly _providedEPKB?: object
  ) {
    super(
      election.electionId,
      election.electionMeta,
      election.electionState,
      election.fingerprint
    )
    // FIXME: figure out constraints to check
  }

  public cloneWithMeta (meta: ElectionMeta): Election {
    return new KeyCeremonyElection(
      super.cloneWithMeta(meta),
      this.state,
      this.waiting,
      this.providedAuxKeys,
      this.providedEPKB
    )
  }

  /**
     * Creates a new KeyCeremonyElection containing all information in the current election as well as the provided
     * auxiliary keys
     * @param auxKeys the auxiliary keys provided by other trustees as a JSON object
     */
  public cloneWithAuxKeys (auxKeys: object): KeyCeremonyElection {
    const clone = new KeyCeremonyElection(
      this.cloneWithMeta(this.electionMeta),
      this.state,
      this.waiting,
      auxKeys,
      this.providedEPKB
    )
    clone.committedAuxKeys = this.committedAuxKeys
    clone.committedEPKB = this.committedEPKB
    return clone
  }

  /**
     * Creates a new KeyCeremonyElection containing all information in the current election as well as the provided
     * EPKBs
     * @param epkb key backups provided by the other trustees as a JSON object
     */
  public cloneWithEPKB (epkb: object): KeyCeremonyElection {
    const clone = new KeyCeremonyElection(
      this.cloneWithMeta(this.electionMeta),
      this.state,
      this.waiting,
      this.providedAuxKeys,
      epkb
    )
    clone.committedAuxKeys = this.committedAuxKeys
    clone.committedEPKB = this.committedEPKB
    return clone
  }

  /**
     * Creates a new KeyCeremonyElection containing all information in the current election as well as the provided
     * {@link KeyCeremonyState}
     * @param stae the current state of the Key Ceremony
     */
  public cloneWithState (state: KeyCeremonyState): KeyCeremonyElection {
    const clone = new KeyCeremonyElection(
      this.cloneWithMeta(this.electionMeta),
      state,
      this.waiting,
      this.providedAuxKeys,
      this.committedEPKB
    )
    clone.committedAuxKeys = this.committedAuxKeys
    clone.committedEPKB = this.committedEPKB
    return clone
  }

  public get state (): KeyCeremonyState { return this._keyCeremonyState }
  public get waiting (): boolean { return this._waiting }
  public set waiting (waiting: boolean) { this._waiting = waiting }
  public get providedAuxKeys (): object | undefined { return this._providedAuxKeys }
  public get providedEPKB (): object | undefined { return this._providedEPKB }
  public get committedAuxKeys (): object | undefined { return this._committedAuxKeys }
  public set committedAuxKeys (auxKeys: object | undefined) { this._committedAuxKeys = auxKeys }
  public get committedEPKB (): object | undefined { return this._committedEPKB }
  public set committedEPKB (epkb: object | undefined) { this._committedEPKB = epkb }
}
