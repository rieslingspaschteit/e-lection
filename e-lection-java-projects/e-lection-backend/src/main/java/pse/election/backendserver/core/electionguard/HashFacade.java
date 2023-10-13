package pse.election.backendserver.core.electionguard;

import com.sunya.electionguard.ElGamal;
import com.sunya.electionguard.Group;
import com.sunya.electionguard.Hash;
import com.sunya.electionguard.Manifest;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import pse.election.backendserver.core.service.ElectionService;
import pse.election.backendserver.core.service.TrusteeService;
import pse.election.backendserver.core.service.VoterService;
import pse.election.backendserver.entity.Ballot;
import pse.election.backendserver.entity.Contest;
import pse.election.backendserver.entity.Election;
import pse.election.backendserver.entity.OptionEncrypted;
import pse.election.backendserver.entity.Trustee;
import pse.election.backendserver.entity.Voter;
import pse.election.backendserver.payload.dto.ElectionDescriptionHashesDTO;
import pse.election.backendserver.payload.response.record.ElectionGuardInitializedWrapper;
import pse.election.backendserver.payload.response.record.ElectionGuardManifestWrapper;

/**
 * Facade for the electionGuard java implementation. Implements the interface Hashable. Uses the
 * hash  tools of the electionGuard java implementation to generate the TrackingCode and the
 * fingerprint.
 */
@Component
public class HashFacade {

  private static final int BIG_INTEGER_RADIX = 16;

  private final ElectionService electionService;

  private final TrusteeService trusteeService;

  private final VoterService voterService;

  private final ElectionGuardManifestWrapper electionGuardManifestWrapper;

  private final ElectionGuardInitializedWrapper electionGuardInitializedWrapper;

  /**
   * Constructor of new HashFacade.
   * */
  @Lazy
  public HashFacade(ElectionService electionService, TrusteeService trusteeService,
      VoterService voterService, ElectionGuardManifestWrapper electionGuardManifestWrapper,
      ElectionGuardInitializedWrapper electionGuardInitializedWrapper) {
    this.electionService = electionService;
    this.trusteeService = trusteeService;
    this.voterService = voterService;
    this.electionGuardManifestWrapper = electionGuardManifestWrapper;
    this.electionGuardInitializedWrapper = electionGuardInitializedWrapper;
  }


  /**
   * Hashes a ciphertext, the date and the election fingerprint and by doing so generating the
   * trackingCode. Also updates the latest Fingerprint of an election for keeping the hash chain
   * intact and to rule a race condition.
   *
   * @param election the election for which the trackingCode gets generated. Will especially use the
   *                 election fingerprint
   */
  public void generateTrackingCode(List<OptionEncrypted> optionEncryptedToBallot, Election election,
      Ballot ballot) {
    ElectionDescriptionHashesDTO electionDescriptionHashesDTO = generateElectionHashes(election);
    String cryptoHash = generateCryptoHash(electionDescriptionHashesDTO, optionEncryptedToBallot,
        election, ballot);
    setTrackingCode(ballot.getEncryptionDate().getTime(), cryptoHash, election, ballot);

  }

