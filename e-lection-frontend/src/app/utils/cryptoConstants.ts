import { SHA256, enc } from 'crypto-js'
import { bigIntContext4096, type CryptoHashable, type ElementModQ } from 'electionguard'
import { intToHex } from '@/app/utils/utils'

export const globalContext = bigIntContext4096() // less secure but faster
export const minimumRand = 2 // Increase this for more security

/**
 * Hashes a list of zero or more things in the list of things we know how
 * to hash (see {@link CryptoHashable}). Returns an {@link ElementModQ},
 * using the global context
 * @author danwallach
 * @author Christoph Niederbudde
 * with slight modifications to fit format, this method is copied from the hash.ts file of electionguard implementation
 * https://github.com/danwallach/ElectionGuard-TypeScript
 */
export function hashElements (
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
                    return hashElements(...arrayView).cryptoHashString
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

export function generateExtendedBaseHash (numberOfGuardian: number,
  quorum: number,
  manifestHash: ElementModQ,
  commitmentHash: ElementModQ
): ElementModQ {
  const cryptoBaseHash = hashElements(
    intToHex(globalContext.P),
    intToHex(globalContext.Q),
    intToHex(globalContext.G),
    numberOfGuardian,
    quorum,
    manifestHash
  )

  const cryptoExtendedBaseHash = hashElements(
    cryptoBaseHash,
    commitmentHash
  )
  return cryptoExtendedBaseHash
}
