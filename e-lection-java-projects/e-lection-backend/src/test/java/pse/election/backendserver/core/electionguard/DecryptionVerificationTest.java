package pse.election.backendserver.core.electionguard;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sunya.electionguard.ElectionCryptoContext;
import com.sunya.electionguard.Group;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import pse.election.backendserver.core.service.BallotService;
import pse.election.backendserver.core.service.ElectionService;
import pse.election.backendserver.core.service.TallyService;
import pse.election.backendserver.entity.Contest;
import pse.election.backendserver.entity.Election;
import pse.election.backendserver.entity.OptionEncrypted;
import pse.election.backendserver.entity.Tally;
import pse.election.backendserver.payload.dto.ChaumPedersenProofDTO;
import pse.election.backendserver.payload.dto.DecryptionDTO;
import pse.election.backendserver.payload.response.record.ElectionGuardInitializedWrapper;

/**
 * Tests the verification of partial and partial partial decryptions Should test for: correct
 * PartialDecryptions for a spoiled ballot are accepted correct PartialDecryptions for the tally are
 * accepted correct PartialPartialDecryptions are accepted PartialDecryptions where a decryption is
 * incorrect are rejected PartialDecryptions where a proof is incorrect are rejected
 * PartialDecryptions where the key is incorrect are rejected
 * <p>
 * Does not test if the format is checked for as that is assumed to have happened previously
 */
public class DecryptionVerificationTest {

  private static final String INPUT_PATH = "./src/test/resources";
  private static final long electionId = 12;
  private static JsonObject input = readDecryptionData();
  @Mock
  Contest contest1;
  @Mock
  Contest contest2;
  @Mock
  ElectionService electionService;
  @Mock
  Election election;
  @Mock
  ElectionGuardInitializedWrapper init;
  @Mock
  ElectionCryptoContext context;
  @Mock
  TallyService tallyService;
  @Mock
  BallotService ballotService;
  @InjectMocks
  VerificationFacade facade;

  private static DecryptionDTO.PartialDecryptionDTO generateDecryptionDTO(boolean ballot,
      JsonObject decryptionsObj) {
    Map<Integer, BigInteger[]> decryptions = new HashMap<>();
    Map<Integer, ChaumPedersenProofDTO[]> proofs = new HashMap<>();
    for (String key : decryptionsObj.get("partialDecryption").getAsJsonObject().keySet()) {
      List<BigInteger> allEncryptions = new ArrayList<>();
      List<ChaumPedersenProofDTO> currentProofs = new ArrayList<>();
      for (JsonElement message : decryptionsObj.get("partialDecryption").getAsJsonObject().get(key)
          .getAsJsonArray()) {
        allEncryptions.add(new BigInteger(message.getAsString(), 16));
      }
      for (JsonElement proof : decryptionsObj.get("proofs").getAsJsonObject().get(key)
          .getAsJsonArray()) {
        currentProofs.add(new ChaumPedersenProofDTO(
            new BigInteger(proof.getAsJsonObject().get("pad").getAsString(), 16),
            new BigInteger(proof.getAsJsonObject().get("data").getAsString(), 16),
            new BigInteger(proof.getAsJsonObject().get("challenge").getAsString(), 16),
            new BigInteger(proof.getAsJsonObject().get("response").getAsString(), 16)));
      }
      decryptions.put(Integer.valueOf(key), allEncryptions.toArray(new BigInteger[]{}));
      proofs.put(Integer.valueOf(key), currentProofs.toArray(new ChaumPedersenProofDTO[]{}));
    }
    return new DecryptionDTO.PartialDecryptionDTO(decryptions, proofs, ballot ? 0L : -1);
  }

  private static JsonObject readDecryptionData() {
    File inputFile = new File(INPUT_PATH + "/decryption.json");
    JsonObject data;
    try {
      Reader reader = new FileReader(inputFile);
      data = JsonParser.parseReader(reader).getAsJsonObject();
      reader.close();
    } catch (IOException e) {
      throw new IllegalArgumentException(e.getMessage());
    }
    return data;
  }

