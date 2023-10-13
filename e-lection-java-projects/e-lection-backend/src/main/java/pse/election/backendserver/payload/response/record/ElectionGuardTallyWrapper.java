package pse.election.backendserver.payload.response.record;

import com.sunya.electionguard.AvailableGuardian;
import com.sunya.electionguard.ChaumPedersen;
import com.sunya.electionguard.CiphertextTally;
import com.sunya.electionguard.DecryptionShare;
import com.sunya.electionguard.ElGamal;
import com.sunya.electionguard.Group;
import com.sunya.electionguard.Manifest;
import com.sunya.electionguard.PlaintextTally;
import electionguard.ballot.Guardian;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pse.election.backendserver.core.electionguard.KeyCeremonyFacade;
import pse.election.backendserver.core.service.BallotService;
import pse.election.backendserver.core.service.DecryptionService;
import pse.election.backendserver.core.service.ElectionService;
import pse.election.backendserver.core.service.TallyService;
import pse.election.backendserver.core.service.TrusteeService;
import pse.election.backendserver.entity.Ballot;
import pse.election.backendserver.entity.Contest;
import pse.election.backendserver.entity.OptionEncrypted;
import pse.election.backendserver.entity.PartialDecryption;
import pse.election.backendserver.entity.PartialPartialDecryption;
import pse.election.backendserver.entity.Tally;
import pse.election.backendserver.entity.Trustee;

/**
 * This class is the election guard tally wrapper.
 * */
@Component
public class ElectionGuardTallyWrapper {

  @Autowired
  ElectionGuardManifestWrapper manifestWrapper;
  @Autowired
  ElectionGuardInitializedWrapper initializedWrapper;
  @Autowired
  KeyCeremonyFacade keyCeremonyFacade;
  @Autowired
  private BallotService ballotService;
  @Autowired
  private ElectionService electionService;
  @Autowired
  private TallyService tallyService;
  @Autowired
  private TrusteeService trusteeService;
  @Autowired
  private DecryptionService decryptionService;

  private Map<Long, Trustee> availableTrustees = new HashMap<>();
  private Map<Long, Trustee> missingTrustees = new HashMap<>();
  private Map<Long, Guardian> availableGuardians = new HashMap<>();
  private Map<Long, Guardian> missingGuardians = new HashMap<>();

  public ElectionGuardTallyWrapper() {
  }

  /**
   * Sets the id of the election the class is concerned with. It must be called before any other
   * method.
   *
   * @param electionId is the id of the election
   */
  public void generateAvailableTrustees(long electionId) {
    availableTrustees = new HashMap<>();
    availableGuardians = new HashMap<>();
    missingTrustees = new HashMap<>();
    missingGuardians = new HashMap<>();
    for (Trustee trustee : trusteeService.getAllTrustees(electionId)) {
      if (trustee.isAvailable()) {
        availableTrustees.put(trustee.getTrusteeId(), trustee);
        availableGuardians.put(trustee.getTrusteeId(),
            initializedWrapper.generateGuardianFromTrustee(trustee));
      } else {
        missingTrustees.put(trustee.getTrusteeId(), trustee);
        missingGuardians.put(trustee.getTrusteeId(),
            initializedWrapper.generateGuardianFromTrustee(trustee));
      }
    }
  }

  /**
   * Generates records of the trustees that were available for decryption.
   *
   * @return List of guardian records
   */
  public List<AvailableGuardian> generateAvailableGuardians(long electionId) {
    List<AvailableGuardian> availableGuardians = new ArrayList<>();
    for (Trustee trustee : trusteeService.getAllTrustees(electionId)) {
      if (decryptionService.getAllPartialDecryptionByTrustee(trustee.getTrusteeId()).size() > 0) {
        Guardian guardian = initializedWrapper.generateGuardianFromTrustee(trustee);
        AvailableGuardian availableGuardian = new AvailableGuardian(
            guardian.getGuardianId(),
            guardian.getXCoordinate(),
            Group.int_to_q_unchecked(trustee.getLagrangeCoefficient())
        );
        availableGuardians.add(availableGuardian);
      }
    }
    return availableGuardians;
  }

