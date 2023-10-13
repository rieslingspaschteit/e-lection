package pse.election.backendserver.payload.response.record;

import com.sunya.electionguard.BallotBox;
import com.sunya.electionguard.ChaumPedersen;
import com.sunya.electionguard.CiphertextBallot;
import com.sunya.electionguard.ElGamal;
import com.sunya.electionguard.Group;
import com.sunya.electionguard.Hash;
import com.sunya.electionguard.Manifest;
import com.sunya.electionguard.SubmittedBallot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pse.election.backendserver.core.service.BallotService;
import pse.election.backendserver.core.service.ElectionService;
import pse.election.backendserver.entity.Ballot;
import pse.election.backendserver.entity.Contest;
import pse.election.backendserver.entity.OptionEncrypted;

/**
 * This class generates records of ballots submitted in an election that are accepted by an
 * electionguard verifier.
 */
@Component
public class ElectionGuardBallotWrapper {

  @Autowired
  private ElectionGuardManifestWrapper manifestWrapper;
  @Autowired
  private BallotService ballotService;
  @Autowired
  private ElectionService electionService;

  public ElectionGuardBallotWrapper() {
  }

  /**
   * Generates records for all submitted ballots of the election This includes spiled and cast
   * ballots.
   *
   * @return List of ballot records
   */
  public List<SubmittedBallot> generateAllSubmittedBallots(long electionId) {
    List<Ballot> allBallots = new ArrayList<>();
    allBallots.addAll(ballotService.getAllSpoiledBallotsOfElection(electionId));
    allBallots.addAll(ballotService.getAllSubmittedBallotsOfElection(electionId));
    List<SubmittedBallot> submittedBallots = new ArrayList<>();
    for (Ballot ballot : allBallots) {
      submittedBallots.add(generateSubmittedBallot(electionId, ballot));
    }
    return submittedBallots;
  }

  /**
   * Generates a record for a submitted ballot.
   *
   * @param ballot the ballot for which the record is created
   * @return electionguard ballot record
   */
  public SubmittedBallot generateSubmittedBallot(long electionId, Ballot ballot) {
    BallotBox.State ballotState = BallotBox.State.SPOILED;
    if (ballot.isSubmitted()) {
      ballotState = BallotBox.State.CAST;
    }
    List<OptionEncrypted> allOptions = ballotService.getAllOptionsEncryptedOfBallot(
        ballot.getBallotId());
    Map<Integer, List<OptionEncrypted>> ballotToOptions = new HashMap<>();

    for (OptionEncrypted option : allOptions) {
      ballotToOptions.computeIfAbsent(option.getContestIndex(), k -> new ArrayList<>());
      ballotToOptions.get(option.getContestIndex()).add(option);
    }
    List<CiphertextBallot.Contest> contests = new ArrayList<>();
    for (Contest contest : electionService.getAllContestsOfElection(electionId)) {
      contests.add(getContest(ballotToOptions.get(contest.getIndex()), contest));
    }

    CiphertextBallot ciphertext = CiphertextBallot.create(
        String.valueOf(ballot.getBallotIdForEncryption()),
        ElectionGuardManifestWrapper.style.ballotStyleId(),
        manifestWrapper.generateElectionGuardManifest(electionId).cryptoHash(),
        Group.hex_to_q(ballot.getPreviousTrackingCode()).orElseThrow(),
        contests,
        Optional.empty(),
        Optional.of(ballot.getEncryptionDate().getTime()),
        Group.hex_to_q(ballot.getLatestTrackingCode())
    );
    return SubmittedBallot.createFromCiphertextBallot(ciphertext, ballotState);
  }


  /**
   * creates a encrypted contest of a submitted ballot for a ballot record.
   *
   * @param options Encrypted options of the ballot
   * @param contest the contest description of the contest
   * @return A ciphertext contest
   */
  private CiphertextBallot.Contest getContest(List<OptionEncrypted> options, Contest contest) {
    List<CiphertextBallot.Selection> selections = new ArrayList<>();
    Manifest.ContestDescription contestDescription = manifestWrapper.generateContestDescription(
        contest);
    for (int count = 0; count < options.size(); count++) {
      Manifest.SelectionDescription selection = manifestWrapper.generateSelctionDescription(contest,
          count);
      selections.add(generateSelection(options.get(count), selection, count,
          count >= contest.getOptions().size()));
    }
    return new CiphertextBallot.Contest(
        contestDescription.contestId(),
        contestDescription.sequenceOrder(),
        contestDescription.cryptoHash(),
        selections,
        CiphertextBallot.ciphertext_ballot_context_crypto_hash(contestDescription.contestId(),
            selections, contestDescription.cryptoHash()),
        Optional.empty(),
        Optional.of(generateConstantProofFromJSON(options.get(0).getAccumulatedProof()))
    );
  }