  /**
   * Setter for the TrackingCode of a Ballot. Synchronized so that the Hash chain stay intact. Do so
   * by updating the latest TrackingCode with the new one. Uses the extendedBaseHash if the
   * generated TrackingCode is the first.
   *
   * @param timestamp  the time when the ballot was encrypted
   * @param cryptoHash the crypto Hash over the election and the EncryptedOptions
   * @param election   the election for which the TrackingCode gets set
   * @param ballot     the ballot that the trackingCode got generated for
   */
  private synchronized void setTrackingCode(long timestamp, String cryptoHash, Election election,
      Ballot ballot) {
    BigInteger latestTrackingCode;

    if (electionService.getLatestTrackingCode(election.getElectionId()) == null) {
      latestTrackingCode = new BigInteger(electionGuardInitializedWrapper
          .generateCryptoContext(election.getElectionId()).cryptoExtendedBaseHash.base16(),
          BIG_INTEGER_RADIX);
    } else {
      latestTrackingCode = new BigInteger(
          electionService.getLatestTrackingCode(election.getElectionId()), BIG_INTEGER_RADIX);
    }

    BigInteger newTrackingCode = Hash.hash_elems(Group.int_to_q_unchecked(latestTrackingCode),
        timestamp, cryptoHash).getBigInt();
    electionService.updateLatestTrackingCode(election.getElectionId(),
        newTrackingCode.toString(BIG_INTEGER_RADIX));
    ballot.setLatestTrackingCode(newTrackingCode.toString(BIG_INTEGER_RADIX));
    ballot.setPreviousTrackingCode(latestTrackingCode.toString(BIG_INTEGER_RADIX));
  }

  /**
   * Generates the crypto Hashes which, with the timestamp and latestTrackingCode gets hashes into a
   * new TrackingCode.
   *
   * @param electionDescriptionHashesDTO Dto with hashes from the electionGuard Java project
   * @param optionEncryptedList          List of all OptionEncrypted to the ballot for which the
   *                                     trackingCode gets generated
   * @param election                     the election for which the cryptoHash gets generated
   * @param ballot                       the ballot for which the cryptoHash gets generated
   * @return the newly generated cryptoHash
   */
  public String generateCryptoHash(
      @NotNull ElectionDescriptionHashesDTO electionDescriptionHashesDTO,
      List<OptionEncrypted> optionEncryptedList,
      Election election, Ballot ballot) {

    List<Contest> contestList = electionService.getAllContestsOfElection(election.getElectionId());
    Collections.sort(optionEncryptedList);
    Collections.sort(contestList);
    List<Object> listOfContestHashes = new ArrayList<>();
    listOfContestHashes.add(ballot.getBallotIdForEncryption());
    listOfContestHashes.add(electionDescriptionHashesDTO.manifestHash());
    for (Contest contest : contestList) {
      generateCryptoHashForContest(electionDescriptionHashesDTO, contest,
          optionEncryptedList, listOfContestHashes);
    }
    return Hash.hash_elems(listOfContestHashes.toArray()).toString();
  }

  /**
   * Generates the cryptoHash for a specific Contest.
   *
   * @param electionDescriptionHashesDTO the Dto containing all the Hashes for an election
   * @param contest                      contest for which the hash gets created
   * @param optionEncryptedList          list of OptionEncrypted for the Hash
   * @param listOfContestHashes          list for saving all the contestHashes
   */
  public void generateCryptoHashForContest(
      @NotNull ElectionDescriptionHashesDTO electionDescriptionHashesDTO,
      Contest contest, List<OptionEncrypted> optionEncryptedList,
      List<Object> listOfContestHashes) {
    int sizeOfContest = contest.getOptions().size() + contest.getMax();
    OptionEncrypted[] optionsForContest =
        getOptionsForContext(optionEncryptedList, contest.getIndex(), sizeOfContest);
    String[] optionDescriptionHashes = electionDescriptionHashesDTO.optionDescriptionHashes()
        .get(contest.getIndex());
    String[] optionDescriptionIds = electionDescriptionHashesDTO.optionIds()
        .get(contest.getIndex());
    List<Object> optionHashes = new ArrayList<>();
    optionHashes.add(electionDescriptionHashesDTO.contestIds().get(contest.getIndex()));
    optionHashes.add(
        electionDescriptionHashesDTO.contestDescriptionHashes().get(contest.getIndex()));
    for (int i = 0; i < sizeOfContest; i++) {
      ElGamal.Ciphertext cipher = new ElGamal.Ciphertext(
          Group.int_to_p_unchecked(optionsForContest[i].getCiphertextPAD()),
          Group.int_to_p_unchecked(optionsForContest[i].getCiphertextDATA()));
      optionHashes.add(Hash.hash_elems(optionDescriptionIds[i],
          optionDescriptionHashes[i], cipher.crypto_hash()).toString());
    }
    listOfContestHashes.add(Hash.hash_elems(optionHashes.toArray()).toString());

  }

