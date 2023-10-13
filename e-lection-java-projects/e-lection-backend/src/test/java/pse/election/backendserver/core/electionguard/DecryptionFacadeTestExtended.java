package pse.election.backendserver.core.electionguard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
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
import pse.election.backendserver.core.service.BallotService;
import pse.election.backendserver.core.service.DecryptionService;
import pse.election.backendserver.core.service.ElectionService;
import pse.election.backendserver.core.service.TallyService;
import pse.election.backendserver.core.service.TrusteeService;
import pse.election.backendserver.entity.Ballot;
import pse.election.backendserver.entity.Contest;
import pse.election.backendserver.entity.Election;
import pse.election.backendserver.entity.OptionEncrypted;
import pse.election.backendserver.entity.PartialDecryption;
import pse.election.backendserver.entity.PartialPartialDecryption;
import pse.election.backendserver.entity.Tally;
import pse.election.backendserver.entity.Trustee;

/**
 * Shall test for: evaluateResult for good case with both spoiled ballot(s) and tally, simple
 * semi-realistic data evaluateResult for edge case with high accumulation, self-generated data
 * evaluateResult for invalid decryptions, reasonably fast failure evaluateResult for partial
 * partial decryption for spoiled ballot and tally, simple semi-realistic data evaluateResult for
 * invalid partial partial decryption
 * <p>
 * Also tests consistency of combineKeyShares of KeyCeremonyFacade and consistency of
 * combineOptionsEncryptedToTallies
 * <p>
 * Shall not test for: combineOptionsEncryptedToTallies working properly (already tested for in
 * DecryptionFacadeTest) Shall not test for: other bad cases including null pointers, incompatible
 * formats etc. as this is assumed to be handled at an earlier point
 * <p>
 * Note that the ballots provided in fullDecryptions.json is NOT fully realistic since the used
 * manifest hashes are incorrect. However, these are actually not relevant for the encryption
 * itself, the manifest is only provided so ballots can be easily created for the election
 */
public class DecryptionFacadeTestExtended {

  private static final String INPUT_PATH = "./src/test/resources";
  private static final int MAX_DURATION = 60000;
  long electionId = 1L;
  Contest contest1;
  Contest contest2;
  @Mock
  Ballot ballot1;
  @Mock
  Ballot ballot2;
  @Mock
  Trustee trustee1;
  @Mock
  Trustee trustee2;
  @Mock
  Trustee trustee3;
  @Mock
  TrusteeService trusteeService;
  @Mock
  ElectionService electionService;
  @Mock
  TallyService tallyService;
  @Mock
  BallotService ballotService;
  @Mock
  DecryptionService decryptionService;
  @Mock
  Election election;
  @InjectMocks
  KeyCeremonyFacade keyCeremonyFacade;
  @InjectMocks
  DecryptionFacade decryptionFacade;
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
  void generalSetup() {
    MockitoAnnotations.openMocks(this);
    input = readDecryptionData();
    when(trustee1.getPrimaryKey()).thenReturn(new BigInteger(
        input.get("trusteeKeys").getAsJsonObject().get("1").getAsJsonArray().get(0).getAsString(),
        16));
    when(trustee2.getPrimaryKey()).thenReturn(new BigInteger(
        input.get("trusteeKeys").getAsJsonObject().get("2").getAsJsonArray().get(0).getAsString(),
        16));
    when(trustee3.getPrimaryKey()).thenReturn(new BigInteger(
        input.get("trusteeKeys").getAsJsonObject().get("3").getAsJsonArray().get(0).getAsString(),
        16));
    when(trustee1.getTrusteeId()).thenReturn(1L);
    when(trustee2.getTrusteeId()).thenReturn(2L);
    when(trustee3.getTrusteeId()).thenReturn(3L);
    when(trustee1.getIndex()).thenReturn(1);
    when(trustee2.getIndex()).thenReturn(2);
    when(trustee3.getIndex()).thenReturn(3);
    List<Trustee> trusteeList = new ArrayList<>();
    trusteeList.addAll(List.of(trustee1, trustee2, trustee3));
    when(trusteeService.getAllTrustees(electionId)).thenReturn(trusteeList);
    when(election.getElectionId()).thenReturn(electionId);
    contest1 = new Contest(electionId, "A", 1, 0, List.of("a", "b", "c"));
    contest2 = new Contest(electionId, "B", 1, 1, List.of("a", "b"));
    List<Contest> contestList = new ArrayList<>();
    contestList.addAll(List.of(contest2, contest1));
    when(electionService.getAllContestsOfElection(electionId)).thenReturn(contestList);
  }