  /**
   * Generates a record of a decrypted contest from the counted tally that is compatible to an
   * ElectionGuard verifier.
   *
   * @param tallies List of encrypted options
   * @param contest the contest the encryptions are for
   * @return contest
   */
  private CiphertextTally.Contest generateCiphertextContest(List<Tally> tallies, Contest contest) {
    Map<String, CiphertextTally.Selection> selections = new HashMap<>();
    for (Tally tally : tallies) {
      Manifest.SelectionDescription description = manifestWrapper.generateSelctionDescription(
          contest, tally.getOptionIndex());
      Group.ElementModP pad = Group.int_to_p(tally.getCiphertextPAD()).orElseThrow();
      Group.ElementModP data = Group.int_to_p(tally.getCiphertextDATA()).orElseThrow();
      selections.put(description.selectionId(), new CiphertextTally.Selection(
          description.selectionId(),
          tally.getOptionIndex(),
          description.cryptoHash(),
          new ElGamal.Ciphertext(pad, data)
      ));
    }
    Manifest.ContestDescription description = manifestWrapper.generateContestDescription(contest);
    return new CiphertextTally.Contest(
        description.contestId(),
        contest.getIndex(),
        description.cryptoHash(),
        selections
    );
  }

  /**
   * Generates a record of the encrypted result of the election.
   *
   * @return ciphertext tally
   */
  public CiphertextTally generateCiphertextTally(long electionId) {
    generateAvailableTrustees(electionId);
    Map<Integer, List<Tally>> talliesByIndex = new HashMap<>();
    List<Contest> allContests = electionService.getAllContestsOfElection(electionId);
    Map<String, CiphertextTally.Contest> cipherContests = new HashMap<>();
    for (Contest contest : allContests) {
      talliesByIndex.put(contest.getIndex(), new ArrayList<>());
    }
    for (Tally tally : tallyService.getAllTalliesOfElection(electionId)) {
      talliesByIndex.get(tally.getContestIndex()).add(tally);
    }
    for (Contest contest : allContests) {
      cipherContests.put(manifestWrapper.generateContestDescription(contest).contestId(),
          generateCiphertextContest(talliesByIndex.get(contest.getIndex()), contest));
    }
    return new CiphertextTally(
        "0",
        cipherContests
    );
  }

  /**
   * Generates Decryption share record for all trustees for an option.
   *
   * @param contest     the contest the option is for
   * @param optionIndex the index of the option
   * @param shares      the decryption shares of the available trustees
   * @param ppshares    the decryption compensations of the available trustee
   * @return list containing the ciphertext decryption selection
   */
  @SuppressWarnings("checkstyle:Indentation")
  private List<DecryptionShare.CiphertextDecryptionSelection>
  getDecryptionShares(Contest contest, int optionIndex, Map<Long, PartialDecryption> shares,
      Map<Long, List<PartialPartialDecryption>> ppshares) {
    List<DecryptionShare.CiphertextDecryptionSelection> originalShares = new ArrayList<>();
    Manifest.SelectionDescription selection = manifestWrapper.generateSelctionDescription(contest,
        optionIndex);
    //First Key: missing guardian, second key: available guardian
    Map<Long, Map<String, DecryptionShare.CiphertextCompensatedDecryptionSelection>> allCompensation = new HashMap<>();
    Map<Long, Map<Long, PartialPartialDecryption>> ppDecryption = new HashMap<>();
    Map<Long, Map<Long, Group.ElementModP>> ppElements = new HashMap<>();
    for (Trustee trustee : missingTrustees.values()) {
      allCompensation.put(trustee.getTrusteeId(), new HashMap<>());
      ppDecryption.put(trustee.getTrusteeId(), new HashMap<>());
      ppElements.put(trustee.getTrusteeId(), new HashMap<>());
    }
    for (Trustee trustee : availableTrustees.values()) {
      PartialDecryption share = shares.get(trustee.getTrusteeId());
      Guardian availableGuardian = availableGuardians.get(trustee.getTrusteeId());
      originalShares.add(new DecryptionShare.CiphertextDecryptionSelection(
          selection.selectionId(),
          availableGuardian.getGuardianId(),
          Group.int_to_p_unchecked(share.getDecryption()),
          Optional.of(generateProof(share.getProof())),
          Optional.empty()
      ));
      for (PartialPartialDecryption compensation : ppshares.get(trustee.getTrusteeId())) {
        long availableTrustee = trustee.getTrusteeId();
        long missingTrustee = compensation.getForWhichTrusteeId();
        //Ignoring Partial Partial decryptions for trustees that are not missing
        if (!missingTrustees.containsKey(missingTrustee)) {
          continue;
        }
        Guardian missingGuardian = missingGuardians.get(missingTrustee);
        ppDecryption.get(missingTrustee).put(availableTrustee, compensation);
        Group.ElementModP element = Group.hex_to_p_unchecked(compensation.getDecryption());
        ppElements.get(missingTrustee).put(availableTrustee, element);
        List<BigInteger> coefficients = new ArrayList<>();
        missingGuardian.getCoefficientCommitments().forEach(p -> coefficients.add(p.getBigInt()));
        allCompensation.get(missingTrustee).put(availableGuardian.getGuardianId(),
            new DecryptionShare.CiphertextCompensatedDecryptionSelection(
                selection.selectionId(),
                availableGuardians.get(availableTrustee).getGuardianId(),
                missingGuardians.get(missingTrustee).getGuardianId(),
                element,
                Group.int_to_p_unchecked(keyCeremonyFacade.generateKeyShare(coefficients,
                    missingGuardian.getXCoordinate())),
                generateProof(compensation.getProof())
            ));
      }
    }

    for (Trustee missingTrustee : missingTrustees.values()) {
      originalShares.add(new DecryptionShare.CiphertextDecryptionSelection(
          selection.selectionId(),
          missingGuardians.get(missingTrustee.getTrusteeId()).getGuardianId(),
          generatePartialDecryption(ppElements.get(missingTrustee.getTrusteeId())),
          Optional.empty(),
          Optional.of(allCompensation.get(missingTrustee.getTrusteeId()))
      ));
    }
    return originalShares;
  }

