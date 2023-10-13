package pse.election.backendserver.core.state.handler;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pse.election.backendserver.core.bot.BotFacade;
import pse.election.backendserver.core.electionguard.DecryptionFacade;
import pse.election.backendserver.core.service.BallotService;
import pse.election.backendserver.core.service.DecryptionService;
import pse.election.backendserver.core.service.ElectionService;
import pse.election.backendserver.core.service.TrusteeService;
import pse.election.backendserver.core.state.ElectionState;
import pse.election.backendserver.entity.Election;
import pse.election.backendserver.entity.Trustee;
import pse.election.backendserver.payload.error.exception.IllegalStateSwitchOperation;
import pse.election.backendserver.repository.PartialDecryptionRepository;

/**
 * Strategy for initialising the first phase of the decryption and switching to the next State of
 * the Election.
 */
@Component
public class DecryptionPhaseOneStrategy implements StateStrategy {

  private static final String TRUSTEES_MISSING_ERROR_MSG
      = "There has to be at least one trustee in the election.";

  private final TrusteeService trusteeService;

  private final DecryptionFacade decryptionFacade;
  private final BotFacade botFacade;

  /**
   * Constructor of new DecryptionPhaseOneStrategy.
   * */
  public DecryptionPhaseOneStrategy(TrusteeService trusteeService, DecryptionFacade decryptionFacade,
                                    BotFacade botFacade) {
    this.trusteeService = trusteeService;
    this.decryptionFacade = decryptionFacade;
    this.botFacade = botFacade;
  }

  /**
   * Checks if a switch in the next phase of the decryption would be possible. The requirement for
   * this switch is that at least threshold trustees uploaded their encryption. Switches to
   * DecryptionPhaseTwoStrategy.
   *
   * @param election The election that gets checked.
   * @return the new ElectionState of the election
   */
  @Override
  public ElectionState switchState(Election election, ElectionState nextState) {

    if (nextState == ElectionState.DONE) {
      return switchToDone(election);
    } else if (nextState == ElectionState.PP_DECRYPTION) {
      return switchToPhaseTwo(election);
    } else {
      throw new IllegalStateSwitchOperation(
          "State switch from " + election.getState().toString() + " to " + nextState.toString()
              + " is not allowed.");
    }
  }

  private ElectionState switchToPhaseTwo(Election election) {

    List<Trustee> allTrusteesOfElection = trusteeService.getAllTrustees(election.getElectionId());

    if (allTrusteesOfElection.isEmpty()) {
      throw new IllegalStateSwitchOperation(TRUSTEES_MISSING_ERROR_MSG);
    }

    int uploadedPartialDecryptionCount = getUploadedPartialDecryptionCount(allTrusteesOfElection);

    if (uploadedPartialDecryptionCount >= election.getTrusteeThreshold()) {
      return ElectionState.PP_DECRYPTION;
    }

    return ElectionState.P_DECRYPTION;
  }

  private ElectionState switchToDone(Election election) {
    List<Trustee> allTrusteesOfElection = trusteeService.getAllTrustees(election.getElectionId());

    if (allTrusteesOfElection.isEmpty()) {
      throw new IllegalStateSwitchOperation(TRUSTEES_MISSING_ERROR_MSG);
    }

    int uploadedPartialDecryptionCount = getUploadedPartialDecryptionCount(allTrusteesOfElection);

    if (uploadedPartialDecryptionCount >= allTrusteesOfElection.size()) {
      return ElectionState.DONE;
    }

    return ElectionState.P_DECRYPTION;
  }

  private int getUploadedPartialDecryptionCount(List<Trustee> trusteeList) {

    int uploadedPartialDecryptionCount = 0;
    for (Trustee trustee : trusteeList) {
      if (trustee.isAvailable()) {
        uploadedPartialDecryptionCount++;
      }
    }

    return uploadedPartialDecryptionCount;
  }

  /**
   * Initialises the state of the decryption when it enters the decryption phase one. This method
   * calls IDecryption for the homomorph addition of the encryption, and if a bot trustee exists, It
   * also decrypts his part of the election.
   *
   * @param election The Election that gets initialised.
   */
  @Override
  public void initialiseState(Election election) {
    decryptionFacade.computeLagrangeCoefficients(election);
    decryptionFacade.combineOptionsEncryptedToTallies(election);

    if (election.hasBot()) {
      botFacade.partialDecryption(election);
    }
  }
}
