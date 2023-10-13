package pse.election.backendserver.core.service;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.Collections;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import pse.election.backendserver.core.electionguard.HashFacade;
import pse.election.backendserver.core.electionguard.VerificationFacade;
import pse.election.backendserver.core.state.ElectionState;
import pse.election.backendserver.core.state.handler.ElectionStateHandler;
import pse.election.backendserver.entity.Ballot;
import pse.election.backendserver.entity.Contest;
import pse.election.backendserver.entity.Election;
import pse.election.backendserver.entity.OptionEncrypted;
import pse.election.backendserver.entity.Voter;
import pse.election.backendserver.payload.dto.BallotProofDTO;
import pse.election.backendserver.payload.dto.ChaumPedersenProofDTO;
import pse.election.backendserver.payload.dto.ConstantChaumPedersenDTO;
import pse.election.backendserver.payload.dto.DisjunctiveChaumPedersenDTO;
import pse.election.backendserver.payload.dto.EncryptedOptionDTO;
import pse.election.backendserver.payload.error.exception.EntityNotFoundException;
import pse.election.backendserver.payload.error.exception.IllegalProofException;
import pse.election.backendserver.payload.error.exception.IllegalStateOperationException;
import pse.election.backendserver.repository.BallotRepository;
import pse.election.backendserver.repository.OptionEncryptedRepository;

/**
 * This class processes all the ballot service functionalities. It is responsible for processing
 * ballot information and communication with the repository.
 *
 * @version 1.0
 */
@Service
public class BallotService {

  private static final Logger logger = LogManager.getLogger(BallotService.class);
  private static final String BALLOT_NOT_FOUND_ERROR_MESSAGE = "No such ballot found.";
  private static final String NO_OPTION_TO_ELECTION_FOUND_ERROR_MESSAGE
      = "There are currently no encrypted options to this election.";
  private static final String INVALID_STATE_TO_DELIVER_BALLOT_ERROR_MESSAGE
      = "Election is in wrong state to deliver ballots.";
  private static final String INVALID_STATE_TO_SUBMIT_BALLOT_ERROR_MESSAGE
      = "Cannot submit a ballot in this election state.";
  private static final String VOTER_NOT_FOUND = "No voter could be found.";
  private static final String INVALID_BALLOT_ERROR_MSG
      = "The ballot does not contain valid proofs.";
  private static final String VOTER_ALREADY_VOTED_ERROR_MSG
      = "The voter has already submitted a ballot.";
  private static final String INVALID_DATE = "Encryption date must be after election start must not be in the future";
  private static final int HEXA_RADIX = 16;

  private final ElectionService electionService;

  private final ElectionStateHandler electionStateHandler;

  private final VerificationFacade verificationFacade;

  private final HashFacade hashFacade;

  private final VoterService voterService;

  private final BallotRepository ballotRepository;

  private final OptionEncryptedRepository optionEncryptedRepository;


  /**
   * Constructor of new BallotService.
   * */
  @Lazy
  public BallotService(ElectionService electionService, ElectionStateHandler electionStateHandler,
      BallotRepository ballotRepository, VerificationFacade verificationFacade,
      HashFacade hashFacade, OptionEncryptedRepository optionEncryptedRepository,
      VoterService voterService) {
    this.electionService = electionService;
    this.electionStateHandler = electionStateHandler;
    this.ballotRepository = ballotRepository;
    this.verificationFacade = verificationFacade;
    this.hashFacade = hashFacade;
    this.optionEncryptedRepository = optionEncryptedRepository;
    this.voterService = voterService;
  }

