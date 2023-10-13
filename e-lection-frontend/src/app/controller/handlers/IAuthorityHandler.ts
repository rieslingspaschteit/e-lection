import type { AuthorityElection, ElectionManifest } from '@/app/model/election'

import type { IElectionHandler } from './IElectionHandler'
/**
 * The interface IAuthorityHandler defines the properties of a handler class that is concerned with the requests required by users in the role authority.
 * @version 1.0
 */
export interface IAuthorityHandler extends IElectionHandler {

  /**
    * This method posts the {@link ElectionManifest} to the server as a newly created election
    */
  postElection: (manifest: ElectionManifest) => Promise<void>

  /**
     * This method fetches the state and the attendance count of the key ceremony of the current election
     */
  fetchKeyCeremonyAttendance: () => Promise<void>

  /**
     * This method fetches the state and the attendance count of the decryption phase of the current election
     */
  fetchDecryptionAttendance: () => Promise<void>

  /**
     * This method opens the current election
     */
  openElection: () => Promise<void>

  /**
     * This method sets the decryption to the next phase for the trustees
     */
  updateDecryption: () => Promise<void>

  /**
     * @returns the current election as {@link AuthorityElection}
     * If the current election is not an {@link AuthorityElection}, it fetches the missing information and creates a new {@link AuthorityElection}
     */
  getAuthorityElection: () => AuthorityElection
}