  /**
   * Generates a record of the decryption of an option.
   *
   * @param tally          the tally the decryption is for
   * @param contest        the contest the tally was for
   * @param result         the decrypted result
   * @param selectionIndex the index of the option
   * @return selection
   */
  private PlaintextTally.Selection generatePlaintextSelection(Tally tally, Contest contest,
      int result, int selectionIndex) {
    Group.ElementModP pad = Group.int_to_p(tally.getCiphertextPAD()).orElseThrow();
    Group.ElementModP data = Group.int_to_p(tally.getCiphertextDATA()).orElseThrow();
    Map<Long, PartialDecryption> pshares = new HashMap<>();
    Map<Long, List<PartialPartialDecryption>> ppshares = new HashMap<>();
    for (Trustee trustee : availableTrustees.values()) {
      pshares.put(trustee.getTrusteeId(),
          decryptionService.getPartialDecryptionOfTrustee(trustee.getTrusteeId(),
              tally.getTallyId()));
      ppshares.put(trustee.getTrusteeId(),
          decryptionService.getAllPartialPartialDecryptionOfTrustee(trustee.getTrusteeId(),
              tally.getTallyId()));
    }
    List<DecryptionShare.CiphertextDecryptionSelection> decryptionShares = getDecryptionShares(
        contest, tally.getOptionIndex(), pshares, ppshares);
    Manifest.SelectionDescription selection = manifestWrapper.generateSelctionDescription(contest,
        selectionIndex);
    return new PlaintextTally.Selection(
        selection.selectionId(),
        result,
        Group.g_pow_p(Group.int_to_q_unchecked(result)),
        new ElGamal.Ciphertext(pad, data),
        decryptionShares
    );
  }


  /**
   * Generates a record for the decryption of a contest.
   *
   * @param contest the contest the decryption is for
   * @param tallies the encrypted tallies for all the options
   * @param results the decryptions of the tallies
   * @return contest
   */
  private PlaintextTally.Contest generatePlaintextContest(Contest contest, List<Tally> tallies,
      Integer[] results) {
    Map<String, PlaintextTally.Selection> plaintextSelections = new HashMap<>();
    Manifest.ContestDescription description = manifestWrapper.generateContestDescription(contest);
    for (Tally tally : tallies) {
      Manifest.SelectionDescription selection = manifestWrapper.generateSelctionDescription(contest,
          tally.getOptionIndex());
      plaintextSelections.put(selection.selectionId(),
          generatePlaintextSelection(tally, contest, results[tally.getOptionIndex()],
              tally.getOptionIndex()));
    }
    return new PlaintextTally.Contest(
        description.contestId(),
        plaintextSelections
    );
  }

