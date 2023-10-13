package pse.election.backendserver.core.service;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Arrays;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import pse.election.backendserver.core.bot.BotFacade;
import pse.election.backendserver.core.electionguard.VerificationFacade;
import pse.election.backendserver.core.state.ElectionState;
import pse.election.backendserver.entity.BotTrustee;
import pse.election.backendserver.entity.Election;
import pse.election.backendserver.entity.Trustee;
import pse.election.backendserver.payload.dto.ElgamalProofDTO;
import pse.election.backendserver.payload.dto.SchnorrProofDTO;
import pse.election.backendserver.payload.error.exception.EntityNotFoundException;
import pse.election.backendserver.payload.error.exception.IllegalStateOperationException;
import pse.election.backendserver.payload.error.exception.InvalidConfigurationException;
import pse.election.backendserver.repository.BotTrusteeRepository;
import pse.election.backendserver.repository.TrusteeRepository;

/**
 * This class processes all the trustee service functionalities.
 *
 * @version 1.0
 */
@Service
public class TrusteeService {

  private static final String EMAIL_REGEX = "^[a-zA-Z0-9]+(?:\\.[a-zA-Z0-9]+)*@[a-zA-Z0-9]+(?:\\.[a-zA-Z0-9]+)*$";
  private static final String invalidConfigErrorMessage = "Inval id trustee information provided";
  private static final String INVALID_KEY_FORMAT = "Invalid number of ElGamal keys or backups provided";
  private static final int HEXA_RADIX = 16;

  private final TrusteeRepository trusteeRepository;

  private final VerificationFacade verificationFacade;

  private final BotTrusteeRepository botTrusteeRepository;

  private final ElectionService electionService;

  private final BotFacade botFacade;

  /**
   * Constructor of new Trustee Service.
   * */
  @Lazy
  public TrusteeService(TrusteeRepository trusteeRepository, VerificationFacade verificationFacade,
      BotTrusteeRepository botTrusteeRepository, ElectionService electionService,
      BotFacade botFacade) {
    this.botFacade = botFacade;
    this.trusteeRepository = trusteeRepository;
    this.verificationFacade = verificationFacade;
    this.botTrusteeRepository = botTrusteeRepository;
    this.electionService = electionService;
  }

  /**
   * Adding a new trustee to the trustee repository.
   *
   * @param trustee is the trustee to be added into the trustee repository
   * @return added trustee
   */
  public Trustee addTrustee(Trustee trustee) {
    if (trustee == null) {
      throw new IllegalArgumentException();
    }

    // Update existing trustee
    if (botTrusteeRepository.existsByTrusteeIdAndElectionId(trustee.getTrusteeId(),
        trustee.getElectionId())) {
      return trusteeRepository.save(trustee);
    }

    if (!trustee.getEmail().matches(EMAIL_REGEX)) {
      throw new InvalidConfigurationException("Invalid trustee email provided.");
    }

    return trusteeRepository.save(trustee);
  }

  /**
   * Adding multiple trustees to the trustee repository.
   *
   * @param trustees is a list containing to be added trustees
   */
  public void addTrustees(List<Trustee> trustees) {
    for (Trustee trustee : trustees) {
      if (trustee == null || !trustee.getEmail().matches(EMAIL_REGEX)) {
        throw new IllegalArgumentException("Invalid trustee email provided.");
      }
    }
    trusteeRepository.saveAll(trustees);
  }

  /**
   * Saves the bot trustee.
   *
   * @param bot entity
   */
  public void saveBotTrustee(BotTrustee bot) {
    botTrusteeRepository.save(bot);
  }

  /**
   * Retrieves the bot trustee of an election.
   *
   * @param electionId election identifier
   * @return desired bot trustee entity
   */
  public BotTrustee getBotTrustee(long electionId) {
    return botTrusteeRepository.findById(electionId).get();
  }

