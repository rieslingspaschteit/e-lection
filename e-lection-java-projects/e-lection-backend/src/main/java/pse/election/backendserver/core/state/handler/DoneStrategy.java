package pse.election.backendserver.core.state.handler;

import pse.election.backendserver.core.electionguard.DecryptionFacade;
import pse.election.backendserver.core.state.ElectionState;
import pse.election.backendserver.entity.Election;

/**
 * This class is the DoneStrategy handling the end of an election.
 *
 * @version 1.0
 * */
public class DoneStrategy implements StateStrategy {

  private final DecryptionFacade decryptionFacade;

  /**
   * Constructor of new DoneStrategy.
   * */
  public DoneStrategy(DecryptionFacade decryptionFacade) {
    this.decryptionFacade = decryptionFacade;
  }

  @Override

  public ElectionState switchState(Election election, ElectionState nextState) {
    return null;
  }

  @Override
  public void initialiseState(Election election) {
    decryptionFacade.computeLagrangeCoefficients(election);
    decryptionFacade.evaluateResult(election);
  }
}
