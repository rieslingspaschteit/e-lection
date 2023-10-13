package pse.election.backendserver.core.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pse.election.backendserver.entity.Election;
import pse.election.backendserver.entity.Voter;
import pse.election.backendserver.payload.error.exception.EntityNotFoundException;
import pse.election.backendserver.payload.error.exception.InvalidConfigurationException;
import pse.election.backendserver.repository.VoterRepository;

/**
 * This class processes the voter service functionalities.
 *
 * @version 1.0
 */
@Service
public class VoterService {

  private static final String EMAIL_REGEX = "^[a-zA-Z0-9]+(?:\\.[a-zA-Z0-9]+)*@[a-zA-Z0-9]+(?:\\.[a-zA-Z0-9]+)*$";
  @Autowired
  private VoterRepository voterRepository;
  @Autowired
  private ElectionService electionService;

  /**
   * Saves as new voter.
   *
   * @param voter is the new voter to be saved in database
   * @return saved voter
   * @throws InvalidConfigurationException in case email has incorrect format
   */
  public Voter addVoter(Voter voter) {
    if (!voter.getEmail().matches(EMAIL_REGEX)) {
      throw new InvalidConfigurationException("Invalid email format provided.");
    }

    return voterRepository.save(voter);
  }

  /**
   * Adding multiple new voters to the database.
   *
   * @param voters is a list containing new voters to be added
   */
  public void addVoters(List<Voter> voters) {
    for (Voter voter : voters) {
      if (voter == null || !voter.getEmail().matches(EMAIL_REGEX)) {
        throw new InvalidConfigurationException("Invalid email format provided.");
      }
    }
    voterRepository.saveAll(voters);
  }

  /**
   * Getter for all voters of an election.
   *
   * @param electionId is the id referencing the election
   * @return collection of voters
   * @throws EntityNotFoundException in case the election could not be found or no voters to this
   *                                 election
   */
  public List<Voter> getAllVoters(long electionId) {
    electionService.checkExistsElection(electionId);

    List<Voter> votersOfElection = (List<Voter>) voterRepository.findByElectionId(electionId);
    if (votersOfElection.isEmpty()) {
      throw new EntityNotFoundException("There are no voters to this election.");
    }

    return (List<Voter>) voterRepository.findByElectionId(electionId);
  }

  /**
   * Getter for all elections a voter is assigned to.
   *
   * @param email is the email address of a voter
   * @return collection of voters
   * @throws IllegalArgumentException in case email is null
   */
  public List<Election> getAllElectionsOfVoter(String email) {
    if (email == null) {
      throw new IllegalArgumentException("Invalid email given.");
    }
    List<Election> electionsOfVoter = new ArrayList<>();
    List<Election> elections = electionService.getAllElections();

    for (Election election : elections) {
      if (voterRepository.findByEmailAndElectionId(email, election.getElectionId()) == null) {
        continue;
      }
      electionsOfVoter.add(election);
    }
    return electionsOfVoter;
  }

  /**
   * Getter for a voter referenced by his email assigned to an election referenced by an id.
   *
   * @param email      is the email of the voter
   * @param electionId is the id of the election
   * @return voter assigned to the election
   * @throws EntityNotFoundException  in case the voter could not be found or the election could not
   *                                  be found
   * @throws IllegalArgumentException in case email is null
   */
  public Voter getVoter(String email, long electionId) {
    electionService.checkExistsElection(electionId);

    if (email == null) {
      throw new IllegalArgumentException();
    }
    Voter desiredVoter = voterRepository.findByEmailAndElectionId(email, electionId);
    if (desiredVoter == null) {
      throw new EntityNotFoundException("No such voter with given email and election to be found.");
    }

    return desiredVoter;
  }

  /**
   * Returns if a voter has submitted a ballot to an election.
   *
   * @param email      is the email of the voter
   * @param electionId election identifier
   * @return true, if election contains voter
   * @throws IllegalArgumentException in case email is null
   * @throws EntityNotFoundException  in case no election or no voter was found
   */
  public boolean hasVoted(String email, long electionId) {
    electionService.checkExistsElection(electionId);
    if (email == null) {
      throw new IllegalArgumentException();
    }

    Voter desiredVoter = voterRepository.findByEmailAndElectionId(email, electionId);
    if (desiredVoter == null) {
      throw new EntityNotFoundException("No such voter with given email and election to be found.");
    }

    return desiredVoter.isHasVoted();
  }

  /**
   * Returns if a voter with an email address exists in the database.
   *
   * @param email is the email to look for
   * @return true in case the voter exists
   * @throws IllegalArgumentException in case email is null
   */
  public boolean existsByEmail(String email) {
    if (email == null) {
      throw new IllegalArgumentException();
    }
    return voterRepository.existsByEmail(email);
  }

  /**
   * Checks whether an election contains a specific voter.
   *
   * @param email      of voter
   * @param electionId election identifier
   * @return true, if voter is in election
   */
  public boolean isVoterInElection(String email, long electionId) {
    return this.voterRepository.existsByEmailAndElectionId(email, electionId);
  }
}
