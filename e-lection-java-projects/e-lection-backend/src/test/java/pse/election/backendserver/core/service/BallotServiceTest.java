package pse.election.backendserver.core.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pse.election.backendserver.core.electionguard.HashFacade;
import pse.election.backendserver.core.electionguard.VerificationFacade;
import pse.election.backendserver.core.state.ElectionState;
import pse.election.backendserver.core.state.handler.ElectionStateHandler;
import pse.election.backendserver.entity.*;
import pse.election.backendserver.payload.dto.BallotProofDTO;
import pse.election.backendserver.payload.dto.ChaumPedersenProofDTO;
import pse.election.backendserver.payload.dto.ConstantChaumPedersenDTO;
import pse.election.backendserver.payload.dto.DisjunctiveChaumPedersenDTO;
import pse.election.backendserver.payload.dto.EncryptedOptionDTO;
import pse.election.backendserver.payload.error.exception.EntityNotFoundException;
import pse.election.backendserver.payload.error.exception.IllegalProofException;
import pse.election.backendserver.payload.error.exception.IllegalStateOperationException;
import pse.election.backendserver.repository.BallotRepository;
import pse.election.backendserver.repository.OptionEncryptedRepository;

@ExtendWith(MockitoExtension.class)
class BallotServiceTest {

  @Mock
  private BallotRepository mockBallotRepository;

  @Mock
  private OptionEncryptedRepository mockOptionEncryptedRepository;

  @InjectMocks
  private BallotService ballotService;

  @Mock
  private ElectionService mockElectionService;

  @Mock
  private ElectionStateHandler mockElectionStateHandler;

  @Mock
  private VoterService mockVoterService;

  @Mock
  private VerificationFacade mockVerificationFacade;
  @Mock
  HashFacade hashfacade;

  @Mock
  private Ballot mockBallot;
  private BallotProofDTO ballotProofDTO;

  private Election election;