  @Test
  /**
   * Tests that the accumulation of ciphertext stays consistent to a reference accumulation that the decryption works for
   */
  void testAccumulation() {
    Mockito.doAnswer(new Answer<Void>() {
      @Override
      public Void answer(final InvocationOnMock invocation) {
        final Object[] args = invocation.getArguments();
        Tally tally = (Tally) args[0];
        assertEquals(tally.getCiphertextPAD().toString(16),
            input.get("accumulation").getAsJsonObject()
                .get(String.valueOf(tally.getContestIndex())).getAsJsonArray()
                .get(tally.getOptionIndex())
                .getAsJsonObject().get("pad").getAsString());
        assertEquals(tally.getCiphertextDATA().toString(16),
            input.get("accumulation").getAsJsonObject()
                .get(String.valueOf(tally.getContestIndex())).getAsJsonArray()
                .get(tally.getOptionIndex())
                .getAsJsonObject().get("data").getAsString());
        return null;
      }
    }).when(tallyService).addTally(any(Tally.class));
    Mockito.doAnswer(new Answer<List<OptionEncrypted>>() {
          @Override
          public List<OptionEncrypted> answer(final InvocationOnMock invocation) {
            final Object[] args = invocation.getArguments();
            return getAllEncryptions((int) args[1], (int) args[2]);
          }
        }).when(ballotService)
        .getAllOptionEncryptedOfContestAndOption(eq(electionId), anyInt(), anyInt());
    decryptionFacade.combineOptionsEncryptedToTallies(election);
  }