  /**
   * Adding an auxiliary key to a trustee referenced by his email address.
   *
   * @param email      is the email of the trustee
   * @param auxKey     is the auxiliary key used for information encryption
   * @param auxKeyType is the type of the key (e.g. RSA)
   * @param electionId is the id of the election to set the aux keys for
   * @return trustee with the added keys
   * @throws EntityNotFoundException in case the trustee could not be found
   */
  public Trustee addAuxKey(String email, String auxKey, String auxKeyType, long electionId) {
    checkExistingEmail(email);
    Election desiredElection = electionService.getElection(electionId);

    if (email == null || auxKey == null || auxKeyType == null) {
      throw new IllegalArgumentException("Invalid trustee information provided");
    }

    if (desiredElection.getState() != ElectionState.AUX_KEYS) {
      throw new IllegalStateOperationException(
          "Invalid election state to add an auxiliary public key.");
    }

    Trustee desiredTrustee = trusteeRepository.findByEmailAndElectionId(email, electionId);
    if (desiredTrustee.isWaiting()) {
      throw new IllegalStateOperationException("Trustee has already sent auxiliary keys.");
    }

    desiredTrustee.setAuxkey(auxKey);
    desiredTrustee.setAuxkeyType(auxKeyType);
    desiredTrustee.setWaiting(true);

    Trustee savedTrustee = trusteeRepository.save(desiredTrustee);

    // Checks whether the election state changes with the newly arrived auxiliary public key.
    electionService.tryUpdateState(electionId, ElectionState.EPKB);

    return savedTrustee;
  }

  /**
   * Adding the elgamal keys and partial key backups of a trustee to an election.
   *
   * @param electionId is the id referencing the election to set the keys for
   * @param email      is the email of the trustee setting the keys and backups
   * @return trustee that has added the keys and backups
   * @throws EntityNotFoundException in case the election could not be found // @throws
   *                                 InvalidAmountException in case the number of keys or backups is
   *                                 invalid
   */
  public Trustee addElgamalKeysAndBackups(ElgamalProofDTO proofDTO, String email, long electionId) {
    Election desiredElection = electionService.getElection(electionId);

    if (desiredElection.getState() != ElectionState.EPKB) {
      throw new IllegalStateOperationException(
          "Election is in an invalid state to still receive public elgamal keys and backups.");
    }

    if (email == null || !trusteeRepository.existsByEmailAndElectionId(email, electionId)) {
      throw new IllegalStateOperationException("Invalid trustee information provided.");
    }

    Trustee desiredTrustee = trusteeRepository.findByEmailAndElectionId(email, electionId);
    if (desiredTrustee.isWaiting()) {
      throw new IllegalStateOperationException("Trustee has already sent elgamal keys and backups");
    }

    if (!checkBackupFormat(proofDTO, electionId, desiredTrustee.getIndex())) {
      throw new IllegalArgumentException(INVALID_KEY_FORMAT);
    }

    if (!verificationFacade.verifyKeyCeremony(proofDTO)) {
      throw new IllegalArgumentException("Proofs are not valid.");
    }
    if (desiredElection.hasBot() && desiredTrustee.getTrusteeId() != getBotTrustee(
            electionId).getTrusteeId()) {
      botFacade.verifyProvidedBackup(electionId, proofDTO);
    }
    List<String> publicElgamalKeyAndProof = new ArrayList<>();
    for (int i = 0; i < desiredElection.getTrusteeThreshold(); i++) {
      publicElgamalKeyAndProof.add(parseElgamalKeyAndProofToString(proofDTO.proofs(), i));
    }

    // Scatter the backups to each corresponding trustee
    for (Map.Entry<Integer, String> entry : proofDTO.backups().entrySet()) {
      Trustee tempTrustee = trusteeRepository.findByTrusteeIndexAndElectionId(entry.getKey(),
          electionId);
      String backup = desiredTrustee.getIndex() + ";" + entry.getValue();
      tempTrustee.addBackup(backup);
      trusteeRepository.save(tempTrustee);
    }

    desiredTrustee.addPublicElgamalKeyAndProof(publicElgamalKeyAndProof);
    desiredTrustee.setWaiting(true);
    trusteeRepository.save(desiredTrustee);

    electionService.tryUpdateState(electionId, ElectionState.KEYCEREMONY_FINISHED);

    return desiredTrustee;
  }

  private boolean checkBackupFormat(ElgamalProofDTO proofs, long electionId, int trusteeIndex) {
    int threshold = electionService.getElection(electionId).getTrusteeThreshold();
    if (proofs.proofs() == null || proofs.backups() == null || proofs.proofs().length != threshold
        ||
        Arrays.asList(proofs.proofs()).contains(null)) {
      return false;
    }
    for (Trustee t : trusteeRepository.findByElectionId(electionId)) {
      if (proofs.backups().get(t.getIndex()) == null && t.getIndex() != trusteeIndex) {
        return false;
      }
    }
    return true;
  }

