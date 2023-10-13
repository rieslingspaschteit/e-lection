package pse.election.backendserver.core.electionguard;

import com.sunya.electionguard.ChaumPedersen;
import com.sunya.electionguard.Dlog;
import com.sunya.electionguard.ElGamal;
import com.sunya.electionguard.ElectionPolynomial;
import com.sunya.electionguard.Group;
import com.sunya.electionguard.Group.ElementModP;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import pse.election.backendserver.core.service.BallotService;
import pse.election.backendserver.core.service.DecryptionService;
import pse.election.backendserver.core.service.ElectionService;
import pse.election.backendserver.core.service.TallyService;
import pse.election.backendserver.core.service.TrusteeService;
import pse.election.backendserver.entity.Ballot;
import pse.election.backendserver.entity.Contest;
import pse.election.backendserver.entity.Election;
import pse.election.backendserver.entity.OptionEncrypted;
import pse.election.backendserver.entity.PartialDecryption;
import pse.election.backendserver.entity.PartialPartialDecryption;
import pse.election.backendserver.entity.Tally;
import pse.election.backendserver.entity.Trustee;
import pse.election.backendserver.payload.error.exception.EntityNotFoundException;


/**
 * Facade for the electionGuard java implementation. Implements the interface IDecryption. Uses the
 * decryption tools of the electionGuard java implementation to combine encryption's and to decrypt
 * them.
 */
@Component
public class DecryptionFacade {

  public static final int MAX_ACCUMULATION = 200000;
  private static final int BIG_INTEGER_RADIX = 16;
  private static final String TRUSTEE_NOT_FOUND_ERROR_MSG = "Trustee could not be found.";
  // Default Ciphertext in case no ballots have been cast.
  // It is equal to the value 0 decrypted with nonce 0
  private static final ElGamal.Ciphertext DEFAULT_ZERO = new ElGamal.Ciphertext(Group.ONE_MOD_P,
      Group.ONE_MOD_P);

  private final BallotService ballotService;

  private final ElectionService electionService;

  private final TallyService tallyService;

  private final DecryptionService decryptionService;

  private final TrusteeService trusteeService;

  /**
   * Constructor of new DecryptionFacade.
   * */
  @Lazy
  public DecryptionFacade(BallotService ballotService, ElectionService electionService,
      TallyService tallyService, DecryptionService decryptionService,
      TrusteeService trusteeService) {
    this.ballotService = ballotService;
    this.electionService = electionService;
    this.tallyService = tallyService;
    this.decryptionService = decryptionService;
    this.trusteeService = trusteeService;
  }

  /**
   * Combines the corresponding OptionEncrypted to generate a Tally for an Option. By doing so, all
   * Pads and Data's of an Option get Homomorphic combined.
   *
   * @param election is the election to combine the encrypted options for
   */
  public void combineOptionsEncryptedToTallies(Election election) {
    List<Contest> sortedListOfContest = electionService.getAllContestsOfElection(
        election.getElectionId());
    Collections.sort(sortedListOfContest);

    for (Contest contest : sortedListOfContest) {
      convertOptionEncryptedToTally(contest, election);
    }
  }

  /**
   * Creates the Tallies for all Options of a specific Contest. By doing so it combines the
   * OptionEncrypted for each method.
   *
   * @param contest  the contest for which the tallies get created
   * @param election the election for which the tallies get created
   */
  private void convertOptionEncryptedToTally(Contest contest, Election election) {
    for (int optionIndex = 0; optionIndex < contest.getOptions().size() + contest.getMax();
        optionIndex++) {
      List<OptionEncrypted> optionEncryptedList = ballotService
          .getAllOptionEncryptedOfContestAndOption(
              election.getElectionId(),
              contest.getIndex(),
              optionIndex
          );

      ElGamal.Ciphertext[] ciphertextsForOptionI
          = new ElGamal.Ciphertext[optionEncryptedList.size()];

      for (int optionEncryptedIndex = 0; optionEncryptedIndex < optionEncryptedList.size();
          optionEncryptedIndex++) {
        ciphertextsForOptionI[optionEncryptedIndex] = new ElGamal.Ciphertext(
            Group.int_to_p_unchecked(
                optionEncryptedList.get(optionEncryptedIndex).getCiphertextPAD()),
            Group.int_to_p_unchecked(
                optionEncryptedList.get(optionEncryptedIndex).getCiphertextDATA()));
      }

      if (ciphertextsForOptionI.length == 0) {
        ciphertextsForOptionI = new ElGamal.Ciphertext[]{DEFAULT_ZERO};
      }

      Tally tally = new Tally(election.getElectionId(), contest.getIndex(), optionIndex);
      ElGamal.Ciphertext combinedElGamalCiphertext = ElGamal.elgamal_add(ciphertextsForOptionI);

      tally.setCiphertextPAD(combinedElGamalCiphertext.pad().base16());
      tally.setCiphertextDATA(combinedElGamalCiphertext.data().base16());
      tallyService.addTally(tally);
    }
  }

