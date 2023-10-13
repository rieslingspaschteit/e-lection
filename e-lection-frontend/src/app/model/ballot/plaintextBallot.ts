import { ConfigError, Messages } from '../error'
import { Ballot } from '../ballot'
import type { Question } from '../ballot'

/**
 * This class is used for tracking state while a voter currently fills out his ballot for an election.
 * The selectedOptions Map keeps track of the selected options by mapping.
 * If the voter as selected option i and j of question q with index k, than selectedOptions maps k to an array with entry-value 1 at i and j.
 * A PlainTextBallot should be constructed by calling {@link Ballot.getPlainTextBallot}.
 *
 * @version 1.0
 */
class PlainTextBallot extends Ballot {
  private readonly _selectedOptions: Map<number, Map<number, number>>

  /**
     * Constructs a new instance
     * @param questions the questions for this PlainTextBallot - should be obtained from the ballot its associated with
     */
  public constructor (questions: ReadonlyMap<number, Question>) {
    super(questions)
    this._selectedOptions = new Map<number, Map<number, number>>()
    for (const key of this.questions.keys()) {
      const questionMap = new Map<number, number>()
      const question = this.questions.get(key) as Question
      for (const innerKey of question.options.keys()) {
        questionMap.set(innerKey, 0)
      }
      this._selectedOptions.set(key, questionMap)
    }
  }

  public static fromSelections (ballot: Ballot, selections: ReadonlyMap<number, ReadonlyMap<number, number>>): PlainTextBallot {
    const plaintextBallot: PlainTextBallot = new PlainTextBallot(ballot.questions)
    for (const question of selections) {
      for (const selection of question[1]) {
        if (selection[1] === 1) {
          plaintextBallot.toggleSelection(question[0], selection[0])
        }
      }
    }
    return plaintextBallot
  }

  public get selectedOptions (): Map<number, Map<number, number>> { return this._selectedOptions }

  /**
     * Selects or unselects a specific option of a question with the provided indices.
     * Fails silently if the provided indices do not match any questions or options.
     * @param questionIndex the index of the question
     * @param optionIndex the index of the option of the question
     */
  public toggleSelection (questionIndex: number, optionIndex: number): void {
    if (typeof questionIndex === 'undefined' || typeof optionIndex === 'undefined') {
      throw new ConfigError(Messages.MISSING_ARGUMENT)
    }
    if (typeof this._selectedOptions.get(questionIndex)?.get(optionIndex) !== 'undefined') {
      const selection = this._selectedOptions.get(questionIndex)?.get(optionIndex) as number
      this._selectedOptions.get(questionIndex)?.set(optionIndex, (selection + 1) % 2)
    }
  }

  /**
     * Returns wheter the given option is selected
     * Returns false if the provided indices do not match any questions or options.
     * @param questionIndex
     * @param optionIndex
     */
  public isSelected (questionIndex: number, optionIndex: number): boolean {
    if (typeof questionIndex === 'undefined' || typeof optionIndex === 'undefined') {
      throw new ConfigError(Messages.MISSING_ARGUMENT)
    }
    return this._selectedOptions.get(questionIndex)?.get(optionIndex) === 1
  }

  /**
     * Resturns the total number of selections made for the provided question
     * Resturns 0 if the provided indices do not match any questions or options.
     * @param questionIndex
     */
  public getNumberOfSelectedOptions (questionIndex: number): number {
    if (typeof questionIndex === 'undefined') {
      throw new ConfigError(Messages.MISSING_ARGUMENT)
    }
    let count = 0
    if (typeof this._selectedOptions.get(questionIndex) === 'undefined') {
      return count
    }
    const selections = this._selectedOptions.get(questionIndex) as Map<number, number>
    for (const option of selections.keys()) {
      if (selections.get(option) === 1) {
        count++
      }
    }
    return count
  }

  /**
     * Checks for each question if the number of selected options is at most maxSelections.
     *
     * @returns true if this is the case, false else
     */
  public check (): boolean {
    for (const index of this.questions.keys()) {
      const question = this.questions.get(index) as Question
      const selections = this.getNumberOfSelectedOptions(index)
      if (selections > question.maxSelections) {
        return false
      }
    }
    return true
  }

  public getWithDummies (): ReadonlyMap<number, ReadonlyMap<number, number>> {
    const plaintextWithDummies = new Map<number, Map<number, number>>()
    for (const key of this.questions.keys()) {
      const question: Map<number, number> = this.selectedOptions.get(key) as Map<number, number>
      const questionWithDummies = new Map<number, number>(question)
      const maxSelections = this.questions.get(key)?.maxSelections as number
      const total = this.getNumberOfSelectedOptions(key)
      const highestKey = Array.from(question.keys()).sort((a, b) => a - b)[question.size - 1]
      for (let dummyOne = 0; dummyOne < maxSelections - total; dummyOne++) {
        questionWithDummies.set(dummyOne + highestKey + 1, 1)
      }
      for (let dummyZero = 0; dummyZero < total; dummyZero++) {
        questionWithDummies.set(dummyZero + highestKey + maxSelections - total + 1, 0)
      }
      plaintextWithDummies.set(key, questionWithDummies)
    }
    return plaintextWithDummies
  }
}

export {
  PlainTextBallot
}
