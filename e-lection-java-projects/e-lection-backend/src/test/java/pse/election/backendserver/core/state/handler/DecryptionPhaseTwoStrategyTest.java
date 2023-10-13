package pse.election.backendserver.core.state.handler;

import static org.mockito.Mockito.when;

import java.sql.Date;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pse.election.backendserver.core.bot.BotFacade;
import pse.election.backendserver.core.service.DecryptionService;
import pse.election.backendserver.core.service.TrusteeService;
import pse.election.backendserver.core.state.ElectionState;
import pse.election.backendserver.entity.Election;
import pse.election.backendserver.entity.PartialDecryption;
import pse.election.backendserver.entity.PartialPartialDecryption;
import pse.election.backendserver.entity.Trustee;
import pse.election.backendserver.payload.error.exception.IllegalStateSwitchOperation;

@ExtendWith(MockitoExtension.class)
public class DecryptionPhaseTwoStrategyTest {

  @Mock
  private TrusteeService trusteeService;

  @Mock
  private DecryptionService decryptionService;

  @Mock
  private BotFacade botFacade;

  @InjectMocks
  private DecryptionPhaseTwoStrategy decryptionPhaseTwoStrategy;

  private Election election;
  private Trustee trustee1;
  private Trustee trustee2;
  private List<Trustee> trustees;
  private PartialDecryption pDecryption;
  private List<PartialDecryption> pDecryptionList;

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

    decryptionPhaseTwoStrategy = new DecryptionPhaseTwoStrategy(trusteeService, decryptionService,
        botFacade);
    trustee1 = new Trustee(election.getElectionId(), "trustee1@example.com", 1);
    trustee2 = new Trustee(election.getElectionId(), "trustee2@example.com", 2);
    trustees = List.of(trustee1, trustee2);
    pDecryption = new PartialDecryption(5, 1, -1, "decryption", "proof");
    pDecryptionList = List.of(pDecryption);
  }

  @Test
  void checkSwitchState_invalidNextStateAndNonExistentTrustees() {
    election.setState(ElectionState.OPEN);

    when(trusteeService.getAllTrustees(election.getElectionId())).thenReturn(new ArrayList<>());

    Assertions.assertThrows(IllegalStateSwitchOperation.class,
        () -> decryptionPhaseTwoStrategy.switchState(election, ElectionState.DONE));

    Assertions.assertThrows(IllegalStateSwitchOperation.class,
        () -> decryptionPhaseTwoStrategy.switchState(election, ElectionState.DONE));
  }

  @Test
  void checkSwitchState_invalidAmountOfPartialPartialDecryptionDelivered() {
    election.setState(ElectionState.PP_DECRYPTION);

    PartialDecryption pDecryption = new PartialDecryption(5, 1, -1, "decryption", "proof");
    List<PartialDecryption> pDecryptionList = List.of(pDecryption);

    when(trusteeService.getAllTrustees(election.getElectionId())).thenReturn(trustees);
    trustee1.setAvailable();
    when(decryptionService.getAllPartialPartialDecryptionOfTrustee(
        trustee1.getTrusteeId())).thenReturn(new ArrayList<>());

    ElectionState newState = decryptionPhaseTwoStrategy.switchState(election, ElectionState.DONE);

    Assertions.assertEquals(ElectionState.PP_DECRYPTION, newState);
  }

  @Test
  void checkSwitchState_validAmountOfPartialPartialDecryptionDelivered() {
    election.setState(ElectionState.PP_DECRYPTION);

    PartialPartialDecryption ppDecryption = new PartialPartialDecryption(1, 2, 5, -1, "decryption",
        "proof");
    List<PartialPartialDecryption> ppDecryptionList = List.of(ppDecryption);

    when(trusteeService.getAllTrustees(election.getElectionId())).thenReturn(trustees);
    trustee1.setAvailable();
    when(decryptionService.getAllPartialPartialDecryptionOfTrustee(
        trustee1.getTrusteeId())).thenReturn(ppDecryptionList);

    ElectionState newState = decryptionPhaseTwoStrategy.switchState(election, ElectionState.DONE);

    Assertions.assertEquals(ElectionState.DONE, newState);
  }

}
