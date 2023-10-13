
/**
 * This error can be thrown if a server request was unsuccessful
 * @version 1.0
 */
class RequestError extends Error {
  private readonly errorType?: RequestErrorTypes

  /**
     * @param message message about the cause of the error
     * @param type The type of error that occurred during the request
     */
  public constructor (message?: string, type?: RequestErrorTypes) {
    super(message)
    this.errorType = type
  }

  public getType (): RequestErrorTypes | undefined {
    return this.errorType
  }
}

/**
 * This error can be thrown if a server response did not contain the expected values
 * This should probably not happen...
 */
class ResponseError extends Error {
  public constructor (private readonly _type: ResponseErrorType) {
    super(_type)
  }

  public get type (): ResponseErrorType {
    return this._type
  }
}

/**
 * Contains different types of errors that can occur at a request
 */
enum RequestErrorTypes {
  CONNECTION_FAILED = 'Connection to server failed, please check your internet connection.',
  REJECTED = 'Request was rejected.',
  OTHER = 'Request failed.',
  NONE = 'Everything is ok'
}

/**
 * Different types of bad responses
 */
enum ResponseErrorType {
  FIELD_MISSING = 'the response is not as expected...',
  FIELD_WITH_WRONG_TYPE = 'a field does not have the expected type'
}

export {
  RequestErrorTypes,
  ResponseErrorType,
  RequestError,
  ResponseError
}
