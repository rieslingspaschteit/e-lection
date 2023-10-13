import { ElectionMeta } from '../election'
import { ConfigError, Messages } from '../error'
import { ElectionState } from './states'

/**
 * This is the base model class for all specific elections classes.
 * It holds its configuration object which can be both an {@link "election-manifest".ElectionMeta}
 * or an {@link ElectionManifest} and is associated with one {@link ElectionState}.
 * This data can be fetched in order to list elections where no in-depth data is required.<
 *
 * @version 1.0
 */
export class Election {
  private static readonly STATES = [
    ElectionState.CREATED, ElectionState.KEY_CEREMONY, ElectionState.OPEN,
    ElectionState.DECRYPTION, ElectionState.FINISHED
  ]

  /**
     * Constructs a new instance and should only be constructed from data fetched from the backend server.
     * @param _electionId the id of the election assigned by the backend
     * @param _electionMeta an election configuration Object, in most cases a slim {@link "../election".ElectionMeta} instance.
     * @param _electionState the current state of the election
     * @param _fingerprint the fingerprint of the election which is an ssh256 Hash of the {@link "../election".ElectionManifest}, may be null if the key-ceremony hasn't finished for this election
     */
  protected constructor (
    private readonly _electionId: number,
    private readonly _electionMeta: ElectionMeta,
    private readonly _electionState: ElectionState,
    private readonly _fingerprint?: string
  ) {
    if (this.electionId < 0) {
      throw new ConfigError(Messages.OTHER, 'cannot be negative') // FIXME: remove magic string
    }
    if (this.fingerprint !== undefined && this.fingerprint === '') {
      throw new ConfigError(Messages.OTHER, 'should probably not happen...') // FIXME: remove magic string and figure out exact length for fingerprints
    }
  }

  /**
     * Deserializes a JSON object into a Election object
     * @param json JSON containing the attributes of the Election
     */
  public static fromJSON (json: any): Election {
    if (json.electionId === undefined || json.electionMeta === undefined || json.state === undefined) {
      throw new ConfigError(Messages.MISSING_ARGUMENT)
    }

    if (typeof json.electionId !== 'number' ||
        (json.fingerprint !== null && typeof json.fingerprint !== 'string')) {
      throw new ConfigError(Messages.OTHER, 'invalid types') // FIXME: remove magic string
    }

    const meta = ElectionMeta.fromJSON(json.electionMeta)
    let state = Election.STATES.find(state =>
      state.name === json.state.toUpperCase())
    if (state === undefined) {
      state = Election.STATES.find(state =>
        state.subStates.includes(json.state.toUpperCase()))
    }

    if (state === undefined) {
      throw new ConfigError(Messages.OTHER, 'cannot find state') // FIXME: remove magic string
    }
    return new Election(json.electionId, meta, state, json.fingerprint)
  }

  /**
     * Creates a new Election containing all information in the current Election as well as the provided
     * {@link ElectionMeta}
     * @param meta the {@link ELectionMeta} of the election
     */
  public cloneWithMeta (meta: ElectionMeta): Election {
    return new Election(this.electionId, meta, this.electionState, this.fingerprint)
  }

  public get electionId (): number { return this._electionId }
  public get electionMeta (): ElectionMeta { return this._electionMeta }
  public get electionState (): ElectionState { return this._electionState }
  public get fingerprint (): string | undefined { return this._fingerprint }
}
