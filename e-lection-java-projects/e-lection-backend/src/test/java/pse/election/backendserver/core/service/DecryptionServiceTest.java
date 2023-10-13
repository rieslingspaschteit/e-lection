package pse.election.backendserver.core.service;

import java.math.BigInteger;
import java.util.*;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import pse.election.backendserver.core.state.ElectionState;
import pse.election.backendserver.entity.BotTrustee;
import pse.election.backendserver.entity.Election;
import pse.election.backendserver.entity.Tally;
import pse.election.backendserver.entity.Trustee;
import pse.election.backendserver.payload.dto.ChaumPedersenProofDTO;
import pse.election.backendserver.payload.dto.DecryptionDTO;
import pse.election.backendserver.payload.error.exception.IllegalStateOperationException;
import pse.election.backendserver.payload.error.exception.UnauthorizedAccessException;
import pse.election.backendserver.repository.ElectionRepository;
import pse.election.backendserver.repository.PartialDecryptionRepository;
import pse.election.backendserver.repository.TallyRepository;
import pse.election.backendserver.repository.TrusteeRepository;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@SpringBootTest(args = "--oidcClients=./oidcClients.toml")
public class DecryptionServiceTest {

  private Election election = new Election(new Date(2000, Calendar.JANUARY, 1), "title", "description",
          "author@example.com", false, 1);
  private Trustee trustee = new Trustee(1, "trustee@example.com", 1);
  private Tally tally0;
  private Tally tally1;
  private Tally tally2;

  private DecryptionDTO decryptionDTO;

  @InjectMocks
  private DecryptionService decryptionService;

  @Mock
  private TrusteeService trusteeService;

  @Mock
  private PartialDecryptionRepository partialDecryptionRepository;

  @Mock
  private TallyService tallyService;

  @Mock
  private ElectionService electionService;

  @Mock
  private TrusteeRepository trusteeRepository;

  @Mock
  private ElectionRepository electionRepository;

  @Mock
  private Election mockElection;

  @Mock
  private BotTrustee mockBotTrustee;

  void setUp() {
    election.setState(ElectionState.P_DECRYPTION);
    List<String> keyAndProof = new ArrayList<>();
    keyAndProof.add("1;key;comm;chal;resp");
    trustee.addPublicElgamalKeyAndProof(keyAndProof);
    tally0 = new Tally(1, 0, 0);
    tally1 = new Tally(1, 0, 1);
    tally2 = new Tally(1, 0, 2);

    Map<Integer, BigInteger[]> partialDecryptedOptions = new HashMap<>();
    BigInteger[] options = new BigInteger[3];
    for (int i = 0; i < 3; i++) {
      options[i] = new BigInteger("" + i);
    }
    partialDecryptedOptions.put(0, options);

    Map<Integer, ChaumPedersenProofDTO[]> chaumPedersonProofs = new HashMap<>();
    ChaumPedersenProofDTO[] proofs = new ChaumPedersenProofDTO[3];
    for (int i = 0; i < 3; i++) {
      BigInteger pad = new BigInteger("1" + i);
      BigInteger data = new BigInteger("2" + i);
      BigInteger challenge = new BigInteger("3" + i);
      BigInteger response = new BigInteger("4" + i);
      proofs[i] = new ChaumPedersenProofDTO(pad, data, challenge, response);
    }
    chaumPedersonProofs.put(0, proofs);

    Map<Integer, DecryptionDTO.PartialDecryptionDTO[]> partialDecryptedSpoiledBallots = new HashMap<>();
    Map<Integer, DecryptionDTO.PartialDecryptionDTO> partialDecryptedTalliedBallots = new HashMap<>();
    partialDecryptedTalliedBallots.put(0,
        new DecryptionDTO.PartialDecryptionDTO(partialDecryptedOptions, chaumPedersonProofs, -1));

    decryptionDTO = new DecryptionDTO(partialDecryptedSpoiledBallots, partialDecryptedTalliedBallots);
  }

  @Test
  void addDecryptionTest_phaseOne() throws UnauthorizedAccessException {
    setUp();
    doNothing().when(trusteeService).checkExistingEmail(trustee.getEmail());
    doNothing().when(electionService).checkExistsElection(1);

    when(trusteeService.getTrustee(trustee.getEmail(), 1)).thenReturn(trustee);
    when(electionService.getElection(1)).thenReturn(election);
    when(tallyService.getSpecificTally(1, 0, 0)).thenReturn(tally0);
    when(tallyService.getSpecificTally(1, 0, 1)).thenReturn(tally1);
    when(tallyService.getSpecificTally(1, 0, 2)).thenReturn(tally2);
    when(partialDecryptionRepository.save(any())).thenReturn(null);

    decryptionService.addDecryption(decryptionDTO, 1, trustee.getEmail());

    Assertions.assertTrue(trusteeService.getTrustee(trustee.getEmail(), 1).isWaiting());
  }

  @Test
  void invalidAddDecryption_InvalidState() {
    Trustee trustee1 = new Trustee();
    Election election1 = new Election();
    election1.setState(ElectionState.OPEN);
    when(trusteeRepository.existsByEmail(any())).thenReturn(true);
    when(electionRepository.existsByElectionId(anyLong())).thenReturn(true);
    when(electionService.getElection(anyLong())).thenReturn(election1);
    when(trusteeService.getTrustee(any(), anyLong())).thenReturn(trustee1);

    Assertions.assertThrows(IllegalStateOperationException.class, () -> decryptionService.addDecryption(null, 1, "email"));
  }


}
