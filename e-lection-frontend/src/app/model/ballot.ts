/**
 * This module contains all model-classes that are required to model a ballot.
 * Those ballots can also be of different types. For each type there is a class that extends the base {@link Ballot} class
 * @module
 */

export { Question } from './ballot/question'
export { Ballot } from './ballot/ballot'
export { PlainTextBallot } from './ballot/plaintextBallot'
export { EncryptedBallot } from './ballot/encryptedBallot'
export { SpoiledBallot } from './ballot/spoiledBallot'
export { MutableBallot } from './ballot/mutableBallot'
export { MutableQuestion } from './ballot/mutableQuestion'
