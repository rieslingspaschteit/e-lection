import { Question } from '../ballot'
import { ConfigError, Messages } from '../error'
import type { Mutable } from '../interfaces'

// eslint-disable-next-line @typescript-eslint/no-extraneous-class
class MutableOption {
  constructor (private _text: string) {
    this.text = this.text === undefined ? '' : this.text
  }

  public get text (): string { return this._text }
  public set text (v: string) { this._text = v }
}

/**
 * A MutableQuestion us used to configure a new {@link Question} associated with a {@link Ballot} for an election.
 * It can store possible options that can be added and removed during the process of configuration.
 */
export class MutableQuestion implements Mutable<Question> {
  private readonly _options: Map<number, MutableOption>
  private _questionText?: string
  private _maxSelections?: number

  /**
     * Constructs a new instance with a new empty map for storing options.
     */
  constructor () {
    this._options = new Map<number, MutableOption>()
    this._questionText = ''
  }

  /**
     * Creates a new {@link Question} object with the given information
     * The indizes of the options argument wil be 0,...,k with the order of options staying the same as in the original options
     * @returns {@link Question} object
     */
  public create (): Question {
    let indexCount = 0
    const compactedMap = new Map(
      [...new Map(this.options).entries()]
        .sort(([key1, _val1], [key2, _val2]) => key1 - key2)
        .map(([_key, val]) => [indexCount++, val.text])
    )
    // No checks here, If arguments are not present Question constructor will throw an exception
    return new Question(this._questionText as string, compactedMap, this._maxSelections as number)
  }

  /**
     * Returns an option by its index.
     * @param index the index of the option
     */
  public getOption (index: number): string | undefined {
    if (typeof index === 'undefined') {
      throw new ConfigError(Messages.MISSING_ARGUMENT)
    }
    return this._options.get(index)?.text
  }

  /**
     * Adds a new option to this MutableQuestion with its associated index.
     * This might silently override an already existing option with the same index.
     * @param index the index of the option
     * @param option the option
     */
  public addOption (index: number, option?: string): void {
    let fixedOption: string = ''
    if (typeof option !== 'undefined') {
      fixedOption = option
    }
    if (typeof index === 'undefined') {
      throw new ConfigError(Messages.MISSING_ARGUMENT)
    }
    this._options.set(index, new MutableOption(fixedOption))
  }

  /**
     * Removes the option with the provided index.
     * Does nothing if no option exists under the provided index.
     * @param index the index of the option
     */
  public removeOption (index: number): void {
    if (typeof index === 'undefined') {
      throw new ConfigError(Messages.MISSING_ARGUMENT)
    }
    this._options.delete(index)
  }

  public get questionText (): string { return this._questionText as string }
  public set questionText (questionText: string) { this._questionText = questionText }
  public get maxSelections (): number { return this._maxSelections as number }
  public set maxSelections (maxSelections: number) { this._maxSelections = maxSelections }

  public get options (): Iterable<[number, MutableOption]> {
    return this._options
  }
}
