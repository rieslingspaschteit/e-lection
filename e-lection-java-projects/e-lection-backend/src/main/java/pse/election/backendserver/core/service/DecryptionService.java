package pse.election.backendserver.core.service;

import com.sunya.electionguard.ChaumPedersen;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import pse.election.backendserver.core.electionguard.DecryptionFacade.Decryption;
import pse.election.backendserver.core.electionguard.KeyCeremonyFacade;
import pse.election.backendserver.core.electionguard.VerificationFacade;
import pse.election.backendserver.core.state.ElectionState;
import pse.election.backendserver.entity.Ballot;
import pse.election.backendserver.entity.Contest;
import pse.election.backendserver.entity.Election;
import pse.election.backendserver.entity.OptionEncrypted;
import pse.election.backendserver.entity.PartialDecryption;
import pse.election.backendserver.entity.PartialPartialDecryption;
import pse.election.backendserver.entity.Tally;
import pse.election.backendserver.entity.Trustee;
import pse.election.backendserver.payload.dto.ChaumPedersenProofDTO;
import pse.election.backendserver.payload.dto.DecryptionDTO;
import pse.election.backendserver.payload.dto.DecryptionDTO.PartialDecryptionDTO;
import pse.election.backendserver.payload.error.exception.IllegalStateOperationException;
import pse.election.backendserver.payload.error.exception.UnauthorizedAccessException;
import pse.election.backendserver.repository.OptionEncryptedRepository;
import pse.election.backendserver.repository.PartialDecryptionRepository;
import pse.election.backendserver.repository.PartialPartialDecryptionRepository;
import pse.election.backendserver.repository.TrusteeRepository;

/**
 * This class processes all the decryption service functionalities. This includes managing partial
 * decryption of a ballot.
 *
 * @version 1.0
 */
@Service
public class DecryptionService {

  private static final String MISSING_DECRYPTION = "Error, a required decryption was missing";
  private static final String UNNECCESSARY_DECRYPTION = "Error, provided decryption for a trustee who is not missing";
  private final PartialDecryptionRepository partialDecryptionRepository;
  private final PartialPartialDecryptionRepository partialPartialDecryptionRepository;
  private final OptionEncryptedRepository optionEncryptedRepository;

  private final BallotService ballotService;

  private final ElectionService electionService;

  private final TrusteeService trusteeService;

  private final TallyService tallyService;

  private final VerificationFacade verificationFacade;

  private final KeyCeremonyFacade keyCeremonyFacade;

  private final TrusteeRepository trusteeRepo;

  /**
   * Constructor of new DecryptionService.
   * */
  @Lazy
  public DecryptionService(PartialDecryptionRepository partialDecryptionRepository,
      PartialPartialDecryptionRepository partialPartialDecryptionRepository,
      OptionEncryptedRepository optionEncryptedRepository, BallotService ballotService,
      ElectionService electionService, TrusteeService trusteeService, TallyService tallyService,
      VerificationFacade verificationFacade, KeyCeremonyFacade keyCeremonyFacade,
      TrusteeRepository trusteeRepo) {
    this.partialDecryptionRepository = partialDecryptionRepository;
    this.partialPartialDecryptionRepository = partialPartialDecryptionRepository;
    this.optionEncryptedRepository = optionEncryptedRepository;
    this.ballotService = ballotService;
    this.electionService = electionService;
    this.trusteeService = trusteeService;
    this.tallyService = tallyService;
    this.verificationFacade = verificationFacade;
    this.keyCeremonyFacade = keyCeremonyFacade;
    this.trusteeRepo = trusteeRepo;
  }

  /**
   * Adding a partial decryption to the partial decryption repository. It will validate the partial
   * decryption before storing it into the repository.
   *
   * @param decryptionDTO is the new partial decryption to be stored
   */
  public void addDecryption(DecryptionDTO decryptionDTO, long electionId, String email)
      throws UnauthorizedAccessException {
    trusteeService.checkExistingEmail(email);
    electionService.checkExistsElection(electionId);

    Trustee trustee = trusteeService.getTrustee(email, electionId);
    Election desiredElection = electionService.getElection(electionId);

    switch (desiredElection.getState()) {
      case P_DECRYPTION -> phaseOneDecryption(decryptionDTO, trustee);
      case PP_DECRYPTION -> phaseTwoDecryption(decryptionDTO, trustee);
      default -> throw new IllegalStateOperationException(
          "Invalid election state for adding decryptions." + desiredElection.getState());
    }
  }

