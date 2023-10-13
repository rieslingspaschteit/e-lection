/* eslint-disable @typescript-eslint/explicit-function-return-type */
import { EncryptedBallot } from '@/app/model/ballot'
import { globalContext } from '@/app/utils/cryptoConstants'
import {
  ConstantChaumPedersenProofKnownNonce,
  DisjunctiveChaumPedersenProofKnownNonce,
  ElGamalCiphertext,
  ExpandedGenericChaumPedersenProof,
  type ElementModP,
  type ElementModQ
} from 'electionguard'

function setupCiphertext () {
  const ciphertextMap: Map<number, Map<number, ElGamalCiphertext>> = new Map<number, Map<number, ElGamalCiphertext>>()
  // const ciphertextArray: Map<number, object[]> = new Map<number, object[]>();
  const number1 = globalContext.createElementModP(10) as ElementModP
  const number2 = globalContext.createElementModP(100) as ElementModP
  const ciphertext1 = new ElGamalCiphertext(number1, number2)
  const ciphertext2 = new ElGamalCiphertext(number2, number1)
  const innerMap = new Map<number, ElGamalCiphertext>()
  innerMap.set(0, ciphertext1)
  innerMap.set(1, ciphertext2)
  ciphertextMap.set(0, innerMap)
  const ciphertextArray = {
    0: [{
      pad: number1.toHex(),
      data: number2.toHex()
    },
    {
      pad: number2.toHex(),
      data: number1.toHex()
    }
    ],
    1: [{
      pad: number1.toHex(),
      data: number2.toHex()
    },
    {
      pad: number1.toHex(),
      data: number2.toHex()
    }
    ]
  }
  const innerMap2 = new Map<number, ElGamalCiphertext>()
  innerMap2.set(0, ciphertext1)
  innerMap2.set(1, ciphertext1)
  ciphertextMap.set(1, innerMap2)
  return [ciphertextMap, ciphertextArray]
}

function setupIndProof () {
  const individualMap: Map<number, Map<number, DisjunctiveChaumPedersenProofKnownNonce>> = new Map<number, Map<number, DisjunctiveChaumPedersenProofKnownNonce>>()
  // const individualArray: Map<number, object[]> = new Map<number, object[]>();
  const number1 = globalContext.createElementModP(10) as ElementModP
  const number2 = globalContext.createElementModP(900) as ElementModP
  const number3 = globalContext.createElementModQ(12) as ElementModQ
  const number4 = globalContext.createElementModQ(180) as ElementModQ
  const proof10 = new ExpandedGenericChaumPedersenProof(number1, number2, number3, number4)
  const proof11 = new ExpandedGenericChaumPedersenProof(number2, number1, number4, number3)
  const proof1 = new DisjunctiveChaumPedersenProofKnownNonce(proof10, proof11, number3)
  const proof2 = new DisjunctiveChaumPedersenProofKnownNonce(proof11, proof10, number4)
  const innerMap = new Map<number, DisjunctiveChaumPedersenProofKnownNonce>()
  innerMap.set(0, proof1)
  innerMap.set(1, proof2)
  individualMap.set(0, innerMap)
  const individualArray = {
    0: [
      {
        proof0: {
          pad: number1.toHex(),
          data: number2.toHex(),
          challenge: number3.toHex(),
          response: number4.toHex()
        },
        proof1: {
          pad: number2.toHex(),
          data: number1.toHex(),
          challenge: number4.toHex(),
          response: number3.toHex()
        },
        challenge: number3.toHex()
      },
      {
        proof1: {
          pad: number1.toHex(),
          data: number2.toHex(),
          challenge: number3.toHex(),
          response: number4.toHex()
        },
        proof0: {
          pad: number2.toHex(),
          data: number1.toHex(),
          challenge: number4.toHex(),
          response: number3.toHex()
        },
        challenge: number4.toHex()
      }
    ],
    1: [
      {
        proof0: {
          pad: number1.toHex(),
          data: number2.toHex(),
          challenge: number3.toHex(),
          response: number4.toHex()
        },
        proof1: {
          pad: number2.toHex(),
          data: number1.toHex(),
          challenge: number4.toHex(),
          response: number3.toHex()
        },
        challenge: number3.toHex()
      },
      {
        proof0: {
          pad: number1.toHex(),
          data: number2.toHex(),
          challenge: number3.toHex(),
          response: number4.toHex()
        },
        proof1: {
          pad: number2.toHex(),
          data: number1.toHex(),
          challenge: number4.toHex(),
          response: number3.toHex()
        },
        challenge: number3.toHex()
      }
    ]
  }
  const innerMap2 = new Map<number, DisjunctiveChaumPedersenProofKnownNonce>()
  innerMap2.set(0, proof1)
  innerMap2.set(1, proof1)
  individualMap.set(1, innerMap2)
  return [individualMap, individualArray]
}

function setupAccProof () {
  const accumulatedMap: Map<number, ConstantChaumPedersenProofKnownNonce> = new Map<number, ConstantChaumPedersenProofKnownNonce>()
  // const accumulatedArray: Map<number, object> = new Map<number, object>();
  const number1 = globalContext.createElementModP(101) as ElementModP
  const number2 = globalContext.createElementModP(100) as ElementModP
  const number3 = globalContext.createElementModQ(12) as ElementModQ
  const number4 = globalContext.createElementModQ(180) as ElementModQ
  const proof10 = new ExpandedGenericChaumPedersenProof(number1, number2, number3, number4)
  const proof11 = new ExpandedGenericChaumPedersenProof(number2, number1, number4, number3)
  const proof1 = new ConstantChaumPedersenProofKnownNonce(proof10, 12)
  const proof2 = new ConstantChaumPedersenProofKnownNonce(proof11, 2)
  accumulatedMap.set(0, proof1)
  const accumulatedArray = {
    0: {
      pad: number1.toHex().toString(),
      data: number2.toHex().toString(),
      challenge: number3.toHex().toString(),
      response: number4.toHex().toString(),
      constant: 12
    },
    1: {
      pad: number2.toHex().toString(),
      data: number1.toHex().toString(),
      challenge: number4.toHex().toString(),
      response: number3.toHex().toString(),
      constant: 2
    }
  }
  accumulatedMap.set(1, proof2)
  return [accumulatedMap, accumulatedArray]
}

function getEnc () {
  const date = new Date(2000)
  const device = 'empty'

  const ciphertext = setupCiphertext()
  const ciphertextMap = ciphertext[0] as Map<number, Map<number, ElGamalCiphertext>>
  const ciphertextArray = ciphertext[1]
  const indProofs = setupIndProof()
  const individualMap = indProofs[0] as Map<number, Map<number, DisjunctiveChaumPedersenProofKnownNonce>>
  const individualArray = indProofs[1]
  const accProofs = setupAccProof()
  const accumulatedMap = accProofs[0] as Map<number, ConstantChaumPedersenProofKnownNonce>
  const accumulatedArray = accProofs[1] as object
  const ballot = new EncryptedBallot(ciphertextMap, individualMap, accumulatedMap, date, '0', device)
  const json = {
    individualProofs: individualArray,
    accumulatedProofs: accumulatedArray,
    cipherText: ciphertextArray,
    deviceInformation: device,
    date: date.toISOString(),
    ballotId: '0'
  }
  return { ballot, json }
}

export { getEnc, setupAccProof, setupIndProof, setupCiphertext }
