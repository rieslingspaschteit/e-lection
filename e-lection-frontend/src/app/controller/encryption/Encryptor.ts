import { EncryptedBallot, type PlainTextBallot } from '@/app/model/ballot'
import {
  ConstantChaumPedersenProofKnownNonce as ConstantChaumPedersen,
  DisjunctiveChaumPedersenProofKnownNonce as DisjunctiveChaumPedersen,
  ElGamalCiphertext,
  type ElementModP,
  type ElementModQ,
  ElGamalPublicKey
} from 'electionguard'
import type { IEncryptor } from './IEncryptor'
import type { ElectionManifest } from '@/app/model/election'
import { NonceGenerator } from './noceGenerator'
import type { ManifestHashes } from '@/app/model/election/manifest/manifestHashes'
import { ConfigError, Messages } from '@/app/model/error'
import {
  generateExtendedBaseHash,
  globalContext,
  minimumRand
} from '@/app/utils/cryptoConstants'
import { indicesMatch } from '@/app/utils/utils'

/**
 * The class Encryptor implements the interface {@link IEncryptor} using the ElectionGuard-TypeScript implementation (https://github.com/danwallach/ElectionGuard-TypeScript)
 * to encrypt ballots
 * @version 1.0
 */
export class Encryptor implements IEncryptor {
  private static readonly INCOMPLETE = 'Incomplete manifest'
  private static readonly INVALID_BALLOT = 'Error, ballot is invalid'
  private static readonly INVALID_OPTION = 'Error, options must be either 0 or 1'
  protected _seed: ElementModQ
  protected _ballotId: string
  private _nonces?: NonceGenerator

  /**
     * The constructor initializes the random seed that will be used to encrypt ballots
     */
  public constructor () {
    this._seed = globalContext.randQ(minimumRand)
    this._ballotId = globalContext.randQ(minimumRand).toHex()
  }

  /**
     * This method encrypts the provided options of the {@link PlainTextBallot} with expotential ElGamal
     * using the provided public key and the random nonce inizialized in the constructor.
     * It also generates ChaumPedersenProofs that the encryptions are valid
     * @param plaintextBallot the {@link PlainTextBallot} that has to be encrypted
     * @param key the election public key
     * @returns A fully instantiated {@link EncryptedBallot} with the encryption of the ballot
     * and the ChaumPedersenProofs
     */
  public encrypt (plaintextBallot: PlainTextBallot, electionManifest: ElectionManifest): EncryptedBallot {
    if (plaintextBallot === undefined || electionManifest === undefined) {
      throw new ConfigError(Messages.MISSING_ARGUMENT)
    }
    if (!plaintextBallot.check()) {
      throw new ConfigError(Messages.OTHER, Encryptor.INVALID_BALLOT)
    }
    if ((electionManifest.trustees == null) || (electionManifest.hashes == null)) {
      throw new ConfigError(Messages.OTHER, Encryptor.INCOMPLETE)
    }
    if (!indicesMatch(plaintextBallot.selectedOptions, electionManifest.hashes.contestHashes)) {
      throw new ConfigError(Messages.INDEX_MISSMATCH)
    }
    const publicKey = electionManifest.key as ElementModP
    const ballot = plaintextBallot.getWithDummies()
    const ciphertext = this.encryptToCiphertext(ballot, publicKey, electionManifest.hashes)
    const individualProofs = new Map<number, Map<number, DisjunctiveChaumPedersen>>()
    const accumulatedProofs = new Map<number, ConstantChaumPedersen>()
    const acceleratedKey: ElGamalPublicKey = new ElGamalPublicKey(publicKey)

    const commitments = electionManifest.hashes.commitments
    if (commitments === undefined) throw new Error('commitments cannot be undefined')

    const cryptoBaseHash = generateExtendedBaseHash(electionManifest.trustees.length, electionManifest.threshold,
      electionManifest.hashes?.manifestHash, commitments)
    // console.log(62, "qbar", cryptoBaseHash.cryptoHashString)
    for (const key of ballot.keys()) {
      const question = ballot.get(key) as Map<number, number>
      const cipher = ciphertext.get(key) as Map<number, ElGamalCiphertext>
      individualProofs.set(key, this.generateIndividualProofs(question, cipher, acceleratedKey, cryptoBaseHash, key))
      accumulatedProofs.set(key, this.generateAccumulatedProof(question, cipher, acceleratedKey, cryptoBaseHash, plaintextBallot.questions.get(key)?.maxSelections as number, key))
    }
    // TODO add device information
    return new EncryptedBallot(ciphertext, individualProofs, accumulatedProofs, new Date(), this.getBallotId())
  }

  /**
     * Encrypts the options of a question. Returns a map of ciphertexts and
     * @param question the plaintext reprsentation of the question
     * @param publicKey the public Key of the election
     */
  private encryptQuestion (question: ReadonlyMap<number, number>, publicKey: ElementModP, questionId: number): Map<number, ElGamalCiphertext> {
    const ciphertext = new Map<number, ElGamalCiphertext>()
    for (const key of question.keys()) {
      const data = question.get(key) as number
      if (data !== 0 && data !== 1) {
        throw new Error(Encryptor.INVALID_OPTION)
      }
      const nonce: ElementModQ = this.nonces.selectionNonces.get(questionId)?.get(key) as ElementModQ
      const cipherPad = globalContext.gPowP(nonce)
      const cipherData = globalContext.multP(globalContext.gPowP(data), globalContext.powP(publicKey, nonce))
      ciphertext.set(key, new ElGamalCiphertext(cipherPad, cipherData))
    }
    return ciphertext
  }