  @BeforeEach
  void setup() {
    Group.ElementModQ qbar = Group.hex_to_q_unchecked(input.get("validQbar").getAsString());
    MockitoAnnotations.openMocks(this);
    ReflectionTestUtils.setField(context, "cryptoExtendedBaseHash", qbar);
    when(init.generateCryptoContext(electionId)).thenReturn(context);
    when(contest1.getIndex()).thenReturn(0);
    when(contest1.getOptions()).thenReturn(List.of("a", "b", "c"));
    when(contest1.getMax()).thenReturn(1);
    when(contest2.getIndex()).thenReturn(1);
    when(contest2.getOptions()).thenReturn(List.of("a", "b"));
    when(contest2.getMax()).thenReturn(1);
    when(election.getElectionId()).thenReturn(electionId);
    when(electionService.getAllContestsOfElection(electionId)).thenReturn(
        List.of(contest1, contest2));
    List<OptionEncrypted> options = new ArrayList<>();
    List<Tally> tallies = new ArrayList<>();
    for (String key : input.get("encryption").getAsJsonObject().keySet()) {
      int count = 0;
      for (JsonElement encryptions : input.get("encryption").getAsJsonObject().get(key)
          .getAsJsonArray()) {
        options.add(new OptionEncrypted(0L, electionId, count, Integer.valueOf(key),
            encryptions.getAsJsonObject().get("pad").getAsString(),
            encryptions.getAsJsonObject().get("data").getAsString(), "", ""));
        Tally currentTally = new Tally(electionId, Integer.valueOf(key), count++);
        currentTally.setCiphertextPAD(encryptions.getAsJsonObject().get("pad").getAsString());
        currentTally.setCiphertextDATA(encryptions.getAsJsonObject().get("data").getAsString());
        tallies.add(currentTally);
      }
    }
    when(tallyService.getAllTalliesOfElection(electionId)).thenReturn(tallies);
    when(ballotService.getAllOptionsEncryptedOfBallot(0L)).thenReturn(options);
  }

  /**
   * Tests that valid partial decryptions and partial partial decryptons are accepted both for
   * spoiled ballots and the tally
   */
  @Test
  void testValidPartialDecryptions() {
    Group.ElementModQ priv = Group.hex_to_q(
        "F6EDD18411BBAF51B19F13DBEFF5EE0D84B18104EA5D85CC01BF897FD4875A4C").orElseThrow();
    System.out.println(Group.g_pow_p(priv).base16());
    JsonArray validDecryptions = input.get("validPartialDecryption").getAsJsonArray();
    for (JsonElement decryption : validDecryptions) {
      DecryptionDTO.PartialDecryptionDTO dto1 = generateDecryptionDTO(false,
          decryption.getAsJsonObject());
      DecryptionDTO.PartialDecryptionDTO dto2 = generateDecryptionDTO(true,
          decryption.getAsJsonObject());
      BigInteger key = new BigInteger(decryption.getAsJsonObject().get("key").getAsString(), 16);
      assertTrue(facade.verifyDecryption(dto1, electionId, key));
      assertTrue(facade.verifyDecryption(dto2, electionId, key));

    }
  }

  @Test
  void testInvalidPartialDecryption() {
    JsonArray invalidDecryptions = input.get("invalidPartialDecryption").getAsJsonArray();
    for (JsonElement decryption : invalidDecryptions) {
      DecryptionDTO.PartialDecryptionDTO dto1 = generateDecryptionDTO(false,
          decryption.getAsJsonObject());
      BigInteger key = new BigInteger(decryption.getAsJsonObject().get("key").getAsString(), 16);
      assertFalse(facade.verifyDecryption(dto1, electionId, key));
    }
  }
}