  /**
   * Saves the partial decryptions for each tallied option and also saves the partial decryptions of
   * the spoiled ballots.
   *
   * @param tallies        collection of partial decryptions of tallies
   * @param spoiledBallots collection of partial decryptions of spoiled Ballots
   */
  public void addPartialDecryptionBotTrustee(Map<Long, Decryption> tallies,
      Map<Long, Map<Long, Decryption>> spoiledBallots,
      Election election) {
    long electionId = election.getElectionId(); // Guaranteed same electionId for every tally
    long botTrusteeId = trusteeService.getBotTrustee(electionId).getTrusteeId();

    if (election.getState() == ElectionState.P_DECRYPTION) {
      savePartialDecryptionTallyBot(tallies, botTrusteeId);
      savePartialDecryptionSpoiledBallotBot(spoiledBallots, botTrusteeId);

    } else {
      throw new IllegalStateOperationException(
          "Election is in an invalid state to add decryptions.");
    }
    Trustee botTrustee = trusteeService.getTrustee(botTrusteeId);
    botTrustee.setWaiting(true);
    botTrustee.setAvailable();
    trusteeService.addTrustee(botTrustee);
  }

  /**
   * Saves the partial partial decryptions for each tallied option and also saves the partial
   * partial decryptions of the spoiled ballots.
   *
   * @param tallies           partial partial decryptions of tallies
   * @param spoiledBallots    partial partial decryptions of spoiled ballots
   * @param forWhichIndex     for whom the decryption was generated
   */
  public void addPartialPartialDecryptionBotTrustee(Map<Long, Decryption> tallies,
      Map<Long, Map<Long, Decryption>> spoiledBallots, int forWhichIndex, Election election) {
    long electionId = election.getElectionId(); // Guaranteed same electionId for every tally
    long botTrusteeId = trusteeService.getBotTrustee(electionId).getTrusteeId();
    long forWhichTrusteeId = trusteeRepo.findByTrusteeIndexAndElectionId(forWhichIndex, electionId).getTrusteeId();
    if (election.getState() == ElectionState.PP_DECRYPTION) {
      savePartialPartialDecryptionTallyBot(tallies, botTrusteeId, forWhichTrusteeId);
      savePartialPartialDecryptionSpoiledBallotBot(spoiledBallots, botTrusteeId,
          forWhichTrusteeId);

    } else {
      throw new IllegalStateOperationException(
          "Election is in an invalid state to add decryptions.");
    }
    Trustee botTrustee = trusteeService.getTrustee(botTrusteeId);
    botTrustee.setWaiting(true);
    trusteeService.addTrustee(botTrustee);
  }

  /**
   * Getter for all partial decryption uploaded by a trustee referenced by an id for a tally
   * referenced by an id.
   *
   * @param trusteeId is the id of the trustee
   * @param tallyId   is the id of the tally
   * @return list containing all partial decryption by a trustee on a tally
   */
  public PartialDecryption getPartialDecryptionOfTrustee(long trusteeId, long tallyId) {
    if (!partialDecryptionRepository.existsByTallyIdAndTrusteeId(tallyId, trusteeId)) {
      return null;
    }

    return partialDecryptionRepository.findByTrusteeIdAndTallyId(trusteeId, tallyId);
  }

  /**
   * Getter for all partial partial decryption uploaded by a trustee referenced by an id for a tally
   * referenced by an id.
   *
   * @param trusteeId is the id of the trustee
   * @param tallyId   is the id of the tally
   * @return list containing all partial partial decryption by a trustee on a tally
   */
  public List<PartialPartialDecryption> getAllPartialPartialDecryptionOfTrustee(long trusteeId,
      long tallyId) {
    if (!partialPartialDecryptionRepository.existsByTallyIdAndTrusteeId(tallyId, trusteeId)) {
      return new ArrayList<>();
    }

    return (List<PartialPartialDecryption>) partialPartialDecryptionRepository
        .findByTrusteeIdAndTallyId(trusteeId, tallyId);
  }

