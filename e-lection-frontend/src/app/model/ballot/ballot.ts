import { hashElements } from '@/app/utils/cryptoConstants'
import { PlainTextBallot, Question } from '../ballot'
import { ConfigError, Messages } from '../error'
import type { Serializable } from '../interfaces'

/**
 * This class models a ballot that belongs to exactly one election.
 * A ballot is immutable and should only be created by using a {@link MutableBallot}
 * A ballot is made up of a set of questions.
 * @see {@link Question}
 *
 * @version 1.0
 */
class Ballot implements Serializable {
  /**
     * Constructs a new immutable instance.
     * @param _questions an array of questions
     */
  public constructor (private readonly _questions: ReadonlyMap<number, Question>) {
    if (typeof this.questions === 'undefined') {
      throw new ConfigError(Messages.MISSING_ARGUMENT)
    }
    if (this.questions.size === 0) {
      throw new ConfigError(Messages.EMPTY)
    }
    for (const question of this.questions.values()) {
      if (typeof question === 'undefined') {
        throw new ConfigError(Messages.MISSING_ARGUMENT_CONTENT)
      }
    }
  }

  public stringify (): object {
    return Object.fromEntries(
      [...this.questions.entries()]
        .sort((e1, e2) => e1[0] - e2[0])
        .map(([index, question]) => [index, question.stringify()])
    )
  }

  public static fromJSON (json: any): Ballot {
    if (typeof json === 'undefined') {
      throw new ConfigError(Messages.MISSING_ARGUMENT)
    }
    return new Ballot(
      new Map(
        Object.entries(json) // FIXME: add checks later on for is number and val not undefined
          .map(([key, val]) => [Number(key), Question.fromJSON(val)])
      )
    )
  }

  public get questions (): ReadonlyMap<number, Question> { return this._questions }

  /**
     * Creates a {@link PlainTextBallot} with the questions of this ballot
     * @returns a new instance of {@link PlainTextBallot}
     */
  public getPlainTextBallot (): PlainTextBallot {
    return new PlainTextBallot(this.questions)
  }

  public getHashString (): string {
    const questionHashes = new Array<string>()
    for (const question of this.questions.values()) {
      questionHashes.push(hashElements(question.questionText, question.options.values(), question.maxSelections).toHex())
    }
    return hashElements(...questionHashes).toHex()
  }
}

export {
  Ballot
}
