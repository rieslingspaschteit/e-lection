/**
 * This package contains the classes that are responsible for creating encryptions and encryption proofs
 * @see {@link IEncryptor} {@link Encryptor}
 * It provides methods to encrypt ballots and create cryptographic proofs of the correctness
 * @see {@link IVerifier} {@link Verifier}
 * It verifies the correctness of the encryption of a ballot
 * @version 1.0
 * @module encryption
 */

export type { IVerifier } from './encryption/IVerifier'
export type { IEncryptor } from './encryption/IEncryptor'
export { Verifier } from './encryption/Verifier'
export { Encryptor } from './encryption/Encryptor'
