package pse.election.backendserver.responseUpdate.record;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.sunya.electionguard.Manifest;
import java.io.IOException;
import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import pse.election.backendserver.core.electionguard.HashFacade;
import pse.election.backendserver.core.service.BallotService;
import pse.election.backendserver.core.service.ElectionService;
import pse.election.backendserver.core.service.TrusteeService;
import pse.election.backendserver.core.service.VoterService;
import pse.election.backendserver.entity.Ballot;
import pse.election.backendserver.entity.Contest;
import pse.election.backendserver.entity.Election;
import pse.election.backendserver.entity.OptionEncrypted;
import pse.election.backendserver.payload.dto.ElectionDescriptionHashesDTO;
import pse.election.backendserver.payload.response.record.ElectionGuardBallotWrapper;
import pse.election.backendserver.payload.response.record.ElectionGuardManifestWrapper;

public class ElectionGuardBallotWrapperTest {

  @Mock
  Ballot ballot1;
  @Mock
  TrusteeService trusteeService;
  @Mock
  VoterService voterService;
  @Mock
  ElectionGuardManifestWrapper wrapper;
  @InjectMocks
  ElectionGuardManifestWrapper manifestWrapper;
  @InjectMocks
  ElectionGuardBallotWrapper ballotWrapper;
  @InjectMocks
  HashFacade hashFacade;
  @Mock
  private Election election;
  @Mock
  private Contest contest1;
  @Mock
  private Contest contest2;
  private long electionId = 12;
  OptionEncrypted option11 = new OptionEncrypted(1, electionId, 0, 0, "A", "B", "A;b;c;d|E;F;1;2|2",
      "1;2;3;4;5");
  OptionEncrypted option12 = new OptionEncrypted(1, electionId, 1, 0, "A", "B", "A;b;c;d|E;F;1;2|2",
      "1;2;3;4;5");
  OptionEncrypted option13 = new OptionEncrypted(1, electionId, 2, 0, "A", "B", "A;b;c;d|E;F;1;2|2",
      "1;2;3;4;5");
  OptionEncrypted option21 = new OptionEncrypted(1, electionId, 0, 1, "A", "B", "A;b;c;d|E;F;1;2|2",
      "1;2;3;4;5");
  OptionEncrypted option22 = new OptionEncrypted(1, electionId, 1, 1, "A", "B", "A;b;c;d|E;F;1;2|2",
      "1;2;3;4;5");
  OptionEncrypted option23 = new OptionEncrypted(1, electionId, 2, 1, "A", "B", "A;b;c;d|E;F;1;2|2",
      "1;2;3;4;5");
  private long contestId1 = 0;
  private long contestid2 = 3;
  @Mock
  private ElectionService electionService;
  @Mock
  private BallotService ballotService;


  public ElectionGuardBallotWrapperTest() throws IOException, ParseException {
  }

