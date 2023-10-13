package pse.election.backendserver.core.state.handler;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import pse.election.backendserver.core.electionguard.HashFacade;
import pse.election.backendserver.core.state.ElectionState;
import pse.election.backendserver.entity.Election;
import pse.election.backendserver.payload.error.exception.IllegalStateSwitchOperation;

/**
 * Election State switch and initialise Strategy for the second Phase of the Key Ceremony. This
 * Strategy can also be used as a kind of precondition before letting somebody vote to verify that
 * the end date of the election hasn't been reached. If so the state switches to the Decryption
 * phase one.
 */
public class OpenElectionStrategy implements StateStrategy {

  private static final String ENDATE_ALREADY_PASSED_ERROR_MSG = "The end date of the election has already been passed.";
  private final HashFacade hashFacade;

  public OpenElectionStrategy(HashFacade hashFacade) {
    this.hashFacade = hashFacade;
  }

  /**
   * Checks if the end date of an election is reached and if so switches the state. Switches to the
   * DecryptionPhaseOne state.
   *
   * @param election The election that gets checked.
   * @return the new state of the election
   */
  @Override
  public ElectionState switchState(Election election, ElectionState nextState) {
    Date now = Date.from(Instant.now());

    if (now.after(election.getEndTime())) {
      return ElectionState.P_DECRYPTION;
    }
    return ElectionState.OPEN;
  }

  /**
   * Initialises an election that just switched into the open State.
   *
   * @param election The Election that gets initialised.
   */
  @Override
  public void initialiseState(Election election) {
    //Truncate time to millisecond to avoid unexpected truncation in database and
    //Since milliseconds is the lowest time unit that is relevant for the fingerprint
    Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    if (election.getEndTime().toInstant().isBefore(now)) {
      throw new IllegalStateSwitchOperation(ENDATE_ALREADY_PASSED_ERROR_MSG);
    }
    election.setStartTime(Date.from(now));
    election.setFingerprint(hashFacade.generateElectionFingerprint(election));
  }
}
