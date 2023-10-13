package pse.election.backendserver.core.electionguard.VerifiactionFacade;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import pse.election.backendserver.core.electionguard.VerificationFacade;
import pse.election.backendserver.core.service.ElectionService;
import pse.election.backendserver.entity.Contest;
import pse.election.backendserver.entity.Election;
import pse.election.backendserver.payload.dto.BallotProofDTO;
import pse.election.backendserver.payload.dto.ChaumPedersenProofDTO;
import pse.election.backendserver.payload.dto.ConstantChaumPedersenDTO;
import pse.election.backendserver.payload.dto.DisjunctiveChaumPedersenDTO;
import pse.election.backendserver.payload.dto.EncryptedOptionDTO;
import pse.election.backendserver.payload.response.record.ElectionGuardInitializedWrapper;

/**
 * Tests the verification of a received ballot Should test acceptance of correct ballots as well as
 * the following issues: ballots encrypted with the wrong election key (ballots where the challenge
 * for either a disjunctive or accumulated proof is faked) if there is time, is hopefully tested for
 * by electionGuard (ballots where the response for either a disjunctive or accumulated proof is
 * incorrect) see above ballots where the proof is made for the wrong ciphertext ballots where the
 * accumulated proof is done for a wrong accumulation ballots where the sum of selections surpasses
 * the maximum number of votes
 * <p>
 * Will not test that ballots encrypted after the end of the election will be rejected, or that the
 * encryption date is before the time of submission as this is assumed to happen at a different
 * time
 * <p>
 * To avoid side effects as best as possible, all invalid ballots will be generated from the same
 * seed as one of the valid ballots
 */
public class BallotVerificationTest {

  private static final String INPUT_PATH = "./src/test/resources";
  private static final JsonObject ballotInput = readBallotData();
  private static final long electionId = 12;
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

  @InjectMocks
  VerificationFacade facade;

  private static DisjunctiveChaumPedersenDTO generateDisjunctiveProof(JsonObject proof) {
    return new DisjunctiveChaumPedersenDTO(
        new ChaumPedersenProofDTO(
            new BigInteger(proof.get("proof0").getAsJsonObject().get("pad").getAsString(), 16),
            new BigInteger(proof.get("proof0").getAsJsonObject().get("data").getAsString(), 16),
            new BigInteger(proof.get("proof0").getAsJsonObject().get("challenge").getAsString(),
                16),
            new BigInteger(proof.get("proof0").getAsJsonObject().get("response").getAsString(), 16)
        ),
        new ChaumPedersenProofDTO(
            new BigInteger(proof.get("proof1").getAsJsonObject().get("pad").getAsString(), 16),
            new BigInteger(proof.get("proof1").getAsJsonObject().get("data").getAsString(), 16),
            new BigInteger(proof.get("proof1").getAsJsonObject().get("challenge").getAsString(),
                16),
            new BigInteger(proof.get("proof1").getAsJsonObject().get("response").getAsString(), 16)
        ),
        new BigInteger(proof.get("challenge").getAsString(), 16)
    );
  }

  private static ConstantChaumPedersenDTO generateConstantProof(JsonObject proof) {
    return new ConstantChaumPedersenDTO(
        new ChaumPedersenProofDTO(
            new BigInteger(proof.get("pad").getAsString(), 16),
            new BigInteger(proof.get("data").getAsString(), 16),
            new BigInteger(proof.get("challenge").getAsString(), 16),
            new BigInteger(proof.get("response").getAsString(), 16)
        ),
        proof.get("constant").getAsInt()
    );
  }