  /**
   * Generates a record for the decryption of the election.
   *
   * @return plaintext tally
   */
  public PlaintextTally generatePlaintextTally(long electionId) {
    generateAvailableTrustees(electionId);
    List<Tally> tallies = tallyService.getAllTalliesOfElection(electionId);
    List<Contest> contests = electionService.getAllContestsOfElection(electionId);
    Map<String, PlaintextTally.Contest> plaintextContests = new HashMap<>();
    Map<Integer, List<Tally>> tallyMap = new HashMap<>();
    Map<Integer, Contest> contestMap = new HashMap<>();
    Map<Integer, Integer[]> results = electionService.getDecryptedResult(electionId);
    for (Contest contest : contests) {
      contestMap.put(contest.getIndex(), contest);
      tallyMap.put(contest.getIndex(), new ArrayList<>());
    }
    for (Tally tally : tallies) {
      Contest contest = contestMap.get(tally.getContestIndex());
      if (tally.getOptionIndex() < contest.getOptions().size()) {
        tallyMap.get(tally.getContestIndex()).add(tally);
      }
    }
    for (Contest contest : contests) {
      Manifest.ContestDescription description = manifestWrapper.generateContestDescription(contest);
      plaintextContests.put(description.contestId(),
          generatePlaintextContest(contest, tallyMap.get(contest.getIndex()),
              results.get(contest.getIndex())));
    }
    return new PlaintextTally(
        "0",
        plaintextContests
    );
  }

  /**
   * Generates a record of a decrypted option from a spoiled ballot.
   *
   * @param option  the ciphertext the decryption is for
   * @param contest the contest the decryption is for
   * @param result  the decryption result
   * @return selection
   */
  @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
  private PlaintextTally.Selection generatePlaintextSelectionForSpoiled(OptionEncrypted option,
                                                                        Contest contest, int result) {
    Group.ElementModP pad = Group.int_to_p(option.getCiphertextPAD()).orElseThrow();
    Group.ElementModP data = Group.int_to_p(option.getCiphertextDATA()).orElseThrow();
    Map<Long, PartialDecryption> pshares = new HashMap<>();
    Map<Long, List<PartialPartialDecryption>> ppshares = new HashMap<>();

    for (Trustee trustee : availableTrustees.values()) {
      ppshares.put(trustee.getTrusteeId(), new ArrayList<>());
    }

    for (PartialDecryption decryption : decryptionService.getAllPartialDecryptionOfSpoiledBallotOption(
        option.getBallotId(), contest.getIndex(), option.getOptionIndex())) {
      pshares.put(decryption.getTrusteeId(), decryption);
    }
    for (PartialPartialDecryption decryption : decryptionService.getAllPartialPartialDecryptionOfSpoiledBallotOption(
        option.getBallotId(), contest.getIndex(), option.getOptionIndex())) {
      ppshares.get(decryption.getTrusteeId()).add(decryption);
    }
    List<DecryptionShare.CiphertextDecryptionSelection> decryptionShares = getDecryptionShares(
        contest, option.getOptionIndex(), pshares, ppshares);
    Manifest.SelectionDescription selection = manifestWrapper.generateSelctionDescription(contest,
        option.getOptionIndex());
    return new PlaintextTally.Selection(
        selection.selectionId(),
        result,
        Group.g_pow_p(Group.int_to_q_unchecked(result)),
        new ElGamal.Ciphertext(pad, data),
        decryptionShares
    );
  }

  /**
   * Generates a record for a decryption of a contest from a spoiled ballot.
   *
   * @param contest the contest the decryptions are for
   * @param options the encrypted options of the contest
   * @param results the decryption results of the contest
   * @return contest
   */
  private PlaintextTally.Contest generatePlaintextContestForSpoiled(Contest contest,
      List<OptionEncrypted> options, List<Integer> results) {
    Map<String, PlaintextTally.Selection> plaintextSelections = new HashMap<>();
    Manifest.ContestDescription description = manifestWrapper.generateContestDescription(contest);
    for (OptionEncrypted option : options) {
      Manifest.SelectionDescription selection = manifestWrapper.generateSelctionDescription(contest,
          option.getOptionIndex());
      plaintextSelections.put(selection.selectionId(),
          generatePlaintextSelectionForSpoiled(option, contest,
              results.get(option.getOptionIndex())));
    }
    return new PlaintextTally.Contest(
        description.contestId(),
        plaintextSelections
    );
  }