  /**
   * Getter for the trustee with a parsed email address.
   *
   * @param electionId election identifier
   * @param email      is the email address of the trustee
   * @return trustee with the email
   * @throws EntityNotFoundException  in case the trustee could not be found
   * @throws IllegalArgumentException in case email is null
   */
  public Trustee getTrustee(String email, long electionId) {
    if (email == null) {
      throw new IllegalArgumentException("Invalid trustee information provided.");
    }
    Trustee desiredTrustee = trusteeRepository.findByEmailAndElectionId(email, electionId);

    if (desiredTrustee == null) {
      throw new EntityNotFoundException(
          "Trustee not found with provided email and election identifier.");
    }

    return desiredTrustee;
  }

  /**
   * Getter for trustee entity.
   *
   * @param trusteeId trustee identifier
   * @return desired trustee
   */
  public Trustee getTrustee(long trusteeId) {
    if (trusteeRepository.findById(trusteeId).isEmpty()) {
      throw new EntityNotFoundException("Trustee not found.");
    }
    return trusteeRepository.findById(trusteeId).get();
  }

  /**
   * Getter for trustee entity.
   *
   * @param electionId election identifier
   * @param index      of trustee
   * @return desired trustee
   */
  public Trustee getTrustee(long electionId, int index) {
    return trusteeRepository.findByTrusteeIndexAndElectionId(index, electionId);
  }

  /**
   * Getter for all trustees assigned to an election referenced by an id.
   *
   * @param electionId is the id referencing the election
   * @return list containing all trustees of an election
   * @throws EntityNotFoundException in case the election could not be found
   */
  public List<Trustee> getAllTrustees(long electionId) {
    List<Trustee> trustees = (List<Trustee>) trusteeRepository.findByElectionId(electionId);
    if (trustees.isEmpty()) {
      throw new EntityNotFoundException("There are no trustees to this election.");
    }
    return trustees;
  }

  /**
   * Getter for all the elections a trustee, referenced by email, is assigned to.
   *
   * @param email is the email of a trustee
   * @return list containing all elections of a trustee
   * @throws IllegalArgumentException in case email is null
   */
  public List<Election> getAllElectionsOfTrustee(String email) {
    if (email == null) {
      throw new IllegalArgumentException("Invalid trustee information provided.");
    }

    List<Election> desiredElections = new ArrayList<>();
    List<Trustee> desiredTrustees = (List<Trustee>) trusteeRepository.findByEmail(email);
    for (Trustee trustee : desiredTrustees) {
      desiredElections.add(electionService.getElection(trustee.getElectionId()));
    }

    return desiredElections;
  }

  /**
   * Returns if a trustee with a parsed email exists in the trustee repository.
   *
   * @param email is the email of the trustee to search for
   * @return true in case a trustee with the email address exists in the trustee repository, otherwise false
   * @throws IllegalArgumentException in case email is null
   */
  public boolean existsByEmail(String email) {
    if (email == null) {
      throw new IllegalArgumentException("Invalid trustee information provided.");
    }
    return trusteeRepository.existsByEmail(email);
  }

  /**
   * Resetting the waiting status for all trustees in an election. This is always performed if a new
   * election state is achieved.
   *
   * @param electionId identifier of election
   */
  public void updateIsWaitingTrustee(long electionId) {
    for (Trustee trustee : getAllTrustees(electionId)) {
      trustee.setWaiting(false);
    }
  }

  /**
   * Returns a Map containing the relevant backups for a trustee for an election.
   *
   * @param email      of trustee
   * @param electionId identifier of election
   * @param missingOnly if this is true, the method will only return the backups by missing trustees
   * @return backups mapped with trustee index and backup
   * @throws IllegalStateOperationException in case election is in an invalid state
   * @throws IllegalArgumentException       in case email is null
   */
  public Map<Integer, String> getBackups(String email, long electionId, boolean missingOnly) {
    if (email == null) {
      throw new IllegalArgumentException(invalidConfigErrorMessage);
    }

    Trustee desiredTrustee = trusteeRepository.findByEmailAndElectionId(email, electionId);
    Map<Integer, String> backupMap = new HashMap<>();

    for (String backupString : desiredTrustee.getBackups()) {
      String[] tempBackup = backupString.split(";");
      Integer trusteeIndex = Integer.valueOf(tempBackup[0]);
      Trustee other = trusteeRepository.findByTrusteeIndexAndElectionId(trusteeIndex, electionId);
      if (!other.isAvailable() || !missingOnly) {
        backupMap.put(trusteeIndex, tempBackup[1]);
      }
    }
    return backupMap;
  }

