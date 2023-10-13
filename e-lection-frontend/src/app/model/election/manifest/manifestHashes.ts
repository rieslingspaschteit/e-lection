import type { ElementModQ } from 'electionguard'
import { globalContext, hashElements } from '@/app/utils/cryptoConstants'
import { indicesMatch } from '@/app/utils/utils'
import { ConfigError, Messages } from '@/app/model/error'
import type { Serializable } from '@/app/model/interfaces'

/**
 * This class holds the hashes that are associated with an ElectionGuard election manifest
 */
export class ManifestHashes implements Serializable {
  /**
     * This class should only be instantiated through the fromJSON method
     */
  protected constructor (
    private readonly _manifestHash: ElementModQ,
    private readonly _contestHashes: Map<number, ElementModQ>,
    private readonly _selectionHashes: Map<number, Map<number, ElementModQ>>,
    private readonly _contestIds: ReadonlyMap<number, string>,
    private readonly _selectionIds: ReadonlyMap<number, ReadonlyMap<number, string>>,
    private readonly _commitments: ElementModQ,
    private readonly _voterHash: ElementModQ
  ) {}

  /**
     *
     * @returns the ManifestHashes as a JSON object
     */
  public stringify (): object {
    const contestObj: Map<number, string> = new Map<number, string>()
    for (const entry of this.contestHashes) {
      contestObj.set(entry[0], entry[1].toHex())
    }
    const selectionObj: Map<number, string[]> = new Map<number, string[]>()
    for (const entry of this.selectionHashes) {
      const selArray = new Array<string>()
      for (const selEntry of entry[1]) {
        selArray[selEntry[0]] = selEntry[1].toHex()
      }
      selectionObj.set(entry[0], selArray)
    }
    const selectionIdObj: Map<number, string[]> = new Map<number, string[]>()
    for (const entry of this.selectionIds) {
      const selArray = new Array<string>()
      for (const selEntry of entry[1]) {
        selArray[selEntry[0]] = selEntry[1]
      }
      selectionIdObj.set(entry[0], selArray)
    }
    return {
      manifestHash: this.manifestHash.toHex(),
      contestHashes: Object.fromEntries(contestObj),
      selectionHashes: Object.fromEntries(selectionObj),
      contestIds: Object.fromEntries(this.contestIds),
      selectionIds: Object.fromEntries(selectionIdObj),
      commitments: this.commitments?.toHex(),
      voterHash: this.voterHash?.toHex()
    }
  }

  /**
     * Instantiates a new ManifestHashes object from a given json object
     * @param json The ManifestHashes as sjon object
     * @returns
     */
  public static fromJSON (json: any): ManifestHashes {
    if (json === undefined || json.manifestHash === undefined ||
      json.contestHashes === undefined || json.selectionHashes === undefined ||
      json.contestIds === undefined || json.selectionIds === undefined) {
      throw new ConfigError(Messages.MISSING_ARGUMENT)
    }

    const manifestHash = globalContext.createElementModQFromHex(json.manifestHash)
    const commitments = globalContext.createElementModQFromHex(json.commitments)
    const voterHash = globalContext.createElementModQFromHex(json.voterHash)
    if (manifestHash === undefined || commitments === undefined || voterHash === undefined) {
      throw new ConfigError(Messages.MISSING_ARGUMENT_CONTENT)
    }

    const contestObj = new Map<string, string>(Object.entries(json.contestHashes))
    const contestHashes = new Map<number, ElementModQ>()
    const contestIdsObj = new Map<string, string>(Object.entries(json.contestIds))
    const contestIds: Map<number, string> = new Map<number, string>()
    for (const entry of contestObj) {
      const key: number = +entry[0]
      const value = globalContext.createElementModQFromHex(entry[1])
      if (value == null) { throw new ConfigError(Messages.MISSING_ARGUMENT_CONTENT, entry[1]) }
      contestHashes.set(key, value)
    }

    for (const entry of contestIdsObj) {
      const key: number = +entry[0]
      contestIds.set(key, entry[1])
    }

    const selectionObj = new Map<string, string[]>(Object.entries(json.selectionHashes))
    const selectionIdsObj = new Map<string, string[]>(Object.entries(json.selectionIds))
    const selectionIds: Map<number, Map<number, string>> = new Map<number, Map<number, string>>()
    const selectionHashes = new Map<number, Map<number, ElementModQ>>()
    for (const entry of selectionObj) {
      const selMap = new Map<number, ElementModQ>()
      for (let index = 0; index < entry[1].length; index++) {
        const value = globalContext.createElementModQFromHex(entry[1][index])
        if (value == null) { throw new ConfigError(Messages.MISSING_ARGUMENT_CONTENT) }
        selMap.set(index, value)
      }
      const key: number = +entry[0]
      selectionHashes.set(key, selMap)
    }

    for (const entry of selectionIdsObj) {
      const selMap = new Map<number, string>()
      for (let index = 0; index < entry[1].length; index++) {
        selMap.set(index, entry[1][index])
      }
      const key: number = +entry[0]
      selectionIds.set(key, selMap)
    }

    return new ManifestHashes(
      manifestHash,
      contestHashes,
      selectionHashes,
      contestIds,
      selectionIds,
      commitments,
      voterHash
    )
  }

  public get manifestHash (): ElementModQ {
    return this._manifestHash
  }

  public get contestHashes (): ReadonlyMap<number, ElementModQ> {
    return this._contestHashes
  }

  public get selectionHashes (): ReadonlyMap<number, ReadonlyMap<number, ElementModQ>> {
    return this._selectionHashes
  }

  public get contestIds (): ReadonlyMap<number, string> {
    return this._contestIds
  }

  public get selectionIds (): ReadonlyMap<number, ReadonlyMap<number, string>> {
    return this._selectionIds
  }

  public get commitments (): ElementModQ | undefined {
    return this._commitments
  }

  public get voterHash (): ElementModQ | undefined {
    return this._voterHash
  }

  public equals (hashes: ManifestHashes): boolean {
    const equals = this.manifestHash.equals(hashes.manifestHash)
    if (!indicesMatch(this.selectionHashes, hashes.selectionHashes)) {
      return false
    }
    for (const key of this.contestHashes.keys()) {
      if (!((this.contestHashes.get(key)?.equals(hashes.contestHashes.get(key) as ElementModQ)) ?? false)) {
        return false
      }
      const selectionKeys = this.selectionHashes.get(key)?.keys() as IterableIterator<number>
      for (const selectionKey of selectionKeys) {
        if (!((this.selectionHashes.get(key)?.get(selectionKey)?.equals(hashes.selectionHashes.get(key)?.get(selectionKey) as ElementModQ)) ?? false)) {
          return false
        }
      }
    }
    return equals
  }

  public getHashString (): ElementModQ {
    const selectionHashesHash = new Array<string>()
    const selectionIdsHash = new Array<string>()
    const contestHashesHash = hashElements(this.contestHashes.values())
    const contestIdsHash = hashElements(this.contestIds.values())
    for (const selectionHash of this.selectionHashes.values()) {
      selectionHashesHash.push(hashElements(Array.from(selectionHash.values())).toHex())
    }
    for (const selectionId of this.selectionIds.values()) {
      selectionIdsHash.push(hashElements(Array.from(selectionId.values())).toHex())
    }
    return hashElements(selectionHashesHash, selectionIdsHash, contestHashesHash, contestIdsHash, this.manifestHash,
      this._commitments, this._voterHash)
  }
}