  /**
     * Generates DisjunctiveChaumPedersenProofs for each option of the question
     * @param question the plaintext reprsentation of the question
     * @param ciphertext the encryption of each option
     * @param publicKey the public key of the election
     * @param extendedBaseHash the Election Extended Base Hash used for generation a challenge
     */
  private generateIndividualProofs (question: Map<number, number>, ciphertext: Map<number, ElGamalCiphertext>, publicKey: ElGamalPublicKey,
    extendedBaseHash: ElementModQ, questionIndex: number): Map<number, DisjunctiveChaumPedersen> {
    const proofs = new Map<number, DisjunctiveChaumPedersen>()
    for (const key of question.keys()) {
      const text = ciphertext.get(key) as ElGamalCiphertext
      const data = question.get(key) as number
      const selectionNonce = this.nonces.selectionNonces.get(questionIndex)?.get(key) as ElementModQ
      const disjunctiveNonce = this.nonces.disjunctiveNonces.get(questionIndex)?.get(key) as ElementModQ
      const proof = DisjunctiveChaumPedersen.create(text, data, selectionNonce, publicKey, disjunctiveNonce, extendedBaseHash)
      proofs.set(key, proof)
    }
    return proofs
  }

  /**
     * Generates an accumulatedChaumPedersenProof for a question
     * @param question plaintext representation of the question
     * @param ciphertext ciphertext of the question
     * @param publicKey public key of the election
     * @param extendedBaseHash election extended base hash
     * @param sum sum of selections made for the question (including dummy options)
     * @param questionIndex index of the question
     * @returns
     */
  private generateAccumulatedProof (question: Map<number, number>, ciphertext: Map<number, ElGamalCiphertext>, publicKey: ElGamalPublicKey,
    extendedBaseHash: ElementModQ, sum: number, questionIndex: number): ConstantChaumPedersen {
    let accPad: ElementModP = globalContext.ONE_MOD_P
    let accData: ElementModP = globalContext.ONE_MOD_P
    let accNonce: ElementModQ = globalContext.ZERO_MOD_Q
    for (const key of question.keys()) {
      const cipher = ciphertext.get(key) as ElGamalCiphertext
      accPad = globalContext.multP(accPad, cipher.pad)
      accData = globalContext.multP(accData, cipher.data)
      const currentNonce = this.nonces.selectionNonces.get(questionIndex)?.get(key) as ElementModQ
      if (currentNonce === undefined) {
        const size = this.nonces.selectionNonces.get(questionIndex)?.size
        throw new Error(`${size ?? ''} ${key} ${questionIndex}`)
      }
      accNonce = globalContext.addQ(accNonce, currentNonce)
    }
    const accCipher = new ElGamalCiphertext(accPad, accData)
    const chaumPedersenNonce = this.nonces.constantNonces.get(questionIndex) as ElementModQ
    return ConstantChaumPedersen.create(accCipher, sum, accNonce, publicKey, chaumPedersenNonce, extendedBaseHash)
  }

  /**
     * This method encrypts a ballot using a given seed
     * It also generates and stores the nonces in the _nonces attribute
     * @param plaintextBallot the selections made for each question, inculding dummy options
     * @param key the public key of the election
     * @param seed the seed for the encryption
     * @returns the encryption for every option using the election public key and given nonce
     */
  protected encryptToCiphertext (plaintextBallot: ReadonlyMap<number, ReadonlyMap<number, number>>, publicKey: ElementModP,
    hashes: ManifestHashes): Map<number, Map<number, ElGamalCiphertext>> {
    this._nonces = new NonceGenerator(this.getSeed(), this.ballotId, hashes)
    const ciphertext = new Map<number, Map<number, ElGamalCiphertext>>()
    for (const entry of plaintextBallot) {
      ciphertext.set(entry[0], this.encryptQuestion(entry[1], publicKey, entry[0]))
    }
    return ciphertext
  }

  /**
     * assures that nonce is defined. Since getter is private, it can only be called inside this class where
     * nonces is defined
     */
  private get nonces (): NonceGenerator {
    return this._nonces as NonceGenerator
  }

  /**
     * Sets the seed used to encrypt ballots
     */
  // eslint-disable-next-line accessor-pairs
  protected set seed (seed: ElementModQ) {
    if (seed === undefined || seed === null) {
      throw new ConfigError(Messages.MISSING_ARGUMENT)
    }
    this._seed = seed
  }

  // eslint-disable-next-line accessor-pairs
  protected set ballotId (ballotId: string) {
    this._ballotId = ballotId
  }

  public getBallotId (): string {
    return this._ballotId
  }

  /**
     * @returns the nonce used for encryptions
     */
  public getSeed (): ElementModQ {
    return this._seed
  }
}
