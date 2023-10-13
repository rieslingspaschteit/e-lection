import type { ElementModP } from 'electionguard'
import type { Ballot } from '@/app/model/ballot'
import { globalContext } from '@/app/utils/cryptoConstants'
import { ConfigError, Messages } from '@/app/model/error'
import { ElectionManifest } from '@/app/model/election'
import type { ManifestHashes } from './manifestHashes'

/**
 * This model class is used to represent information for an election that can easily be displayed and does not contain any large data.
 * It serves as an data store for elections that shall be listed for overview purposes.
 * The data is role unspecific, which means, that every user regardless of his role can access this data as long as he is associated with this election in some way.
 *
 * @version 1.0
 */
export class ElectionMeta {
  protected static readonly THRESHOLD_INVALID: string = 'Error, threshold should be greater or equal one'
  public static readonly MIN_THRESHOLD = 1

  /**
     * Constructs a new instance.
     * This class should only be constructed from data provided by the backend server.
     * @param _title the tile of the election
     * @param _description the description of the election, can be blank
     * @param _authority the e-mail of the authority that configured this election
     * @param _end the date at which the ballot submission is closed
     * @param _threshold the minimum number of trustees required to decrypt the result
     * @param _start the start date of the election, may be null if the election authority hasn't opened the election yet
     * @param _key the public ElGamal-Key of this election
     */
  public constructor (
    private readonly _title: string,
    private readonly _description: string,
    private readonly _authority: string,
    private readonly _end: Date,
    private readonly _threshold: number,
    private readonly _start?: Date | null,
    private readonly _key?: ElementModP | null
  ) {
    if (this.title === '' || this.authority === '') throw new ConfigError(Messages.EMPTY_CONTENT)
    if (this.threshold < ElectionMeta.MIN_THRESHOLD) throw new ConfigError(Messages.OTHER, ElectionMeta.THRESHOLD_INVALID)
    try {
      if (this.start !== null && this.end.toISOString() <= this.start.toISOString()) {
        throw new ConfigError(Messages.OTHER, 'end cannot before start date') // FIXME: remove magic string
      } else {
        this.end.toISOString()
      }
    } catch (e) {
      if (e instanceof RangeError) {
        throw new ConfigError(Messages.OTHER, 'not a valid date') // FIXME: remove magic string
      }
      if (e instanceof ConfigError) {
        const configError = e
        throw new ConfigError(configError.message)
      }
    }
  }

  /**
     * Deserializes a JSON object into a ElectionMeta object
     * @param json JSON containing the attributes of the ELectionMeta
     */
  public static fromJSON (json: any): ElectionMeta {
    const title: any = json.title
    const description: any = json.description !== undefined ? json.description : ''
    const authority: any = json.authority
    const endObj: any = json.end
    const threshold: any = json.threshold
    const startObj: any = json.start
    const keyObj: any = json.key
    if (
      title === undefined ||
      authority === undefined ||
      endObj === undefined ||
      threshold === undefined
    ) {
      throw new ConfigError(Messages.MISSING_ARGUMENT)
    }
    if (typeof title !== 'string') {
      throw new ConfigError(Messages.OTHER, 'Error, title should be of type string') // FIXME: remove magic string
    }
    if (typeof description !== 'string') {
      throw new ConfigError(Messages.OTHER, 'Error, description should be of type string') // FIXME: remove magic string
    }
    if (typeof authority !== 'string') {
      throw new ConfigError(Messages.OTHER, 'Error, authority should be of type string') // FIXME: remove magic string
    }
    if (typeof threshold !== 'number') {
      throw new ConfigError(Messages.OTHER, 'Error, threshold should be of type number') // FIXME: remove magic string
    }
    if (startObj !== null && typeof startObj !== 'string') {
      throw new ConfigError(Messages.OTHER, 'Error, startObj should be of type string') // FIXME: remove magic string
    }
    if (keyObj !== null && typeof keyObj !== 'string') {
      throw new ConfigError(Messages.OTHER, 'Error, keyObj should be of type string') // FIXME: remove magic string
    }

    const end: Date = new Date(endObj)
    let start: Date | undefined
    if (startObj !== null) start = new Date(startObj)
    const key: ElementModP | undefined = globalContext.createElementModPFromHex(keyObj)

    return new ElectionMeta(title, description, authority, end, threshold, start, key)
  }

  /**
     * Creates a new ElectionManifest containing all information in the ELectionMeta as well as the provided
     * trustee information
     * @param trustees List containing information about each trustee, usually the trustees email
     * @param isBotEnabled whether the election uses a bot to simulate a trustee
     */
  public cloneWithTrustees (trustees: string[], isBotEnabled: boolean): ElectionManifest {
    return new ElectionManifest(this, undefined, trustees, isBotEnabled)
  }

  /**
     * Creates a new ElectionManifest containing all information in the ElectionMeta as well as the provided
     * {@link Ballot}
     * @param ballot The ballot style of the eLection
     */
  public cloneWithBallot (ballot: Ballot): ElectionManifest {
    return new ElectionManifest(this, undefined, undefined, undefined, ballot)
  }

  /**
     * Creates a new ElectionManifest containing all information in the ELectionMeta as well as the provided
     * manifest hashes information
     * @param hashes a {@link ManifestHashes} object with the ElectionGuard crypto hashes to the election
     */
  public cloneWithHashes (hashes: ManifestHashes): ElectionManifest {
    return new ElectionManifest(this, undefined, undefined, undefined, undefined, hashes)
  }

  public get title (): string { return this._title }

  public get description (): string { return this._description }

  public get authority (): string { return this._authority }

  public get end (): Date { return this._end }

  public get threshold (): number { return this._threshold }

  public get start (): Date | null { return this._start === undefined ? null : this._start }

  public get key (): ElementModP | null { return this._key === undefined ? null : this._key }
}
