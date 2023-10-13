/**
 * This package contains the classes that are responsible for the webapp's communication with external sources, namely the e-lection server
 * @see {@link ElectionHandler } {@link AuthorityHandler } {@link TrusteeHandler} {@link VoterHandler}
 * It contains handler classes that fetch or send information required or provided by other modules to the server over the Web Endpoints
 * @see {@link } {@link IElectionHandler } {@link IAuthorityHandler } {@link ITrusteeHandler} {@link IVoterHandler}
 * It also contains interfaces of handler classes that can be implemented to fit different external sources
 * @version 1.0
 * @module handler
 *
 */

import type { IElectionHandler } from './handlers/IElectionHandler'
import type { IAuthorityHandler } from './handlers/IAuthorityHandler'
import type { ITrusteeHandler } from './handlers/ITrusteeHandler'
import type { IVoterHandler } from './handlers/IVoterHandler'
import { ElectionHandler } from './handlers/ElectionHandler'
import { AuthorityHandler } from './handlers/AuthorityHandler'
import { TrusteeHandler } from './handlers/TrusteeHandler'
import { VoterHandler } from './handlers/VoterHandler'
import type * as error from './handlers/error'

export { type IElectionHandler, type IAuthorityHandler, type ITrusteeHandler, type IVoterHandler, ElectionHandler, AuthorityHandler, TrusteeHandler, VoterHandler, error }
