package pse.election.backendserver.core.bot;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import electioncli.core.Identifiers;
import electioncli.handle.key_generation.AuxKeyHandler;
import electioncli.handle.key_generation.BackupHandler;
import electioncli.handle.key_generation.ElectionKeyHandler;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import pse.election.backendserver.core.electionguard.KeyCeremonyFacade;
import pse.election.backendserver.core.service.ElectionService;
import pse.election.backendserver.core.service.TrusteeService;
import pse.election.backendserver.entity.BotTrustee;
import pse.election.backendserver.entity.Election;
import pse.election.backendserver.entity.Trustee;
import pse.election.backendserver.payload.dto.ElgamalProofDTO;
import pse.election.backendserver.payload.dto.SchnorrProofDTO;

/**
 * Tests the intergation of the bot into Key Ceremony and Decryption Test data for decryption will
 * come from fullDecryptions. Data for key cermony will be generated at runtime by the CLI. The Bot
 * will take the place of Trustee1 Shall test for: Bot generates an auxiliary key Bot generates
 * backups Generated backups can be verified using the CLI Bot can verify backups prepared for it
 * Bot will recognize if it was given invalid backups
 * <p>
 * Bot can perform partial and partial partial decryption(using keys prepared by the CLI, not the
 * BotFacade to avoid integrating ballot generation) Result can be fully decrypted when using the
 * bots partial and partial partial decryptions
 */
public class BotIntegrationTest {

  @Mock
  ElectionService electionService;
  @Mock
  TrusteeService trusteeService;

  @Mock
  KeyCeremonyFacade keyCeremonyFacade;
  @InjectMocks
  BotFacade botFacade;

  String botEmail = "bot@bot.de";
  long electionId = 0L;
  Trustee botTrustee;
  Trustee trustee2 = new Trustee(0L, "trustee1@gmail.com", 1);
  Trustee trustee3 = new Trustee(0L, "trustee2@gmail.com", 2);
  BotTrustee bot;
  JsonObject[] auxPrivate = new JsonObject[2];
  @Mock
  Election election;

  @BeforeEach
  void generalSetup() {
    MockitoAnnotations.openMocks(this);
    when(keyCeremonyFacade.generateRsaKey()).thenCallRealMethod();
    when(election.getElectionId()).thenReturn(electionId);
    when(election.getTrusteeThreshold()).thenReturn(2);
  }

