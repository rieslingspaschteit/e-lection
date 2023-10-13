import type { ElectionMeta } from '../election'
import { Election } from './election'

/**
 * This specific election model class holds additional data that are relevant for a finished, (decrypted) election.
 * Therefore it provides the final result and also the electionguard compatible election-record.
 *
 * @see [electionguard](https://www.electionguard.vote/develop/Election_Record/)
 */
export class FinishedElection extends Election {
  /**
     * Constructs a new instance and should only be constructed from data fetched from the backend server.
     * @param _result a map, mapping each question to an array of ints, representing the result of the election.
     */
  public constructor (
    election: Election,
    private readonly _result: Map<number, number[]>,
    private readonly _electionRecord?: Blob
  ) {
    super(
      election.electionId,
      election.electionMeta,
      election.electionState,
      election.fingerprint
    )
    // FIXME : figure out constraints to check
  }

  /**
     * Creates a new FinishedElection containing all information in the ccurret FinishedElection as well as the provided
     * Election Record
     * @param electionRecord an ElectionRecord that can be used to verify the election independantly
     */
  public cloneWithElectionRecord (electionRecord: Blob): FinishedElection {
    return new FinishedElection(
      this.cloneWithMeta(this.electionMeta),
      this.result,
      electionRecord
    )
  }

  public override cloneWithMeta (meta: ElectionMeta): Election {
    return new FinishedElection(
      super.cloneWithMeta(meta), this.result, this.electionRecord
    )
  }

  public get result (): Map<number, number[]> { return this._result }
  public get electionRecord (): Blob | undefined { return this._electionRecord }
}