  /**
   * Generates the Hash for a specific voter.
   *
   * @param voters the voter for which the hash gets generated
   * @return the hash
   */
  public String generateVoterHash(List<Voter> voters) {
    return Hash.hash_elems(voters).getBigInt().toString(16);
  }

  /**
   * Generates the election specific Hash using the record Wrapper. Gets used in TrackingCode and
   * Fingerprint.
   *
   * @param election election for which the hashes get created.
   * @return Dto containing optionDescriptionHashes, contestDescriptionHashes, optionIds, contestIds and manifest hash
   * */
  public ElectionDescriptionHashesDTO generateElectionHashes(Election election) {
    Map<Integer, String[]> listOfSelectionDescriptionHash = new HashMap<>();
    Map<Integer, String> listOfContextHashes = new HashMap<>();
    Map<Integer, String[]> listOfSelectionIds = new HashMap<>();
    Map<Integer, String> listOfContestIds = new HashMap<>();

    String manifestHash = electionGuardManifestWrapper
        .generateElectionGuardManifest(election.getElectionId()).cryptoHash().toString();
    List<Contest> contestList = electionService.getAllContestsOfElection(election.getElectionId());
    Collections.sort(contestList);
    for (Contest contest : contestList) {
      Manifest.ContestDescription description = electionGuardManifestWrapper.generateContestDescription(
          contest);
      listOfContextHashes.put(contest.getIndex(), description.cryptoHash().toString());
      listOfContestIds.put(contest.getIndex(), description.contestId());
      String[] hashesForOption = new String[contest.getOptions().size() + contest.getMax()];
      String[] optionIds = new String[contest.getOptions().size() + contest.getMax()];
      for (int i = 0; i < contest.getOptions().size() + contest.getMax(); i++) {
        Manifest.SelectionDescription optionDescription = electionGuardManifestWrapper.generateSelctionDescription(
            contest, i);
        hashesForOption[i] = optionDescription.cryptoHash().toString();
        optionIds[i] = optionDescription.selectionId();
      }
      listOfSelectionDescriptionHash.put(contest.getIndex(), hashesForOption);
      listOfSelectionIds.put(contest.getIndex(), optionIds);
    }
    String commitments = commitmentHash(trusteeService.getAllTrustees(election.getElectionId()));
    String voterHashes = generateVoterHash(voterService.getAllVoters(election.getElectionId()));
    return new ElectionDescriptionHashesDTO(listOfSelectionDescriptionHash, listOfContextHashes,
        listOfSelectionIds, listOfContestIds, manifestHash, commitments, voterHashes);
  }


  /**
   * Generates the election fingerprint for a specific election, which is used to clearly identify a
   * certain election.
   *
   * @param election for which the fingerprint gets generated
   * @return the fingerprint
   */
  public String generateElectionFingerprint(Election election) {
    ElectionDescriptionHashesDTO electionDescriptionHashesDTO = generateElectionHashes(election);
    List<Trustee> allTrustees = trusteeService.getAllTrustees(election.getElectionId());
    List<Contest> contestList = electionService.getAllContestsOfElection(election.getElectionId());
    return Hash.hash_elems(
        election.getTitle(),
        election.getAuthorityEmail(),
        election.getStartTime().getTime(),
        election.getEndTime().getTime(),
        trusteeHash(allTrustees),
        election.hasBot() ? 1 : 0,
        election.getTrusteeThreshold(),
        election.getPublicKey().toString(16).toUpperCase(),
        ballotHash(contestList),
        getHash(electionDescriptionHashesDTO)
    ).toString();
  }

