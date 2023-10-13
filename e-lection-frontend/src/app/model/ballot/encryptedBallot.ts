import { globalContext } from '@/app/utils/cryptoConstants'
import { indicesMatch } from '@/app/utils/utils'
import {
  ElGamalCiphertext,
  ExpandedGenericChaumPedersenProof,
  ConstantChaumPedersenProofKnownNonce as ConstantChaumPedersen,
  DisjunctiveChaumPedersenProofKnownNonce as DisjunctiveChaumPedersen
} from 'electionguard'
import { ConfigError, Messages } from '../error'
import type { Serializable } from '../interfaces'

/**
 * An EncryptedBallot is used for holding the encrypted version of a {@link PlainTextBallot},
 * but also the associated proofs and other details of the encryption.
 *
 * An encrypted ballot is, as longs as handled on the client side, considered as a spoiled, (not submitted)
 * It can be send to the server, and if the proofs are valid, the server responds with a valid tracking code for this encrypted ballot.
 *
 * @version 1.0
 */
export class EncryptedBallot implements Serializable {
  /**
     * Constructs a new instance. Should only be instantiated by an Encryptor.
     * Correctness of cryptographic proofs is not checked for
     * @param _ciphertext the encrypted questions with their selected options of a {@link PlainTextBallot} instance.
     * @param _individualProofs proofs that proof that an encrypted selected option encrypts either 0 or 1.
     * @param _accumulatedProofs proofs that proof that an encrypted question does not violate the min max restrictions.
     * @param _encryptionDate the date at which the encryption was computed.
     * @param _deviceInformation information about the device the encryption was computed on. May be missing
     *
     * @see {@link ChaumPedersen}
     */
  public constructor (
    private readonly _ciphertext: ReadonlyMap<number, ReadonlyMap<number, ElGamalCiphertext>>,
    private readonly _individualProofs: ReadonlyMap<number, ReadonlyMap<number, DisjunctiveChaumPedersen>>,
    private readonly _accumulatedProofs: ReadonlyMap<number, ConstantChaumPedersen>,
    private readonly _encryptionDate: Date,
    private readonly _ballotId: string,
    private readonly _deviceInformation?: string
  ) {
    if (
      this.ciphertext === undefined ||
      this.individualProofs === undefined ||
      this.accumulatedProofs === undefined ||
      this.encryptionDate === undefined ||
      this.ballotId === undefined
    ) {
      throw new ConfigError(Messages.MISSING_ARGUMENT)
    }
    if (!indicesMatch(this.ciphertext, this.individualProofs) || !indicesMatch(this.ciphertext, this.accumulatedProofs)) {
      throw new ConfigError(Messages.INDEX_MISSMATCH)
    }
    if (this.ballotId.length === 0) {
      throw new ConfigError(Messages.EMPTY)
    }
  }

  public static fromJSON (json: any): EncryptedBallot {
    if (typeof json === 'undefined') {
      throw new ConfigError(Messages.MISSING_ARGUMENT)
    }
    if (json.cipherText === undefined ||
      json.individualProofs === undefined ||
      json.accumulatedProofs === undefined ||
      json.date === undefined
    ) {
      throw new ConfigError(Messages.MISSING_ARGUMENT_CONTENT)
    }

    const ciphertext = EncryptedBallot.fromJSONciphertext(new Map(Object.entries(json.cipherText)))
    const individual = EncryptedBallot.fromJSONindividual(new Map(Object.entries(json.individualProofs)))
    const accumulated = EncryptedBallot.fromJSONaccumulated(new Map(Object.entries(json.accumulatedProofs)))
    const date: Date = new Date(json.date)
    const device: string = json.deviceInformation
    const ballotId: string = json.ballotId
    return new EncryptedBallot(ciphertext, individual, accumulated, date, ballotId, device)
  }

  private static fromJSONciphertext (ciphertext: Map<string, any[]>): Map<number, Map<number, ElGamalCiphertext>> {
    const ciphertextMap = new Map<number, Map<number, ElGamalCiphertext>>()
    for (const key of ciphertext.keys()) {
      const numberKey: number = +key
      ciphertextMap.set(numberKey, new Map<number, ElGamalCiphertext>())
      const innerArray = ciphertext.get(key) as any[]
      for (let index = 0; index < innerArray.length; index++) {
        if (innerArray[index].pad === undefined || innerArray[index].data === 'undefined') {
          throw new ConfigError(Messages.MISSING_ARGUMENT_CONTENT)
        }
        const pad = globalContext.createElementModPFromHex(innerArray[index].pad)
        const data = globalContext.createElementModPFromHex(innerArray[index].data)
        if ((pad == null) || (data == null)) {
          throw new ConfigError(Messages.MISSING_ARGUMENT_CONTENT)
        }
        ciphertextMap.get(numberKey)?.set(index, new ElGamalCiphertext(pad, data))
      }
    }
    return ciphertextMap
  }

  private static fromJSONindividual (proofs: Map<string, any[]>): Map<number, Map<number, DisjunctiveChaumPedersen>> {
    const proofMap = new Map<number, Map<number, DisjunctiveChaumPedersen>>()
    for (const key of proofs.keys()) {
      const numberKey: number = +key
      proofMap.set(numberKey, new Map<number, DisjunctiveChaumPedersen>())
      const innerArray = proofs.get(key) as any[]
      for (let index = 0; index < innerArray.length; index++) {
        if (innerArray[index].proof0 === undefined || innerArray[index].proof1 === 'undefined' ||
                innerArray[index].challenge === 'undefined') {
          throw new ConfigError(Messages.MISSING_ARGUMENT_CONTENT)
        }
        const proof0 = EncryptedBallot.fromJSONgeneric(innerArray[index].proof0)
        const proof1 = EncryptedBallot.fromJSONgeneric(innerArray[index].proof1)
        const challenge = globalContext.createElementModQFromHex(innerArray[index].challenge)
        if (challenge == null) {
          throw new ConfigError(Messages.MISSING_ARGUMENT_CONTENT)
        }
        proofMap.get(numberKey)?.set(index, new DisjunctiveChaumPedersen(proof0, proof1, challenge))
      }
    }
    return proofMap
  }

