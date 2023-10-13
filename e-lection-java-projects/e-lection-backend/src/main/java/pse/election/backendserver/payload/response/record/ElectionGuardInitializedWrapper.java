package pse.election.backendserver.payload.response.record;


import com.sunya.electionguard.ElectionCryptoContext;
import com.sunya.electionguard.Group;
import com.sunya.electionguard.GuardianRecord;
import com.sunya.electionguard.Hash;
import com.sunya.electionguard.Manifest;
import com.sunya.electionguard.SchnorrProof;
import electionguard.ballot.Guardian;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pse.election.backendserver.core.service.ElectionService;
import pse.election.backendserver.core.service.TrusteeService;
import pse.election.backendserver.entity.Election;
import pse.election.backendserver.entity.Trustee;

/**
 * This class generates records of the election configuration and crypto constants and parameters
 * compatible for an ElectionGuard verifier.
 */
@Component
public class ElectionGuardInitializedWrapper {

  @Autowired
  private ElectionGuardManifestWrapper manifestWrapper;
  @Autowired
  private ElectionService electionService;
  @Autowired
  private TrusteeService trusteeService;

  public ElectionGuardInitializedWrapper() {

  }

  /**
   * Generates a ElectionCryptoContext used for computing the ElectionBaseHash
   * and ElectionExtendedBaseHash.
   *
   * @return the electionCryptoContext of th current election.
   * */
  public ElectionCryptoContext generateCryptoContext(long electionId) {
    Election election = electionService.getElection(electionId);
    List<Trustee> trustees = trusteeService.getAllTrustees(electionId);
    Manifest manifest = manifestWrapper.generateElectionGuardManifest(electionId);
    List<Trustee> sorted = trustees.stream()
        .sorted(Comparator.comparing(Trustee::getIndex)).toList();
    List<Group.ElementModP> commitments = new ArrayList<>();
    for (Trustee trustee : sorted) {
      Guardian guardian = this.generateGuardianFromTrustee(trustee);
      commitments.addAll(guardian.getCoefficientCommitments());
    }
    //TODO possible difference to python version
    Group.ElementModQ commitmentHash = Hash.hash_elems(commitments);
    return ElectionCryptoContext.create(
        trusteeService.getAllTrustees(electionId).size(),
        election.getTrusteeThreshold(),
        Group.int_to_p(election.getPublicKey()).orElseThrow(),
        manifest,
        commitmentHash,
        null
    );
  }

  /**
   * Generates guardian records for all the guardians (trustees) of the election that are compatible
   * with an ElectionGuard verifier.
   *
   * @return list of guardian records
   */
  public List<GuardianRecord> generateGuardianRecords(long electionId) {
    List<GuardianRecord> records = new ArrayList<>();
    for (Trustee trustee : trusteeService.getAllTrustees(electionId)) {
      Guardian guardian = generateGuardianFromTrustee(trustee);
      GuardianRecord record = new GuardianRecord(
          guardian.getGuardianId(),
          guardian.getXCoordinate(),
          guardian.publicKey(),
          guardian.getCoefficientCommitments(),
          guardian.getCoefficientProofs()
      );
      records.add(record);
    }
    return records;
  }

  /**
   * Generates a Guardian object for a given trustee.
   *
   * @param email email of the trustee
   * @return guardian
   */
  public Guardian generateGuardian(long electionId, String email) {
    Trustee guardian = trusteeService.getTrustee(email, electionId);
    return generateGuardianFromTrustee(guardian);
  }

  /**
   * Generates a Guardian object compatible with ElectionGuard from a given Trustee object.
   *
   * @return guardian
   */
  public Guardian generateGuardianFromTrustee(Trustee guardian) {
    List<String> proofs = guardian.getPublicElgamalKeyAndProof();
    List<Group.ElementModP> keys = new ArrayList<>();
    Map<Integer, SchnorrProof> schnorrProofMap = new TreeMap<>();
    for (String proof : proofs) {
      String[] contents = proof.split(";");
      Group.ElementModP key = Group.hex_to_p_unchecked(contents[1]);
      Group.ElementModP commitment = Group.hex_to_p_unchecked(contents[2]);
      Group.ElementModQ challenge = Group.hex_to_q(contents[3]).orElseThrow();
      Group.ElementModQ response = Group.hex_to_q(contents[4]).orElseThrow();
      schnorrProofMap.put(Integer.valueOf(contents[0]),
          new SchnorrProof(key, commitment, challenge, response));
    }
    SortedSet<Integer> keySet = (SortedSet) schnorrProofMap.keySet();
    List<SchnorrProof> schnorrProofs = new ArrayList<>();
    for (int index : keySet) {
      schnorrProofs.add(schnorrProofMap.get(index));
      keys.add(schnorrProofs.get(index).publicKey);
    }
    GuardianRecord record = new GuardianRecord(
        String.valueOf(guardian.getTrusteeId()),
        guardian.getIndex(),
        null,
        keys,
        schnorrProofs
    );
    return new Guardian(
        record
    );
  }
}
