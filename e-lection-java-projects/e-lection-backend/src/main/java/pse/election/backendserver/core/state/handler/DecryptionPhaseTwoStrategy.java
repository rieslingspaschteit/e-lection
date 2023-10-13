package pse.election.backendserver.core.state.handler;

import java.util.List;
import org.springframework.stereotype.Component;
import pse.election.backendserver.core.bot.BotFacade;
import pse.election.backendserver.core.service.DecryptionService;
import pse.election.backendserver.core.service.TrusteeService;
import pse.election.backendserver.core.state.ElectionState;
import pse.election.backendserver.entity.Election;
import pse.election.backendserver.entity.Trustee;
import pse.election.backendserver.payload.error.exception.IllegalStateSwitchOperation;


/**
 * Strategy for initialising the second phase of the decryption and switching to the next State of
 * the Election.
 */
@Component
public class DecryptionPhaseTwoStrategy implements StateStrategy {

  private final TrusteeService trusteeService;

  private final DecryptionService decryptionService;

  private final BotFacade botFacade;

  /**
   * Constructor of new DecryptionPhaseTwoStrategy.
   * */
  public DecryptionPhaseTwoStrategy(TrusteeService trusteeService,
      DecryptionService decryptionService, BotFacade botFacade) {
    this.trusteeService = trusteeService;
    this.decryptionService = decryptionService;
    this.botFacade = botFacade;
  }

  /**
   * Checks if the Decryption Phase two is over or not. This happens when all
   * PartialPartialDecryption of each trustee got submitted. Switches to the Finished state of an
   * election.
   *
   * @param election The election that gets checked.
   * @return the new ElectionState of an Election
   */
  @Override
  public ElectionState switchState(Election election, ElectionState nextState) {

    if (nextState != ElectionState.DONE) {
      throw new IllegalStateSwitchOperation(
          "State switch from " + election.getState().toString() + " to " + nextState.toString()
              + " is not allowed.");
    }

    List<Trustee> trusteeList = trusteeService.getAllTrustees(election.getElectionId());

    if (trusteeList.isEmpty()) {
      throw new IllegalStateSwitchOperation("There must be at least one trustee in the election.");
    }

    for (Trustee trustee : trusteeList) {
      if (trustee.isAvailable()) {
        if (decryptionService.getAllPartialPartialDecryptionOfTrustee(trustee.getTrusteeId())
            .isEmpty()) {
          return ElectionState.PP_DECRYPTION;
        }
      }
    }

    return ElectionState.DONE;
  }

  /**
   * Initialises this State of the Election. By doing so generating the trustee bot
   * PartialPartialDecryptions if one exists.
   *
   * @param election The Election that gets initialised.
   */
  @Override
  public void initialiseState(Election election) {
    for (Trustee trustee : trusteeService.getAllTrustees(election.getElectionId())) {
      trustee.setWaiting(decryptionService.getAllPartialDecryptionByTrustee(trustee.getTrusteeId()).isEmpty());
    }
    if (election.hasBot()) {
      botFacade.partialPartialDecryption(election);
    }
  }


}