  /**
   * Getter for all partial partial decryption of a trustee.
   *
   * @param trusteeId is the id of the trustee
   * @return list containing all partial partial decryption of a trustee
   * */
  public List<PartialPartialDecryption> getAllPartialPartialDecryptionOfTrustee(long trusteeId) {
    if (!partialPartialDecryptionRepository.existsByTrusteeId(trusteeId)) {
      return new ArrayList<>();
    }

    return (List<PartialPartialDecryption>) partialPartialDecryptionRepository.findByTrusteeId(
        trusteeId);
  }

  /**
   * Retrieves all partial decryptions of a trustee for a specific election.
   * // @param electionId election identifier
   *
   * @param trusteeId trustee identifier
   * @return collection of found partial decryptions
   */
  public List<PartialDecryption> getAllPartialDecryptionByTrustee(long trusteeId) {
    trusteeService.checkExistingId(trusteeId);
    return (List<PartialDecryption>) partialDecryptionRepository.findByTrusteeId(trusteeId);
  }

  /**
   * Retrieves all partial decryptions of a tallied encrypted option to a specific election.
   *
   * @param electionId   election identifier
   * @param optionIndex  index of option
   * @param contestIndex index of contest
   * @return collection of all found partial decryptions
   */
  public List<PartialDecryption> getAllPartialDecryptionOfTally(long electionId, int contestIndex,
      int optionIndex) {
    Tally desiredTally = tallyService.getSpecificTally(electionId, contestIndex, optionIndex);

    return (List<PartialDecryption>) partialDecryptionRepository.findByTallyId(
        desiredTally.getTallyId());
  }

  /**
   * Retrieves all partial partial decryptions of a tallied encrypted option to a specific
   * election.
   *
   * @param electionId   election identifier
   * @param optionIndex  index of option
   * @param contestIndex index of contest
   * @return collection of all found partial partial decryptions
   */
  public List<PartialPartialDecryption> getAllPartialPartialDecryptionOfTally(long electionId,
      int contestIndex, int optionIndex) {
    Tally desiredTally = tallyService.getSpecificTally(electionId, contestIndex, optionIndex);

    return (List<PartialPartialDecryption>) partialPartialDecryptionRepository.findByTallyId(
        desiredTally.getTallyId());
  }

  /**
   * Retrieves all partial decryptions of an encrypted option of a spoiled ballot.
   *
   * @param ballotId     ballot identifier
   * @param contestIndex index of contest
   * @param optionIndex  index of option
   * @return collection of found partial decryptions
   */
  public List<PartialDecryption> getAllPartialDecryptionOfSpoiledBallotOption(long ballotId,
      int contestIndex, int optionIndex) {
    OptionEncrypted desiredOption = optionEncryptedRepository
        .findByBallotIdAndContestIndexAndOptionIndex(ballotId, contestIndex, optionIndex);

    return getAllPartialDecryptionOfOption(desiredOption.getOptionEncryptedId());
  }

  /**
   * Retrieves all partial partial decryptions of an encrypted option of a spoiled ballot.
   *
   * @param ballotId     ballot identifier
   * @param contestIndex index of contest
   * @param optionIndex  index of option
   * @return collection of found partial partial decryptions
   */
  public List<PartialPartialDecryption> getAllPartialPartialDecryptionOfSpoiledBallotOption(
      long ballotId, int contestIndex, int optionIndex) {
    OptionEncrypted desiredOption = ballotService.getSpecificOptionOfBallot(ballotId, contestIndex,
        optionIndex);
    return getAllPartialPartialDecryptionOfOption(desiredOption.getOptionEncryptedId());
  }