  /**
   * Evaluated the Result of an Election. First, if PartialPartial Descriptions are available, it
   * combines them and then generates the Result for each value. Does this for the Election and for
   * all SpoiledBallots
   *
   * @param election the election for which the results gets evaluated
   */
  public void evaluateResult(Election election) {
    Map<Integer, Integer[]> results = reconstructElectionShares(election);
    electionService.setResult(election.getElectionId(), results);
    Map<Long, Map<Integer, Integer[]>> resultsSpoiledBallots = reconstructedSpoiledBallotShares(
        election);
    ballotService.saveDecryptedSpoiledBallot(resultsSpoiledBallots);
  }

  /**
   * Gets all Partial and PartialPartial Decryptions of an Election by Option, generates the
   * PartialDecryption out of all corresponding PartialPartial Decryptions and combines them.
   *
   * @param election is the election to reconstruct the election shares
   * @return map containing the decrypted result to each option of each contest
   */
  private Map<Integer, Integer[]> reconstructElectionShares(Election election) {
    List<Contest> contestList = electionService.getAllContestsOfElection(election.getElectionId());
    Map<Integer, Integer[]> combinedResultForContests = new HashMap<>();
    for (Contest contest : contestList) {
      Integer[] combinedResultOption = new Integer[contest.getOptions().size() + contest.getMax()];
      for (int i = 0; i < contest.getOptions().size() + contest.getMax(); i++) {
        List<PartialDecryption> shares =
            reconstructPartialDecryption(
                decryptionService.getAllPartialDecryptionOfTally(election.getElectionId(),
                    contest.getIndex(), i),
                decryptionService.getAllPartialPartialDecryptionOfTally(election.getElectionId(),
                    contest.getIndex(), i),
                election);
        Group.ElementModP data = Group.int_to_p_unchecked(
            tallyService.getSpecificTally(election.getElectionId(), contest.getIndex(), i)
                .getCiphertextDATA());
        combinedResultOption[i] = combineShares(shares, data);
      }
      combinedResultForContests.put(contest.getIndex(), combinedResultOption);
    }
    return combinedResultForContests;
  }

  /**
   * Reconstructs the missing PartialDecryption by combining the ParitalPartialDecryption of this
   * Option.
   *
   * @param partialDecryptions                         PartialDecryption for an Option
   * @param partialPartialDecryptionOfOptionForTrustee PartialPartial Decryption for the Option
   * @param election                                   Election of which the Option is
   * @return all the initiale Partial Decryption plus the newly created PartialDecryptions
   */
  private List<PartialDecryption> reconstructPartialDecryption(
      List<PartialDecryption> partialDecryptions,
      List<PartialPartialDecryption> partialPartialDecryptionOfOptionForTrustee,
      Election election) {
    List<Trustee> trusteeList = trusteeService.getAllTrustees(election.getElectionId());
    List<Trustee> missingTrusteeList = new ArrayList<>();
    List<Trustee> availableTrusteeList = new ArrayList<>();
    List<PartialDecryption> shares = new ArrayList<>(partialDecryptions);

    for (Trustee trustee : trusteeList) {
      if (!trustee.isAvailable()) {
        missingTrusteeList.add(trustee);
      } else {
        availableTrusteeList.add(trustee);
      }
    }

    if (!missingTrusteeList.isEmpty()) {
      shares.addAll(reconstructPartialPartialDecryptionOfMissingTrustees(
              missingTrusteeList,
              availableTrusteeList,
              partialPartialDecryptionOfOptionForTrustee
          )
      );
    }

    return shares;
  }