  /**
   * Adding a new Ballot into the ballot repository.
   *
   * @param ballot is the ballot to be added into the repository
   * @return added ballot
   */
  public Ballot addBallot(BallotProofDTO ballot, long electionId, String ballotIdForEncryption,
      String voterEmail) {
    Election desiredElection = electionService.getElection(electionId);

    //checking if the election end date has been reached
    if (desiredElection.getState() != ElectionState.OPEN
        || electionStateHandler.testAndSet(desiredElection, ElectionState.P_DECRYPTION)
        != ElectionState.OPEN) {
      throw new IllegalStateOperationException(INVALID_STATE_TO_DELIVER_BALLOT_ERROR_MESSAGE);
    }

    if (voterService.hasVoted(voterEmail, electionId)) {
      throw new IllegalStateOperationException(VOTER_ALREADY_VOTED_ERROR_MSG);
    }

    if (ballot.date() == null || ballot.date().after(Date.from(Instant.now()))
            || ballot.date().before(desiredElection.getStartTime())) {
      throw new IllegalProofException(INVALID_DATE);
    }

    if (!checkBallotFormat(ballot, electionId) || !verificationFacade.verifyBallot(ballot,
        desiredElection)) {
      throw new IllegalProofException(INVALID_BALLOT_ERROR_MSG);
    }

    Ballot desiredBallot = new Ballot(electionId, ballotIdForEncryption, ballot.deviceInformation(),
        ballot.date(), false);
    Ballot storedBallot = ballotRepository.save(desiredBallot);

    // Conversion from complex map to simple list of pad and data for tracking code hashing
    List<OptionEncrypted> optionEncryptedList = new ArrayList<>();
    int contestIndex = 0;
    for (EncryptedOptionDTO[] option : ballot.cipherText().values()) {
      for (int optionIndex = 0; optionIndex < option.length; optionIndex++) {
        DisjunctiveChaumPedersenDTO disjunctiveProof = ballot.individualProofs()
            .get(contestIndex)[optionIndex];
        ConstantChaumPedersenDTO constantProof = ballot.accumulatedProofs().get(contestIndex);
        String individualProof = parseIndividualProofToString(disjunctiveProof);
        String accumulatedProof = parseAccumulatedProofToString(constantProof);

        OptionEncrypted optionEncrypted = new OptionEncrypted(storedBallot.getBallotId(),
            electionId, optionIndex,
            contestIndex, option[optionIndex].pad().toString(HEXA_RADIX),
            option[optionIndex].data().toString(HEXA_RADIX), individualProof,
            accumulatedProof);
        optionEncryptedRepository.save(optionEncrypted);
        optionEncryptedList.add(optionEncrypted);
      }
      contestIndex++;
    }
    hashFacade.generateTrackingCode(optionEncryptedList, desiredElection, storedBallot);
    logger.info("Adding ballot with tracking code: " + storedBallot.getLatestTrackingCode());
    return ballotRepository.save(storedBallot);
  }

  /**
   * Saves the decrypted spoiled ballots results.
   *
   * @param resultSpoiledBallot decrypted spoiled ballots
   */
  public void saveDecryptedSpoiledBallot(Map<Long, Map<Integer, Integer[]>> resultSpoiledBallot) {
    for (Map.Entry<Long, Map<Integer, Integer[]>> ballot : resultSpoiledBallot.entrySet()) {
      long ballotId = ballot.getKey();
      for (Map.Entry<Integer, Integer[]> contest : ballot.getValue().entrySet()) {
        int contestIndex = contest.getKey();
        for (int optionIndex = 0; optionIndex < contest.getValue().length; optionIndex++) {
          int resultInt = contest.getValue()[optionIndex];
          OptionEncrypted option = optionEncryptedRepository
              .findByBallotIdAndContestIndexAndOptionIndex(ballotId, contestIndex, optionIndex);
          option.setSelectionMade(resultInt != 0);
          optionEncryptedRepository.save(option);
        }
      }
    }
  }

  /**
   * Getter for a ballot referenced by an identifier.
   *
   * @param ballotId is the identifier of the ballot
   * @return ballot entity
   * @throws EntityNotFoundException in case the ballot could not be found
   */
  public Ballot getBallot(long ballotId) {
    if (!ballotRepository.existsById(ballotId)) {
      throw new EntityNotFoundException(BALLOT_NOT_FOUND_ERROR_MESSAGE);
    }

    return ballotRepository.findByBallotId(ballotId);
  }

