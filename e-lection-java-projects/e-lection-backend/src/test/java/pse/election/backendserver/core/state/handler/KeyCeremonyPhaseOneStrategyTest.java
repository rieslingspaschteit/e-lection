package pse.election.backendserver.core.state.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pse.election.backendserver.core.service.TrusteeService;
import pse.election.backendserver.core.state.ElectionState;
import pse.election.backendserver.entity.Election;
import pse.election.backendserver.entity.Trustee;
import pse.election.backendserver.payload.error.exception.EntityNotFoundException;
import pse.election.backendserver.payload.error.exception.IllegalStateSwitchOperation;

@ExtendWith(MockitoExtension.class)
class KeyCeremonyPhaseOneStrategyTest {

  private static final long ELECTION_ID = 1;
  private static final int THRESHOLD = 1;
  private static final String TRUSTEE_EMAIL = "trustee@email.de";

  @Mock
  private TrusteeService mockTrusteeService;

  @InjectMocks
  private KeyCeremonyPhaseOneStrategy keyCeremonyPhaseOneStrategyUnderTest;

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
    keyCeremonyPhaseOneStrategyUnderTest = new KeyCeremonyPhaseOneStrategy(mockTrusteeService);
  }

  @Test
  void testSwitchState_ElectionKeyCeremonyPhaseOne() {

    when(mockTrusteeService.getAllTrustees(0L)).thenReturn(
        List.of(new Trustee(0L, TRUSTEE_EMAIL, THRESHOLD)));

    ElectionState result = keyCeremonyPhaseOneStrategyUnderTest.switchState(election,
        ElectionState.EPKB);

    assertThat(result).isEqualTo(ElectionState.AUX_KEYS);
  }

  @Test
  void testSwitchState_TrusteeServiceReturnsNoItems() {

    when(mockTrusteeService.getAllTrustees(0L)).thenReturn(Collections.emptyList());

    assertThatThrownBy(
        () -> keyCeremonyPhaseOneStrategyUnderTest.switchState(election, ElectionState.EPKB))
        .isInstanceOf(IllegalStateSwitchOperation.class);
  }

  @Test
  void testSwitchState_TrusteeServiceThrowsEntityNotFoundException() {

    when(mockTrusteeService.getAllTrustees(0L)).thenThrow(EntityNotFoundException.class);

    assertThatThrownBy(
        () -> keyCeremonyPhaseOneStrategyUnderTest.switchState(election, ElectionState.EPKB))
        .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  void testSwitchState_KeyCeremonyPhaseTwo() {

    Trustee trustee1 = new Trustee(0L, TRUSTEE_EMAIL, 1);
    Trustee trustee2 = new Trustee(0L, TRUSTEE_EMAIL, 2);

    trustee1.setAuxkey("AUXKEY_1");
    trustee2.setAuxkey("AUXKEY_2");

    when(mockTrusteeService.getAllTrustees(0L)).thenReturn(List.of(trustee1, trustee2));

    ElectionState result = keyCeremonyPhaseOneStrategyUnderTest.switchState(election,
        ElectionState.EPKB);
    assertThat(result).isEqualTo(ElectionState.EPKB);
  }

  @Test
  void testSwitchState_OneKeyMissing() {

    Trustee trustee1 = new Trustee(0L, TRUSTEE_EMAIL, 1);
    Trustee trustee2 = new Trustee(0L, TRUSTEE_EMAIL, 2);

    trustee1.setAuxkey("AUXKEY_1");

    when(mockTrusteeService.getAllTrustees(0L)).thenReturn(List.of(trustee1, trustee2));

    ElectionState result = keyCeremonyPhaseOneStrategyUnderTest.switchState(election,
        ElectionState.EPKB);
    assertThat(result).isEqualTo(ElectionState.AUX_KEYS);
  }
}
