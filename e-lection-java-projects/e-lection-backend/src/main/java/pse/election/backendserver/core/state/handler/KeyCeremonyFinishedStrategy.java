package pse.election.backendserver.core.state.handler;

import pse.election.backendserver.core.electionguard.KeyCeremonyFacade;
import pse.election.backendserver.core.service.TrusteeService;
import pse.election.backendserver.core.state.ElectionState;
import pse.election.backendserver.entity.Election;
import pse.election.backendserver.payload.error.exception.IllegalStateSwitchOperation;

/**
 * Election State switch and initialise Strategy for when the Key Ceremony is finished but the
 * Authority hasn't opened the Election.
 */
public class KeyCeremonyFinishedStrategy implements StateStrategy {

  private final KeyCeremonyFacade keyCeremonyFacade;
  private final TrusteeService trusteeService;

  /**
   * Constructor of new KeyCeremonyFinishedStrategy.
   * */
  public KeyCeremonyFinishedStrategy(KeyCeremonyFacade keyCeremonyFacade,
      TrusteeService trusteeService) {
    this.keyCeremonyFacade = keyCeremonyFacade;
    this.trusteeService = trusteeService;
  }

  /**
   * Checks if the Authority can open the Election and verifies if this is possible.
   *
   * @param election The election that gets checked.
   * @return the new ElectionState of the election
   */

  public ElectionState switchState(Election election, ElectionState nextState) {

    if (nextState != ElectionState.OPEN) {
      throw new IllegalStateSwitchOperation(
          "State switch from " + election.getState().toString() + " to " + nextState.toString()
              + " is not allowed.");
    }

    return ElectionState.OPEN;
  }

  /**
   * Initialises the KeyCeremonyFinished state of an election. By doing so the JointElGamalKey gets
   * created by the KeyCeremonyFacade.
   *
   * @param election The Election that gets initialised.
   */
  @Override
  public void initialiseState(Election election) {
    trusteeService.updateIsWaitingTrustee(election.getElectionId());
    election.setPublicKey(keyCeremonyFacade.combineKeys(election).toString(16));
  }
}
