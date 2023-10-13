package pse.election.backendserver.core.service;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.sql.Date;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pse.election.backendserver.core.electionguard.VerificationFacade;
import pse.election.backendserver.core.state.ElectionState;
import pse.election.backendserver.entity.Election;
import pse.election.backendserver.entity.Trustee;
import pse.election.backendserver.payload.dto.ElgamalProofDTO;
import pse.election.backendserver.payload.dto.SchnorrProofDTO;
import pse.election.backendserver.payload.error.exception.IllegalStateOperationException;
import pse.election.backendserver.payload.error.exception.InvalidConfigurationException;
import pse.election.backendserver.repository.BotTrusteeRepository;
import pse.election.backendserver.repository.TrusteeRepository;

@ExtendWith(MockitoExtension.class)
public class TrusteeServiceTest {

  private static Election election;
  @Mock
  private TrusteeRepository trusteeRepository;
  @Mock
  private VerificationFacade verificationFacade;
  @Mock
  private BotTrusteeRepository botTrusteeRepository;

  @Mock
  private ElectionService electionService;
  @InjectMocks
  private TrusteeService trusteeService;

  @BeforeAll
  static void setUp() {
    election = new Election(
        Date.from(Instant.now().plus(Duration.ofDays(1))),
        "Vacation to Ghana",
        "election-description",
        "authority@example.com",
        false,
        1
    );
  }

  @Test
  void checkAddTrustee() {
    Trustee trusteeOfUruguay = new Trustee(1, "uruguay@example.com", 1);
    Trustee trusteeInvalid = new Trustee(1, "invalidEmailFormat", 2);

    when(botTrusteeRepository.existsByTrusteeIdAndElectionId(trusteeOfUruguay.getTrusteeId(),
        1)).thenReturn(true);
    when(botTrusteeRepository.existsByTrusteeIdAndElectionId(trusteeInvalid.getTrusteeId(),
        1)).thenReturn(false);
    when(trusteeRepository.save(trusteeOfUruguay)).thenReturn(trusteeOfUruguay);

    Assertions.assertEquals(trusteeOfUruguay, trusteeService.addTrustee(trusteeOfUruguay));
    assertThrows(InvalidConfigurationException.class,
        () -> trusteeService.addTrustee(trusteeInvalid));
  }

  @Test
  void checkAddAuxKey() {
    election.setState(ElectionState.AUX_KEYS);
    Trustee trusteeOne = new Trustee(election.getElectionId(), "trustee1@example.com", 1);
    Trustee trusteeTwo = new Trustee(election.getElectionId(), "trustee2@example.com", 2);
    List<Trustee> trustees = new ArrayList<>();
    trustees.add(trusteeOne);
    trustees.add(trusteeTwo);

    when(trusteeRepository.findByEmailAndElectionId(trusteeOne.getEmail(),
        election.getElectionId())).thenReturn(trusteeOne);
    when(trusteeRepository.existsByEmail(trusteeOne.getEmail())).thenReturn(true);
    when(electionService.getElection(election.getElectionId())).thenReturn(election);
    when(trusteeRepository.save(trusteeOne)).thenReturn(trusteeOne);

    Trustee desiredTrustee = trusteeService.addAuxKey(trusteeOne.getEmail(), "key", "type",
        election.getElectionId());

    Assertions.assertTrue(desiredTrustee.isWaiting());
    Assertions.assertEquals("key", desiredTrustee.getAuxkey());
    Assertions.assertEquals("type", desiredTrustee.getAuxkeyType());
    Assertions.assertEquals(ElectionState.AUX_KEYS, election.getState());
  }

