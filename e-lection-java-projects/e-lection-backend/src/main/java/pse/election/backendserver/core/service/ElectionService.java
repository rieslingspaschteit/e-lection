package pse.election.backendserver.core.service;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import pse.election.backendserver.core.state.ElectionState;
import pse.election.backendserver.core.state.handler.ElectionStateHandler;
import pse.election.backendserver.entity.Contest;
import pse.election.backendserver.entity.Election;
import pse.election.backendserver.payload.error.exception.EntityNotFoundException;
import pse.election.backendserver.payload.error.exception.IdentityConflictException;
import pse.election.backendserver.payload.error.exception.IllegalElectionStateSwitchException;
import pse.election.backendserver.payload.error.exception.InvalidConfigurationException;
import pse.election.backendserver.repository.ContestRepository;
import pse.election.backendserver.repository.ElectionRepository;

/**
 * This class processes the election service responsibilities, such as saving, maintaining and
 * retrieving election entities.
 *
 * @version 1.0
 */
@Service
public class ElectionService {

  private static final Logger logger = LogManager.getLogger(ElectionService.class);
  private static final String INVALID_CONFIG_ERROR_MESSAGE
      = "Invalid election information provided";
  private static final String DESIRED_ELECTION_TITLE_HAS_ALREADY_BEEN_TAKEN
      = "Desired election title has already been taken";
  private static final String ELECTION_ID_NOT_FOUND_ERROR_MESSAGE
      = "No corresponding election with given election id was found.";
  private static final String INVALID_MIN_MAX_ERROR_MESSAGE
      = "Maximum amount cannot lower or equals than 0.";
  private static final String INVALID_OPTION_AMOUNT_ERROR_MESSAGE
      = "There cannot be contests without options.";
  private static final String ELECTION_NOT_FOUND_ERROR_MESSAGE = "Election could not be found.";
  private static final String NOT_ENOUGH_CONTESTS_ERROR_MESSAGE
      = "The election must have at least one contest.";

  private final ElectionRepository electionRepository;

  private final ElectionStateHandler electionStateHandler;

  private final ContestRepository contestRepository;

  /**
   * Constructor of new ElectionService.
   * */
  @Lazy
  public ElectionService(ElectionRepository electionRepository,
      ElectionStateHandler electionStateHandler, ContestRepository contestRepository) {
    this.electionRepository = electionRepository;
    this.electionStateHandler = electionStateHandler;
    this.contestRepository = contestRepository;
  }

  /**
   * Saving a newly created election into the election table. Before saving, the election
   * configuration will be checked for validity.
   *
   * @param election is the new created election from the controller
   * @return saved election
   * @throws InvalidConfigurationException in case the created election has an invalid
   *                                       configuration
   * @throws IdentityConflictException     in case the election title is already a title of another
   *                                       existing election
   */
  public Election initialSave(Election election) {
    if (election == null || election.getDescription() == null || election.getTitle() == null
        || election.getAuthorityEmail() == null) {
      throw new InvalidConfigurationException(INVALID_CONFIG_ERROR_MESSAGE);
    }

    // Checks for unique election title
    if (electionRepository.existsByTitle(election.getTitle())) {
      throw new IdentityConflictException(DESIRED_ELECTION_TITLE_HAS_ALREADY_BEEN_TAKEN);
    }

    Election savedElection = electionRepository.save(election);
    logger.info("Added election with title: " + savedElection.getTitle() + " (id:"
        + savedElection.getElectionId() + ")");
    return savedElection;
  }

