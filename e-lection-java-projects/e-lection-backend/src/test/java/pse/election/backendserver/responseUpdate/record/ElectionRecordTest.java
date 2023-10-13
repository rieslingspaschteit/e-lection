package pse.election.backendserver.responseUpdate.record;

import static org.mockito.Mockito.when;

import com.sunya.electionguard.AvailableGuardian;
import com.sunya.electionguard.ElectionCryptoContext;
import com.sunya.electionguard.GuardianRecord;
import com.sunya.electionguard.Manifest;
import com.sunya.electionguard.PlaintextTally;
import com.sunya.electionguard.SubmittedBallot;
import electionguard.ballot.Guardian;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
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
import pse.election.backendserver.payload.response.record.ElectionGuardBallotWrapper;
import pse.election.backendserver.payload.response.record.ElectionGuardInitializedWrapper;
import pse.election.backendserver.payload.response.record.ElectionGuardManifestWrapper;
import pse.election.backendserver.payload.response.record.ElectionGuardRecord;
import pse.election.backendserver.payload.response.record.ElectionGuardTallyWrapper;

public class ElectionRecordTest {

  @Mock
  TallyService tallyService;
  @Mock
  ElectionService electionService;
  @Mock
  BallotService ballotService;
  @Mock
  DecryptionService decryptionService;
  @Mock
  TrusteeService trusteeService;
  @Mock
  ElectionGuardInitializedWrapper initializedWrapper;
  @Mock
  ElectionGuardManifestWrapper wrapper;
  @Mock
  ElectionGuardTallyWrapper tallyWrapper;
  @Mock
  ElectionGuardBallotWrapper ballotWrapper;
  @Mock
  Election election;
  @Mock
  Contest contest1;
  @Mock
  Ballot ballot1;
  long electionId = 12;
  long ballotId = 1;
  OptionEncrypted option1;
  OptionEncrypted option2;

  Tally tally1;

  Tally tally2;

  @Mock
  Trustee trustee1;
  @Mock
  Trustee trustee2;

  @InjectMocks
  ElectionGuardManifestWrapper manifestWrapper;

  @InjectMocks
  ElectionGuardTallyWrapper resultWrapper;
  @InjectMocks
  ElectionGuardInitializedWrapper initWrapper;

  @InjectMocks
  ElectionGuardBallotWrapper submittedBallot;

  @InjectMocks
  ElectionGuardRecord builder;

  PartialDecryption decryption1;
  PartialDecryption decryption2;
  PartialPartialDecryption pdecryption1;
  PartialPartialDecryption pdecryption2;

  public void manifestSetup() {
    when(electionService.getAllContestsOfElection(12)).thenReturn(List.of(contest1));
    when(electionService.getElection(12)).thenReturn(election);
    when(election.getElectionId()).thenReturn((long) 12);
    when(election.getStartTime()).thenReturn(Date.from(Instant.parse("2022-12-23T13:12:00Z")));
    when(election.getEndTime()).thenReturn(Date.from(Instant.parse("2022-12-23T13:12:00Z")));

    when(contest1.getContestId()).thenReturn((long) 14);
    when(contest1.getName()).thenReturn("Choose");
    when(contest1.getOptions()).thenReturn(List.of("No"));
    when(contest1.getIndex()).thenReturn(0);
    when(contest1.getMax()).thenReturn(1);

    Manifest manifest = manifestWrapper.generateElectionGuardManifest(electionId);
    Manifest.ContestDescription contestDesc1 = manifestWrapper.generateContestDescription(contest1);

    Manifest.SelectionDescription selectionDesc11 = manifestWrapper.generateSelctionDescription(
        contest1, 0);
    Manifest.SelectionDescription selectionDesc12 = manifestWrapper.generateSelctionDescription(
        contest1, 1);

    when(wrapper.generateElectionGuardManifest(electionId)).thenReturn(manifest);

    when(wrapper.generateContestDescription(contest1)).thenReturn(contestDesc1);

    when(wrapper.generateSelctionDescription(contest1, 0)).thenReturn(selectionDesc11);
    when(wrapper.generateSelctionDescription(contest1, 1)).thenReturn(selectionDesc12);
  }

  public void ballotSetup() {

    option1 = new OptionEncrypted(1, electionId, 0, 0, "A", "B", "A;b;c;d|E;F;1;2|2", "1;2;3;4;5");
    option2 = new OptionEncrypted(1, electionId, 1, 0, "A", "B", "A;b;c;d|E;F;1;2|2", "1;2;3;4;5");

    manifestSetup();

    when(ballot1.isSubmitted()).thenReturn(true);
    when(ballot1.getBallotId()).thenReturn((long) 1);
    when(ballot1.getLatestTrackingCode()).thenReturn("abc");
    when(ballot1.getPreviousTrackingCode()).thenReturn("123");

    Instant instant = Instant.parse("2023-02-02T14:54:05.099Z");
    Date encryptionDate = Date.from(instant);
    when(ballot1.getEncryptionDate()).thenReturn(encryptionDate);

    when(ballotService.getAllOptionsEncryptedOfBallot(1)).thenReturn(List.of(option1, option2));
    when(ballotService.getAllSubmittedBallotsOfElection(electionId)).thenReturn(List.of());
    when(ballotService.getAllSpoiledBallotsOfElection(electionId)).thenReturn(List.of(ballot1));
  }