  /**
   * Getter for all partial decryption to an encrypted option.
   *
   * @param optionId option identifier
   * @return collection of partial decryptions
   */
  public List<PartialDecryption> getAllPartialDecryptionOfOption(long optionId) {
    ballotService.checkExistingOption(optionId);
    return (List<PartialDecryption>) partialDecryptionRepository.findByOptionEncryptedId(optionId);
  }

  /**
   * Getter for all the partial partial decryptions of an encrypted option.
   *
   * @param optionId option identifier
   * @return collection of partial partial decryptions
   */
  public List<PartialPartialDecryption> getAllPartialPartialDecryptionOfOption(long optionId) {
    ballotService.checkExistingOption(optionId);
    return (List<PartialPartialDecryption>) partialPartialDecryptionRepository
        .findByOptionEncryptedId(optionId);
  }

  /**
   * Saves all the partial decryptions as well as the partial partial decryptions to all spoiled
   * ballots of an election.
   */
  private void saveDecryptionOfSpoiledBallot(PartialDecryptionDTO currentSpoiledBallot,
      Trustee trustee, long otherTrusteeId) {
    Election election = electionService.getElection(trustee.getElectionId());
    for (Map.Entry<Integer, BigInteger[]> partialDecryptedOption
        : currentSpoiledBallot.partialDecryptedOptions().entrySet()) {

      for (int optionIndex = 0; optionIndex < partialDecryptedOption.getValue().length;
          optionIndex++) {
        OptionEncrypted option = optionEncryptedRepository.findByBallotIdAndContestIndexAndOptionIndex(
            currentSpoiledBallot.ballotId(),
                partialDecryptedOption.getKey(),
            optionIndex);

        ChaumPedersenProofDTO proof = currentSpoiledBallot.chaumPedersonProofs()
            .get(partialDecryptedOption.getKey())[optionIndex];
        String parsedProof = parseProofToString(proof);

        if (otherTrusteeId != 0) {
          PartialPartialDecryption decryption = new PartialPartialDecryption(
              trustee.getTrusteeId(),
              otherTrusteeId,
              -1, // To mark that the partial decryption is not for a tallied option.
              option.getOptionEncryptedId(),
              partialDecryptedOption.getValue()[optionIndex].toString(16),
              parsedProof);
          partialPartialDecryptionRepository.save(decryption);
        } else {
          PartialDecryption decryption = new PartialDecryption(
              -1, // To mark that the partial decryption is not for a tallied option.
              trustee.getTrusteeId(),
              option.getOptionEncryptedId(),
              partialDecryptedOption.getValue()[optionIndex].toString(16),
              parsedProof);
          partialDecryptionRepository.save(decryption);
        }
      }
    }
  }

  /**
   * Saves all the partial decryptions of the tallies of an election.
   */
  private void savePartialDecryptionOfTally(long electionId, Trustee trustee,
      PartialDecryptionDTO partialTally) {
    //trusteeService.
    for (Map.Entry<Integer, BigInteger[]> partialDecryptedOption : partialTally.partialDecryptedOptions()
        .entrySet()) {
      for (int optionIndex = 0; optionIndex < partialDecryptedOption.getValue().length;
          optionIndex++) {
        Tally currentTally = tallyService.getSpecificTally(electionId,
            partialDecryptedOption.getKey(), optionIndex);

        ChaumPedersenProofDTO proof = partialTally.chaumPedersonProofs()
            .get(partialDecryptedOption.getKey())[optionIndex];

        String parsedProof = parseProofToString(proof);

        PartialDecryption decryption = new PartialDecryption(
            currentTally.getTallyId(),
            trustee.getTrusteeId(),
            -1, // To mark that the partial decryption is not for an encrypted option.
            partialDecryptedOption.getValue()[optionIndex].toString(16),
            parsedProof);
        partialDecryptionRepository.save(decryption);
      }
    }
  }

