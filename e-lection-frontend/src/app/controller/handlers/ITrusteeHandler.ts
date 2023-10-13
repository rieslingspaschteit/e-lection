import type { DecryptionElection, KeyCeremonyElection } from '@/app/model/election'
import type { IElectionHandler } from './IElectionHandler'

/**
 * The interface ITrusteeHandler defines the properties of a handler class that is concerned with the requests required by users in the role trustee
 * @version 1.0
 */
export interface ITrusteeHandler extends IElectionHandler {
  /**
     * This method fetches the phase of the key ceremony of the current election
     * If the key ceremony is in the state EPKB it fetches the auxiliary keys provided by the other trustees
     * If the key ceremony is in the state FINISHED it fetches the EPKBs provided by the other trustees
     */
  fetchKeyCeremony: () => Promise<void>

  /**
     * If the key ceremony is in the state AUX_KEYS, this method posts the auxiliary key of the current election to the server.
     * If the key ceremony is in the state EPKB, it posts the EPKBs of the current election to the server
     */
  postKeyCeremony: () => Promise<void>

  /**
     * This method fetches the phase of the decryption of the current election and the encrypted result of the election
     */
  fetchDecryption: () => Promise<void>

  /**
     * This method posts the partial decryptions of the result of the current election
     */
  postDecryption: () => Promise<void>

  /**
     * @returns the current election as {@link KeyCeremonyElection}
     * If the current election is not a {@link KeyCeremonyElection}, it fetches the missing information and creates a new {@link KeyCeremonyElection}
     */
  getKeyCeremonyElection: () => Promise<KeyCeremonyElection>

  /**
     * @returns the current election as {@link DecryptionElection}
     * If the current election is not a {@link DecryptionElection}, it fetches the missing information and creates a new {@link DecryptionElection}
     */
  getDecryptionElection: () => Promise<DecryptionElection>

}