  private static BallotProofDTO generateBallot(JsonObject ballot) {
    Map<Integer, EncryptedOptionDTO[]> ciphertext = new HashMap<>();
    Map<Integer, DisjunctiveChaumPedersenDTO[]> indProofs = new HashMap<>();
    Map<Integer, ConstantChaumPedersenDTO> accProofs = new HashMap<>();
    for (String key : ballot.get("cipherText").getAsJsonObject().keySet()) {
      List<EncryptedOptionDTO> options = new ArrayList<>();
      List<DisjunctiveChaumPedersenDTO> proofs = new ArrayList<>();
      for (JsonElement cipher : ballot.get("cipherText").getAsJsonObject().get(key)
          .getAsJsonArray()) {
        options.add(new EncryptedOptionDTO(
            new BigInteger(cipher.getAsJsonObject().get("pad").getAsString(), 16),
            new BigInteger(cipher.getAsJsonObject().get("data").getAsString(), 16)
        ));
      }
      for (JsonElement proof : ballot.get("individualProofs").getAsJsonObject().get(key)
          .getAsJsonArray()) {
        proofs.add(generateDisjunctiveProof(proof.getAsJsonObject()));
      }
      ciphertext.put(Integer.valueOf(key), options.toArray(new EncryptedOptionDTO[]{}));
      indProofs.put(Integer.valueOf(key), proofs.toArray(new DisjunctiveChaumPedersenDTO[]{}));
      accProofs.put(Integer.valueOf(key),
          generateConstantProof(
              ballot.get("accumulatedProofs").getAsJsonObject().get(key).getAsJsonObject()));
    }
    Instant instant = Instant.parse(ballot.get("date").getAsString());
    Date encryptionDate = Date.from(instant);
    return new BallotProofDTO(
        ciphertext,
        indProofs,
        accProofs,
        null,
        encryptionDate
    );
  }

  private static JsonObject readBallotData() {
    File inputFile = new File(INPUT_PATH + "/ballots.json");
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
    Group.ElementModQ qbar = Group.hex_to_q_unchecked(ballotInput.get("validQbar").getAsString());
    Group.ElementModP publicKey = Group.hex_to_p_unchecked(
        ballotInput.get("validPublicKey").getAsString());
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
    when(election.getPublicKey()).thenReturn(publicKey.getBigInt());
    when(electionService.getAllContestsOfElection(electionId)).thenReturn(
        List.of(contest1, contest2));
  }

  /**
   * Tests that a valid ballot will be accepted
   */
  @Test
  void testValidBallot() {
    for (JsonElement ballot : ballotInput.get("validBallots").getAsJsonArray()) {
      assertTrue(facade.verifyBallot(generateBallot(ballot.getAsJsonObject()), election));
    }
  }

  /**
   * Tests that a ballot encrypted with the wrong key will be rejected
   */
  @Test
  void testInvalidKey() {
    when(election.getPublicKey()).thenReturn(BigInteger.valueOf(100));
    for (JsonElement ballot : ballotInput.get("validBallots").getAsJsonArray()) {
      assertFalse(facade.verifyBallot(generateBallot(ballot.getAsJsonObject()), election));
    }
  }

  /**
   * Tests that a ballot with proofs for the wrong ciphertext will be rejected
   */
  @Test
  void testInvalidProofs() {
    for (JsonElement ballot : ballotInput.get("invalidBallotProofs").getAsJsonArray()) {
      assertFalse(facade.verifyBallot(generateBallot(ballot.getAsJsonObject()), election));
    }
  }

  /**
   * Tests that a ballot with incorrect ciphertext accumulation is rejected The last option of
   * question1 is removed from a valid ballot to cause an additional option (which is zero) to be
   * added into the accumulated proof
   */
  @Test
  void testInvalidAccumulation() {
    JsonElement ballot = ballotInput.get("invalidCiphertextAccumulation").getAsJsonObject();
    assertFalse(facade.verifyBallot(generateBallot(ballot.getAsJsonObject()), election));
  }

  /**
   * Tests that ballots are rejected that either lie about the count of the accumulation
   */
  @Test
  void testIncorretAccumulationCount() {
    for (JsonElement ballot : ballotInput.get("incorrectAccumulationCount").getAsJsonArray()) {
      assertFalse(facade.verifyBallot(generateBallot(ballot.getAsJsonObject()), election));
    }
  }

  /**
   * Tests that ballots that are honest about violating a selection limit are rejected
   */
  @Test
  void testLimitViolation() {
    when(contest2.getOptions()).thenReturn(List.of("a", "b", "c"));
    when(contest2.getMax()).thenReturn(0);
    for (JsonElement ballot : ballotInput.get("validBallots").getAsJsonArray()) {
      assertFalse(facade.verifyBallot(generateBallot(ballot.getAsJsonObject()), election));
    }
  }
}