  /**
   * Getter for all the submitted ballots of an election referenced by an identifier.
   *
   * @param electionId is the id referencing an election
   * @return collection of submitted ballots
   * @throws EntityNotFoundException in case the election could not be found
   */
  public List<Ballot> getAllSubmittedBallotsOfElection(long electionId) {
    electionService.checkExistsElection(electionId);
    return (List<Ballot>) ballotRepository.findByElectionIdAndIsSubmitted(electionId, true);
  }

  /**
   * Getter for all the spoiled ballots of an election referenced by an identifier.
   *
   * @param electionId is the id referencing an election
   * @return collection of spoiled ballots
   * @throws EntityNotFoundException in case the election could not be found
   */
  public List<Ballot> getAllSpoiledBallotsOfElection(long electionId) {
    electionService.checkExistsElection(electionId);

    return (List<Ballot>) ballotRepository.findByElectionIdAndIsSubmitted(electionId, false);
  }


  /**
   * Getter for all encrypted options of a ballot referenced by an identifier.
   *
   * @param ballotId is the id referencing a ballot
   * @return sorted collection of encrypted options
   * @throws EntityNotFoundException in case the ballot could not be found
   */
  public List<OptionEncrypted> getAllOptionsEncryptedOfBallot(long ballotId) {
    if (!optionEncryptedRepository.existsById(ballotId)) {
      throw new EntityNotFoundException(NO_OPTION_TO_ELECTION_FOUND_ERROR_MESSAGE);
    }
    List<OptionEncrypted> optionsOfBallot = (List<OptionEncrypted>) optionEncryptedRepository.findByBallotId(ballotId);
    Collections.sort(optionsOfBallot);
    return optionsOfBallot;
  }

  /**
   * This method is used to convert a ballot from spoiled to submit.
   *
   * @param electionId   election identifier
   * @param trackingCode of the spoiled ballot that should be submitted
   * @throws EntityNotFoundException        in case the ballot or election could not be found
   * @throws IllegalStateOperationException in case election is in wrong state
   * @throws IllegalArgumentException       in case of tracking code being null
   */
  public Ballot convertSpoiledToSubmitted(String trackingCode, long electionId, String email) {
    Election desiredElection = electionService.getElection(electionId);

    //checking if the election is open
    if (desiredElection.getState() != ElectionState.OPEN
        || electionStateHandler.testAndSet(desiredElection, ElectionState.P_DECRYPTION)
        != ElectionState.OPEN) {
      throw new IllegalStateOperationException(INVALID_STATE_TO_SUBMIT_BALLOT_ERROR_MESSAGE);
    }

    if (trackingCode == null) {
      throw new IllegalArgumentException();
    }
    if (!ballotRepository.existsByLatestTrackingCode(trackingCode)
        || !ballotRepository.existsByElectionId(electionId)) {
      throw new EntityNotFoundException(BALLOT_NOT_FOUND_ERROR_MESSAGE);
    }
    if (!voterService.isVoterInElection(email, electionId)) {
      throw new EntityNotFoundException(VOTER_NOT_FOUND);
    }

    Voter voter = voterService.getVoter(email, electionId);
    if (voter.isHasVoted()) {
      throw new EntityNotFoundException("Voter already submitted a ballot.");
    }

    Ballot desiredBallot = ballotRepository.findByLatestTrackingCodeAndElectionId(trackingCode,
        electionId);
    desiredBallot.setSubmitted(true);
    voter.setHasVoted(true);

    return ballotRepository.save(desiredBallot);
  }

  /**
   * Returns all encrypted options to an election, contestIndex and optionIndex and also filtering
   * out the encrypted options of spoiled ballots.
   *
   * @param electionId   election identifier
   * @param contestIndex index of contest
   * @param optionIndex  index of option
   * @return collection of encrypted options
   */
  public List<OptionEncrypted> getAllOptionEncryptedOfContestAndOption(long electionId,
      int contestIndex, int optionIndex) {
    List<OptionEncrypted> options = (List<OptionEncrypted>) optionEncryptedRepository
        .findByElectionIdAndContestIndexAndOptionIndex(electionId, contestIndex, optionIndex);

    return options
        .stream()
        .filter(option -> ballotRepository.findByBallotId(option.getBallotId()).isSubmitted())
        .toList();
  }