  /**
   * Saves all the partial partial decryptions of the tallies of an election.
   */
  private void savePartialPartialDecryptionOfTally(long electionId, Trustee trustee,
      Map.Entry<Integer, PartialDecryptionDTO> partialPartialTally) {
    for (Map.Entry<Integer, BigInteger[]> partialDecryptedOption : partialPartialTally.getValue()
        .partialDecryptedOptions().entrySet()) {
      for (int optionIndex = 0; optionIndex < partialDecryptedOption.getValue().length;
          optionIndex++) {
        Tally currentTally = tallyService.getSpecificTally(electionId,
            partialDecryptedOption.getKey(), optionIndex);

        ChaumPedersenProofDTO proof = partialPartialTally.getValue().chaumPedersonProofs()
            .get(partialDecryptedOption.getKey())[optionIndex];
        String parsedProof = parseProofToString(proof);
        PartialPartialDecryption decryption = new PartialPartialDecryption(
            trustee.getTrusteeId(),
            partialPartialTally.getKey(),
            currentTally.getTallyId(),
            -1, // To mark that the partial decryption is not for an encrypted option.
            partialDecryptedOption.getValue()[optionIndex].toString(16),
            parsedProof);
        partialPartialDecryptionRepository.save(decryption);
      }
    }
  }


  private void savePartialDecryptionTallyBot(Map<Long, Decryption> tallies, long botTrusteeId) {
    for (Map.Entry<Long, Decryption> tally : tallies.entrySet()) {
      PartialDecryption partialDecryption = new PartialDecryption(
          tally.getKey(),
          botTrusteeId,
          -1, // To mark that the partial decryption is not for an encrypted option.
          tally.getValue().decryption().getBigInt().toString(16),
          parseProofToString(tally.getValue().chaumPedersenProof()));
      partialDecryptionRepository.save(partialDecryption);
    }
  }

  private void savePartialDecryptionSpoiledBallotBot(
      Map<Long, Map<Long, Decryption>> spoiledBallots, long botTrusteeId) {
    for (Map.Entry<Long, Map<Long, Decryption>> spoiledBallot : spoiledBallots.entrySet()) {
      for (Long decryptionId : spoiledBallot.getValue().keySet()) {
        PartialDecryption partialDecryption = new PartialDecryption(
            -1L,
            botTrusteeId,
            decryptionId,
            spoiledBallot.getValue().get(decryptionId).decryption().base16(),
            parseProofToString(spoiledBallot.getValue().get(decryptionId).chaumPedersenProof())
        );
        partialDecryptionRepository.save(partialDecryption);
      }
    }
  }

  private void savePartialPartialDecryptionTallyBot(Map<Long, Decryption> tallies,
      long botTrusteeId, long forWhichTrusteeId) {
    for (Map.Entry<Long, Decryption> tally : tallies.entrySet()) {
      PartialPartialDecryption decryption = new PartialPartialDecryption(
          botTrusteeId,
          forWhichTrusteeId,
          tally.getKey(),
          -1, // To mark that the partial decryption is not for a tally.
          tally.getValue().decryption().getBigInt().toString(16),
          parseProofToString(tally.getValue().chaumPedersenProof()));
      partialPartialDecryptionRepository.save(decryption);
    }
  }

  private void savePartialPartialDecryptionSpoiledBallotBot(
      Map<Long, Map<Long, Decryption>> spoiledBallots, long botTrusteeId,
      long forWhichTrusteeId) {
    for (Map.Entry<Long, Map<Long, Decryption>> spoiledBallot : spoiledBallots.entrySet()) {
      for (Map.Entry<Long, Decryption> decryption : spoiledBallot.getValue().entrySet()) {
        PartialPartialDecryption ppDecryption = new PartialPartialDecryption(
            botTrusteeId,
            forWhichTrusteeId,
            -1, // To mark that the partial decryption is not for a tally.
            decryption.getKey(),
            decryption.getValue().decryption().base16(),
            parseProofToString(decryption.getValue().chaumPedersenProof()));
        partialPartialDecryptionRepository.save(ppDecryption);
      }
    }
  }