  @Test
  void checkAddElgamalKeysAndBackups_resultNextState() {
    election.setState(ElectionState.EPKB);
    Trustee trusteeOne = new Trustee(1, "trustee1@example.com", 1);
    Trustee trusteeTwo = new Trustee(1, "trustee2@example.com", 2);
    Trustee trusteeThree = new Trustee(1, "trustee3@example.com", 3);
    trusteeTwo.setWaiting(true);
    trusteeThree.setWaiting(true);

    Map<Integer, String> backups = new HashMap<>();
    backups.put(2, "backupFor2");
    backups.put(3, "backupFor3");
    SchnorrProofDTO[] schnorrProofs = new SchnorrProofDTO[1];
    schnorrProofs[0] = new SchnorrProofDTO(new BigInteger("1"), new BigInteger("2"),
        new BigInteger("3"), new BigInteger("4"));
    ElgamalProofDTO elgamalProofDTO = new ElgamalProofDTO(schnorrProofs, backups);

    when(electionService.getElection(1)).thenReturn(election);
    when(trusteeRepository.findByElectionId(1)).thenReturn(
        List.of(trusteeOne, trusteeTwo, trusteeThree));
    when(trusteeRepository.existsByEmailAndElectionId(trusteeOne.getEmail(), 1)).thenReturn(true);
    when(verificationFacade.verifyKeyCeremony(any())).thenReturn(true);
    when(trusteeRepository.findByEmailAndElectionId(trusteeOne.getEmail(), 1)).thenReturn(
        trusteeOne);
    when(trusteeRepository.findByTrusteeIndexAndElectionId(2, 1)).thenReturn(trusteeTwo);
    when(trusteeRepository.findByTrusteeIndexAndElectionId(3, 1)).thenReturn(trusteeThree);
    doAnswer(invocation -> {
      ElectionState state = invocation.getArgument(1);
      election.setState(state);
      return election;
    }).when(electionService).tryUpdateState(1, ElectionState.KEYCEREMONY_FINISHED);

    Trustee result = trusteeService.addElgamalKeysAndBackups(elgamalProofDTO, trusteeOne.getEmail(),
        1);

    Assertions.assertEquals(List.of("0;1;2;3;4"), result.getPublicElgamalKeyAndProof());
    Assertions.assertEquals(List.of("1;backupFor2"), trusteeTwo.getBackups());
    Assertions.assertEquals(List.of("1;backupFor3"), trusteeThree.getBackups());
    Assertions.assertSame(ElectionState.KEYCEREMONY_FINISHED, election.getState());
  }

  @Test
  void checkGetBackups_correctState() {
    election.setState(ElectionState.KEYCEREMONY_FINISHED);
    Trustee trusteeOne = new Trustee(election.getElectionId(), "trustee1@example.com", 1);
    trusteeOne.addBackup("2;backupOf2");
    trusteeOne.addBackup("3;backupOf3");

    when(trusteeRepository.findByEmailAndElectionId(trusteeOne.getEmail(),
        trusteeOne.getElectionId())).thenReturn(trusteeOne);
    when(trusteeRepository.findByTrusteeIndexAndElectionId(anyInt(), anyLong())).thenReturn(new Trustee());
    Map<Integer, String> result = trusteeService.getBackups(trusteeOne.getEmail(),
        election.getElectionId(), false);

    Assertions.assertTrue(result.containsKey(2) && result.containsValue("backupOf2"));
    Assertions.assertTrue(result.containsKey(3) && result.containsValue("backupOf3"));
  }

  @Test
  @Disabled
    // TODO State muss geprüft werden, also es dürfen keine Backups vor KEYCEREMONY_FINISHED angefragt werden -> FIXME im TrusteeService
  void checkGetBackupsAndGetElgamalKeys_invalidState() {
    election.setState(ElectionState.OPEN);
    Trustee trusteeOne = new Trustee(election.getElectionId(), "trustee1@example.com", 1);

    when(electionService.getElection(election.getElectionId())).thenReturn(election);

    assertThrows(IllegalStateOperationException.class,
        () -> trusteeService.getBackups(trusteeOne.getEmail(), election.getElectionId(), false));
    assertThrows(IllegalStateOperationException.class,
        () -> trusteeService.getElgamalKeys(trusteeOne.getEmail(), election.getElectionId()));
  }

  @Test
  void checkGetElgamalKeys_correctState() {
    election.setState(ElectionState.KEYCEREMONY_FINISHED);
    Trustee trusteeOne = new Trustee(1, "trustee1@example.com", 1);
    Trustee trusteeTwo = new Trustee(1, "trustee2@example.com", 2);
    Trustee trusteeThree = new Trustee(1, "trustee3@example.com", 3);
    List<Trustee> trustees = new ArrayList<>();
    trustees.add(trusteeOne);
    trustees.add(trusteeTwo);
    trustees.add(trusteeThree);

    trusteeOne.addBackup("2;backupOf2");
    trusteeOne.addBackup("3;backupOf3");

    List<String> keys = new ArrayList<>();
    keys.add("0;123123;commitment;challenge;response");
    trusteeTwo.addPublicElgamalKeyAndProof(keys);
    trusteeThree.addPublicElgamalKeyAndProof(keys);

    when(electionService.getElection(1)).thenReturn(election);
    when(trusteeRepository.findByElectionId(1)).thenReturn(trustees);

    Map<Integer, String[]> result = trusteeService.getElgamalKeys(trusteeOne.getEmail(), 1);

    Assertions.assertFalse(result.containsKey(1) && result.containsValue(result.get(1)));
    Assertions.assertTrue(result.containsKey(2) && result.containsValue(result.get(2)));
    Assertions.assertTrue(result.containsKey(3) && result.containsValue(result.get(3)));
  }

  @Test
  void addInvalidAuxKey_noEmail() {
    when(trusteeRepository.existsByEmail(any())).thenReturn(true);

    Assertions.assertThrows(IllegalArgumentException.class, () -> trusteeService.addAuxKey(null, null, null, 1));
  }

