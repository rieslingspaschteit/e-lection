package pse.election.backendserver.core.state.handler;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import pse.election.backendserver.core.state.ElectionState;
import pse.election.backendserver.entity.Election;
import pse.election.backendserver.payload.error.exception.IllegalStateOperationException;

/**
 * Manages the State of an Election and initialises new election States after a successful State
 * switch.
 */
@Service
public class ElectionStateHandler {

  /**
   * Currently chosen Strategy to execute.
   */
  private StateStrategy currentStrategy;

  private final StrategyFactory strategyFactory;

  @Lazy
  public ElectionStateHandler(StrategyFactory strategyFactory) {
    this.strategyFactory = strategyFactory;
  }


  /**
   * Decides which strategy for an Election Switch has to be picked. If the State of an Election
   * gets update it also initialises the new State.
   *
   * @param election This is the Election which gets updated.
   */
  public ElectionState testAndSet(Election election, ElectionState nextState) {
    ElectionState entryState = election.getState();
    setCurrentStrategy(entryState);
    ElectionState newState = currentStrategy.switchState(election, nextState);
    if (!entryState.equals(newState)) {
      election.setState(newState);
      setCurrentStrategy(election.getState());
      currentStrategy.initialiseState(election);
    }
    return election.getState();
  }

  /**
   * Selects the correct strategy for the ElectionState of an Election.
   *
   */
  private void setCurrentStrategy(ElectionState electionState) {
    switch (electionState) {
      case AUX_KEYS -> currentStrategy = strategyFactory.getKeyCeremonyPhaseOneStrategy();
      case EPKB -> currentStrategy = strategyFactory.getKeyCeremonyPhaseTwoStrategy();
      case KEYCEREMONY_FINISHED ->
          currentStrategy = strategyFactory.getKeyCeremonyFinishedStrategy();
      case OPEN -> currentStrategy = strategyFactory.getOpenElectionStrategy();
      case P_DECRYPTION -> currentStrategy = strategyFactory.getDecryptionPhaseOneStrategy();
      case PP_DECRYPTION -> currentStrategy = strategyFactory.getDecryptionPhaseTwoStrategy();
      case DONE -> currentStrategy = strategyFactory.getDoneStrategy();
      default -> throw new IllegalStateOperationException(
          "The Election is in no State in which a state switch would be possible"
      );
    }
  }

}
