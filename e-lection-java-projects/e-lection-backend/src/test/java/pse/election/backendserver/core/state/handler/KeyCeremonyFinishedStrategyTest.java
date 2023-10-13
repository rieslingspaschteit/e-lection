package pse.election.backendserver.core.state.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.sql.Date;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pse.election.backendserver.core.electionguard.HashFacade;
import pse.election.backendserver.core.electionguard.KeyCeremonyFacade;
import pse.election.backendserver.core.service.TrusteeService;
import pse.election.backendserver.core.state.ElectionState;
import pse.election.backendserver.entity.Election;
import pse.election.backendserver.entity.Trustee;

@ExtendWith(MockitoExtension.class)
class KeyCeremonyFinishedStrategyTest {

  @Mock
  private KeyCeremonyFacade mockKeyCeremonyFacade;
  @Mock
  private HashFacade mockHashFacade;

  @Mock
  private TrusteeService mockTrusteeService;

  @InjectMocks
  private KeyCeremonyFinishedStrategy keyCeremonyFinishedStrategyUnderTest;

  private Election election;

  @BeforeEach
  void setUp() {
    election = new Election(
        Date.from(Instant.parse("2020-01-01T00:00:00Z")),
        "title",
        "description",
        "authorityEmail",
        false,
        1);
    keyCeremonyFinishedStrategyUnderTest = new KeyCeremonyFinishedStrategy(mockKeyCeremonyFacade, mockTrusteeService);
  }

  @Test
  void testSwitchState() {
    assertThat(keyCeremonyFinishedStrategyUnderTest.switchState(election, ElectionState.OPEN))
        .isEqualTo(ElectionState.OPEN);
  }

  @Test
  void testInitialiseState() {
    Trustee trustee1 = new Trustee(1, "trustee1@example.com", 1);
    Trustee trustee2 = new Trustee(1, "trustee2@example.com", 2);

    doAnswer(invocation -> {
      trustee1.setWaiting(false);
      trustee2.setWaiting(false);
      return List.of(trustee1, trustee2);
    }).when(mockTrusteeService).updateIsWaitingTrustee(election.getElectionId());
    when(mockKeyCeremonyFacade.combineKeys(any(Election.class))).thenReturn(new BigInteger("100"));
    //when(mockHashFacade.generateElectionFingerprint(any(Election.class))).thenReturn("fingerprint");

    keyCeremonyFinishedStrategyUnderTest.initialiseState(election);

    assertFalse(trustee1.isWaiting() && trustee2.isWaiting());
    assertTrue(election.getPublicKey().equals(new BigInteger("100")));
    assertSame(election.getFingerprint(), null);
  }
}