  /**
   * Adding contest information to an election. This is performed when a new election has been
   * created.
   *
   * @param contests contest of election
   * @return collection of all saved contests
   * @throws InvalidConfigurationException in case of invalid contest configurations
   * @throws EntityNotFoundException       in case desired election does not exist
   */
  public List<Contest> addContest(List<Contest> contests) {
    if (contests.size() < 1) {
      throw new InvalidConfigurationException(NOT_ENOUGH_CONTESTS_ERROR_MESSAGE);
    }

    for (Contest contest : contests) {
      checkExistsElection(contest.getElectionId());
      if (contest.getMax() <= 0) {
        throw new InvalidConfigurationException(INVALID_MIN_MAX_ERROR_MESSAGE);
      }
      if (contest.getOptions() == null || contest.getOptions().size() < 1) {
        throw new InvalidConfigurationException(INVALID_OPTION_AMOUNT_ERROR_MESSAGE);
      }
    }
    return (List<Contest>) contestRepository.saveAll(contests);
  }

  /**
   * Changing the state of an election referenced by an id.
   *
   * @param electionId    is the id of an election
   * @param electionState is the state the election should be set to
   * @return the election that has been updated
   * @throws IllegalElectionStateSwitchException in case the state switch violated any set rules
   */
  public Election tryUpdateState(long electionId, ElectionState electionState) {
    checkExistsElection(electionId);

    Election desiredElection = electionRepository.findByElectionId(electionId);
    electionStateHandler.testAndSet(desiredElection, electionState);

    return electionRepository.save(desiredElection);
  }

  /**
   * Updating the latest tracking code of an election referenced by an id. The latest tracking code
   * is needed to calculate the tracking code of the next ballot.
   *
   * @param electionId   is the id of the election the tracking code belongs to
   * @param trackingCode is the latest tracking code
   * @throws EntityNotFoundException  in case no ballot with the parsed tracking code could be found
   *                                  to the election or the election could not be found
   * @throws IllegalArgumentException in case the trackingcode is null
   */
  public void updateLatestTrackingCode(long electionId, String trackingCode) {
    if (trackingCode == null) {
      throw new IllegalArgumentException();
    }
    checkExistsElection(electionId);

    Election desiredElection = electionRepository.findByElectionId(electionId);
    desiredElection.setTrackingCodeLatest(trackingCode);
    electionRepository.save(desiredElection);
  }

  /**
   * Setter for the result of an election.
   *
   * @param electionId     is the id of the election
   * @param resultElection is the result formatted into a string
   * @throws EntityNotFoundException in case the entity could not be found
   */
  public void setResult(long electionId, Map<Integer, Integer[]> resultElection) {
    checkExistsElection(electionId);

    Election election = electionRepository.findByElectionId(electionId);
    StringBuilder decryptedResult = new StringBuilder();

    for (Map.Entry<Integer, Integer[]> electionResult : resultElection.entrySet()) {
      for (int optionIndex = 0; optionIndex < electionResult.getValue().length; optionIndex++) {
        Integer decryptedOption = electionResult.getValue()[optionIndex];
        decryptedResult.append(decryptedOption);
        decryptedResult.append(";");
      }
      decryptedResult.deleteCharAt(decryptedResult.length() - 1);
      decryptedResult.append("|");
    }
    decryptedResult.deleteCharAt(decryptedResult.length() - 1);
    election.setCleartextResult(decryptedResult.toString());
    electionRepository.save(election);
  }

  /**
   * Getter for the state of an election referenced by an id.
   *
   * @param electionId is the id of the election
   * @return the current state of the election
   * @throws EntityNotFoundException in case the election could not be found
   */
  public ElectionState getState(long electionId) {
    checkExistsElection(electionId);

    Election desiredElection = electionRepository.findByElectionId(electionId);
    return desiredElection.getState();
  }

  /**
   * Getter for the election referenced by an id.
   *
   * @param electionId is the id of the election
   * @return the election referenced by the id
   * @throws EntityNotFoundException in case the election could not be found
   */
  public Election getElection(long electionId) {
    checkExistsElection(electionId);
    Election election = electionRepository.findByElectionId(electionId);
    if (election.getState() == ElectionState.OPEN && Instant.now()
        .isAfter(election.getEndTime().toInstant())) {
      electionStateHandler.testAndSet(election, ElectionState.P_DECRYPTION);
      electionStateHandler.testAndSet(election, ElectionState.DONE);
      electionRepository.save(election);
    }
    return electionRepository.findByElectionId(electionId);
  }

