package pse.election.backendserver.core.electionguard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sunya.electionguard.Group;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import pse.election.backendserver.core.service.TrusteeService;
import pse.election.backendserver.entity.Election;
import pse.election.backendserver.entity.Trustee;

/**
 * Shall test for: consistent key combining (testing against combined key previously computed by the
 * same method) correct computation of key shares for compensation keys
 */
public class KeyCeremonyFacadeTest {

  private static final String INPUT_PATH = "./src/test/resources";
  long electionId = 1L;
  @Mock
  Trustee trustee1;
  @Mock
  Trustee trustee2;
  @Mock
  Trustee trustee3;
  @Mock
  TrusteeService trusteeService;
  @Mock
  Election election;
  @InjectMocks
  KeyCeremonyFacade keyCeremonyFacade;
  private JsonObject input;

  private static JsonObject readDecryptionData() {
    File inputFile = new File(INPUT_PATH + "/fullDecryptions.json");
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
    MockitoAnnotations.openMocks(this);
    input = readDecryptionData();
    when(election.getElectionId()).thenReturn(electionId);
  }

  @Test
  void testKeyJoining() {
    when(trustee1.getPrimaryKey()).thenReturn(new BigInteger(
        input.get("trusteeKeys").getAsJsonObject().get("1").getAsJsonArray().get(0).getAsString(),
        16));
    when(trustee2.getPrimaryKey()).thenReturn(new BigInteger(
        input.get("trusteeKeys").getAsJsonObject().get("2").getAsJsonArray().get(0).getAsString(),
        16));
    when(trustee3.getPrimaryKey()).thenReturn(new BigInteger(
        input.get("trusteeKeys").getAsJsonObject().get("3").getAsJsonArray().get(0).getAsString(),
        16));
    when(trusteeService.getAllTrustees(electionId)).thenReturn(
        List.of(trustee1, trustee2, trustee3));
    assertEquals(keyCeremonyFacade.combineKeys(election).toString(16).toUpperCase(),
        input.get("combinedKey").getAsString());
  }

  @Test
    //Tests that generateKeyShare calculates the correct key share
  void testKeyShare() {
    //Iterate over all trustees
    for (int i = 1; i < 4; i++) {
      //coeffients of trustee i
      List<BigInteger> coefficients = new ArrayList<>();
      for (JsonElement coefficient : input.get("trusteeKeys").getAsJsonObject()
          .get(String.valueOf(i)).getAsJsonArray()) {
        coefficients.add(new BigInteger(coefficient.getAsString(), 16));
      }
      for (int j = 1; j < 4; j++) {
        if (i != j) {
          //keyShare of trustee j for trustee i
          BigInteger reconstructedShare = keyCeremonyFacade.generateKeyShare(coefficients, j);
          String point = input.get("trusteePoints").getAsJsonObject().get(String.valueOf(i))
              .getAsJsonObject().get(String.valueOf(j)).getAsString();
          Group.ElementModP actualShare = Group.g_pow_p(Group.hex_to_q_unchecked(point));
          assertEquals(reconstructedShare, actualShare.getBigInt());
        }
      }
    }
  }
}
