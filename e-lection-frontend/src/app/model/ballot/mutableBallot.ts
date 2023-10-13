import { ConfigError, Messages } from '../error'
import type { Mutable } from '../interfaces'
import { Ballot } from './ballot'
import type { MutableQuestion } from './mutableQuestion'
import type { Question } from './question'

/**
 * A MutableBallot is used to configure a new {@link "ballot".Ballot} for an election.
 * It can store questions that still can be modified during the process of configuration.
 * @see {@link MutableQuestion}
 */
class MutableBallot implements Mutable<Ballot> {
  private readonly _questions: Map<number, MutableQuestion>

  /**
     * Constructs a new instance with a new empty map for MutableQuestions
     */
  public constructor () {
    this._questions = new Map<number, MutableQuestion>()
  }

  public create (): Ballot {
    const questionMap = new Map<number, Question>()
    const questionArray = Array.from(this._questions.keys()).sort((a, b) => a - b)
    for (let i = 0; i < questionArray.length; i++) {
      const question = this._questions.get(questionArray[i]) as MutableQuestion
      questionMap.set(i, question.create())
    }
    return new Ballot(questionMap)
  }

  /**
     * Adds a new {@link MutableQuestion} to this MutableBallot with its associated index.
     * @param question a new MutableQuestion that shall be added to this MutableBallot
     * @param index the index of the question
     */
  public addQuestion (index: number, question: MutableQuestion): void {
    if (typeof index === 'undefined' || typeof question === 'undefined') {
      throw new ConfigError(Messages.MISSING_ARGUMENT)
    }
    this._questions.set(index, question)
  }

  /**
     * Gets the {@link MutableQuestion} by its index.
     * @param index the index of the MutableQuestion.
     */
  public getQuestion (index: number): MutableQuestion {
    if (typeof index === 'undefined') {
      throw new ConfigError(Messages.EMPTY_CONTENT)
    }
    return this._questions.get(index) as MutableQuestion
  }

  /**
     * Removes the {@link MutableQuestion} by its index.
     * @param index the index of th MutableQuestion.
     */
  public removeQuestion (index: number): void {
    if (typeof index === 'undefined') {
      throw new ConfigError(Messages.EMPTY_CONTENT)
    }
    this._questions.delete(index)
  }

  public get questions (): Iterable<[number, MutableQuestion]> {
    return this._questions
  }
}

export {
  MutableBallot
}
