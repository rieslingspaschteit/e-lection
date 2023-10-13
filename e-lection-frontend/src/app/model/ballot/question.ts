import { ConfigError, Messages } from '../error'
import type { Serializable } from '../interfaces'

/**
 * This class models a question that belongs to exactly one ballot.
 * A Question instance holds the question-text, an array of selectable options
 * and an interval describing how many options may be selected by the voter.
 *
 * @version 1.0
 */
class Question implements Serializable {
  private static readonly INVALID_CONSTRAINTS: string = 'Error, constraints are impossible to complete'

  // TODO should options indizes be required to be 0,...,n? Information would get lost on transformation
  /**
     * Constructs a new question instance
     * @param _questionText the question-text for this question. Must not be empty
     * @param _options a Map of option indices to the options text. The map and each option text must not be empty
     * @param _minSelections the minimum number of selected options. Must not be greater than the size of the _options map
     * or negative
     * @param _maxSelections the maximum number of selected options. Must not be smaller than _minSelections
     */
  public constructor (
    private readonly _questionText: string,
    private readonly _options: ReadonlyMap<number, string>,
    private readonly _maxSelections: number
  ) {
    if (
      this.questionText === undefined || this.questionText === null ||
      this.options === undefined || this.options === null ||
      this.maxSelections === undefined || this.maxSelections === null
    ) {
      throw new ConfigError(Messages.MISSING_ARGUMENT)
    }

    if (this.questionText.length === 0) {
      throw new ConfigError(Messages.EMPTY)
    } else if (this.options.size === 0) {
      throw new ConfigError(Messages.EMPTY)
    } else if (Array.from(this.options.values()).includes('')) {
      throw new ConfigError(Messages.EMPTY_CONTENT)
    } else if (this.maxSelections < 0) {
      throw new ConfigError(Messages.OTHER, Question.INVALID_CONSTRAINTS)
    }
  }

  public stringify (): object {
    return {
      questionText: this.questionText,
      options: Array.from(this.options.values()),
      max: this.maxSelections
    }
  }

  public static fromJSON (json: any): Question {
    if (typeof json === 'undefined') {
      throw new ConfigError(Messages.MISSING_ARGUMENT)
    }
    const options = new Map<number, string>()
    const optis: string[] = json.options
    if (typeof optis === 'undefined') {
      throw new ConfigError(Messages.MISSING_ARGUMENT_CONTENT)
    }
    for (let i = 0; i < optis.length; i++) {
      options.set(i, optis[i])
    }
    return new Question(json.questionText, options as ReadonlyMap<number, string>, json.max)
  }

  public get questionText (): string { return this._questionText }
  public get options (): ReadonlyMap<number, string> { return this._options }
  public get maxSelections (): number { return this._maxSelections }
}

export {
  Question
}