  /**
   * Getter for a specified option of a ballot.
   *
   * @param ballotId is the id of the ballot
   * @param contestIndex is the index of the contest
   * @param optionIndex is the index of the option in the contest
   * @return option of specified ballot
   * */
  public OptionEncrypted getSpecificOptionOfBallot(long ballotId, int contestIndex,
      int optionIndex) {
    if (!ballotRepository.existsById(ballotId)) {
      throw new EntityNotFoundException("No such ballot was found.");
    }
    return optionEncryptedRepository.findByBallotIdAndContestIndexAndOptionIndex(ballotId,
        contestIndex, optionIndex);
  }

  /**
   * Checker if an option referenced by an id exists.
   *
   * @param optionId is the id of the option
   * */
  public void checkExistingOption(long optionId) {
    if (!optionEncryptedRepository.existsById(optionId)) {
      throw new EntityNotFoundException("No option was found with given identifier.");
    }
  }

  private boolean checkBallotFormat(BallotProofDTO ballot, long electionId) {
    Map<Integer, EncryptedOptionDTO[]> ciphertext = ballot.cipherText();
    Map<Integer, DisjunctiveChaumPedersenDTO[]> individualProofs = ballot.individualProofs();
    Map<Integer, ConstantChaumPedersenDTO> accumulatedProofs = ballot.accumulatedProofs();
    List<Contest> contests = electionService.getAllContestsOfElection(electionId);
    if (ciphertext.size() != contests.size() || accumulatedProofs.size() != contests.size()
            || individualProofs.size() != contests.size()) {
      return false;
    }
    for (Contest contest : electionService.getAllContestsOfElection(electionId)) {
      int index = contest.getIndex();
      int optionCount = contest.getOptions().size() + contest.getMax();
      if (ciphertext.get(index) == null || individualProofs.get(index) == null
              || accumulatedProofs.get(index) == null || ciphertext.get(index).length != optionCount
              || individualProofs.get(index).length != optionCount
              || Arrays.asList(individualProofs.get(index)).contains(null)
              || Arrays.asList(ciphertext.get(index)).contains(null)) {
        return false;
      }
    }
    return true;
  }

  /**
   * See javadoc in OptionEncrypted for further format information.
   *
   * @param proof is the disjunctive chaum pederson proof
   * @return parsed proof
   */
  private String parseIndividualProofToString(DisjunctiveChaumPedersenDTO proof) {
    String pad0 = proof.proof0().pad().toString(HEXA_RADIX);
    String data0 = proof.proof0().data().toString(HEXA_RADIX);
    String challenge0 = proof.proof0().challenge().toString(HEXA_RADIX);
    String response0 = proof.proof0().response().toString(HEXA_RADIX);

    String pad1 = proof.proof1().pad().toString(HEXA_RADIX);
    String data1 = proof.proof1().data().toString(HEXA_RADIX);
    String challenge1 = proof.proof1().challenge().toString(HEXA_RADIX);
    String response1 = proof.proof1().response().toString(HEXA_RADIX);

    String challenge = proof.challenge().toString(HEXA_RADIX);

    return pad0 + ";" + data0 + ";" + challenge0 + ";" + response0 + "|"
        + pad1 + ";" + data1 + ";" + challenge1 + ";" + response1 + "|"
        + challenge;
  }

  /**
   * See javadoc in OptionEncrypted for further format information.
   *
   * @param proof is the constant chaum pederson proof
   * @return parsed proof
   */
  private String parseAccumulatedProofToString(ConstantChaumPedersenDTO proof) {
    ChaumPedersenProofDTO chaum = proof.pedersenProofDTO();
    String pad = chaum.pad().toString(HEXA_RADIX);
    String data = chaum.data().toString(HEXA_RADIX);
    String challenge = chaum.challenge().toString(HEXA_RADIX);
    String response = chaum.response().toString(HEXA_RADIX);
    String constant = Integer.toString(proof.constant());

    return pad + ";" + data + ";" + challenge + ";" + response + ";" + constant;
  }
}
