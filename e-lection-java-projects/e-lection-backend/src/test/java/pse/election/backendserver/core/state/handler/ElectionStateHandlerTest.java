package pse.election.backendserver.core.state.handler;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import pse.election.backendserver.core.service.TrusteeService;
import pse.election.backendserver.core.state.ElectionState;
import pse.election.backendserver.entity.Election;
import pse.election.backendserver.entity.Trustee;
import pse.election.backendserver.repository.ElectionRepository;

@SpringBootTest(args = "--oidcClients=./oidcClients.toml")
class ElectionStateHandlerTest {

  private static final String RSA_KEY_ONE =
      "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCY1J8Fd0GN5cTwd7m4cZxZIqUQ\n" +
          "BX0SH5nsIe0LDRbaxe26AbM3mc8DT8/NOzRoOInpygVPm+mg5810vERPTgG2Ba7U\n" +
          "bf0UcPqhIQTegkkS0b4G9SbO3maHoJ6C5ckWsyegZVeM307BwVopqkCgVWFBjT45\n" +
          "3BNOdq0eRDs2QjL2cQIDAQAB";

  private static final String RSA_KEY_TWO =
      "MIGeMA0GCSqGSIb3DQEBAQUAA4GMADCBiAKBgHjh5/F6AjYCth5fTgei3IpwTuc4\n" +
          "Isn6guuPjzz2KLzPsf1IQ715KDe6SiQZohcButvFlFd8jultnjvHDrWmE3WrB+dB\n" +
          "I+GO1yIMHqw/acCQ05xtI1qULvabh6hGtEFmbusmcSmXITPo5EwAov1G91xwwHgq\n" +
          "JGlgoszKw7EpY1KzAgMBAAE=";

  private static final String RSA_KEY_THREE =
      "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCNkLdTtDcALem60Qla10XIEXk4\n" +
          "n3dkJTmuJjaokNd/JcfFoxNCIBGz8+vefvQvfECpcBxkXDef+/xJdisRRZN3xBg1\n" +
          "qpgHNKbyk9mFJp/Lnen8vjLjkkMl0Ept6Bykp77lamenMhqwimddsXlFIq2EevPY\n" +
          "90a/1WUM3sh9UA+TSwIDAQAB";

  @Autowired
  private ElectionStateHandler electionStateHandler;

  @Autowired
  private ElectionRepository electionRepository;


  @Autowired
  private TrusteeService trusteeService;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
    electionRepository.deleteAll();
  }

  @Test
  void ElectionStateHandler_from_KEYONE_to_KEYTWO_Success() {
    Election createdElection = new Election();
    createdElection.setState(ElectionState.AUX_KEYS);

    Election storedElection = electionRepository.save(createdElection);

    int trusteeIndex = 1;

    Trustee trustee1 = new Trustee(storedElection.getElectionId(), "first@trustee.de",
        trusteeIndex++);
    Trustee trustee2 = new Trustee(storedElection.getElectionId(), "second@trustee.de",
        trusteeIndex++);
    Trustee trustee3 = new Trustee(storedElection.getElectionId(), "thrird@trustee.de",
        trusteeIndex++);

    trustee1.setAuxkey(RSA_KEY_ONE);
    trustee2.setAuxkey(RSA_KEY_TWO);
    trustee3.setAuxkey(RSA_KEY_THREE);

    trusteeService.addTrustees(List.of(trustee1, trustee2, trustee3));

    electionStateHandler.testAndSet(storedElection, ElectionState.EPKB);

    Assertions.assertEquals(ElectionState.EPKB, storedElection.getState());
  }

  @Test
  void ElectionStateHandler_from_KEYONE_to_KEYTWO_Failure() {
    Election createdElection = new Election();
    createdElection.setState(ElectionState.AUX_KEYS);

    Election storedElection = electionRepository.save(createdElection);

    int trusteeIndex = 1;

    Trustee trustee1 = new Trustee(storedElection.getElectionId(), "first@trustee.de",
        trusteeIndex++);
    Trustee trustee2 = new Trustee(storedElection.getElectionId(), "second@trustee.de",
        trusteeIndex++);
    Trustee trustee3 = new Trustee(storedElection.getElectionId(), "thrird@trustee.de",
        trusteeIndex++);

    trustee1.setAuxkey(RSA_KEY_ONE);
    trustee2.setAuxkey(RSA_KEY_TWO);

    trusteeService.addTrustees(List.of(trustee1, trustee2, trustee3));

    electionStateHandler.testAndSet(storedElection, ElectionState.EPKB);

    Assertions.assertEquals(ElectionState.AUX_KEYS, storedElection.getState());
  }

}