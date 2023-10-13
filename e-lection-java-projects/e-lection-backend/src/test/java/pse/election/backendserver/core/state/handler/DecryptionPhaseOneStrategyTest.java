package pse.election.backendserver.core.state.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import pse.election.backendserver.core.bot.BotFacade;
import pse.election.backendserver.core.electionguard.DecryptionFacade;
import pse.election.backendserver.core.service.BallotService;
import pse.election.backendserver.core.service.DecryptionService;
import pse.election.backendserver.core.service.ElectionService;
import pse.election.backendserver.core.service.TrusteeService;
import pse.election.backendserver.core.state.ElectionState;
import pse.election.backendserver.entity.Election;
import pse.election.backendserver.entity.PartialDecryption;
import pse.election.backendserver.entity.Trustee;
import pse.election.backendserver.payload.error.exception.EntityNotFoundException;
import pse.election.backendserver.payload.error.exception.IllegalStateSwitchOperation;
import pse.election.backendserver.repository.TrusteeRepository;

@SpringBootTest(args = "--oidcClients=./oidcClients.toml")
class DecryptionPhaseOneStrategyTest {

  @Mock
  private DecryptionService mockDecryptionService;
  @Mock
  private ElectionService mockElectionService;
  @Mock
  private TrusteeService mockTrusteeService;
  @Mock
  private DecryptionFacade mockDecryptionFacade;
  @Mock
  private BallotService mockBallotService;
  @Mock
  private BotFacade mockBotFacade;

  @Autowired
  private TrusteeRepository trusteeRepository;

  @InjectMocks
  private DecryptionPhaseOneStrategy decryptionPhaseOneStrategyUnderTest;

  private Election election;

  @BeforeEach
  void setUp() {
    election = new Election(
        Date.from(Instant.parse("2020-01-01T00:00:00Z")),
        "title",
        "description",
        "authorityEmail",
        false,
        0);

    decryptionPhaseOneStrategyUnderTest = new DecryptionPhaseOneStrategy(
        mockTrusteeService, mockDecryptionFacade, mockBotFacade);
  }

  @Test
  void testSwitchState_toDecPhaseTwo() {
    final List<PartialDecryption> partialDecryptions = List.of(
        new PartialDecryption(0L, 0L, 0L, "decryption", "proof"));

    when(mockDecryptionService.getAllPartialDecryptionByTrustee(0L)).thenReturn(partialDecryptions);

    when(mockTrusteeService.getAllTrustees(0L)).thenReturn(List.of(new Trustee(0L, "email", 0)));

    final ElectionState result = decryptionPhaseOneStrategyUnderTest.switchState(election,
        ElectionState.PP_DECRYPTION);

    assertThat(result).isEqualTo(ElectionState.PP_DECRYPTION);
  }

  @Test
  void testSwitchState_toPhaseDone() {
    final List<PartialDecryption> partialDecryptions = List.of(
        new PartialDecryption(0L, 0L, 0L, "decryption", "proof"));

    when(mockDecryptionService.getAllPartialDecryptionByTrustee(0L)).thenReturn(partialDecryptions);
    Trustee trustee = new Trustee(0L, "email", 0);
    trustee.setAvailable();
    when(mockTrusteeService.getAllTrustees(0L)).thenReturn(List.of(trustee));

    final ElectionState result = decryptionPhaseOneStrategyUnderTest.switchState(election,
        ElectionState.DONE);

    assertThat(result).isEqualTo(ElectionState.DONE);
  }

  @Test
  void testSwitchState_toPhaseDone_NotEnoughDecryption() {
    final List<PartialDecryption> partialDecryptions = List.of(
        new PartialDecryption(0L, 0L, 0L, "decryption", "proof"));

    Trustee trustee1 = trusteeRepository.save(new Trustee(0L, "email", 0));
    Trustee trustee2 = trusteeRepository.save(new Trustee(0L, "email2", 1));

    when(
        mockDecryptionService.getAllPartialDecryptionByTrustee(trustee1.getTrusteeId())).thenReturn(
        partialDecryptions);
    when(
        mockDecryptionService.getAllPartialDecryptionByTrustee(trustee2.getTrusteeId())).thenReturn(
        Collections.emptyList());

    when(mockTrusteeService.getAllTrustees(0L)).thenReturn(List.of(trustee1, trustee2));

    final ElectionState result = decryptionPhaseOneStrategyUnderTest.switchState(election,
        ElectionState.DONE);

    assertThat(result).isEqualTo(ElectionState.P_DECRYPTION);
  }

  @Test
  void testSwitchState_TrusteeServiceReturnsNoItems() {
    when(mockTrusteeService.getAllTrustees(0L)).thenReturn(Collections.emptyList());

    final List<PartialDecryption> partialDecryptions = List.of(
        new PartialDecryption(0L, 0L, 0L, "decryption", "proof"));
    when(mockDecryptionService.getAllPartialDecryptionByTrustee(0L)).thenReturn(partialDecryptions);

    assertThatThrownBy(() -> decryptionPhaseOneStrategyUnderTest.switchState(election,
        ElectionState.PP_DECRYPTION)).isInstanceOf(IllegalStateSwitchOperation.class);
  }

  @Test
  void testSwitchState_TrusteeServiceThrowsEntityNotFoundException() {

    when(mockTrusteeService.getAllTrustees(0L)).thenThrow(EntityNotFoundException.class);

    assertThatThrownBy(() -> decryptionPhaseOneStrategyUnderTest.switchState(election,
        ElectionState.PP_DECRYPTION)).isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  void testSwitchState_DecryptionServiceReturnsNoItems_NotEnoughDecryptions() {

    Election election = new Election(Date.from(Instant.parse("2020-01-01T00:00:00Z")),
        "title", "description", "authorityEmail", false, 2);

    when(mockTrusteeService.getAllTrustees(0L)).thenReturn(List.of(new Trustee(0L, "email", 0)));
    when(mockDecryptionService.getAllPartialDecryptionByTrustee(0L)).thenReturn(
        Collections.emptyList());

    final ElectionState result = decryptionPhaseOneStrategyUnderTest.switchState(election,
        ElectionState.PP_DECRYPTION);

    assertThat(result).isEqualTo(ElectionState.P_DECRYPTION);
  }

  @Test
  void testInitialiseState_electionHasBot() {

    Election election = new Election(Date.from(Instant.parse("2020-01-01T00:00:00Z")),
        "title", "description", "authorityEmail", true, 2);

    decryptionPhaseOneStrategyUnderTest.initialiseState(election);

    verify(mockDecryptionFacade).computeLagrangeCoefficients(any(Election.class));
    verify(mockDecryptionFacade).combineOptionsEncryptedToTallies(any(Election.class));
    verify(mockBotFacade).partialDecryption(any(Election.class));
  }

  @Test
  void testInitialiseState_noBot() {
    decryptionPhaseOneStrategyUnderTest.initialiseState(election);

    verify(mockDecryptionFacade).computeLagrangeCoefficients(any(Election.class));
    verify(mockDecryptionFacade).combineOptionsEncryptedToTallies(any(Election.class));
  }
}
