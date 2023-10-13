import type { Serializable } from '../interfaces'
import { EncryptedBallot } from './encryptedBallot'
import { ElectionManifest } from '../election'
import { ConfigError, Messages } from '../error'
import type { ElementModQ } from 'electionguard'
import { globalContext } from '@/app/utils/cryptoConstants'

/**
 * A spoiled Ballot can be downloaded after a voter decides to spoil his ballot.
 * A spoiled Ballot contains all necessary data for validation the encryption of the ballot.
 *
 * @version 1.0
 */
export class SpoiledBallot implements Serializable {
  private static readonly INCOMPLETE = 'Error, incomplete Spoiled Ballot must not be exported'
  private static readonly FROM_HEX_FAILED = 'Error, illegal input for bigint'

  private _manifest?: ElectionManifest

  /**
     * Constructs a new instance
     * @param _trackingCode the tracking-code of the encrypted-ballot
     * @param _lastTrackingCode the tracking-code of the ballot that was submitted last
     * @param _nonce the nonce the was used for the encryption of the ballot
     * @param _encryptedBallot the {@link EncryptedBallot} this ballot spoils
     * @param _plaintextBallot the {@link PlainTextBallot} the _encryptedBallot encrypted
     */
  public constructor (
    private readonly _trackingCode: ElementModQ,
    private readonly _lastTrackingCode: ElementModQ,
    private readonly _nonce: ElementModQ,
    private readonly _encryptedBallot: EncryptedBallot,
    private readonly _plaintextBallot: ReadonlyMap<number, ReadonlyMap<number, number>>
  ) {
    if (
      this.trackingCode === undefined || this.trackingCode === null ||
      this.lastTrackingCode === undefined || this.lastTrackingCode === null ||
      this.nonce === undefined || this.nonce === null ||
      this.encryptedBallot === undefined || this.encryptedBallot === null ||
      this.plaintextBallot === undefined || this.plaintextBallot === null ||
      this.plaintextBallot.size === 0
    ) {
      throw new ConfigError(Messages.EMPTY_CONTENT)
    }
  }

  private stringifyMapToArray (map: ReadonlyMap<number, any>): number[] {
    const array = new Array<number>()
    let count = 0
    for (const key of [...map.keys()].sort((a, b) => a - b)) { // Sort is prob. unnecessary but required according to docs
      array[count++] = map.get(key)
    }
    return array
  }

  private static fromJSONArrayToMap (array: number[]): ReadonlyMap<number, number> {
    const map = new Map<number, number>()
    for (let index = 0; index < array.length; index++) { // Sort is prob. unnecessary but required according to docs
      map.set(index, array[index])
    }
    return map as ReadonlyMap<number, number>
  }

  public stringify (): object {
    if (typeof this.manifest === 'undefined') {
      throw new ConfigError(Messages.OTHER, SpoiledBallot.INCOMPLETE)
    }
    const plaintextArray = new Map<number, number[]>()
    for (const entry of this.plaintextBallot) {
      plaintextArray.set(entry[0], this.stringifyMapToArray(entry[1]))
    }
    return {
      plaintextBallot: Object.fromEntries(plaintextArray),
      encryptedBallot: this.encryptedBallot.stringify(),
      nonce: this.nonce.toHex(),
      trackingCode: this.trackingCode.toHex(),
      lastTrackingCode: this.lastTrackingCode.toHex(),
      manifest: this.manifest?.stringify()
    }
  }

  public static fromJSON (json: any): SpoiledBallot {
    if (typeof json === 'undefined') {
      throw new ConfigError(Messages.MISSING_ARGUMENT)
    }
    if (
      json.plaintextBallot === undefined || json.encryptedBallot === undefined ||
      json.trackingCode === undefined || json.lastTrackingCode === undefined ||
      json.manifest === undefined
    ) {
      throw new ConfigError(Messages.MISSING_ARGUMENT_CONTENT)
    }
    const plaintextBallotBasic = new Map<string, number[]>(Object.entries(json.plaintextBallot))
    const encryptedBallot = EncryptedBallot.fromJSON(json.encryptedBallot)
    const nonce: ElementModQ = globalContext.createElementModQFromHex(json.nonce) as ElementModQ

    if (typeof nonce === 'undefined') {
      throw new ConfigError(Messages.MISSING_ARGUMENT_CONTENT, SpoiledBallot.FROM_HEX_FAILED)
    }

    const trackingCode: string = json.trackingCode
    const lastTrackingCode: string = json.lastTrackingCode
    const manifest = ElectionManifest.fromJSON(json.manifest)
    const plaintextBallot = new Map<number, ReadonlyMap<number, number>>()
    for (const key of plaintextBallotBasic.keys()) {
      const numberKey: number = +key
      plaintextBallot.set(numberKey, SpoiledBallot.fromJSONArrayToMap(plaintextBallotBasic.get(key) as number[]))
    }
    const spoiledBallot = new SpoiledBallot(globalContext.createElementModQFromHex(trackingCode) as ElementModQ,
      globalContext.createElementModQFromHex(lastTrackingCode) as ElementModQ,
      nonce, encryptedBallot, plaintextBallot as ReadonlyMap<number, ReadonlyMap<number, number>>)
    spoiledBallot.manifest = manifest
    return spoiledBallot
  }

  public get trackingCode (): ElementModQ { return this._trackingCode }
  public get lastTrackingCode (): ElementModQ { return this._lastTrackingCode }
  public get nonce (): ElementModQ { return this._nonce }
  public get encryptedBallot (): EncryptedBallot { return this._encryptedBallot }
  public get plaintextBallot (): ReadonlyMap<number, ReadonlyMap<number, number>> { return this._plaintextBallot }
  public get manifest (): ElectionManifest | undefined { return this._manifest }
  public set manifest (electionManifest: ElectionManifest | undefined) { this._manifest = electionManifest }
}