  /**
   * Generates a record for a encrypted option of a ballot.
   *
   * @param option        Ciphertext of the encrypted option
   * @param selection     description of the selection
   * @param count         sequence order of the selection
   * @param isPlaceholder wether or not the encrypted option is a placeholder option
   * @return Record of encrypted option
   */
  private CiphertextBallot.Selection generateSelection(OptionEncrypted option,
      Manifest.SelectionDescription selection, int count, boolean isPlaceholder) {
    Group.ElementModP pad = Group.int_to_p(option.getCiphertextPAD()).orElseThrow();
    Group.ElementModP data = Group.int_to_p(option.getCiphertextDATA()).orElseThrow();
    ElGamal.Ciphertext ciphertext = new ElGamal.Ciphertext(pad, data);
    return new CiphertextBallot.Selection(
        selection.selectionId(),
        count,
        selection.cryptoHash(),
        ciphertext,
        Hash.hash_elems(selection.selectionId(), selection.cryptoHash(), ciphertext.crypto_hash()),
        isPlaceholder,
        Optional.empty(),
        Optional.of(generateDisjunctiveProofFromJSON(option.getIndividualProof())),
        Optional.empty()
    );
  }

  /**
   * Generates a DisjunctiveChaumPedersenProof for the format specified in the OptionEncrypted
   * entity.
   *
   * @param json String containing the proof
   * @return ChaumPedersenProof
   */
  private ChaumPedersen.DisjunctiveChaumPedersenProof generateDisjunctiveProofFromJSON(
      String json) {
    String[] proof0 = json.split("\\|")[0].split(";");
    String[] proof1 = json.split("\\|")[1].split(";");
    String challenge = json.split("\\|")[2];
    Group.ElementModP p0p = Group.hex_to_p_unchecked(proof0[0]);
    Group.ElementModP p0d = Group.hex_to_p_unchecked(proof0[1]);
    Group.ElementModQ p0c = Group.hex_to_q_unchecked(proof0[2]);
    Group.ElementModQ p0r = Group.hex_to_q_unchecked(proof0[3]);

    Group.ElementModP p1p = Group.hex_to_p_unchecked(proof1[0]);
    Group.ElementModP p1d = Group.hex_to_p_unchecked(proof1[1]);
    Group.ElementModQ p1c = Group.hex_to_q_unchecked(proof1[2]);
    Group.ElementModQ p1r = Group.hex_to_q_unchecked(proof1[3]);

    Group.ElementModQ c = Group.hex_to_q_unchecked(challenge);

    return new ChaumPedersen.DisjunctiveChaumPedersenProof(
        new ChaumPedersen.ChaumPedersenProof(p0p, p0d, p0c, p0r),
        new ChaumPedersen.ChaumPedersenProof(p1p, p1d, p1c, p1r),
        c
    );
  }

  /**
   * Generates a ConstantChaumPedersenProof for the format specified in the OptionEncrypted entity.
   *
   * @param json String containing the proof
   * @return ChaumPedersenProof
   */
  private ChaumPedersen.ConstantChaumPedersenProof generateConstantProofFromJSON(String json) {
    String[] proof1 = json.split(";");
    Group.ElementModP p1p = Group.hex_to_p_unchecked(proof1[0]);
    Group.ElementModP p1d = Group.hex_to_p_unchecked(proof1[1]);
    Group.ElementModQ p1c = Group.hex_to_q_unchecked(proof1[2]);
    Group.ElementModQ p1r = Group.hex_to_q_unchecked(proof1[3]);

    int sum = Integer.parseInt(proof1[4]);
    return new ChaumPedersen.ConstantChaumPedersenProof(p1p, p1d, p1c, p1r, sum);
  }
}