  /**
   * Reconstructs the missing PartialDecryptions for the missing Trustees.
   *
   * @param missingTrusteeList   trustees that didn't attend the decrpytion
   * @param availableTrusteeList trustees that attended the decryption
   * @param ppDecryptionOfOption List with PartialPartialDecryptions for the missing Trustees
   * @return List with old PartialDecryptions and newly created PartialDecryptions
   */
  private List<PartialDecryption> reconstructPartialPartialDecryptionOfMissingTrustees(
      List<Trustee> missingTrusteeList, List<Trustee> availableTrusteeList,
      List<PartialPartialDecryption> ppDecryptionOfOption) {
    List<PartialDecryption> shares = new ArrayList<>();

    for (Trustee missingTrustee : missingTrusteeList) {
      List<PartialPartialDecryption> ppDecryptionForTrustee
          = collectPartialPartialDecryptionOfOptionForTrustee(
              ppDecryptionOfOption, missingTrustee.getTrusteeId());

      List<Group.ElementModP> sharePowP = collectShareOfPowP(ppDecryptionForTrustee,
          availableTrusteeList);
      Group.ElementModP reconstructedShare = Group.mult_p(sharePowP);

      PartialDecryption partialDecryption = new PartialDecryption(reconstructedShare.toString(),
          missingTrustee.getTrusteeId());
      shares.add(partialDecryption);
    }

    return shares;
  }

  /**
   * Reconstructs the shares with the specific LagrangeCoefficient.
   *
   * @param ppDecryptionForTrustee partialDecryption for an Option
   * @param availableTrusteeList   trustees which attend the reconstruction
   * @return the list with the generated shares
   */
  private List<Group.ElementModP> collectShareOfPowP(
      List<PartialPartialDecryption> ppDecryptionForTrustee, List<Trustee> availableTrusteeList) {
    List<Group.ElementModP> sharePowP = new ArrayList<>();
    for (PartialPartialDecryption partialPartialDecryption : ppDecryptionForTrustee) {
      Trustee correspondingTrustee = availableTrusteeList
          .stream()
          .filter(trustee -> trustee.getTrusteeId() == partialPartialDecryption.getTrusteeId())
          .findAny()
          .orElseThrow(() -> new EntityNotFoundException(TRUSTEE_NOT_FOUND_ERROR_MSG));

      sharePowP.add(Group.pow_p(
          Group.int_to_p_unchecked(
              new BigInteger(partialPartialDecryption.getDecryption(), BIG_INTEGER_RADIX)),
          Group.int_to_p_unchecked(correspondingTrustee.getLagrangeCoefficient()))
      );
    }
    return sharePowP;
  }

  /**
   * Filters the needed PartialPartialDecryptions for a missing Trustee.
   *
   * @param ppDecryptionOfOption the PartialPartialDecryption that got generated for the trustee
   * @param missingTrusteeId     the id of the trustee
   * @return the list of all PartialPartialDecryptions
   */
  private List<PartialPartialDecryption> collectPartialPartialDecryptionOfOptionForTrustee(
      List<PartialPartialDecryption> ppDecryptionOfOption, long missingTrusteeId) {

    List<PartialPartialDecryption> ppDecryptionForTrustee = new ArrayList<>();
    for (PartialPartialDecryption partialPartialDecryption : ppDecryptionOfOption) {
      if (partialPartialDecryption.getForWhichTrusteeId() == missingTrusteeId) {
        ppDecryptionForTrustee.add(partialPartialDecryption);
      }
    }

    return ppDecryptionForTrustee;
  }