  /**
   * Returns the ElGamal keys of every trustee in the election except for the trustee the keys are requested for.
   *
   * @param email      of trustee
   * @param electionId identifier of election
   * @return elgamal keys mapped with trustee index and order of elgamal keys
   * @throws EntityNotFoundException        in case election does not exists
   * @throws IllegalStateOperationException in case election is in a false state
   */
  public Map<Integer, String[]> getElgamalKeys(String email, long electionId) {
    Election election = electionService.getElection(electionId);
    if (election == null) {
      throw new EntityNotFoundException("No such election found with given identifier.");
    }

    Map<Integer, String[]> elgamalKeysMap = new HashMap<>();
    List<Trustee> trusteesOfElection = (List<Trustee>) trusteeRepository.findByElectionId(
        electionId);

    // Get for each trustee of an election the necessary public keys in the correct order.
    for (Trustee trustee : trusteesOfElection) {
      String[] elgamalKeys = new String[election.getTrusteeThreshold()];
      if (!Objects.equals(trustee.getEmail(), email)) {
        List<String> publicKeysAndProofs = trustee.getPublicElgamalKeyAndProof();

        for (String keyAndProof : publicKeysAndProofs) {
          String[] tempKeyAndProof = keyAndProof.split(";");
          Integer order = Integer.valueOf(tempKeyAndProof[0]);
          elgamalKeys[order] = tempKeyAndProof[1];
        }
        elgamalKeysMap.put(trustee.getIndex(), elgamalKeys);
      }
    }
    return elgamalKeysMap;
  }

  /**
   * Counts how many trustees have finished their duties in the state of the election and are
   * waiting for the other trustees to fulfill their part of the duties.
   *
   * @param electionId identifier of election
   * @return amount of waiting trustees
   */
  public int getTrusteesWaitingCount(long electionId) {
    electionService.checkExistsElection(electionId);

    List<Trustee> trustees = (List<Trustee>) trusteeRepository.findByElectionIdAndIsWaiting(
        electionId, true);
    return trustees.size();
  }

  /**
   * Checks whether there exists a trustee with the given email in the given election.
   *
   * @param email      of trustee
   * @param electionId identifier of election
   * @return true, if trustee exists in election
   * @throws IllegalArgumentException in case email is null
   * @throws EntityNotFoundException  in case election does not exist
   */
  public boolean isTrusteeInElection(String email, long electionId) {
    electionService.checkExistsElection(electionId);
    if (email == null) {
      throw new IllegalArgumentException();
    }

    return this.trusteeRepository.existsByEmailAndElectionId(email, electionId);
  }

  /**
   * Checks whether a trustee exists with the given email.
   *
   * @param email of trustee
   */
  public void checkExistingEmail(String email) {
    if (!trusteeRepository.existsByEmail(email)) {
      throw new EntityNotFoundException("No such trustee found with given email.");
    }
  }

  /**
   * Checks whether a trustee exists with given identifier.
   *
   * @param trusteeId trustee identifier
   */
  public void checkExistingId(long trusteeId) {
    if (!trusteeRepository.existsById(trusteeId)) {
      throw new EntityNotFoundException("No such trustee was found.");
    }
  }

  private String parseElgamalKeyAndProofToString(SchnorrProofDTO[] schnorr, int order) {
    String publicKey = schnorr[order].publicKey().toString(HEXA_RADIX);
    String commitment = schnorr[order].commitment().toString(HEXA_RADIX);
    String challenge = schnorr[order].challenge().toString(HEXA_RADIX);
    String response = schnorr[order].response().toString(HEXA_RADIX);
    return order + ";" + publicKey + ";" + commitment + ";" + challenge + ";" + response;
  }
}