package pse.election.backendserver.core.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.sql.Date;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pse.election.backendserver.core.state.ElectionState;
import pse.election.backendserver.core.state.handler.ElectionStateHandler;
import pse.election.backendserver.entity.Contest;
import pse.election.backendserver.entity.Election;
import pse.election.backendserver.payload.error.exception.InvalidConfigurationException;
import pse.election.backendserver.repository.ContestRepository;
import pse.election.backendserver.repository.ElectionRepository;

@ExtendWith(MockitoExtension.class)
public class ElectionServiceTest {

  private static Election election;
  @Mock
  private ElectionRepository electionRepository;

  @Mock
  private ElectionStateHandler electionStateHandler;

  @Mock
  private ContestRepository contestRepository;
  @InjectMocks
  private ElectionService electionService;

  @BeforeAll
  static void setUp() {
    election = new Election(
        Date.from(Instant.now().now().plus(Duration.ofDays(1))),
        "Vacation to Ghana",
        "election-description",
        "authority@example.com",
        false,
        1
    );
  }

  @Test
  void checkUpdateState() {
    election.setState(ElectionState.EPKB);

    when(electionRepository.existsByElectionId(1)).thenReturn(true);
    when(electionRepository.findByElectionId(1)).thenReturn(election);
    when(electionRepository.save(any(Election.class))).thenReturn(election);

    Election updatedElection = electionService.tryUpdateState(1, ElectionState.EPKB);

    Assertions.assertSame(ElectionState.EPKB, updatedElection.getState());
  }

  @Test
  void checkSetResult() {
    Map<Integer, Integer[]> result = new HashMap<>();
    result.put(1, new Integer[]{0, 12, 4});
    result.put(2, new Integer[]{5, 19});

    when(electionRepository.existsByElectionId(1)).thenReturn(true);
    when(electionRepository.findByElectionId(1)).thenReturn(election);
    when(electionRepository.save(any())).thenReturn(election);

    electionService.setResult(1, result);

    Assertions.assertEquals("0;12;4|5;19", election.getCleartextResult());
    Assertions.assertNotEquals("0;12;5|5;19", election.getCleartextResult());
    Assertions.assertNotEquals("0;12;4;5;19", election.getCleartextResult());
  }

  @Test
  void checkGetDecryptedResult_NonExistantResult() {
    election.setCleartextResult(null);

    when(electionRepository.existsByElectionId(1)).thenReturn(true);
    when(electionRepository.findByElectionId(1)).thenReturn(election);

    Assertions.assertThrows(InvalidConfigurationException.class,
        () -> electionService.getDecryptedResult(1));
  }

  @Test
  void checkGetDecryptedResult() {
    election.setCleartextResult("0;5;2;17|1;1");

    when(electionRepository.existsByElectionId(1)).thenReturn(true);
    when(electionRepository.findByElectionId(1)).thenReturn(election);

    Map<Integer, Integer[]> result = electionService.getDecryptedResult(1);

    Assertions.assertSame(17, result.get(0)[3]);
    Assertions.assertSame(1, result.get(1)[0]);
    Assertions.assertFalse(result.containsKey(15));
    Assertions.assertEquals(2, result.keySet().size());
  }

  @Test
  void checkAddContest() {
    Contest contestInvalid = new Contest(1, "contest1", -1, 0, List.of("A", "B"));
    Contest contestValid1 = new Contest(1, "contest2", 1, 1, List.of("C", "D"));
    Contest contestValid2 = new Contest(1, "contest3", 1, 1, List.of("E", "F"));
    List<Contest> contests = List.of(contestValid1, contestValid2);

    when(electionRepository.existsByElectionId(1)).thenReturn(true);
    when(contestRepository.saveAll(contests)).thenReturn(contests);

    Assertions.assertThrows(InvalidConfigurationException.class,
        () -> electionService.addContest(new ArrayList<>()));
    Assertions.assertThrows(InvalidConfigurationException.class,
        () -> electionService.addContest(List.of(contestInvalid, contestValid1)));
    Assertions.assertEquals(List.of(contestValid1, contestValid2),
        electionService.addContest(contests));
  }

  @Test
  void initialSave_electionIsNull() {
    Assertions.assertThrows(InvalidConfigurationException.class, () -> electionService.initialSave(null));
  }

  @Test
  void initialSave_electionWithoutDescription() {
    Assertions.assertThrows(InvalidConfigurationException.class,
            () -> electionService.initialSave(new Election(null, "Title", null, "email", false, 1)));
  }

  @Test
  void initialSave_electionWithoutTitle() {
    Assertions.assertThrows(InvalidConfigurationException.class,
            () -> electionService.initialSave(new Election(null, null, "Des", "email", false, 1)));
  }

  @Test
  void initialSave_electionWithoutAuthorirty() {
    Assertions.assertThrows(InvalidConfigurationException.class,
            () -> electionService.initialSave(new Election(null, "T", "Des", null, false, 1)));
  }

  @Test
  void updateLatestTrackingCode_noTrackingCode() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> electionService.updateLatestTrackingCode(1, null));
  }

  @Test
  void updateLatestTrackingCode_valid() {
    Election election1 = new Election();
    when(electionRepository.existsByElectionId(anyLong())).thenReturn(true);
    when(electionRepository.findByElectionId(anyLong())).thenReturn(election1);

    electionService.updateLatestTrackingCode(1, "TrackingCode");
    Assertions.assertEquals("TrackingCode", election1.getTrackingCodeLatest());
  }


  @Test
  void addInvalidContest_noOptions() {
    when(electionRepository.existsByElectionId(anyLong())).thenReturn(true);

    Contest invalidContest = new Contest(1, "InvalidContest", 0, 1, Collections.emptyList());
    Assertions.assertThrows(InvalidConfigurationException.class, () -> electionService.addContest(List.of(invalidContest)));
  }
}