  private void phaseOneDecryption(DecryptionDTO decryptionDTO, Trustee trustee)
      throws UnauthorizedAccessException {
    long electionId = trustee.getElectionId();

    if (trustee.isWaiting()) {
      throw new UnauthorizedAccessException("Trustee has already sent partial decryptions.");
    }

    checkCompleteDecryptionForTrustee(decryptionDTO.partialDecryptedSpoiledBallots().get(0),
        decryptionDTO.partialDecryptedTalliedBallots().get(0), trustee.getPrimaryKey(), electionId);

    // Partial decryption of tallied options
    if (decryptionDTO.partialDecryptedTalliedBallots().containsKey(0)) {
      PartialDecryptionDTO partialTally = decryptionDTO.partialDecryptedTalliedBallots().get(0);
      savePartialDecryptionOfTally(electionId, trustee, partialTally);
    } else {
      throw new IllegalStateOperationException(
          "Election is in wrong state to receive partial decryptions.");
    }

    // Partial decryption of spoiled ballots
    if (!decryptionDTO.partialDecryptedSpoiledBallots().isEmpty()) {
      if (decryptionDTO.partialDecryptedSpoiledBallots().containsKey(0)) {
        PartialDecryptionDTO[] spoiledBallots = decryptionDTO.partialDecryptedSpoiledBallots()
                .get(0);
        for (PartialDecryptionDTO spoiledBallot : spoiledBallots) {
          saveDecryptionOfSpoiledBallot(spoiledBallot, trustee, 0);
        }
      } else {
        throw new IllegalStateOperationException(
                "Election is in wrong state to receive partial decryptions.");
      }
    }

    trustee.setWaiting(true);
    trustee.setAvailable();
    trusteeService.addTrustee(trustee);
    electionService.tryUpdateState(electionId, ElectionState.DONE);
  }

  private void phaseTwoDecryption(DecryptionDTO decryptionDTO, Trustee trustee)
      throws UnauthorizedAccessException {
    long electionId = trustee.getElectionId();

    if (trustee.isWaiting()) {
      throw new UnauthorizedAccessException(
          "Trustee has already sent partial partial decryptions.");
    }
    Map<Integer, String[]> allCoefficients = trusteeService.getElgamalKeys(trustee.getEmail(),
        electionId);
    checkCompleteDecryptionPhaseTwo(electionId, decryptionDTO, trustee.getIndex(), allCoefficients);

    // Partial partial decryption of tallied options
    if (decryptionDTO.partialDecryptedTalliedBallots().containsKey(0)) {
      throw new IllegalStateOperationException(
          "Election is in wrong state to receive partial decryption.");
    }

    for (Map.Entry<Integer, PartialDecryptionDTO> partialPartialTally : decryptionDTO.partialDecryptedTalliedBallots()
        .entrySet()) {
      savePartialPartialDecryptionOfTally(electionId, trustee, partialPartialTally);
    }

    // Partial partial decryption of spoiled ballots
    if (decryptionDTO.partialDecryptedSpoiledBallots().containsKey(0)) {
      throw new IllegalStateOperationException(
          "Election is in wrong state to receive partial decryption.");
    }

    for (Map.Entry<Integer, PartialDecryptionDTO[]> allSpoiledBallots : decryptionDTO.partialDecryptedSpoiledBallots()
        .entrySet()) {
      Trustee missingTrustee = trusteeRepo.findByTrusteeIndexAndElectionId(allSpoiledBallots.getKey(), electionId);
      for (int i = 0; i < allSpoiledBallots.getValue().length; i++) {
        saveDecryptionOfSpoiledBallot(allSpoiledBallots.getValue()[i], trustee, missingTrustee.getTrusteeId());
      }
    }

    trustee.setWaiting(true);
    trusteeService.addTrustee(trustee);
    electionService.tryUpdateState(electionId, ElectionState.DONE);
  }