  /**
   * Compute the lagrange coefficient for a specific trusteeIndex against #available Trustees - 1
   * degrees Does this for all available Trustees. Compute the lagrange coefficient for a specific
   * trusteeIndex against #availableTrustees-1 degrees. Does this for all Trustees.
   *
   * @param election Election for which the LagrangeCoefficients get generated.
   */
  public void computeLagrangeCoefficients(Election election) {
    List<Trustee> trusteeList = trusteeService.getAllTrustees(election.getElectionId()).stream()
        .filter(Trustee::isAvailable).toList();
    List<Integer> trusteeIndexList = new ArrayList<>();
    trusteeList.forEach(t -> trusteeIndexList.add(t.getIndex()));
    for (Trustee trustee : trusteeList) {
      trusteeIndexList.remove((Integer) trustee.getIndex());

      Group.ElementModQ lagrangeCoefficient = ElectionPolynomial
          .compute_lagrange_coefficient(trustee.getIndex(), trusteeIndexList);
      trustee.setLagrangeCoefficient(lagrangeCoefficient.toString());
      trusteeService.addTrustee(trustee);
      trusteeIndexList.add(trustee.getIndex());
    }
  }

  /**
   * Generates the combination of all PartialDecryption of an Option.
   *
   * @param partialDecryptions the PartialDecryptions
   * @param data               the corresponding encrypted data
   * @return the decrypted value, so the total votes for this Option
   */
  private Integer combineShares(List<PartialDecryption> partialDecryptions,
      Group.ElementModP data) {
    Collection<ElementModP> shares = new ArrayList<>();
    for (PartialDecryption partialDecryption : partialDecryptions) {
      shares.add(Group.int_to_p_unchecked(partialDecryption.getDecryption()));
    }
    Group.ElementModP allSharesProduct = Group.mult_p(shares);
    Group.ElementModP decryptedValue = Group.div_p(data, allSharesProduct);
    Dlog.setMax(MAX_ACCUMULATION);
    return Dlog.discrete_log(decryptedValue);
  }

  /**
   * Collects all PartialDecryption for all Spoiled Ballot and if necessary combines the
   * PartialPartial to ParitalDecrypitons.
   *
   * @param election the election which gets decrypted
   * @return Map with all results of each spoiled Ballot
   */
  private Map<Long, Map<Integer, Integer[]>> reconstructedSpoiledBallotShares(Election election) {
    List<Contest> contestList = electionService.getAllContestsOfElection(election.getElectionId());
    List<Ballot> spoiledBallot = ballotService.getAllSpoiledBallotsOfElection(
        election.getElectionId());
    Map<Long, Map<Integer, Integer[]>> output = new HashMap<>();

    for (Ballot ballot : spoiledBallot) {
      Map<Integer, Integer[]> combinedResultForContests = new HashMap<>();
      for (Contest contest : contestList) {
        Integer[] combinedResultOption = collectCombinedResultOption(contest, ballot, election);
        combinedResultForContests.put(contest.getIndex(), combinedResultOption);
      }
      output.put(ballot.getBallotId(), combinedResultForContests);
    }
    return output;
  }

  /**
   * Collects all Partial and PartialPartial Decryption for a Ballot and a specific Contest.
   *
   * @param contest  the contest
   * @param ballot   the ballot
   * @param election the election for which the results gets evaluated
   * @return the results mapped to their optionIndex
   */
  private Integer[] collectCombinedResultOption(Contest contest, Ballot ballot, Election election) {
    Integer[] combinedResultOption = new Integer[contest.getOptions().size()];
    for (int optionIndex = 0; optionIndex < contest.getOptions().size(); optionIndex++) {
      List<PartialDecryption> shares = reconstructPartialDecryption(
          decryptionService.getAllPartialDecryptionOfSpoiledBallotOption(
              ballot.getBallotId(),
              contest.getIndex(),
              optionIndex
          ),
          decryptionService.getAllPartialPartialDecryptionOfSpoiledBallotOption(
              ballot.getBallotId(),
              contest.getIndex(),
              optionIndex
          ),
          election
      );
      Group.ElementModP data = Group.int_to_p_unchecked(ballotService.getSpecificOptionOfBallot(
          ballot.getBallotId(),
          contest.getIndex(),
          optionIndex
      ).getCiphertextDATA());

      combinedResultOption[optionIndex] = combineShares(shares, data);
    }
    return combinedResultOption;
  }

  /**
   * Combines encryption's by homomorphically adding them together.
   *
   * @param decryption the election for which the encryption's get combined
   */
  public record Decryption(Group.ElementModP decryption,
                           ChaumPedersen.ChaumPedersenProof chaumPedersenProof) {

  }

}