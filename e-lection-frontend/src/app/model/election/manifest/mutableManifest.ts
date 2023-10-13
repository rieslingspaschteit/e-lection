import { MutableBallot } from '@/app/model/ballot'
import { ElectionManifest, ElectionMeta } from '@/app/model/election'
import { ConfigError, Messages } from '@/app/model/error'
import type { Mutable } from '@/app/model//interfaces'

/**
 * A MutableElectionManifest is used to track state while an authority configures a new election.
 * After the configuration a immutable {@link ElectionManifest} can be obtained.
 */
export class MutableElectionManifest implements Mutable<ElectionManifest> {
  private readonly emailRegex = '[a-zA-Z0-9]+(?:.[a-zA-Z0-9]+)*@[a-zA-Z0-9]+(?:.[a-zA-Z0-9]+)*'
  private readonly invalidEmail = 'Error, an email did not match required format'

  private readonly _mutableBallot: MutableBallot
  private _title: string
  private _description: string
  private _authority?: string
  private _end: string
  private _threshold: number
  private _isBotEnabled: boolean
  private _voters?: string[]
  private _trustees?: string[]

  /**
     * Constructs a new instance with a new {@link MutableBallot} that then can be modified.
     */
  public constructor () {
    this._title = ''
    this._description = ''
    this._end = ''
    this._threshold = ElectionMeta.MIN_THRESHOLD
    this._mutableBallot = new MutableBallot()
    this._isBotEnabled = false
    this._trustees = new Array<string>()
  }

  public create (): ElectionManifest {
    console.log(`config: {
      title: ${this.title}
      description: ${this.description}
      authority: ${this.authority ?? ''}
      end: ${this.end ?? ''}
      voters: ${this.voters === undefined ? [].toLocaleString() : this.voters.toLocaleString()}
      trustees: ${this.trustees === undefined ? [].toLocaleString() : this.trustees.toLocaleString()}
      isBotEnabled: ${this.isBotEnabled ? 'true' : 'false'}
      threshold: ${this.threshold}
    }`)

    if ((this.voters == null) || ((this.trustees == null) && typeof (this.isBotEnabled) === 'undefined') || this.authority === undefined) {
      throw new ConfigError(Messages.MISSING_ARGUMENT)
    }
    const endDate = new Date(this.end)
    return new ElectionManifest(
      new ElectionMeta(this.title, this.description, this.authority, endDate, this.threshold),
      this.voters,
      this.trustees,
      this.isBotEnabled,
      this.mutableBallot.create()
    )
  }

  public get title (): string { return this._title }
  public set title (v: string) { this._title = v }

  public get description (): string { return this._description }
  public set description (v: string) { this._description = v }

  public get authority (): string | undefined { return this._authority }
  public set authority (v: string | undefined) { this._authority = v }

  public get end (): string { return this._end }
  public set end (v: string) {
    console.log(`set end: ${v}`)
    this._end = v
  }

  public get threshold (): number { return this._threshold }
  public set threshold (v: number) { this._threshold = v }

  public get voters (): string[] | undefined { return this._voters }
  public set voters (v: string[] | undefined) {
    this._voters = new Array<string>()
    if (v != null) {
      for (const trustee of v) {
        const email = trustee.match(this.emailRegex)
        if ((email == null || email.length) !== 1 && trustee.length > 0) {
          throw new ConfigError(Messages.OTHER, this.invalidEmail)
        }
        if (email != null && email.length === 1) {
          this._voters.push(email[0])
        }
      }
    }
  }

  public get trustees (): string[] | undefined { return this._trustees }
  public set trustees (v: string[] | undefined) {
    this._trustees = new Array<string>()
    if (v != null) {
      for (const trustee of v) {
        const email = trustee.match(this.emailRegex)
        if ((email == null || email.length) !== 1 && trustee.length > 0) {
          throw new ConfigError(Messages.OTHER, this.invalidEmail)
        }
        if (email != null && email.length === 1) {
          this._trustees.push(email[0])
        }
      }
    }
  }

  public get isBotEnabled (): boolean { return this._isBotEnabled }
  public set isBotEnabled (isBotEnabled: boolean) { this._isBotEnabled = isBotEnabled }

  public get mutableBallot (): MutableBallot { return this._mutableBallot }
}