  @BeforeEach
  void setUp() {
    election = new Election(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)), "1", "", "a@.d", true, 1);
    election.setStartTime(Date.from(Instant.now().minus(10, ChronoUnit.SECONDS)));
    BigInteger one = BigInteger.ONE;
    BigInteger two = BigInteger.TWO;
    BigInteger ten = BigInteger.TEN;
    EncryptedOptionDTO option = new EncryptedOptionDTO(one, two);
    ChaumPedersenProofDTO proofB = new ChaumPedersenProofDTO(one, two, ten, two);
    DisjunctiveChaumPedersenDTO disProof = new DisjunctiveChaumPedersenDTO(proofB, proofB, ten);
    Date date = Date.from(Instant.now());
    ballotProofDTO = new BallotProofDTO(
            Map.of(0, new EncryptedOptionDTO[]{option, option, option, option, option, option}),
            Map.of(0, new DisjunctiveChaumPedersenDTO[]{disProof, disProof, disProof, disProof, disProof, disProof}),
            Map.of(0, new ConstantChaumPedersenDTO(proofB, 2)),
            "", date);

    when(mockElectionService.getElection(anyLong())).thenReturn(election);
  }

  @Test
  void addBallot_keyCeremonyPhaseOneState() {
    setUp();
    election.setState(ElectionState.KEYCEREMONY_FINISHED);
    assertThrows(IllegalStateOperationException.class,
        () -> ballotService.addBallot(ballotProofDTO, 0L, "ballotId", "a@gmail.com"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"contest+", "contest-", "options+", "options-"})
  void addBallot_ConsistentInvalidFormat(String cause) {
    setUp();
    List<Contest> contests = switch (cause) {
      case "contest+" -> List.of(new Contest(election.getElectionId(), "A", 6, 0, List.of()),
              new Contest(election.getElectionId(), "A", 6, 1, List.of()));
      case "contest-" -> List.of();
      case "options+" -> List.of(new Contest(election.getElectionId(), "A", 6, 0, List.of("a")));
      case "options-" -> List.of(new Contest(election.getElectionId(), "A", 5, 0, List.of()));
      default -> null;
    };
    election.setState(ElectionState.OPEN);
    when(mockElectionService.getAllContestsOfElection(election.getElectionId())).thenReturn(contests);
    when(mockElectionStateHandler.testAndSet(any(Election.class),
            any(ElectionState.class))).thenReturn(ElectionState.OPEN);
    when(mockElectionService.getElection(anyLong())).thenReturn(election);
    assertThrows(IllegalProofException.class,
            () -> ballotService.addBallot(ballotProofDTO, 0L, "ballotId", "a@gmail.com"));
  }

  @Test
  void addBallot_InvalidDate() {
    BigInteger one = BigInteger.ONE;
    BigInteger two = BigInteger.TWO;
    BigInteger ten = BigInteger.TEN;
    EncryptedOptionDTO option = new EncryptedOptionDTO(one, two);
    ChaumPedersenProofDTO proofB = new ChaumPedersenProofDTO(one, two, ten, two);
    DisjunctiveChaumPedersenDTO disProof = new DisjunctiveChaumPedersenDTO(proofB, proofB, ten);
    BallotProofDTO ballotProofDTO1 = new BallotProofDTO(
            Map.of(0, new EncryptedOptionDTO[]{option, option}),
            Map.of(0, new DisjunctiveChaumPedersenDTO[]{disProof, disProof},
                    1, new DisjunctiveChaumPedersenDTO[]{disProof, disProof}),
            Map.of(0, new ConstantChaumPedersenDTO(proofB, 2)),
            "", Date.from(election.getEndTime().toInstant().plus(1, ChronoUnit.HOURS)));
    BallotProofDTO ballotProofDTO2 = new BallotProofDTO(
            Map.of(0, new EncryptedOptionDTO[]{option, option}),
            Map.of(0, new DisjunctiveChaumPedersenDTO[]{disProof, disProof},
                    1, new DisjunctiveChaumPedersenDTO[]{disProof, disProof}),
            Map.of(0, new ConstantChaumPedersenDTO(proofB, 2)),
            "", Date.from(election.getStartTime().toInstant().minus(1, ChronoUnit.HOURS)));
    election.setState(ElectionState.OPEN);

    when(mockElectionStateHandler.testAndSet(any(Election.class),
            any(ElectionState.class))).thenReturn(ElectionState.OPEN);
    when(mockElectionService.getElection(anyLong())).thenReturn(election);
    assertEquals("Encryption date must be after election start must not be in the future",
            assertThrows(IllegalProofException.class, () -> ballotService.addBallot(ballotProofDTO1, election.getElectionId(),
                    "123", "voter@mail.de")).getMessage());
    assertEquals("Encryption date must be after election start must not be in the future",
            assertThrows(IllegalProofException.class, () -> ballotService.addBallot(ballotProofDTO2, election.getElectionId(),
                    "123", "voter@mail.de")).getMessage());
  }

  /**
   * Ballot contains null or error is in number of proofs
   */
  @Test
  void addBallot_InconsistentFormat() {
    BigInteger one = BigInteger.ONE;
    BigInteger two = BigInteger.TWO;
    BigInteger ten = BigInteger.TEN;
    EncryptedOptionDTO option = new EncryptedOptionDTO(one, two);
    ChaumPedersenProofDTO proofB = new ChaumPedersenProofDTO(one, two, ten, two);
    DisjunctiveChaumPedersenDTO disProof = new DisjunctiveChaumPedersenDTO(proofB, proofB, ten);
    Date date = Date.from(Instant.now());
    Map<Integer, EncryptedOptionDTO[]> nullMap1 = new HashMap<>();
    nullMap1.put(0, null);
    Map<Integer, DisjunctiveChaumPedersenDTO[]> nullMap2 = new HashMap<>();
    nullMap2.put(0, null);
    Map<Integer, ConstantChaumPedersenDTO> nullMap3 = new HashMap<>();
    nullMap3.put(0, null);
    BallotProofDTO ballotProofDTO1 = new BallotProofDTO(
            Map.of(0, new EncryptedOptionDTO[]{option, option}),
            Map.of(0, new DisjunctiveChaumPedersenDTO[]{disProof, disProof},
                    1, new DisjunctiveChaumPedersenDTO[]{disProof, disProof}),
            Map.of(0, new ConstantChaumPedersenDTO(proofB, 2)),
            "", date);
    BallotProofDTO ballotProofDTO2 = new BallotProofDTO(
            Map.of(0, new EncryptedOptionDTO[]{option, option}),
            Map.of(0, new DisjunctiveChaumPedersenDTO[]{disProof, disProof}),
            Map.of(0, new ConstantChaumPedersenDTO(proofB, 2),
                    1, new ConstantChaumPedersenDTO(proofB, 2)),
            "", date);
    BallotProofDTO ballotProofDTO3 = new BallotProofDTO(
            Map.of(0, new EncryptedOptionDTO[]{option, option}),
            Map.of(0, new DisjunctiveChaumPedersenDTO[]{disProof}),
            Map.of(0, new ConstantChaumPedersenDTO(proofB, 2)),
            "", date);
    BallotProofDTO ballotProofDTO4 = new BallotProofDTO(
            nullMap1,
            Map.of(0, new DisjunctiveChaumPedersenDTO[]{disProof, disProof}),
            Map.of(0, new ConstantChaumPedersenDTO(proofB, 2)),
            "", date);
    BallotProofDTO ballotProofDTO5 = new BallotProofDTO(
            Map.of(0, new EncryptedOptionDTO[]{option, option}),
            nullMap2,
            Map.of(0, new ConstantChaumPedersenDTO(proofB, 2)),
            "", date);
    BallotProofDTO ballotProofDTO6 = new BallotProofDTO(
            Map.of(0, new EncryptedOptionDTO[]{option, option}),
            Map.of(0, new DisjunctiveChaumPedersenDTO[]{disProof, disProof}),
            nullMap3,
            "", date);
    BallotProofDTO ballotProofDTO7 = new BallotProofDTO(
            Map.of(0, new EncryptedOptionDTO[]{option, null}),
            Map.of(0, new DisjunctiveChaumPedersenDTO[]{disProof, disProof}),
            Map.of(0, new ConstantChaumPedersenDTO(proofB, 2)),
            "", date);
    BallotProofDTO ballotProofDTO8 = new BallotProofDTO(
            Map.of(0, new EncryptedOptionDTO[]{option, option}),
            Map.of(0, new DisjunctiveChaumPedersenDTO[]{disProof, null}),
            Map.of(0, new ConstantChaumPedersenDTO(proofB, 2)),
            "", date);
    election.setState(ElectionState.OPEN);
    when(mockElectionService.getAllContestsOfElection(election.getElectionId())).thenReturn(List.of(
            new Contest(election.getElectionId(), "A", 6, 0, List.of())));

    when(mockElectionStateHandler.testAndSet(any(Election.class),
            any(ElectionState.class))).thenReturn(ElectionState.OPEN);
    when(mockElectionService.getElection(anyLong())).thenReturn(election);
    assertEquals("The ballot does not contain valid proofs.",
            assertThrows(IllegalProofException.class, () -> ballotService.addBallot(ballotProofDTO1, election.getElectionId(),
            "123", "voter@mail.de")).getMessage());
    assertEquals("The ballot does not contain valid proofs.",
            assertThrows(IllegalProofException.class, () -> ballotService.addBallot(ballotProofDTO2, election.getElectionId(),
                    "123", "voter@mail.de")).getMessage());
    assertEquals("The ballot does not contain valid proofs.",
            assertThrows(IllegalProofException.class, () -> ballotService.addBallot(ballotProofDTO3, election.getElectionId(),
                    "123", "voter@mail.de")).getMessage());
    assertEquals("The ballot does not contain valid proofs.",
            assertThrows(IllegalProofException.class, () -> ballotService.addBallot(ballotProofDTO4, election.getElectionId(),
                    "123", "voter@mail.de")).getMessage());
    assertEquals("The ballot does not contain valid proofs.",
            assertThrows(IllegalProofException.class, () -> ballotService.addBallot(ballotProofDTO5, election.getElectionId(),
                    "123", "voter@mail.de")).getMessage());
    assertEquals("The ballot does not contain valid proofs.",
            assertThrows(IllegalProofException.class, () -> ballotService.addBallot(ballotProofDTO6, election.getElectionId(),
                    "123", "voter@mail.de")).getMessage());
    assertEquals("The ballot does not contain valid proofs.",
            assertThrows(IllegalProofException.class, () -> ballotService.addBallot(ballotProofDTO7, election.getElectionId(),
                    "123", "voter@mail.de")).getMessage());
    assertEquals("The ballot does not contain valid proofs.",
            assertThrows(IllegalProofException.class, () -> ballotService.addBallot(ballotProofDTO8, election.getElectionId(),
                    "123", "voter@mail.de")).getMessage());

  }

  @Test
  void addBallot_InvalidProofs() {
    setUp();
    election.setState(ElectionState.OPEN);
    when(mockElectionService.getAllContestsOfElection(election.getElectionId())).thenReturn(List.of(
            new Contest(election.getElectionId(), "A", 6, 0, List.of())));
    when(mockElectionStateHandler.testAndSet(any(Election.class),
        any(ElectionState.class))).thenReturn(ElectionState.OPEN);
    when(mockElectionService.getElection(anyLong())).thenReturn(election);
    when(mockVerificationFacade.verifyBallot(any(BallotProofDTO.class),
        any(Election.class))).thenReturn(false);

    assertThrows(IllegalProofException.class,
        () -> ballotService.addBallot(ballotProofDTO, 0L, "ballotId", "a@gmail.com"));
  }

  @Test
  void addBallot_voterAlreadyVoted() {
    setUp();

    election.setState(ElectionState.OPEN);
    when(mockElectionStateHandler.testAndSet(any(Election.class),
        any(ElectionState.class))).thenReturn(ElectionState.OPEN);
    when(mockElectionService.getElection(anyLong())).thenReturn(election);
    when(mockVoterService.hasVoted(any(String.class), anyLong())).thenReturn(true);

    assertThrows(IllegalStateOperationException.class,
        () -> ballotService.addBallot(ballotProofDTO, 0L, "ballotId", "a@gmail.com"));
  }

  @Test
  void addBallot_validProofs() {
    setUp();

    election.setState(ElectionState.OPEN);
    when(mockElectionService.getAllContestsOfElection(election.getElectionId())).thenReturn(List.of(
            new Contest(election.getElectionId(), "A", 6, 0, List.of())));
    Ballot ballot = new Ballot();

    when(mockElectionStateHandler.testAndSet(any(Election.class),
        any(ElectionState.class))).thenReturn(ElectionState.OPEN);
    when(mockElectionService.getElection(anyLong())).thenReturn(election);
    when(mockVerificationFacade.verifyBallot(any(BallotProofDTO.class),
        any(Election.class))).thenReturn(true);
    when(mockBallotRepository.save(any(Ballot.class))).thenReturn(ballot);

    Ballot response = ballotService.addBallot(ballotProofDTO, 0L, "ballotId", "a@gmail.com");
    assertEquals(ballot, response);
  }

  @Test
  void getAllOptionsEncryptedOfBallotSorted() {
    List<OptionEncrypted> orgOptions = new ArrayList<>(List.of(
            new OptionEncrypted(0L, election.getElectionId(), 1, 0, "B", "", "", ""),
            new OptionEncrypted(0L, election.getElectionId(), 0, 1, "C", "", "", ""),
            new OptionEncrypted(0L, election.getElectionId(), 0, 0, "A", "", "", "")));
    when(mockOptionEncryptedRepository.findByBallotId(0L)).thenReturn(orgOptions);
    when(mockOptionEncryptedRepository.existsById(0L)).thenReturn(true);
    mockElectionService.getElection(election.getElectionId());
    List<OptionEncrypted> options = ballotService.getAllOptionsEncryptedOfBallot(0L);
    assertEquals(10, options.get(0).getCiphertextPAD().intValue());
    assertEquals(11, options.get(1).getCiphertextPAD().intValue());
    assertEquals(12, options.get(2).getCiphertextPAD().intValue());
  }

  @Test
  void convertBallotToSpoiled() {

    Voter voter = new Voter();

    election.setState(ElectionState.DONE);
    when(mockElectionService.getElection(anyLong())).thenReturn(election);
    assertThrows(IllegalStateOperationException.class, () -> ballotService.convertSpoiledToSubmitted(mockBallot.getLatestTrackingCode(), election.getElectionId(), "a@gmail.com"));
    election.setState(ElectionState.OPEN);
    assertThrows(IllegalStateOperationException.class, () -> ballotService.convertSpoiledToSubmitted(mockBallot.getLatestTrackingCode(), election.getElectionId(), "a@gmail.com"));
    when(mockElectionStateHandler.testAndSet(any(), any())).thenReturn(ElectionState.OPEN);
    assertThrows(IllegalArgumentException.class, () -> ballotService.convertSpoiledToSubmitted(null, election.getElectionId(), "a@gmail.com"));
    assertThrows(EntityNotFoundException.class, () -> ballotService.convertSpoiledToSubmitted("123", election.getElectionId(), "a@gmail.com"));
    assertThrows(EntityNotFoundException.class, (() -> ballotService.convertSpoiledToSubmitted("123", election.getElectionId() + 1, "a@gmail.com")));
    when(mockVoterService.getVoter("a@gmail.com", election.getElectionId())).thenReturn(voter);
    when(mockVoterService.isVoterInElection("a@gmail.com", election.getElectionId())).thenReturn(true);
    when(mockBallotRepository.existsByLatestTrackingCode("123")).thenReturn(true);
    assertThrows(EntityNotFoundException.class, () -> ballotService.convertSpoiledToSubmitted("123", election.getElectionId(), "a@gmail.com"));
    when(mockBallotRepository.existsByElectionId(election.getElectionId())).thenReturn(true);
    when(mockBallotRepository.findByLatestTrackingCodeAndElectionId("123", election.getElectionId())).thenReturn(new Ballot());
    assertDoesNotThrow(() -> ballotService.convertSpoiledToSubmitted("123", election.getElectionId(), "a@gmail.com"));
    assertThrows(EntityNotFoundException.class, () -> ballotService.convertSpoiledToSubmitted("123", election.getElectionId(), "q@gmail.com"));
  }

}