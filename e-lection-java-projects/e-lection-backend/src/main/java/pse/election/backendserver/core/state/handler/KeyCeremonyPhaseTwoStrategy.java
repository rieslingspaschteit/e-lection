package pse.election.backendserver.core.state.handler;

import java.util.List;
import pse.election.backendserver.core.bot.BotFacade;
import pse.election.backendserver.core.service.TrusteeService;
import pse.election.backendserver.core.state.ElectionState;
import pse.election.backendserver.entity.Election;
import pse.election.backendserver.entity.Trustee;
import pse.election.backendserver.payload.error.exception.IllegalStateSwitchOperation;

/**
 * Election State switch and initialise Strategy for the second Phase of the Key Ceremony phase
 * two.
 */
public class KeyCeremonyPhaseTwoStrategy implements StateStrategy {

  private static final String NO_TRUSTEES_ERROR_MESSAGE = "The number of trustees cannot be 0";

  private final TrusteeService trusteeService;
  private final BotFacade botFacade;

  /**
   * Constructor of new KeyCeremonyPhaseTwoStrategy.
   * */
  public KeyCeremonyPhaseTwoStrategy(TrusteeService trusteeService, BotFacade botFacade) {
    this.trusteeService = trusteeService;
    this.botFacade = botFacade;
  }

  /**
   * Checks if the KeyCeremony Phase two is over or not. This happens when all coefficients and
   * partial BackUps of all trustees were collected. Switches to the KeyCeremonyFinishedState
   *
   * @param election The election that gets checked.
   * @return the new state of the election
   */
  @Override
  public ElectionState switchState(Election election, ElectionState nextState) {

    if (nextState != ElectionState.KEYCEREMONY_FINISHED) {
      throw new IllegalStateSwitchOperation(
          "State switch from " + election.getState().toString() + " to " + nextState.toString()
              + " is not allowed.");
    }

    List<Trustee> trusteeList = trusteeService.getAllTrustees(election.getElectionId());

    if (trusteeList.isEmpty()) {
      throw new IllegalStateSwitchOperation(NO_TRUSTEES_ERROR_MESSAGE);
    }

    for (Trustee trustee : trusteeList) {
      if (!trustee.isWaiting()) {
        return ElectionState.EPKB;
      }
    }
    return ElectionState.KEYCEREMONY_FINISHED;
  }

  /**
   * Initialises the second phase of an election. Checks if this Election operates with a Bot and if
   * so generates the necessary elements for a Bot in the second phase of a KeyCeremony
   *
   * @param election The Election which just switched to state two of the Key Ceremony.
   */
  @Override
  public void initialiseState(Election election) {
    trusteeService.updateIsWaitingTrustee(election.getElectionId());
    if (election.hasBot()) {
      botFacade.addElgamalKeysAndBackups(election);
    }
  }


}
