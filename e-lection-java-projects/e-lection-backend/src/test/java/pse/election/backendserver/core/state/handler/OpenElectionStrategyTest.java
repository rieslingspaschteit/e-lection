package pse.election.backendserver.core.state.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pse.election.backendserver.core.electionguard.HashFacade;
import pse.election.backendserver.core.service.ElectionService;
import pse.election.backendserver.core.state.ElectionState;
import pse.election.backendserver.entity.Election;
import pse.election.backendserver.payload.error.exception.IllegalStateSwitchOperation;

@ExtendWith(MockitoExtension.class)
class OpenElectionStrategyTest {

  @Mock
  private ElectionService mockElectionService;
  @Mock
  private HashFacade hashfacade;

  private OpenElectionStrategy openElectionStrategyUnderTest;

  private Election election;

  @BeforeEach
  void setUp() {

    election = new Election(Date.from(Instant.parse("2020-01-01T00:00:00Z")),
        "title",
        "description",
        "authorityEmail",
        false,
        0
    );
    openElectionStrategyUnderTest = new OpenElectionStrategy(hashfacade);
  }


  @Test
  void testInitalizeState_EndDateNotReachedWhenStateSwitch() {

    Date endDate = Date.from(Instant.now().plus(Duration.ofSeconds(12)));

    Election election = new Election(endDate,
        "title",
        "description",
        "authorityEmail",
        false,
        0
    );

    openElectionStrategyUnderTest.initialiseState(election);

    assertThat(election.getStartTime()).isNotNull();
  }

  @Test
  void testInitialiseState_EndDateAlreadyPassedWhenOpening() {

    Date endDate = Date.from(Instant.now().minusSeconds(12));

    Election election = new Election(endDate,
        "title",
        "description",
        "authorityEmail",
        false,
        0
    );

    assertThatThrownBy(() -> openElectionStrategyUnderTest.initialiseState(election))
        .isInstanceOf(IllegalStateSwitchOperation.class);
  }

  @Test
  void testSwitchState() {

    Date endDate = Date.from(Instant.now().minusSeconds(12));

    Election election1 = new Election(endDate, "title", "description",
        "authorityEmail", false, 0);

    ElectionState response = openElectionStrategyUnderTest.switchState(election1,
        ElectionState.DONE);
    assertThat(response).isEqualTo(ElectionState.P_DECRYPTION);

  }

}
