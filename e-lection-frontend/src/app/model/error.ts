/**
 * A ConfigError can be thrown to indicate that a {@link Mutable} cannot be transformed
 * into its associated immutable model class.
 *
 * @version 1.0
 */
export class ConfigError extends Error {
  private static readonly _name: string = 'ConfigError'
  private readonly _message: Messages
  /**
     * Constructs a new instance.
     * @param message the message describing the cause for the thrown exception.
     */
  public constructor (message: Messages, text?: string) {
    super(typeof text === 'undefined' ? message : message + text)
    this.name = ConfigError._name
    this._message = message
  }

  public get message (): Messages { return this._message }
}

export enum Messages {
  MISSING_ARGUMENT = 'Error, an argument was undefined: ',
  MISSING_ARGUMENT_CONTENT = 'Error, an argument contained undefined content: ',
  EMPTY = 'Error, argument was empty: ',
  EMPTY_CONTENT = 'Error, argument contains empty content: ',
  OTHER = '',
  INDEX_MISSMATCH = 'Error, indeices must match',

}