  private void checkTallyDecryption(PartialDecryptionDTO decryptionDTO, BigInteger key,
      long electionId) {
    List<Contest> contestsOfElection = electionService.getAllContestsOfElection(electionId);
    Map<Integer, BigInteger[]> partialDecryptions = decryptionDTO.partialDecryptedOptions();
    Map<Integer, ChaumPedersenProofDTO[]> proofs = decryptionDTO.chaumPedersonProofs();
    for (Contest contest : contestsOfElection) {
      int expectedOptions = contest.getOptions().size() + contest.getMax();
      BigInteger[] contestDecryptions = partialDecryptions.get(contest.getIndex());
      ChaumPedersenProofDTO[] contestProofs = proofs.get(contest.getIndex());
      //This makes sure that the decryptions for every contest exist and have exactly the right number of entries
      if (contestDecryptions == null || contestProofs == null
          || contestDecryptions.length != expectedOptions || contestProofs.length != expectedOptions
          || Arrays.asList(contestDecryptions).contains(null)
      ) {
        throw new IllegalArgumentException(MISSING_DECRYPTION);
      }
    }
    verificationFacade.verifyDecryption(decryptionDTO, electionId, key);
  }

  private void checkCompleteDecryptionForTrustee(PartialDecryptionDTO[] spoiledBallotsDecryption,
      PartialDecryptionDTO tallyDecryption,
      BigInteger key, long electionId) {
    if (electionService.getState(electionId) == ElectionState.P_DECRYPTION) {
      if (spoiledBallotsDecryption == null || tallyDecryption == null) {
        throw new IllegalArgumentException(MISSING_DECRYPTION);
      }
      Set<Long> foundBallotIds = new HashSet<>();
      Set<Long> expectedBallotIds = new HashSet<>();
      for (PartialDecryptionDTO spoiledBallotDecryption : spoiledBallotsDecryption) {
        foundBallotIds.add(spoiledBallotDecryption.ballotId());
        checkTallyDecryption(spoiledBallotDecryption, key, electionId);
      }
      for (Ballot spoiledBallot : ballotService.getAllSpoiledBallotsOfElection(electionId)) {
        expectedBallotIds.add(spoiledBallot.getBallotId());
      }
      checkTallyDecryption(tallyDecryption, key, electionId);
      if (!foundBallotIds.equals(expectedBallotIds)) {
        throw new IllegalArgumentException(MISSING_DECRYPTION);
      }
    }
  }

  private void checkCompleteDecryptionPhaseTwo(long electionId, DecryptionDTO decryption,
      int trusteeIndex,
      Map<Integer, String[]> allCoefficients) {
    List<Trustee> allTrustees = trusteeService.getAllTrustees(electionId);
    for (Trustee missingTrustee : allTrustees) {
      if (getAllPartialDecryptionByTrustee(missingTrustee.getTrusteeId()).isEmpty()) {
        List<BigInteger> coefficients = new ArrayList<>();
        Arrays.stream(allCoefficients.get(missingTrustee.getIndex()))
                .forEach(t -> coefficients.add(new BigInteger(t, 16)));
        BigInteger keyShare = keyCeremonyFacade.generateKeyShare(coefficients, trusteeIndex);
        checkCompleteDecryptionForTrustee(
            decryption.partialDecryptedSpoiledBallots().get(missingTrustee.getIndex()),
            decryption.partialDecryptedTalliedBallots().get(missingTrustee.getIndex()),
            keyShare, electionId);
      } else {
        if (decryption.partialDecryptedSpoiledBallots().containsKey(missingTrustee.getIndex())
            || decryption.partialDecryptedTalliedBallots().containsKey(missingTrustee.getIndex())) {
          throw new IllegalStateOperationException(UNNECCESSARY_DECRYPTION);
        }
      }
    }
  }

  private String parseProofToString(ChaumPedersenProofDTO proof) {
    String pad = proof.pad().toString(16);
    String data = proof.data().toString(16);
    String challenge = proof.challenge().toString(16);
    String response = proof.response().toString(16);
    return pad + ";" + data + ";" + challenge + ";" + response;
  }

  private String parseProofToString(ChaumPedersen.ChaumPedersenProof proof) {
    assert proof.pad != null && proof.data != null;

    String pad = proof.pad.getBigInt().toString(16);
    String data = proof.data.getBigInt().toString(16);
    String challenge = proof.challenge.getBigInt().toString(16);
    String response = proof.response.getBigInt().toString(16);
    return pad + ";" + data + ";" + challenge + ";" + response;
  }
}

