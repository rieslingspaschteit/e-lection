import type { CryptoHashable, ElementModQ } from 'electionguard'
import { SHA256, enc } from 'crypto-js'
import type { ManifestHashes } from '@/app/model/election/manifest/manifestHashes'
import { globalContext } from '@/app/utils/cryptoConstants'

export class NonceGenerator {
  private readonly _seed: ElementModQ
  private readonly _selectionNonces: Map<number, Map<number, ElementModQ>>
  private readonly _constantNonces: Map<number, ElementModQ>
  private readonly _disjunctiveNonces: Map<number, Map<number, ElementModQ>>
  private readonly _ballotNonce: ElementModQ

  constructor (seed: ElementModQ, ballotId: string, hashes: ManifestHashes) {
    this._seed = seed
    this._selectionNonces = new Map<number, Map<number, ElementModQ>>()
    this._constantNonces = new Map<number, ElementModQ>()
    this._disjunctiveNonces = new Map<number, Map<number, ElementModQ>>()
    this._ballotNonce = this.hashElements(hashes.manifestHash, ballotId, this.seed)
    for (const contest of hashes.contestHashes) {
      this.generateContestNonces(contest[0], this.ballotNonce, contest[1], hashes.selectionHashes.get(contest[0]) as Map<number, ElementModQ>)
    }
  }

  private generateContestNonces (
    contestId: number,
    ballotNonce: ElementModQ,
    contestDescriptionHash: ElementModQ,
    selectionDescriptionHashes: Map<number, ElementModQ>
  ): void {
    const nonceSequence = this.hashElements(contestDescriptionHash, ballotNonce)
    const contestNonce = this.hashElements(nonceSequence, contestId)
    this._constantNonces.set(contestId, this.hashElements(nonceSequence, 0)) // ConstantChaumPedersen nonce
    this._selectionNonces.set(contestId, new Map<number, ElementModQ>()) // Initializing map for encryption nonces
    this._disjunctiveNonces.set(contestId, new Map<number, ElementModQ>()) // Initializing map for proof nonces
    for (const selection of selectionDescriptionHashes) {
      this.generateSelectionNonces(contestId, selection[0], contestNonce, selection[1])
    }
  }

  private generateSelectionNonces (
    contestId: number,
    selectionId: number,
    contestNonce: ElementModQ,
    selectionDescriptionHash: ElementModQ
  ): void {
    const nonceSequence = this.hashElements(selectionDescriptionHash, contestNonce)
    this._selectionNonces.get(contestId)?.set(selectionId, this.hashElements(nonceSequence, selectionDescriptionHash)) // encryption nonce
    this.disjunctiveNonces.get(contestId)?.set(selectionId, this.hashElements(nonceSequence, 0))
  }

  public get seed (): ElementModQ {
    return this._seed
  }

  public get ballotNonce (): ElementModQ {
    return this._ballotNonce
  }

  public get selectionNonces (): Map<number, Map<number, ElementModQ>> {
    return this._selectionNonces
  }

  public get constantNonces (): Map<number, ElementModQ> {
    return this._constantNonces
  }

  public get disjunctiveNonces (): Map<number, Map<number, ElementModQ>> {
    return this._disjunctiveNonces
  }

  /**
 * Hashes a list of zero or more things in the list of things we know how
 * to hash (see {@link CryptoHashable}). Returns an {@link ElementModQ},
 * using the global context
 * @author danwallach
 * @author Christoph Niederbudde
 * with slight modifications to fit format, this method is copied from the hash.ts file of electionguard implementation
 * https://github.com/danwallach/ElectionGuard-TypeScript
 */
  public hashElements (
    ...elements: CryptoHashable[]
  ): ElementModQ {
    // In the Kotlin version of this, where we weren't worried about matching
    // the EG 1.0 spec, we wrote this code entirely in terms of UInt256, which
    // meant that it didn't need to know anything about the GroupContxt. That
    // simplifies everything, but would break backward compatibility with the
    // existing hash function, at least for the iterable case where it needs
    // to do a recursive call.

    const hashMe: string =
      elements.length === 0
        ? 'null'
        : elements
          .map(e => {
            switch (typeof e) {
              case 'undefined':
                return 'null'
              case 'string':
                return e
              case 'bigint':
              case 'number':
                return e?.toString(10) // base 10 in the reference code
              default:
                if ('cryptoHashString' in e) {
                  return e.cryptoHashString
                } else if ('cryptoHashElement' in e) {
                  return e.cryptoHashElement.cryptoHashString
                } else if (
                  Symbol.iterator in e &&
                    typeof e[Symbol.iterator] === 'function'
                ) {
                  const arrayView = Array.from(e)
                  if (arrayView.length === 0) {
                    // special-case for empty-list, for compatibility with reference code
                    return 'null'
                  } else {
                    return this.hashElements(...arrayView).cryptoHashString
                  }
                } else {
                  throw new Error('Unsupported / unexpected type')
                }
            }
          })
          .join('|')

    const hash = SHA256(`|${hashMe}|`).toString(enc.Hex)
    return globalContext.createElementModQFromHex(hash) as ElementModQ
  }
}