  @Test
  void addInvalidAuxKey_noKey() {
    when(trusteeRepository.existsByEmail(any())).thenReturn(true);
    Assertions.assertThrows(IllegalArgumentException.class, () -> trusteeService.addAuxKey("email", null, null, 1));
  }

  @Test
  void addInvalidAuxKey_noKeyType() {
    when(trusteeRepository.existsByEmail(any())).thenReturn(true);
    Assertions.assertThrows(IllegalArgumentException.class, () -> trusteeService.addAuxKey("email", "key", null, 1));
  }

  @Test
  void addInvalidAuxKey_wrongState() {
    Election election1 = new Election();
    election1.setState(ElectionState.OPEN);
    when(trusteeRepository.existsByEmail(any())).thenReturn(true);
    when(electionService.getElection(anyLong())).thenReturn(election1);

    assertThrows(IllegalStateOperationException.class, () -> trusteeService.addAuxKey("email", "key", "type", 1));
  }

  @Test
  void addInvalidAuxKey_trusteeAlreadyUploadedAKey() {
    Election election1 = new Election();
    election1.setState(ElectionState.AUX_KEYS);
    Trustee trustee = new Trustee();
    trustee.setWaiting(true);
    when(trusteeRepository.existsByEmail(any())).thenReturn(true);
    when(electionService.getElection(anyLong())).thenReturn(election1);
    when(trusteeRepository.findByEmailAndElectionId(any(), anyLong())).thenReturn(trustee);

    assertThrows(IllegalStateOperationException.class, () -> trusteeService.addAuxKey("email", "key", "type", 1));
  }

  @Test
  void addInvalidElgamal_invalidState() {
    Election election1 = new Election();
    election1.setState(ElectionState.OPEN);
    when(electionService.getElection(anyLong())).thenReturn(election1);

    assertThrows(IllegalStateOperationException.class, () -> trusteeService.addElgamalKeysAndBackups(null, null, 1));
  }

  @Test
  void addInvalidElgamal_noTrustee() {
    Election election1 = new Election();
    election1.setState(ElectionState.EPKB);
    when(electionService.getElection(anyLong())).thenReturn(election1);

    assertThrows(IllegalStateOperationException.class, () -> trusteeService.addElgamalKeysAndBackups(null, null, 1));
  }

  @Test
  void addInvalidElgamal_trusteeAlreadyUploaded() {
    Election election1 = new Election();
    election1.setState(ElectionState.EPKB);

    Trustee trustee = new Trustee();
    trustee.setWaiting(true);

    when(electionService.getElection(anyLong())).thenReturn(election1);
    when(trusteeRepository.existsByEmailAndElectionId("email", 1)).thenReturn(true);
    when(trusteeRepository.findByEmailAndElectionId("email", 1)).thenReturn(trustee);

    assertThrows(IllegalStateOperationException.class, () -> trusteeService.addElgamalKeysAndBackups(null, "email", 1));
  }


  @Test
  void addInvalidElgamal_BackupHasNullValues() {
    Election election1 = new Election();
    election1.setState(ElectionState.EPKB);

    Trustee trustee = new Trustee();
    trustee.setWaiting(true);

    ElgamalProofDTO elgamalProofDTO = new ElgamalProofDTO(null, null);

    when(electionService.getElection(anyLong())).thenReturn(election1);
    when(trusteeRepository.existsByEmailAndElectionId("email", 1)).thenReturn(true);
    when(trusteeRepository.findByEmailAndElectionId("email", 1)).thenReturn(trustee);

    assertThrows(IllegalArgumentException.class, () -> trusteeService.addElgamalKeysAndBackups(elgamalProofDTO, "email", 1));
  }

  @Test
  void addInvalidElgamal_invalidBackupFormat() {
    Election election1 = new Election();
    election1.setState(ElectionState.EPKB);

    Trustee trustee = new Trustee();
    trustee.setWaiting(true);

    ElgamalProofDTO elgamalProofDTO = new ElgamalProofDTO(new SchnorrProofDTO[] {
            new SchnorrProofDTO(new BigInteger("1"),
                    new BigInteger("1"),
                    new BigInteger("1"),
                    new BigInteger("1"))},
            Collections.emptyMap());

    when(electionService.getElection(anyLong())).thenReturn(election1);
    when(trusteeRepository.existsByEmailAndElectionId("email", 1)).thenReturn(true);
    when(trusteeRepository.findByEmailAndElectionId("email", 1)).thenReturn(trustee);

    assertThrows(IllegalArgumentException.class, () -> trusteeService.addElgamalKeysAndBackups(elgamalProofDTO, "email", 1));
  }

}