  /**
   * Sets up the mocking for a decryption. Allows up to one trustee to be missing (threshold = 2, n
   * = 3)
   *
   * @param missingTrustee the index of the trustee that is not available
   */
  void decryptionSetup(int missingTrustee, boolean faultyDecryptions, List<Trustee> trustees) {
    when(ballot1.getBallotId()).thenReturn(0L);
    when(ballot2.getBallotId()).thenReturn(1L);
    when(ballotService.getAllSpoiledBallotsOfElection(electionId)).thenReturn(
        List.of(ballot1, ballot2));
    Mockito.doAnswer(new Answer<List<PartialDecryption>>() {
      @Override
      public List<PartialDecryption> answer(InvocationOnMock invocation) throws Throwable {
        List<PartialDecryption> allDec = new ArrayList<>();
        for (int i = 1; i < 4; i++) {
          if (i != missingTrustee) {
            int contest = (int) invocation.getArguments()[1];
            int option = (int) invocation.getArguments()[2];
            String decryption = input.get("decryptions").getAsJsonObject().get(String.valueOf(i))
                .getAsJsonObject().get("partialDecryptedTally").getAsJsonObject().get("0")
                .getAsJsonObject().get("partialDecryption").getAsJsonObject()
                .get(String.valueOf(contest))
                .getAsJsonArray().get(option).getAsString();
            if (faultyDecryptions) {
              decryption = decryption.substring(1);
            }
            allDec.add(new PartialDecryption(0L, i, -1L, decryption, ""));
          }
        }
        return allDec;
      }
    }).when(decryptionService).getAllPartialDecryptionOfTally(eq(electionId), anyInt(), anyInt());
    Mockito.doAnswer(new Answer<List<PartialPartialDecryption>>() {
          @Override
          public List<PartialPartialDecryption> answer(InvocationOnMock invocation) throws Throwable {
            List<PartialPartialDecryption> allDec = new ArrayList<>();
            for (int i = 1; i < 4; i++) {
              if (i != missingTrustee) {
                int contest = (int) invocation.getArguments()[1];
                int option = (int) invocation.getArguments()[2];
                String decryption = input.get("decryptions").getAsJsonObject().get(String.valueOf(i))
                    .getAsJsonObject().get("partialDecryptedTally").getAsJsonObject()
                    .get(String.valueOf(missingTrustee))
                    .getAsJsonObject().get("partialDecryption").getAsJsonObject()
                    .get(String.valueOf(contest))
                    .getAsJsonArray().get(option).getAsString();
                allDec.add(new PartialPartialDecryption(i, missingTrustee, 0L, -1L, decryption, ""));
              }
            }
            return allDec;
          }
        }).when(decryptionService)
        .getAllPartialPartialDecryptionOfTally(eq(electionId), anyInt(), anyInt());
    Mockito.doAnswer(new Answer<Tally>() {
      @Override
      public Tally answer(InvocationOnMock invocation) throws Throwable {
        int contest = invocation.getArgument(1);
        int option = invocation.getArgument(2);
        String data = input.get("accumulation").getAsJsonObject().get(String.valueOf(contest))
            .getAsJsonArray().get(option).getAsJsonObject().get("data").getAsString();
        return new Tally(electionId, contest, option, "", data);
      }
    }).when(tallyService).getSpecificTally(eq(electionId), anyInt(), anyInt());

    Mockito.doAnswer(new Answer<List<PartialDecryption>>() {
          @Override
          public List<PartialDecryption> answer(InvocationOnMock invocation) throws Throwable {
            List<PartialDecryption> allDec = new ArrayList<>();
            for (int i = 1; i < 4; i++) {
              if (i != missingTrustee) {
                int contest = (int) invocation.getArguments()[1];
                int option = (int) invocation.getArguments()[2];
                long ballotId = invocation.getArgument(0);
                String decryption = input.get("decryptions").getAsJsonObject().get(String.valueOf(i))
                    .getAsJsonObject().get("partialDecryptedSpoiledBallots").getAsJsonObject().get("0")
                    .getAsJsonArray().get(((Long) ballotId).intValue())
                    .getAsJsonObject().get("partialDecryption").getAsJsonObject()
                    .get(String.valueOf(contest))
                    .getAsJsonArray().get(option).getAsString();
                allDec.add(new PartialDecryption(-1L, i, 0L, decryption, ""));
              }
            }
            return allDec;
          }
        }).when(decryptionService)
        .getAllPartialDecryptionOfSpoiledBallotOption(anyLong(), anyInt(), anyInt());
    Mockito.doAnswer(new Answer<List<PartialPartialDecryption>>() {
          @Override
          public List<PartialPartialDecryption> answer(InvocationOnMock invocation) throws Throwable {
            List<PartialPartialDecryption> allDec = new ArrayList<>();
            for (int i = 1; i < 4; i++) {
              if (i != missingTrustee) {
                int contest = (int) invocation.getArguments()[1];
                int option = (int) invocation.getArguments()[2];
                long ballotId = invocation.getArgument(0);
                String decryption = input.get("decryptions").getAsJsonObject().get(String.valueOf(i))
                    .getAsJsonObject().get("partialDecryptedSpoiledBallots").getAsJsonObject()
                    .get(String.valueOf(missingTrustee))
                    .getAsJsonArray().get(((Long) ballotId).intValue())
                    .getAsJsonObject().get("partialDecryption").getAsJsonObject()
                    .get(String.valueOf(contest))
                    .getAsJsonArray().get(option).getAsString();
                allDec.add(new PartialPartialDecryption(i, missingTrustee, -1L, 0L, decryption, ""));
              }
            }
            return allDec;
          }
        }).when(decryptionService)
        .getAllPartialPartialDecryptionOfSpoiledBallotOption(anyLong(), anyInt(), anyInt());
    Mockito.doAnswer(new Answer<OptionEncrypted>() {
      @Override
      public OptionEncrypted answer(InvocationOnMock invocation) throws Throwable {
        int contest = invocation.getArgument(1);
        int option = invocation.getArgument(2);
        long ballotId = invocation.getArgument(0);
        String data = input.get("encryptions").getAsJsonObject().get(String.valueOf(ballotId))
            .getAsJsonObject().get(String.valueOf(contest))
            .getAsJsonArray().get(option).getAsJsonObject().get("data").getAsString();
        return new OptionEncrypted(ballotId, electionId, option, contest, "", data, "", "");
      }
    }).when(ballotService).getSpecificOptionOfBallot(anyLong(), anyInt(), anyInt());

    for (Trustee trustee : trustees) {
      if (trustee.getIndex() != missingTrustee) {
        when(trustee.isAvailable()).thenReturn(true);
      } else {
        when(trustee.isAvailable()).thenReturn(false);
      }
    }

    BigInteger[] lagrange = new BigInteger[3];
    Mockito.doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        lagrange[0] = new BigInteger(invocation.getArgument(0), 16);
        return null;
      }
    }).when(trustee1).setLagrangeCoefficient(anyString());
    Mockito.doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        lagrange[1] = new BigInteger(invocation.getArgument(0), 16);
        return null;
      }
    }).when(trustee2).setLagrangeCoefficient(anyString());
    Mockito.doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        lagrange[2] = new BigInteger(invocation.getArgument(0), 16);
        return null;
      }
    }).when(trustee3).setLagrangeCoefficient(anyString());
    decryptionFacade.computeLagrangeCoefficients(election);
    when(trustee1.getLagrangeCoefficient()).thenReturn(lagrange[0]);
    when(trustee2.getLagrangeCoefficient()).thenReturn(lagrange[1]);
    when(trustee3.getLagrangeCoefficient()).thenReturn(lagrange[2]);
  }

  void setupResultCheck() {
    Mockito.doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        Map<Integer, Integer[]> result = (Map<Integer, Integer[]>) invocation.getArgument(1);
        for (int contest : result.keySet()) {
          for (int option = 0; option < result.get(contest).length; option++) {
            assertEquals(result.get(contest)[option], input.get("result").getAsJsonObject()
                .get(String.valueOf(contest)).getAsJsonArray().get(option).getAsInt());
          }
        }
        return null;
      }
    }).when(electionService).setResult(eq(electionId), any(Map.class));
    Mockito.doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        Map<Long, Map<Integer, Integer[]>> results = invocation.getArgument(0);
        for (long ballodId : results.keySet()) {
          Map<Integer, Integer[]> result = results.get(ballodId);
          for (int contest : result.keySet()) {
            for (int option = 0; option < result.get(contest).length; option++) {
              assertEquals(result.get(contest)[option], input.get("ballotResults").getAsJsonObject()
                  .get(String.valueOf(ballodId)).getAsJsonObject().get(String.valueOf(contest))
                  .getAsJsonArray().get(option).getAsInt());
            }
          }
        }
        return null;
      }
    }).when(ballotService).saveDecryptedSpoiledBallot(any(Map.class));
  }

  @Test
  void testValidDecryptionAllTrustees() {
    decryptionSetup(0, false, List.of(trustee1, trustee2, trustee3));
    setupResultCheck();
    decryptionFacade.evaluateResult(election);
  }

  @Test
  void testValidDecryptionMissingTrustees() {
    setupResultCheck();
    for (int i = 1; i < 3; i++) {
      decryptionSetup(i, false, List.of(trustee1, trustee2, trustee3));
      decryptionFacade.evaluateResult(election);
    }
  }

  /**
   * Tests that an invalid decryption does not deadlock the system
   */
  @Test
  void testInvalidDecryption() {
    decryptionSetup(0, true, List.of(trustee1, trustee2, trustee3));
    Date start = Date.from(Instant.now());
    assertThrows(Exception.class, () -> decryptionFacade.evaluateResult(election));
    Date end = Date.from(Instant.now());
    assertTrue(end.getTime() - start.getTime() <= MAX_DURATION);
    System.out.println(end.getTime() - start.getTime());
  }

  private List<OptionEncrypted> getAllEncryptions(int contestIndex, int optionIndex) {
    JsonObject encryptions = input.get("encryptions").getAsJsonObject();
    List<OptionEncrypted> options = new ArrayList<>();
    for (String key : encryptions.keySet()) {
      JsonObject enc = encryptions.get(key).getAsJsonObject().get(String.valueOf(contestIndex))
          .getAsJsonArray().get(optionIndex).getAsJsonObject();
      OptionEncrypted option = new OptionEncrypted(Long.valueOf(key), electionId, optionIndex,
          contestIndex,
          enc.get("pad").getAsString(), enc.get("data").getAsString(), "", "");
      options.add(option);
    }
    return options;
  }
}
