/**
 * This module provides constants describing all states a model class instance can have.
 *
 * @module
 */

/**
 * Constants describing key-ceremony specific states.
 */
enum KeyCeremonyState {

  /**
     * The trustees can upload their public aux-keys
     */
  AUX_KEYS = 'AUX_KEYS',

  /**
     * The trustees can download the aux-keys for each trustee and upload their
     * elGamal public and backups for each trustee.
     */
  EPKB = 'EPKB',

  /**
     * Each trustee has uploaded his keys. The public elGamal key for the election can now be constructed.
     * The authority is allowed to open the election for submissions.
     */
  FINISHED = 'FINISHED'
}

/**
 * Constants describing decryption specific states.
 */
enum DecryptionState {

  /**
     * The trustees have to partial-decrypt the result and spoiled ballots with their private elGamal key.
     */
  P_DECRYPTION = 'P_DECRYPTION',

  /**
     * The trustees have to partial- partial-decrypt the result and spoiled ballots for each missing trustee
     * with their backup they received for this trustee.
     */
  PP_DECRYPTION = 'PP_DECRYPTION'

}

/**
 * Constants describing the global state of an election.
 * This class uses the enum pattern.
 */
class ElectionState {
  name: string = ''
  subStates = ['']
  // FIXME: we probably do not need this state? after creation it should by instantly in key-ceremony state... ?
  // should stay here for now since it is in our diagrams ;)
  /**
     * The election was successfully configured by the authority and the key-ceremony hasn't started yet.
     */
  public static readonly CREATED = {
    name: 'CREATED',
    subStates: []
  }

  /**
     * The trustees can exchange their aux-keys and backups in order to enable threshold encryption
     * and together generate the public election elGamal key.
     */
  public static readonly KEY_CEREMONY = {
    name: 'KEY_CEREMONY',
    subStates: [
      KeyCeremonyState.AUX_KEYS as string,
      KeyCeremonyState.EPKB as string,
      KeyCeremonyState.FINISHED as string
    ]
  }

  /**
     * The authority has opened the election for submitting an voters can submit their filled in ballots.
     */
  public static readonly OPEN = {
    name: 'OPEN',
    subStates: []
  }

  /**
     * The end date of submission has been reached and the trustees now can decrypted the tally and spoiled ballots.
     */
  public static readonly DECRYPTION = {
    name: 'DECRYPTION',
    subStates: [
      DecryptionState.P_DECRYPTION as string,
      DecryptionState.PP_DECRYPTION as string
    ]
  }

  /**
     * The decrypted result is now available for all attendees of the election.
     * Also the electionguard record can be downloaded.
     */
  public static readonly FINISHED = {
    name: 'FINISHED',
    subStates: []
  }
}

export { ElectionState, KeyCeremonyState, DecryptionState }
