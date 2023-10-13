package pse.election.backendserver.core.state.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pse.election.backendserver.core.bot.BotFacade;
import pse.election.backendserver.core.electionguard.KeyCeremonyFacade;
import pse.election.backendserver.core.service.TrusteeService;
import pse.election.backendserver.core.state.ElectionState;
import pse.election.backendserver.entity.Election;
import pse.election.backendserver.entity.Trustee;
import pse.election.backendserver.payload.error.exception.EntityNotFoundException;
import pse.election.backendserver.payload.error.exception.IllegalStateSwitchOperation;

@ExtendWith(MockitoExtension.class)
class KeyCeremonyPhaseTwoStrategyTest {

  @Mock
  private TrusteeService mockTrusteeService;
  @Mock
  private KeyCeremonyFacade mockKeyCeremonyFacade;
  @Mock
  private BotFacade mockBotFacade;

  @InjectMocks
  private KeyCeremonyPhaseTwoStrategy keyCeremonyPhaseTwoStrategyUnderTest;

  private Election election;

  @BeforeEach
  void setUp() {
    election = new Election(
        Date.from(Instant.parse("2020-01-01T00:00:00Z")),
        "title",
        "description",
        "authorityEmail",
        false,
        1
    );
    keyCeremonyPhaseTwoStrategyUnderTest = new KeyCeremonyPhaseTwoStrategy(mockTrusteeService, mockBotFacade);
  }

  @Test
  void testSwitchState_OnlyTrusteeWithNoKey() {

    when(mockTrusteeService.getAllTrustees(0L)).thenReturn(List.of(new Trustee(0L, "email", 1)));

    final ElectionState result = keyCeremonyPhaseTwoStrategyUnderTest.switchState(election,
        ElectionState.KEYCEREMONY_FINISHED);

    assertThat(result).isEqualTo(ElectionState.EPKB);
  }

  @Test
  void testSwitchState_OneTrusteeWithNoKey() {

    Trustee trustee1 = new Trustee(0L, "email", 1);
    Trustee trustee2 = new Trustee(0L, "email", 2);

    trustee1.addPublicElgamalKeyAndProof(List.of("KEYS_AND_BACKUPS"));

    when(mockTrusteeService.getAllTrustees(0L)).thenReturn(List.of(trustee1, trustee2));

    ElectionState result = keyCeremonyPhaseTwoStrategyUnderTest.switchState(election,
        ElectionState.KEYCEREMONY_FINISHED);

    assertThat(result).isEqualTo(ElectionState.EPKB);
  }

  @Test
  void testSwitchState_BothTrusteeWithKeys() {
    Trustee trustee1 = new Trustee(0L, "email", 1);
    Trustee trustee2 = new Trustee(0L, "email", 2);

    trustee1.addPublicElgamalKeyAndProof(List.of("KEYS_AND_BACKUPS"));
    trustee2.addPublicElgamalKeyAndProof(List.of("KEYS_AND_BACKUPS"));
    trustee1.setWaiting(true);
    trustee2.setWaiting(true);

    when(mockTrusteeService.getAllTrustees(0L)).thenReturn(List.of(trustee1, trustee2));

    ElectionState result = keyCeremonyPhaseTwoStrategyUnderTest.switchState(election,
        ElectionState.KEYCEREMONY_FINISHED);

    assertThat(result).isEqualTo(ElectionState.KEYCEREMONY_FINISHED);
  }

  @Test
  void testSwitchState_TrusteeServiceReturnsNoItems() {
    when(mockTrusteeService.getAllTrustees(0L)).thenReturn(Collections.emptyList());

    assertThatThrownBy(() -> keyCeremonyPhaseTwoStrategyUnderTest.switchState(election,
        ElectionState.KEYCEREMONY_FINISHED))
        .isInstanceOf(IllegalStateSwitchOperation.class);
  }

  @Test
  void testSwitchState_TrusteeServiceThrowsEntityNotFoundException() {

    when(mockTrusteeService.getAllTrustees(0L)).thenThrow(EntityNotFoundException.class);

    assertThatThrownBy(() -> keyCeremonyPhaseTwoStrategyUnderTest.switchState(election,
        ElectionState.KEYCEREMONY_FINISHED))
        .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  void testInitialiseState() {
    Trustee trustee1 = new Trustee(0L, "email", 1);
    Trustee trustee2 = new Trustee(0L, "email", 2);
    trustee1.setWaiting(true);
    trustee2.setWaiting(true);

    doAnswer(invocation -> {
      trustee1.setWaiting(false);
      trustee2.setWaiting(false);
      return List.of(trustee1, trustee2);
    }).when(mockTrusteeService).updateIsWaitingTrustee(0L);

    keyCeremonyPhaseTwoStrategyUnderTest.initialiseState(election);

    Assertions.assertFalse(trustee1.isWaiting());
    Assertions.assertFalse(trustee2.isWaiting());
  }
}
