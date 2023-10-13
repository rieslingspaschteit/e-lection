package pse.election.backendserver.core.electionguard.VerifiactionFacade;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sunya.electionguard.Group;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import pse.election.backendserver.core.electionguard.VerificationFacade;
import pse.election.backendserver.payload.dto.ElgamalProofDTO;
import pse.election.backendserver.payload.dto.SchnorrProofDTO;

public class KeyCeremonyVerificationTest {

  private static final String INPUT_PATH = "./src/test/resources";
  @InjectMocks
  VerificationFacade verificationFacade;

  private static List<ElgamalProofDTO> readElGamalProofs(String key) throws IOException {
    File inputFile = new File(INPUT_PATH + "/keyCeremony.json");
    Reader reader = new FileReader(inputFile);
    JsonElement element = JsonParser.parseReader(reader);
    reader.close();
    JsonArray relevantProofs = element.getAsJsonObject().get(key).getAsJsonArray();
    List<ElgamalProofDTO> proofDTOs = new ArrayList<>();
    for (JsonElement elGamalProof : relevantProofs) {
      List<SchnorrProofDTO> schnorrDTOs = parseSchnorrProof(elGamalProof.getAsJsonObject());
      Map<Integer, String> backups = new HashMap<>();
      for (String backup : elGamalProof.getAsJsonObject().get("backups").getAsJsonObject()
          .keySet()) {
        backups.put(Integer.valueOf(backup),
            elGamalProof.getAsJsonObject().get("backups").getAsJsonObject().get(backup)
                .getAsString());
      }
      SchnorrProofDTO[] a = new SchnorrProofDTO[schnorrDTOs.size()];
      schnorrDTOs.toArray(a);
      proofDTOs.add(new ElgamalProofDTO(a, backups));
    }
    return proofDTOs;
  }

  private static final List<SchnorrProofDTO> parseSchnorrProof(JsonObject jsonProof) {
    JsonArray proofs = jsonProof.get("proofs").getAsJsonArray();
    List<SchnorrProofDTO> proofDTOs = new ArrayList<>();
    for (JsonElement proof : proofs) {
      JsonObject rawProof = proof.getAsJsonObject();
      proofDTOs.add(new SchnorrProofDTO(
          Group.hex_to_p_unchecked(rawProof.get("publicKey").getAsString()).getBigInt(),
          Group.hex_to_p_unchecked(rawProof.get("commitment").getAsString()).getBigInt(),
          Group.hex_to_q_unchecked(rawProof.get("challenge").getAsString()).getBigInt(),
          Group.hex_to_q_unchecked(rawProof.get("response").getAsString()).getBigInt()
      ));
    }
    return proofDTOs;
  }

  @BeforeEach
  void mockSetup() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void validProofs() throws IOException {
    List<ElgamalProofDTO> allDTOs = readElGamalProofs("validElectionKeys");
    for (ElgamalProofDTO electionKey : allDTOs) {
      assertTrue(verificationFacade.verifyKeyCeremony(electionKey));
    }
  }

  @Test
  void invalidProofs() throws IOException {
    List<ElgamalProofDTO> allDTOs = readElGamalProofs("invalidElectionKeys");
    for (ElgamalProofDTO electionKey : allDTOs) {
      assertThrows(IllegalStateException.class,
          () -> verificationFacade.verifyKeyCeremony(electionKey));
    }
  }
}
