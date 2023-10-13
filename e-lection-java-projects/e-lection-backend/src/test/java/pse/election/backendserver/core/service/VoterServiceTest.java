package pse.election.backendserver.core.service;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pse.election.backendserver.entity.Election;
import pse.election.backendserver.entity.Voter;
import pse.election.backendserver.payload.error.exception.EntityNotFoundException;
import pse.election.backendserver.payload.error.exception.InvalidConfigurationException;
import pse.election.backendserver.repository.ElectionRepository;
import pse.election.backendserver.repository.VoterRepository;

@ExtendWith(MockitoExtension.class)
public class VoterServiceTest {

  private final Voter voterFrankreich = new Voter(1, "frankreich@example.com");
  private final Voter voterGhana = new Voter(1, "ghana@example.com");
  private final Voter voterWrongFormatEmail = new Voter(1, "ghana");
  @Mock
  private VoterRepository voterRepository;
  @Mock
  private ElectionRepository electionRepository;
  @Mock
  private ElectionService electionService;
  @InjectMocks
  private VoterService voterService;

  @Test
  void checkAddVoter() {
    when(voterRepository.save(any(Voter.class))).thenReturn(voterFrankreich);

    Voter desiredVoter = voterService.addVoter(voterFrankreich);
    assertEquals(desiredVoter.getVoterId(), voterFrankreich.getVoterId());

    assertThrows(InvalidConfigurationException.class, () -> {
      voterService.addVoter(voterWrongFormatEmail);
    });
  }

  @Test
  void checkGetAllVotersToElection() {
    List<Voter> voters = new ArrayList<>();
    voters.add(voterFrankreich);
    voters.add(voterGhana);

    when(voterRepository.findByElectionId(1)).thenReturn(voters);

    assertTrue(voterService.getAllVoters(1).contains(voterFrankreich));
    assertTrue(voterService.getAllVoters(1).contains(voterGhana));
    assertThrows(EntityNotFoundException.class, () -> voterService.getAllVoters(777));
  }

  @Test
  void checkExistsByEmail() {
    when(voterRepository.existsByEmail("frankreich@example.com")).thenReturn(true);
    when(voterRepository.existsByEmail("island@example.com")).thenReturn(false);

    assertTrue(voterService.existsByEmail("frankreich@example.com"));
    assertFalse(voterService.existsByEmail("island@example.com"));
  }

  @Test
  void checkGetAllElectionsOfVoter() {
    Election electionOne = new Election(
        Date.from(Instant.now().plus(Duration.ofDays(1))),
        "Vacation to Ghana",
        "election-description",
        voterFrankreich.getEmail(),
        false,
        1
    );
    Election electionTwo = new Election(
        Date.from(Instant.now().plus(Duration.ofDays(1))),
        "Vacation to France",
        "election-description",
        voterFrankreich.getEmail(),
        false,
        1
    );
    List<Election> elections = new ArrayList<>();
    elections.add(electionOne);
    elections.add(electionTwo);
    when(electionService.getAllElections()).thenReturn(elections);

    when(voterRepository.findByEmailAndElectionId(voterGhana.getEmail(),
        electionOne.getElectionId())).thenReturn(voterGhana);
    when(voterRepository.findByEmailAndElectionId(voterGhana.getEmail(),
        electionTwo.getElectionId())).thenReturn(voterGhana);

    List<Election> allElections = voterService.getAllElectionsOfVoter(voterGhana.getEmail());

    assertNotNull(allElections);
    assertEquals(2, allElections.size());
    assertTrue(allElections.contains(electionOne));
    assertTrue(allElections.contains(electionTwo));
  }

  @Test
  void checkHasVoted() {
    doThrow(EntityNotFoundException.class).when(electionService).checkExistsElection(777);
    doNothing().when(electionService).checkExistsElection(1);
    when(voterRepository.findByEmailAndElectionId("email", 1)).thenReturn(null);
    when(voterRepository.findByEmailAndElectionId(voterGhana.getEmail(),
        voterGhana.getElectionId())).thenReturn(voterGhana);
    voterGhana.setHasVoted(true);

    assertTrue(voterService.hasVoted(voterGhana.getEmail(), voterGhana.getElectionId()));
    assertThrows(EntityNotFoundException.class, () -> voterService.hasVoted("email", 1));
    assertThrows(EntityNotFoundException.class, () -> voterService.hasVoted("email", 777));
    assertThrows(IllegalArgumentException.class, () -> voterService.hasVoted(null, 1));
  }

}