  void manifestSetupt() {
    List<Contest> mutableContestList = new ArrayList<>();
    mutableContestList.addAll(List.of(contest1, contest2));
    when(electionService.getAllContestsOfElection(12)).thenReturn(mutableContestList);
    when(electionService.getElection(12)).thenReturn(election);
    when(election.getElectionId()).thenReturn((long) 12);
    when(election.getStartTime()).thenReturn(Date.from(Instant.parse("2022-12-23T13:12:00Z")));
    when(election.getEndTime()).thenReturn(Date.from(Instant.parse("2022-12-25T13:12:00Z")));

    when(contest1.getContestId()).thenReturn((long) 14);
    when(contest1.getName()).thenReturn("Choose");
    when(contest1.getOptions()).thenReturn(List.of("Yes", "No"));
    when(contest1.getIndex()).thenReturn(0);
    when(contest1.getMax()).thenReturn(1);

    when(contest2.getContestId()).thenReturn((long) 19);
    when(contest2.getName()).thenReturn("Do not choose");
    when(contest2.getOptions()).thenReturn(List.of("True", "True"));
    when(contest2.getIndex()).thenReturn(1);
    when(contest2.getMax()).thenReturn(1);

  }

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);

    manifestSetupt();
    Manifest manifest = manifestWrapper.generateElectionGuardManifest(electionId);
    Manifest.ContestDescription contestDesc1 = manifestWrapper.generateContestDescription(contest1);
    Manifest.ContestDescription contestDesc2 = manifestWrapper.generateContestDescription(contest2);

    Manifest.SelectionDescription selectionDesc11 = manifestWrapper.generateSelctionDescription(
        contest1, 0);
    Manifest.SelectionDescription selectionDesc12 = manifestWrapper.generateSelctionDescription(
        contest1, 1);
    Manifest.SelectionDescription selectionDesc13 = manifestWrapper.generateSelctionDescription(
        contest1, 2);

    Manifest.SelectionDescription selectionDesc21 = manifestWrapper.generateSelctionDescription(
        contest2, 0);
    Manifest.SelectionDescription selectionDesc22 = manifestWrapper.generateSelctionDescription(
        contest2, 1);
    Manifest.SelectionDescription selectionDesc23 = manifestWrapper.generateSelctionDescription(
        contest2, 2);

    when(wrapper.generateElectionGuardManifest(electionId)).thenReturn(manifest);

    when(wrapper.generateContestDescription(contest1)).thenReturn(contestDesc1);
    when(wrapper.generateContestDescription(contest2)).thenReturn(contestDesc2);

    when(wrapper.generateSelctionDescription(contest1, 0)).thenReturn(selectionDesc11);
    when(wrapper.generateSelctionDescription(contest1, 1)).thenReturn(selectionDesc12);
    when(wrapper.generateSelctionDescription(contest1, 2)).thenReturn(selectionDesc13);
    when(wrapper.generateSelctionDescription(contest2, 0)).thenReturn(selectionDesc21);
    when(wrapper.generateSelctionDescription(contest2, 1)).thenReturn(selectionDesc22);
    when(wrapper.generateSelctionDescription(contest2, 2)).thenReturn(selectionDesc23);

    when(ballot1.isSubmitted()).thenReturn(true);
    when(ballot1.getBallotId()).thenReturn((long) 1);
    when(ballot1.getLatestTrackingCode()).thenReturn("abc");
    when(ballot1.getPreviousTrackingCode()).thenReturn("123");

    Instant instant = Instant.parse("2023-02-02T14:54:05.099Z");
    Date encryptionDate = Date.from(instant);
    when(ballot1.getEncryptionDate()).thenReturn(encryptionDate);
    when(ballot1.getBallotIdForEncryption()).thenReturn("ABC");
    when(ballotService.getAllOptionsEncryptedOfBallot(1)).thenReturn(
        List.of(option11, option12, option13, option21, option22, option23));
    when(ballotService.getAllSubmittedBallotsOfElection(electionId)).thenReturn(List.of(ballot1));
    when(ballotService.getAllSpoiledBallotsOfElection(electionId)).thenReturn(List.of());
    when(trusteeService.getAllTrustees(electionId)).thenReturn(new ArrayList<>());
    when(voterService.getAllVoters(electionId)).thenReturn(new ArrayList<>());
  }

  @Test
  @Disabled
  void test() {
    ballotWrapper.generateSubmittedBallot(electionId, ballot1);
    ballotWrapper.generateAllSubmittedBallots(electionId);
  }

  @Test
  void testCryptoHash() {
    ElectionDescriptionHashesDTO dto = hashFacade.generateElectionHashes(election);
    List<OptionEncrypted> optionsOfBallot = new ArrayList<>();
    optionsOfBallot.add(option11);
    optionsOfBallot.add(option12);
    optionsOfBallot.add(option13);
    optionsOfBallot.add(option21);
    optionsOfBallot.add(option22);
    optionsOfBallot.add(option23);
    String ourCryptoHash = hashFacade.generateCryptoHash(dto, optionsOfBallot, election, ballot1);
    String electionGurdCryptohash = ballotWrapper.generateSubmittedBallot(electionId,
        ballot1).crypto_hash.base16();
    System.out.println(ourCryptoHash);
    System.out.println(electionGurdCryptohash);
    assertEquals(ourCryptoHash, electionGurdCryptohash);
    //Group.ElementModP
  }


}