  /**
   * Gets all ballotHash by iterating over the contestList.
   *
   * @param contestList List of all contests of an election
   * @return the list of all contestHashes
   */
  private List<String> ballotHash(List<Contest> contestList) {
    List<String> output = new ArrayList<>();
    for (Contest contest : contestList) {
      output.add(contest.getHashString().toUpperCase());
    }
    return output;
  }

  /**
   * Returns a List with all trusteeEmails. Used for the Fingerprint.
   *
   * @param trusteesList List of all Trustees
   * @return the list with the emails
   */
  private List<String> trusteeHash(List<Trustee> trusteesList) {
    List<String> output = new ArrayList<>();
    for (Trustee trustee : trusteesList) {
      output.add(trustee.getEmail());
    }
    return output;
  }

  /**
   * Collects all Public Keys of Trustees for generating the commitment Hash.
   *
   * @param trusteeList List of all Trustees
   * @return the List with all Public ElGamal keys
   */
  private String commitmentHash(List<Trustee> trusteeList) {
    List<Group.ElementModP> output = new ArrayList<>();
    for (Trustee trustee : trusteeList) {
      List<String> trusteeKeys = trustee.getPublicElgamalKeyAndProof();
      Collections.sort(trusteeKeys);
      trusteeKeys.forEach(t -> output.add(Group.hex_to_p_unchecked(t.split(";")[1].toUpperCase())));
    }
    return Hash.hash_elems(output).toString();
  }

  /**
   * Generates a Contest and Decryption specific Hash. Uses the ElectionGuard Ids. The generated
   * Hash gets used in the Fingerprint.
   *
   * @param hashesDTO contains the electionGuard specific Hashes
   * @return election specific Hash
   */
  private String getHash(ElectionDescriptionHashesDTO hashesDTO) {
    List<String> contestHashes = new ArrayList<>();
    hashesDTO.contestDescriptionHashes().values()
        .forEach(t -> contestHashes.add(t.toUpperCase()));
    String contestHashesHash = Hash.hash_elems(contestHashes).toString();
    String contestIdHash = Hash.hash_elems(hashesDTO.contestIds().values()).toString();
    List<String> selectionHashesHashes = new ArrayList<>();
    List<String> selectionIdHashes = new ArrayList<>();
    for (Map.Entry<Integer, String[]> elem : hashesDTO.optionDescriptionHashes().entrySet()) {
      List<String> optionHashes = new ArrayList<>();
      Arrays.stream(elem.getValue()).forEach(t -> optionHashes.add(t.toUpperCase()));
      selectionHashesHashes.add(Hash.hash_elems(optionHashes).toString());
    }
    for (Map.Entry<Integer, String[]> elem : hashesDTO.optionIds().entrySet()) {
      selectionIdHashes.add(Hash.hash_elems(Arrays.asList(elem.getValue())).toString());
    }
    return Hash.hash_elems(selectionHashesHashes, selectionIdHashes, contestHashesHash,
        contestIdHash,
        hashesDTO.manifestHash().toUpperCase(), hashesDTO.commitments().toUpperCase(),
        hashesDTO.voterHash().toUpperCase()).toString();
  }

  /**
   * Searches for OptionEncrypted with a specific contestIndex.
   *
   * @param optionEncryptedList   List of Options
   * @param searchingContestIndex ContestIndex which gets searched
   * @param contestSize           elements in the contest
   * @return specific OptionEncrypted for a contest
   */
  private OptionEncrypted[] getOptionsForContext(List<OptionEncrypted> optionEncryptedList,
      int searchingContestIndex, int contestSize) {
    int counter = 0;
    OptionEncrypted[] output = new OptionEncrypted[contestSize];
    for (OptionEncrypted optionEncrypted : optionEncryptedList) {
      if (optionEncrypted.getContestIndex() == searchingContestIndex) {
        output[counter++] = optionEncrypted;
      }
    }
    return output;
  }
}
