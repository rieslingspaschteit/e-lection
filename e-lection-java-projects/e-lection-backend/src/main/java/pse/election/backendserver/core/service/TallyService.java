package pse.election.backendserver.core.service;

import jakarta.persistence.EntityNotFoundException;

import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pse.election.backendserver.entity.Tally;
import pse.election.backendserver.payload.error.exception.IllegalStateOperationException;
import pse.election.backendserver.repository.TallyRepository;

/**
 * This class processes all the tally service functionalities.
 *
 * @version 1.0
 */
@Service
public class TallyService {

  private static final String TALLY_NOT_FOUND_ERROR_MSG = "Tally entity cannot be null.";

  @Autowired
  private TallyRepository tallyRepository;

  @Autowired
  private ElectionService electionService;

  /**
   * Saving a tally to the database.
   *
   * @param tally is the tally to be saved
   * @return saved tally
   * @throws IllegalArgumentException in case of tally entity being null
   */
  public Tally addTally(Tally tally) {
    if (tally == null) {
      throw new IllegalArgumentException(TALLY_NOT_FOUND_ERROR_MSG);
    }
    return tallyRepository.save(tally);
  }

  /**
   * Getter for all tallies of an election referenced by an id.
   *
   * @param electionId election identifier
   * @return collection of tallies
   * @throws EntityNotFoundException        in case the election could not be found
   * @throws IllegalStateOperationException in case election has an invalid state
   */
  public List<Tally> getAllTalliesOfElection(long electionId) {
    electionService.checkExistsElection(electionId);
    List<Tally> tallies = (List<Tally>) tallyRepository.findByElectionId(electionId);
    Collections.sort(tallies);
    return tallies;
  }

  /**
   * Receives a specific tally to an election.
   *
   * @param electionId   election identifier
   * @param contestIndex index of contest
   * @param optionIndex  index of option
   * @return desired tally entity
   */
  public Tally getSpecificTally(long electionId, int contestIndex, int optionIndex) {
    electionService.checkExistsElection(electionId);
    return tallyRepository.findByElectionIdAndContestIndexAndOptionIndex(electionId, contestIndex,
        optionIndex);
  }

}