  private static fromJSONgeneric (proof: any): ExpandedGenericChaumPedersenProof {
    if (proof.pad === undefined || proof.data === undefined || proof.challenge === undefined || proof.response === undefined) {
      throw new ConfigError(Messages.MISSING_ARGUMENT_CONTENT)
    }
    const pad = globalContext.createElementModPFromHex(proof.pad)
    const data = globalContext.createElementModPFromHex(proof.data)
    const challenge = globalContext.createElementModQFromHex(proof.challenge)
    const response = globalContext.createElementModQFromHex(proof.response)
    if ((pad == null) || (data == null) || (challenge == null) || (response == null)) {
      throw new ConfigError(Messages.MISSING_ARGUMENT_CONTENT)
    }
    return new ExpandedGenericChaumPedersenProof(pad, data, challenge, response)
  }

  private static fromJSONaccumulated (proofs: Map<string, any>): Map<number, ConstantChaumPedersen> {
    const proofMap = new Map<number, ConstantChaumPedersen>()
    for (const key of proofs.keys()) {
      const numberKey: number = +key
      if (typeof proofs.get(key).constant === 'undefined') {
        throw new ConfigError(Messages.MISSING_ARGUMENT_CONTENT)
      }
      const proof = EncryptedBallot.fromJSONgeneric(proofs.get(key))
      proofMap.set(numberKey, new ConstantChaumPedersen(proof, proofs.get(key).constant))
    }
    return proofMap
  }

  public stringify (): object {
    const ciphertextArray = new Map<number, object[]>()
    for (const key of this._ciphertext.keys()) {
      const array = this.stringifyMapToArray(this.ciphertext.get(key) as ReadonlyMap<number, ElGamalCiphertext>, this.stringifyCiphertext)
      ciphertextArray.set(key, array)
    }

    const proofArray = new Map<number, object[]>()
    for (const key of this.individualProofs.keys()) {
      const array = this.stringifyMapToArray(this.individualProofs.get(key) as ReadonlyMap<number, DisjunctiveChaumPedersen>, this.stringifyIndividualProof)
      proofArray.set(key, array)
    }

    const sumProofArray = new Map<number, object>()
    for (const key of this.accumulatedProofs.keys()) {
      sumProofArray.set(key, this.stringifyAccumulatedProof(this.accumulatedProofs.get(key) as ConstantChaumPedersen))
    }
    const ciphertextObj = Object.fromEntries(ciphertextArray)
    const indProofsObj = Object.fromEntries(proofArray)
    const accProofsObj = Object.fromEntries(sumProofArray)
    const ballotObj = {
      individualProofs: indProofsObj,
      accumulatedProofs: accProofsObj,
      cipherText: ciphertextObj,
      ballotId: this.ballotId,
      date: this.encryptionDate.toISOString()
    }
    if (this.deviceInformation !== undefined) (ballotObj as any).deviceInformation = this.deviceInformation
    return ballotObj
  }

  private stringifyCiphertext (ciphertext: ElGamalCiphertext): object {
    return {
      pad: ciphertext.pad.toHex(),
      data: ciphertext.data.toHex()
    }
  }

  private stringifyIndividualProof (proof: DisjunctiveChaumPedersen): object {
    return {
      proof0: {
        pad: proof.proof0.a.toHex(),
        data: proof.proof0.b.toHex(),
        challenge: proof.proof0.c.toHex(),
        response: proof.proof0.r.toHex()
      },
      proof1: {
        pad: proof.proof1.a.toHex(),
        data: proof.proof1.b.toHex(),
        challenge: proof.proof1.c.toHex(),
        response: proof.proof1.r.toHex()
      },
      challenge: proof.c.toHex()
    }
  }

  private stringifyAccumulatedProof (proof: ConstantChaumPedersen): object {
    return {
      pad: proof.proof.a.toHex(),
      data: proof.proof.b.toHex(),
      challenge: proof.proof.c.toHex(),
      response: proof.proof.r.toHex(),
      constant: proof.constant
    }
  }

  // eslint-disable-next-line @typescript-eslint/ban-types
  private stringifyMapToArray (ciphertext: ReadonlyMap<number, any>, stringify: Function): object[] {
    const array = new Array<object>()
    let count = 0
    // eslint-disable-next-line @typescript-eslint/require-array-sort-compare
    for (const key of [...ciphertext.keys()].sort()) { // Sort is prob. unnecessary bur required according to docs
      array[count++] = stringify(ciphertext.get(key))
    }
    return array
  }

  public get ciphertext (): ReadonlyMap<number, ReadonlyMap<number, ElGamalCiphertext>> { return this._ciphertext }
  public get individualProofs (): ReadonlyMap<number, ReadonlyMap<number, DisjunctiveChaumPedersen>> { return this._individualProofs }
  public get accumulatedProofs (): ReadonlyMap<number, ConstantChaumPedersen> { return this._accumulatedProofs }
  public get deviceInformation (): string | undefined { return this._deviceInformation }
  public get encryptionDate (): Date { return this._encryptionDate }
  public get ballotId (): string { return this._ballotId }
}
