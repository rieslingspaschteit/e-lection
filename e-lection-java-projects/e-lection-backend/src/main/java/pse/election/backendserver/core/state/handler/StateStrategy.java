package pse.election.backendserver.core.state.handler;

import pse.election.backendserver.core.state.ElectionState;
import pse.election.backendserver.entity.Election;

/**
 * Interface which provides StateStrategies with the methods to switch and initialise States of an
 * Election.
 */
public interface StateStrategy {

  /**
   * Method which checks if the State of the Election is correct or if a switch is necessary or
   * sometime even possible.
   *
   * @param election The election that gets checked.
   * @return the new ElectionState of an election
   */
  ElectionState switchState(Election election, ElectionState nextState);

  /**
   * Gets called after the state of an election got switched. Initialises this new state by
   * generating necessary election attributes and bot information.
   *
   * @param election The Election that gets initialised.
   */
  void initialiseState(Election election);
}
