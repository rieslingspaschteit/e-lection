package pse.election.backendserver.responseUpdate.record;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.sunya.electionguard.Manifest;
import com.sunya.electionguard.input.ManifestInputValidation;
import java.time.Instant;
import java.util.Date;
import java.util.Formatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import pse.election.backendserver.core.service.ElectionService;
import pse.election.backendserver.entity.Contest;
import pse.election.backendserver.entity.Election;
import pse.election.backendserver.payload.response.record.ElectionGuardManifestWrapper;

public class ElectionGuardManifestWrapperTest {

  @Mock
  private ElectionService electionService;

  @Mock
  private Election election;

  @Mock
  private Contest contest1;

  @Mock
  private Contest contest2;

  private long electionId = 12;
  private long contestId1 = 0;
  private long contestid2 = 3;
  @InjectMocks
  private ElectionGuardManifestWrapper wrapper;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);

    when(electionService.getAllContestsOfElection(12)).thenReturn(List.of(contest2, contest1));
    when(electionService.getElection(12)).thenReturn(election);
    when(election.getElectionId()).thenReturn((long) 12);
    when(election.getStartTime()).thenReturn(Date.from(Instant.parse("2003-12-23T13:12:00Z")));
    when(election.getEndTime()).thenReturn(Date.from(Instant.parse("2003-12-25T13:12:00Z")));

    when(contest1.getContestId()).thenReturn((long) 14);
    when(contest1.getName()).thenReturn("Choose");
    when(contest1.getOptions()).thenReturn(List.of("Yes", "No"));
    when(contest1.getIndex()).thenReturn(0);
    when(contest1.getMax()).thenReturn(1);

    when(contest2.getContestId()).thenReturn((long) 19);
    when(contest2.getName()).thenReturn("Do not choose");
    when(contest2.getOptions()).thenReturn(List.of("True", "True"));
    when(contest2.getIndex()).thenReturn(1);
    when(contest2.getMax()).thenReturn(2);
  }

  @Test
  void generateSelectionFromTitle() {
    Manifest.SelectionDescription selection = wrapper.generateSelctionDescription("14", 0, "Yes");
    assertEquals(selection.candidateId(), selection.selectionId());
    assertEquals(selection.candidateId(), "14-0-Yes");
    assertNotNull(selection.cryptoHash());
  }

  @Test
  void generateDummySelectionFromTitle() {
    Manifest.SelectionDescription selection = wrapper.generateSelctionDescription("14", 0, null);
    assertEquals(selection.candidateId(), selection.selectionId());
    assertEquals(selection.candidateId(), "14-0-placeholder");
    assertNotNull(selection.cryptoHash());
  }

  @Test
  void generateSelectionFromContest() {
    Manifest.SelectionDescription selection = wrapper.generateSelctionDescription(contest1, 0);
    assertEquals(selection.candidateId(), selection.selectionId());
    assertEquals(selection.candidateId(), "14-0-Yes");
    assertNotNull(selection.cryptoHash());
  }

  @Test
  void generateDummySelectionFromContest() {
    Manifest.SelectionDescription selection = wrapper.generateSelctionDescription(contest1, 3);
    assertEquals(selection.candidateId(), selection.selectionId());
    assertEquals(selection.candidateId(), "14-3-placeholder");
    assertNotNull(selection.cryptoHash());
  }

  @Test
  void generateContest() {
    Manifest.ContestDescription contest = wrapper.generateContestDescription(contest1);
    assertEquals(contest.contestId(), String.valueOf(contest1.getContestId()));
    assertEquals(contest.name(), contest1.getName());
    assertNotNull(contest.cryptoHash());
    assertEquals(contest.votesAllowed(), 1);
    assertNotNull(contest.selections().get(0));
    assertNotNull(contest.selections().get(1));
    assertNotNull(contest.name());
    assertThrows(IndexOutOfBoundsException.class, () -> {
      contest.selections().get(2);
    });
    assertEquals(contest.selections().get(0).candidateId(), "14-0-Yes");
    assertEquals(contest.voteVariation(), Manifest.VoteVariationType.n_of_m);
    assertEquals(contest.geopoliticalUnitId(), "kit");
  }

  @Test
  void generateManifestTest() {
    Manifest manifest = wrapper.generateElectionGuardManifest(electionId);
    Manifest.ContestDescription description1 = wrapper.generateContestDescription(contest1);
    Manifest.ContestDescription description2 = wrapper.generateContestDescription(contest2);
    //List is not sorted by sequence order
    assertEquals(manifest.contests().get(0), description2);
    assertEquals(manifest.contests().get(1), description1);
    assertNotNull(manifest.cryptoHash());
    Set<Manifest.Candidate> expectedCandidates = new HashSet<>();
    expectedCandidates.add(new Manifest.Candidate("14-0-Yes"));
    expectedCandidates.add(new Manifest.Candidate("14-1-No"));
    expectedCandidates.add(new Manifest.Candidate("19-0-True"));
    expectedCandidates.add(new Manifest.Candidate("19-1-True"));
    Set<Manifest.Candidate> actualCandidates = new HashSet<>();
    actualCandidates.addAll(manifest.candidates());
    assertEquals(expectedCandidates, actualCandidates);
    assertNotEquals(0, manifest.geopoliticalUnits().size());
    // Is this important?
    // assertNotEquals(0, manifest.ballotStyles().size());
    assertNotNull(manifest.cryptoHash());
  }

  @Test
  void manifestValidTest() {
    Manifest manifest = wrapper.generateElectionGuardManifest(electionId);
    ManifestInputValidation validator = new ManifestInputValidation(manifest);
    StringBuffer buffer = new StringBuffer();
    Formatter formatter = new Formatter(buffer);
    validator.validateElection(formatter);
    System.out.println(buffer.toString());
    assertTrue(validator.validateElection(formatter));
  }


}
