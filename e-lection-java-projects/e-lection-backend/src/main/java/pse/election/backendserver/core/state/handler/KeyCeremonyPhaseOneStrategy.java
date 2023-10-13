package pse.election.backendserver.core.state.handler;

import java.util.List;
import pse.election.backendserver.core.service.TrusteeService;
import pse.election.backendserver.core.state.ElectionState;
import pse.election.backendserver.entity.Election;
import pse.election.backendserver.entity.Trustee;
import pse.election.backendserver.payload.error.exception.IllegalStateSwitchOperation;

/**
 * Election State switch and initialise Strategy for the first Phase of the Key Ceremony.
 */
public class KeyCeremonyPhaseOneStrategy implements StateStrategy {

  private final TrusteeService trusteeService;

  /**
   * Constructor of new KeyCeremonyPhaseOneStrategy.
   * */
  public KeyCeremonyPhaseOneStrategy(TrusteeService trusteeService) {
    this.trusteeService = trusteeService;
  }

  /**
   * Checks if the KeyCeremony Phase one is over or not. This happens when all Aux Key of the
   * trustees were collected. Switches to KeyCeremony Phase Two.
   *
   * @param election The election that gets checked.
   * @return returns the new state of the election
   */
  @Override
  public ElectionState switchState(Election election, ElectionState nextState) {

    if (nextState != ElectionState.EPKB) {
      return ElectionState.AUX_KEYS;
    }

    List<Trustee> trusteeList = trusteeService.getAllTrustees(election.getElectionId());

    if (trusteeList.isEmpty()) {
      throw new IllegalStateSwitchOperation(
          "There must be at least one trustee for the key ceremony.");
    }

    for (Trustee trustee : trusteeList) {
      if (trustee.getAuxkey() == null) {
        return ElectionState.AUX_KEYS;
      }
    }
    return ElectionState.EPKB;
  }


  /**
   * Initialises the first phase of the KeyCeremony. Due to the fact that any election which was
   * created by the frontend, is instantly in the key ceremony phase one for the backend. For the
   * backend, there is no need for a state which should represent the logical phase before the key
   * ceremony phase one.
   *
   * @param election The Election that gets initialised.
   */
  @Override
  public void initialiseState(Election election) {
  }
}
