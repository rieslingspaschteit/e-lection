import { Ballot } from '../../ballot'
import { ConfigError, Messages } from '../../error'
import type { Serializable } from '../../interfaces'
import { ManifestHashes } from './manifestHashes'
import { ElectionMeta } from './meta'

/**
 * This model class holds all information to an election that was set by the authority during configuration
 * and is an extended model of the {@link ElectionMeta} with all the data, that uniquely identifies an election.
 * This data is also used for calculating the fingerprint for the election which is a sha265 hash over all class attributes.
 *
 * @version 1.0
 */
export class ElectionManifest extends ElectionMeta implements Serializable {
  private static readonly VOTERS_INVALID = 'Error, voters cannot have empty names'
  private static readonly TRUSTEES_INVALID = 'Error, trustees, cannot have empty names'
  private static readonly TRUSTEES_MISSING = 'Error, at least one trustee required'

  /**
     * Constructs a new instance.
     * This class should only be constructed from data provided by the backend server or from.
     * @param electionMeta the electionMeta instance this manifest shall extend
     * @param _voters an array of the emails of all voters than are able to vote for this election
     * @param _trustees an array of the emails of all trustees for this election
     * @param _isBotEnabled: wether the system should simulate a trustee.
     * @param _ballot the ballot for this election
     */
  public constructor (
    electionMeta: ElectionMeta,
    private readonly _voters?: readonly string[],
    private readonly _trustees?: readonly string[],
    private readonly _isBotEnabled?: boolean,
    private readonly _ballot?: Ballot,
    private readonly _hashes?: ManifestHashes
  ) {
    super(
      electionMeta.title,
      electionMeta.description,
      electionMeta.authority,
      electionMeta.end,
      electionMeta.threshold,
      electionMeta.start,
      electionMeta.key
    )
    if (!this.checkAllStringsNotBlank(this.voters)) {
      throw new ConfigError(Messages.OTHER, ElectionManifest.VOTERS_INVALID)
    }
    if (this.trustees?.length === 0 && !(this.isBotEnabled as boolean)) {
      throw new ConfigError(Messages.OTHER, ElectionManifest.TRUSTEES_MISSING)
    }
    if (this.trustees !== undefined && this.trustees !== null && this.trustees.length > 0 && !this.checkAllStringsNotBlank(this.trustees)) {
      throw new ConfigError(
        Messages.OTHER,
        `
          ${ElectionManifest.TRUSTEES_INVALID} 
          ${this.trustees.toLocaleString()}
          ${this.trustees?.length}
        `
      )
    }
    if (!this.checkAllStringsNotBlank(this.trustees)) {
      throw new ConfigError(Messages.OTHER, ElectionManifest.TRUSTEES_INVALID)
    }
  }

  /**
     * @returns the properties of the ElectionManifest as a JSON string
     */
  public stringify (): object {
    const electionMeta: object = {
      title: this.title,
      description: this.description,
      authority: this.authority,
      start: this.start?.toISOString(),
      end: this.end.toISOString(),
      threshold: this.threshold
    }
    if (this.start != null) (electionMeta as any).start = this.start.toISOString()
    if (this.key != null) (electionMeta as any).key = this.key.toHex()
    const manifest: object = {
      electionMeta,
      trustees: this.trustees,
      isBotEnabled: this.isBotEnabled,
      questions: this.ballot?.stringify()
    }
    if (this.voters != null) (manifest as any).voters = this.voters
    if (this.hashes != null) (manifest as any).hashes = this.hashes.stringify()
    return manifest
  }

  /**
     * Deserializes a JSON object into a ElectionManifest object
     * @param json JSON containing the attributes of the ELectionManifest
     */
  public static fromJSON (json: any): ElectionManifest {
    const meta: ElectionMeta = super.fromJSON(json.electionMeta)

    const trusteesObj: any = json.trustees
    const isBotEnabledObj: any = json.isBotEnabled
    const ballotObj: any = json.questions
    const hashesObj: any = json.hashes

    if (isBotEnabledObj !== null && typeof isBotEnabledObj !== 'boolean') {
      throw new ConfigError(Messages.OTHER, 'Error, provided value is not a boolean') // FIXME: remove magic string
    }

    if (trusteesObj !== null && !Array.isArray(trusteesObj)) {
      throw new ConfigError(Messages.OTHER, 'should be array')
    }

    const trustees: string[] | undefined = trusteesObj ?? trusteesObj as string[]
    let hashes: ManifestHashes | undefined
    if (hashesObj !== null) {
      hashes = ManifestHashes.fromJSON(hashesObj)
    }
    let ballot: Ballot | undefined
    if (ballotObj !== null) {
      ballot = Ballot.fromJSON(ballotObj)
    }

    return new ElectionManifest(
      meta,
      undefined,
      trustees,
      isBotEnabledObj,
      ballot,
      hashes
    )
  }

  public override cloneWithBallot (ballot: Ballot): ElectionManifest {
    return new ElectionManifest(
      this.toElectionMeta(),
      this.voters,
      this.trustees,
      this.isBotEnabled,
      ballot,
      this.hashes
    )
  }

  public override cloneWithTrustees (trustees: string[], isBotEnabled: boolean): ElectionManifest {
    return new ElectionManifest(
      this.toElectionMeta(),
      this.voters,
      trustees,
      isBotEnabled,
      this.ballot,
      this.hashes
    )
  }

  public override cloneWithHashes (hashes: ManifestHashes): ElectionManifest {
    return new ElectionManifest(
      this.toElectionMeta(),
      undefined,
      this.trustees,
      this.isBotEnabled,
      this.ballot,
      hashes
    )
  }

  public get hashes (): ManifestHashes | undefined { return this._hashes }

  public get voters (): readonly string[] | undefined { return this._voters }

  public get trustees (): readonly string[] | undefined { return this._trustees }

  public get isBotEnabled (): boolean | undefined { return this._isBotEnabled }

  public get ballot (): Ballot | undefined {
    return this._ballot
  }

  private checkAllStringsNotBlank (values: readonly string[] | undefined): boolean {
    try {
      return (values != null) ? values.reduce((valid, val) => valid ? val !== '' : false, true) : true
    } catch (e) {
      throw new Error(typeof values)
    }
  }

  public toElectionMeta (): ElectionMeta {
    return new ElectionMeta(
      this.title,
      this.description,
      this.authority,
      this.end,
      this.threshold,
      this.start,
      this.key
    )
  }
}