  /**
   * Sets up the bot trustee and creates its auxiliary key
   */
  void generateAuxKey() {
    Mockito.doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        botTrustee = invocation.getArgument(0);
        return null;
      }
    }).when(trusteeService).addTrustee(any(Trustee.class));
    Mockito.doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        bot = invocation.getArgument(0);
        return null;
      }
    }).when(trusteeService).saveBotTrustee(any(BotTrustee.class));
    botFacade.createBot(electionId, 2);
  }

  /**
   * Generates the auxiliary key for a Trustee using the CLI
   *
   * @param trustee Trustee the aux Key is generated for
   */
  void generateAuxKey(Trustee trustee) {
    AuxKeyHandler auxKeyHandler = new AuxKeyHandler();
    auxKeyHandler.initialize(new JsonObject[0], new String[0]);
    JsonObject[] auxKeys = auxKeyHandler.execute();
    auxPrivate[trustee.getIndex() - 1] = auxKeys[0];
    trustee.setAuxkey(auxKeys[1].get("publicKey").getAsString());
    trustee.setAuxkeyType(auxKeys[1].get("keyType").getAsString());
  }

  /**
   * Generates auxiliary keys for all trustees, genetates the ElectionKey for the trustee
   */
  void generateElectionKey() {
    generateAuxKey();
    assertNotNull(botTrustee);
    generateAuxKey(trustee2);
    generateAuxKey(trustee3);
    List<Trustee> allTrustees = new ArrayList<>();
    allTrustees.addAll(List.of(botTrustee, trustee2, trustee3));
    when(trusteeService.getAllTrustees(electionId)).thenReturn(allTrustees);
    when(trusteeService.getBotTrustee(electionId)).thenReturn(bot);
    when(trusteeService.getTrustee(botEmail, electionId)).thenReturn(botTrustee);
    Mockito.doAnswer(new Answer<Trustee>() {
      @Override
      public Trustee answer(InvocationOnMock invocation) {
        long trustteId = invocation.getArgument(0);
        if (trustteId == botTrustee.getTrusteeId()) {
          return botTrustee;
        }
        return null;
      }
    }).when(trusteeService).getTrustee(any(Long.class));
    Mockito.doAnswer(new Answer<Trustee>() {
          @Override
          public Trustee answer(InvocationOnMock invocation) throws Throwable {
            ElgamalProofDTO proofDTO = invocation.getArgument(0);
            String email = invocation.getArgument(1);
            if (email != botEmail) {
              return null;
            }
            List<String> publicElgamalKeyAndProof = List.of(
                parseElgamalKeyAndProofToString(proofDTO.proofs(), 0),
                parseElgamalKeyAndProofToString(proofDTO.proofs(), 1));
            botTrustee.addPublicElgamalKeyAndProof(publicElgamalKeyAndProof);
            trustee2.addBackup(
                botTrustee.getIndex() + ";" + proofDTO.backups().get(trustee2.getIndex()));
            trustee3.addBackup(
                botTrustee.getIndex() + ";" + proofDTO.backups().get(trustee3.getIndex()));
            return botTrustee;
          }
        }).when(trusteeService)
        .addElgamalKeysAndBackups(any(ElgamalProofDTO.class), anyString(), anyLong());
    botFacade.addElgamalKeysAndBackups(election);
  }

  @Test
  /**
   * Tests that BotFacade does not throw ongenerating Aux Key
   */
  void testGenerateAuxKey() {
    assertDoesNotThrow(() -> generateAuxKey());
    assertNotNull(botTrustee);
    assertNotNull(botTrustee.getAuxkey());
    assertNotNull(bot);
    assertNotEquals(botTrustee.getAuxkey().length(), 0);
    assertEquals(botTrustee.getAuxkeyType(), "RSA");
  }

  @Test
  void testGenerateBackups() {
    generateElectionKey();
    assertNotNull(botTrustee.getPublicElgamalKeyAndProof());
    assertNotEquals(botTrustee.getPublicElgamalKeyAndProof().size(), 0);
    assertNotNull(bot.getPrivateElgamalKey());
    assertNotNull(trustee2.getBackups());
    assertNotEquals(trustee2.getBackups().size(), 0);
    assertNotNull(trustee3.getBackups());
    assertNotEquals(trustee3.getBackups().size(), 0);
  }

  @Test
  void testBackupVerification() {
    generateAuxKey();
    generateElectionKey();
    generateElectionKey(trustee2, List.of(trustee3, botTrustee));
    generateElectionKey(trustee3, List.of(trustee2, botTrustee));
    when(trusteeService.getBackups(botEmail, electionId, false)).thenReturn(Map.of(1,
        botTrustee.getBackups().get(0).split(";")[1], 2,
        botTrustee.getBackups().get(1).split(";")[1]));
    assertTrue(verifyBackups(trustee2, List.of(botTrustee, trustee3)));
    assertTrue(verifyBackups(trustee3, List.of(botTrustee, trustee2)));
  }

  @Test
  void testInvalidBackupVerification() {
    generateAuxKey();
    generateElectionKey();
    generateElectionKey(trustee2, List.of(trustee3, botTrustee));
    generateElectionKey(trustee3, List.of(trustee2, botTrustee));
  }

  void generateElectionKey(Trustee trustee, List<Trustee> otherTrustees) {
    assertNotNull(botTrustee);
    JsonObject auxKeys = new JsonObject();
    JsonObject keys = new JsonObject();
    for (Trustee t : otherTrustees) {
      JsonObject auxKey = new JsonObject();
      auxKey.addProperty(Identifiers.PUBLIC_KEY.getName(), t.getAuxkey());
      auxKey.addProperty(Identifiers.ENCRYPTION_TYPE.getName(), t.getAuxkeyType());
      keys.add(String.valueOf(t.getIndex()), auxKey);
    }
    auxKeys.add(Identifiers.AUX_KEY.getName(), keys);
    auxKeys.addProperty(Identifiers.THRESHOLD.getName(), 2);
    ElectionKeyHandler handler = new ElectionKeyHandler();
    handler.initialize(new JsonObject[]{auxKeys}, new String[0]);
    JsonObject keysAndBackups = handler.execute()[1];
    SchnorrProofDTO[] proofs = new SchnorrProofDTO[2];
    for (int i = 0; i < 2; i++) {
      JsonObject proofObj = keysAndBackups.get(Identifiers.PROOF.getName()).getAsJsonArray().get(i)
          .getAsJsonObject();
      proofs[i] = new SchnorrProofDTO(
          new BigInteger(proofObj.get(Identifiers.PUBLIC_KEY.getName()).getAsString(), 16),
          new BigInteger(proofObj.get(Identifiers.SCNORR_COMMITMENT.getName()).getAsString(), 16),
          new BigInteger(proofObj.get(Identifiers.PROOF_CHALLENGE.getName()).getAsString(), 16),
          new BigInteger(proofObj.get(Identifiers.PROOF_RESPONSE.getName()).getAsString(), 16)
      );
    }
    Map<Integer, String> backups = new HashMap<>();
    for (Trustee t : otherTrustees) {
      JsonObject backupObj = keysAndBackups.get(Identifiers.BACKUP.getName()).getAsJsonObject();
      backups.put(t.getIndex(), backupObj.get(String.valueOf(t.getIndex())).getAsString());
    }
    ElgamalProofDTO dto = new ElgamalProofDTO(proofs, backups);
    botFacade.verifyProvidedBackup(electionId, dto);
    Trustee botTrustee2 = new Trustee(electionId, botEmail, 6);
    when(trusteeService.getBotTrustee(1L)).thenReturn(bot);
    when(trusteeService.getTrustee(botEmail, 1L)).thenReturn(botTrustee2);
    assertThrows(Exception.class, () -> botFacade.verifyProvidedBackup(1L, dto));
    trustee.addPublicElgamalKeyAndProof(List.of(parseElgamalKeyAndProofToString(proofs, 0),
        parseElgamalKeyAndProofToString(proofs, 1)));
    for (Trustee t : otherTrustees) {
      t.addBackup(trustee.getIndex() + ";" + backups.get(t.getIndex()));
    }
  }

  boolean verifyBackups(Trustee trustee, List<Trustee> otherTrustees) {
    JsonObject backups = new JsonObject();
    JsonObject keys = new JsonObject();
    for (String backup : trustee.getBackups()) {
      backups.addProperty(String.valueOf(backup.split(";")[0]), backup.split(";")[1]);
    }
    for (Trustee t : otherTrustees) {
      JsonArray keysOfTrustee = new JsonArray();
      for (String proof : t.getPublicElgamalKeyAndProof()) {
        keysOfTrustee.add(proof.split(";")[1]);
      }
      keys.add(String.valueOf(t.getIndex()), keysOfTrustee);
    }
    JsonObject keysAndBackups = new JsonObject();
    keysAndBackups.add(Identifiers.BACKUP.getName(), backups);
    keysAndBackups.add(Identifiers.PUBLIC_KEYS.getName(), keys);
    keysAndBackups.addProperty(Identifiers.ID.getName(), trustee.getIndex());
    BackupHandler handler = new BackupHandler();
    handler.initialize(new JsonObject[]{keysAndBackups, auxPrivate[trustee.getIndex() - 1]},
        new String[0]);
    handler.execute();
    return !handler.message().contains("inkorrekt");
  }

  @Test
  void testVerifyElectionKey() {
    generateAuxKey();
    generateElectionKey();
    assertNotNull(botTrustee.getPublicElgamalKeyAndProof());
    assertNotEquals(botTrustee.getPublicElgamalKeyAndProof().size(), 0);
    assertNotNull(bot.getPrivateElgamalKey());
  }


  private String parseElgamalKeyAndProofToString(SchnorrProofDTO[] schnorr, int order) {
    String publicKey = schnorr[order].publicKey().toString(16);
    String commitment = schnorr[order].commitment().toString(16);
    String challenge = schnorr[order].challenge().toString(16);
    String response = schnorr[order].response().toString(16);
    return order + ";" + publicKey + ";" + commitment + ";" + challenge + ";" + response;
  }
}