  /**
   * Getter for an election referenced by the fingerprint.
   *
   * @param fingerPrint is the fingerprint of the election
   * @return the election with the fingerprint
   * @throws EntityNotFoundException  in case the election could not be found
   * @throws IllegalArgumentException in case the fingerprint is null
   */
  public Election getElection(String fingerPrint) {
    if (fingerPrint == null) {
      throw new IllegalArgumentException(ELECTION_NOT_FOUND_ERROR_MESSAGE);
    }

    if (!electionRepository.existsByFingerprint(fingerPrint)) {
      throw new EntityNotFoundException(ELECTION_ID_NOT_FOUND_ERROR_MESSAGE);
    }

    return electionRepository.findByFingerprint(fingerPrint);
  }

  /**
   * Getter for all the elections in the system.
   *
   * @return collection of elections
   */
  public List<Election> getAllElections() {
    return (List<Election>) electionRepository.findAll();
  }

  /**
   * Getter for the latest tracking code of an election.
   *
   * @param electionId is the id referencing the election
   * @return newest latest tracking code of the election
   * @throws EntityNotFoundException in case the election could not be found
   */
  public String getLatestTrackingCode(long electionId) {
    checkExistsElection(electionId);

    return electionRepository.findByElectionId(electionId).getTrackingCodeLatest();
  }

  /**
   * Getter for all contests of an election.
   *
   * @param electionId election identifier
   * @return sorted list containing all contests of an election
   * @throws EntityNotFoundException in case the election could not be found
   */
  public List<Contest> getAllContestsOfElection(long electionId) {
    checkExistsElection(electionId);

    List<Contest> contest = (List<Contest>) contestRepository.findByElectionId(electionId);
    Collections.sort(contest);
    return contest;
  }

  /**
   * Getter for all elections created by an authority.
   *
   * @param email is the email of the authority
   * @return list containing all elections created by the authority
   * @throws IllegalArgumentException in case email is null
   */
  public List<Election> getAllElectionsOfAuthority(String email) {
    if (email == null) {
      throw new IllegalArgumentException();
    }

    return (List<Election>) electionRepository.findByAuthorityEmail(email);
  }

  /**
   * Checks whether the election has a bot trustee.
   *
   * @param electionId election identifier
   * @return true, if election has a trustee bot
   * @throws EntityNotFoundException in case no election with election identifier found
   */
  public boolean hasBot(long electionId) {
    checkExistsElection(electionId);
    return electionRepository.findByElectionId(electionId).hasBot();
  }

  /**
   * Retrieves the decrypted election result.
   *
   * @param electionId election identifier
   * @return mapped result
   */
  public Map<Integer, Integer[]> getDecryptedResult(long electionId) {
    Election election = getElection(electionId);
    if (election.getCleartextResult() == null) {
      throw new InvalidConfigurationException("Election does not yet have a result.");
    }

    String electionResult = election.getCleartextResult();
    Map<Integer, Integer[]> result = new HashMap<>();

    String[] contestResult = electionResult.split("\\|");
    for (int i = 0; i < contestResult.length; i++) {
      String[] optionResult = contestResult[i].split(";");
      Integer[] optionResultInt = Stream.of(optionResult).mapToInt(Integer::parseInt).boxed()
          .toArray(Integer[]::new);
      result.put(i, optionResultInt);
    }
    return result;
  }

  /**
   * Checks whether the database contains an election with the given election identifier.
   *
   * @param electionId election identifier
   * @throws EntityNotFoundException in case no election was found
   */
  public void checkExistsElection(long electionId) {
    if (!electionRepository.existsByElectionId(electionId)) {
      throw new EntityNotFoundException(ELECTION_ID_NOT_FOUND_ERROR_MESSAGE);
    }
  }

}