  public void spoiledBallotSetup() {
    manifestSetup();
    ballotSetup();
    when(trustee1.getIndex()).thenReturn(1);
    when(trustee1.getEmail()).thenReturn("A");
    when(trustee1.getTrusteeId()).thenReturn((long) 2);
    when(trustee1.getLagrangeCoefficient()).thenReturn(BigInteger.valueOf(7));
    when(trustee2.getIndex()).thenReturn(2);
    when(trustee2.getEmail()).thenReturn("B");
    when(trustee2.getTrusteeId()).thenReturn((long) 5);
    when(trustee1.getLagrangeCoefficient()).thenReturn(BigInteger.valueOf(4));
    when(trustee1.getPublicElgamalKeyAndProof()).thenReturn(List.of("0;B;CD;E;F"));
    when(trustee2.getPublicElgamalKeyAndProof()).thenReturn(List.of("0;B;CD;E;F"));

    long trusteeId1 = 2;
    long trusteeId2 = 5;
    long optionId1 = option1.getOptionEncryptedId();
    long optionId2 = option2.getOptionEncryptedId();
    decryption1 = new PartialDecryption(0, trusteeId1, optionId1, "AB", "AB;CD;EF;12");
    decryption2 = new PartialDecryption(0, trusteeId1, optionId2, "AB", "AB;CD;EF;12");
    pdecryption1 = new PartialPartialDecryption(trusteeId1, trusteeId2, 0, optionId1, "AB",
        "AB;CD;EF;12");
    pdecryption2 = new PartialPartialDecryption(trusteeId1, trusteeId2, 0, optionId2, "AB",
        "AB;CD;EF;12");
    when(trusteeService.getAllTrustees(electionId)).thenReturn(List.of(trustee1, trustee2));
    when(election.getTrusteeThreshold()).thenReturn(1);
    when(decryptionService.getAllPartialDecryptionOfOption(optionId1)).thenReturn(
        List.of(decryption1));
    when(decryptionService.getAllPartialDecryptionOfOption(optionId2)).thenReturn(
        List.of(decryption2));
    when(decryptionService.getAllPartialPartialDecryptionOfOption(optionId1)).thenReturn(
        List.of(pdecryption1));
    when(decryptionService.getAllPartialPartialDecryptionOfOption(optionId2)).thenReturn(
        List.of(pdecryption2));
    when(decryptionService.getAllPartialDecryptionByTrustee(trustee1.getTrusteeId())).thenReturn(
        List.of(decryption1, decryption2));
    when(decryptionService.getPartialDecryptionOfTrustee(trusteeId1, optionId1)).thenReturn(
        decryption1);
    when(decryptionService.getAllPartialPartialDecryptionOfTrustee(trusteeId1,
        optionId1)).thenReturn(List.of(pdecryption1));
    when(decryptionService.getAllPartialPartialDecryptionOfTrustee(trusteeId1,
        optionId2)).thenReturn(List.of(pdecryption2));
    when(decryptionService.getPartialDecryptionOfTrustee(trusteeId1, optionId2)).thenReturn(
        decryption2);
    when(trusteeService.getTrustee(trustee1.getEmail(), electionId)).thenReturn(trustee1);
    when(trusteeService.getTrustee(trustee2.getEmail(), electionId)).thenReturn(trustee2);
    //trustee1.addBackup("123");
    //trustee1.addBackup("456");
    Guardian guardian1 = initWrapper.generateGuardian(electionId, trustee1.getEmail());
    Guardian guardian2 = initWrapper.generateGuardian(electionId, trustee2.getEmail());
    when(initializedWrapper.generateGuardianFromTrustee(trustee1)).thenReturn(guardian1);
    when(initializedWrapper.generateGuardianFromTrustee(trustee2)).thenReturn(guardian2);
  }

  public void tallySetup() {
    spoiledBallotSetup();
    tally1 = new Tally(electionId, 0, 0);
    tally2 = new Tally(electionId, 0, 1);
    tally1.setCiphertextDATA("AB");
    tally1.setCiphertextPAD("CD");
    tally2.setCiphertextDATA("AB");
    tally2.setCiphertextPAD("CD");
    when(tallyService.getAllTalliesOfElection(electionId)).thenReturn(List.of(tally1, tally2));
    when(election.getPublicKey()).thenReturn(BigInteger.valueOf(12345));
    //ElectionInitialized init = initWrapper.generateInitializedElection();
    //when(initializedWrapper.generateInitializedElection()).thenReturn(init);
    List<SubmittedBallot> allBallots = submittedBallot.generateAllSubmittedBallots(electionId);
    when(ballotWrapper.generateAllSubmittedBallots(electionId)).thenReturn(allBallots);
    resultWrapper.generateCiphertextTally(electionId);
    PlaintextTally result = resultWrapper.generatePlaintextTally(electionId);
    when(tallyWrapper.generatePlaintextTally(electionId)).thenReturn(result);
    PlaintextTally dresult = resultWrapper.generatePlaintextTally(electionId);
    when(tallyWrapper.generatePlaintextTally(electionId)).thenReturn(dresult);
  }

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
    Integer[] result1 = new Integer[]{1, 0};
    when(electionService.getDecryptedResult(electionId)).thenReturn(Map.of(0, result1));
    tallySetup();
    List<GuardianRecord> records = initWrapper.generateGuardianRecords(electionId);
    List<AvailableGuardian> availables = resultWrapper.generateAvailableGuardians(electionId);
    System.out.println("Test" + availables.size());
    ElectionCryptoContext context = initWrapper.generateCryptoContext(electionId);
    when(initializedWrapper.generateGuardianRecords(electionId)).thenReturn(records);
    when(tallyWrapper.generateAvailableGuardians(electionId)).thenReturn(availables);
    when(initializedWrapper.generateCryptoContext(electionId)).thenReturn(context);
    List<AvailableGuardian> availableGuardians = resultWrapper.generateAvailableGuardians(
        electionId);
    when(tallyWrapper.generateAvailableGuardians(electionId)).thenReturn(availableGuardians);
  }
}
