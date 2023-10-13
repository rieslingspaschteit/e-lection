import type { ElectionMeta } from '../election'
import { Election } from './election'

/**
 * This model class represents an role unspecific open election that can hold additional data
 * like the tracking-codes of the  submitted ballots.
 * This set of data is required when providing an overview about one specific election.
 *
 * @version 1.0
 */
export class OpenElection extends Election {
  /**
     * Constructs a new instance and should only be constructed from data fetched from the backend server.
     * @param election the Election this OpenElection shall extend.
     * @param _submittedBallots an array holding the tracking-codes of all submitted ballots of the election.
     * @param _hasVoted wether the user requesting the data has already voted,
     * undefined if the user is not a voter for this election.
     */
  public constructor (
    election: Election,
    private readonly _submittedBallots: readonly string[],
    private readonly _eligibleToVote?: boolean
  ) {
    super(
      election.electionId,
      election.electionMeta,
      election.electionState,
      election.fingerprint
    )
    console.log('create open election with eligible to vote = ', this.eligibleToVote)
  }

  public override cloneWithMeta (meta: ElectionMeta): Election {
    return new OpenElection(
      super.cloneWithMeta(meta), this.submittedBallots, this._eligibleToVote
    )
  }

  public get submittedBallots (): readonly string[] { return this._submittedBallots }
  public get eligibleToVote (): boolean | undefined { return this._eligibleToVote }
}