  /**
   * Generates records for the decryption of all spoiled ballots in the election.
   *
   * @return list of plain text tally
   */
  public List<PlaintextTally> generateAllSpoiledBallots(long electionId) {
    generateAvailableTrustees(electionId);
    List<PlaintextTally> spoiledBallots = new ArrayList<>();
    for (Ballot ballot : ballotService.getAllSpoiledBallotsOfElection(electionId)) {
      spoiledBallots.add(generateSpoiledBallot(electionId, ballot));
    }
    return spoiledBallots;
  }

  /**
   * Generates a record for the decryption of a spoiled ballot.
   *
   * @param ballot the spoiled ballot
   * @return plaintext tally
   */
  public PlaintextTally generateSpoiledBallot(long electionId, Ballot ballot) {
    List<OptionEncrypted> options = ballotService.getAllOptionsEncryptedOfBallot(
        ballot.getBallotId());
    Map<Integer, List<OptionEncrypted>> optionsByContest = new HashMap<>();
    Map<Integer, Contest> contestByIndex = new HashMap<>();
    List<Contest> contests = electionService.getAllContestsOfElection(electionId);
    Map<String, PlaintextTally.Contest> electionguardContests = new HashMap<>();
    Map<Integer, List<Integer>> results = new HashMap<>();
    for (Contest contest : contests) {
      contestByIndex.put(contest.getIndex(), contest);
      optionsByContest.put(contest.getIndex(), new ArrayList<>());
      results.put(contest.getIndex(), new ArrayList<>());
    }

    for (OptionEncrypted option : options) {
      optionsByContest.get(option.getContestIndex()).add(option);
      results.get(option.getContestIndex()).add(option.isSelectionMade() ? 1 : 0);
    }

    for (Contest contest : contests) {
      Manifest.ContestDescription description = manifestWrapper.generateContestDescription(contest);
      electionguardContests.put(description.contestId(),
          generatePlaintextContestForSpoiled(contest, optionsByContest.get(contest.getIndex()),
              results.get(contest.getIndex())));
    }
    return new PlaintextTally(
        //Assumes ElectionGuard ballotId equals e-lection Ballot ID - otherwise use ballotWrapper
        String.valueOf(ballot.getBallotId()),
        electionguardContests
    );
  }

  /**
   * Combines partial partial decryptions to a partial decryption.
   *
   * @param partialPartialDecryptions all decryption compensations for a option for a missing
   *                                  spoiled ballot
   * @return element mod p
   */
  @SuppressWarnings("checkstyle:LocalVariableName")
  private Group.ElementModP generatePartialDecryption(
      Map<Long, Group.ElementModP> partialPartialDecryptions) {
    List<Group.ElementModP> share_pow_p = new ArrayList<>();
    for (Map.Entry<Long, Group.ElementModP> entry : partialPartialDecryptions.entrySet()) {
      Trustee availableTrustee = availableTrustees.get(entry.getKey());
      Group.ElementModQ c = Group.int_to_q_unchecked(availableTrustee.getLagrangeCoefficient());
      Group.ElementModP pp = entry.getValue();
      Group.ElementModP p = Group.pow_p(pp, c);
      share_pow_p.add(p);
    }

    // product M_il^w_l
    Group.ElementModP reconstructed_share = Group.mult_p(share_pow_p);
    return reconstructed_share;
  }

  /**
   * Decodes a Chaum-Pedersen proof from the database representation.
   *
   * @param json is the json
   * @return chaum pederson proof
   */
  private ChaumPedersen.ChaumPedersenProof generateProof(String json) {
    String[] elems = json.split(";");
    Group.ElementModP pad = Group.hex_to_p_unchecked(elems[0]);
    Group.ElementModP data = Group.hex_to_p_unchecked(elems[1]);
    Group.ElementModQ challemge = Group.hex_to_q(elems[2]).orElseThrow();
    Group.ElementModQ response = Group.hex_to_q(elems[3]).orElseThrow();
    return new ChaumPedersen.ChaumPedersenProof(pad, data, challemge, response);
  }
}

