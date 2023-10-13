package pse.election.backendserver.core.bot;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sunya.electionguard.ElectionCryptoContext;
import com.sunya.electionguard.Group;
import electioncli.handle.decryption.CompensationHandler;
import electioncli.handle.decryption.DecryptionHandler;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.math.BigInteger;
import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;
import pse.election.backendserver.core.electionguard.DecryptionFacade;
import pse.election.backendserver.core.service.BallotService;
import pse.election.backendserver.core.service.DecryptionService;
import pse.election.backendserver.core.service.ElectionService;
import pse.election.backendserver.core.service.TallyService;
import pse.election.backendserver.core.service.TrusteeService;
import pse.election.backendserver.entity.Ballot;
import pse.election.backendserver.entity.BotTrustee;
import pse.election.backendserver.entity.Contest;
import pse.election.backendserver.entity.Election;
import pse.election.backendserver.entity.OptionEncrypted;
import pse.election.backendserver.entity.PartialDecryption;
import pse.election.backendserver.entity.PartialPartialDecryption;
import pse.election.backendserver.entity.Tally;
import pse.election.backendserver.entity.Trustee;
import pse.election.backendserver.payload.response.record.ElectionGuardInitializedWrapper;

/**
 * Tests the decryption with a bot and the cli Shall test for: Decryption works Partial partial
 * decryption by bot works The bot will replace trustee3 Result does not be checked for, decryption
 * not throwing will be enough since DecryptionFacadeTestExtended will test that decryption either
 * works correctly or throw
 */
public class BotIntegrationTestDecryption {

  long electionId = 0;
  String botMail = "bot@bot.de";
  JsonObject result;
  @Mock
  Trustee trustee1 = new Trustee(electionId, "trustee1@gmail.com", 1);
  @Mock
  Trustee trustee2 = new Trustee(electionId, "trustee2@gmail.com", 2);
  @Mock
  Election election;
  @Mock
  Trustee botTrustee;
  @Mock
  BotTrustee bot;
  List<PartialDecryption> botDecryptionTally;
  List<PartialDecryption> botDecryptionSpoiled;
  List<Tally> talliesOfElection;
  List<OptionEncrypted> talliesOfSpoiledBallots1;
  List<OptionEncrypted> talliesOfSpoiledBallots2;
  Map<Long, Tally> tallyMap = new HashMap<>();
  Map<Long, Map<Long, OptionEncrypted>> optionMap = new HashMap<>();

  //Question, Option, all trustees
  Map<Integer, Map<Integer, List<PartialDecryption>>> pDecryptionTally;
  //Question, Option, all trustees for all trustees
  Map<Integer, Map<Integer, List<PartialPartialDecryption>>> ppDecryptionTally;

  //Ballot, Question, option, all trustees
  Map<Integer, Map<Integer, Map<Integer, List<PartialDecryption>>>> pDecryptionSpoiled;
  //Ballot, Question, option, all trustees for all trustees
  Map<Integer, Map<Integer, Map<Integer, List<PartialPartialDecryption>>>> ppDecryptionSpoiled;
  Map<String, Long> trusteeMap;
  @Mock
  TrusteeService trusteeService;
  @Mock
  DecryptionService decryptionService;
  @Mock
  TallyService tallyService;
  @Mock
  BallotService ballotService;
  @Mock
  ElectionGuardInitializedWrapper wrapper;
  @Mock
  ElectionCryptoContext context;
  @Mock
  Ballot ballot1;
  @Mock
  Ballot ballot2;
  @Mock
  ElectionService electionService;
  @InjectMocks
  BotFacade botFacadeUnderTest;
  @InjectMocks
  DecryptionFacade decryptionFacade;
  private static final String INPUT_PATH = "./TestingSet2/";

  @BeforeEach
  void botDecryptionSetup() {
    MockitoAnnotations.openMocks(this);
    JsonObject result = readInput(INPUT_PATH + "trustee1/encrypted_tallies.json");
    when(botTrustee.getIndex()).thenReturn(3);
    when(botTrustee.getEmail()).thenReturn(botMail);
    when(trusteeService.getBotTrustee(electionId)).thenReturn(bot);
    when(trusteeService.getTrustee(3L)).thenReturn(botTrustee);
    String privateElGamalKey = readInput(INPUT_PATH + "trustee3/ceremony_private.Json").get(
            "privateKey").getAsJsonArray()
        .get(0).getAsString();
    String privateAuxKey = readInput(INPUT_PATH + "trustee3/aux_private.Json").get("privateKey")
        .getAsString();
    when(bot.getPrivateAuxKey()).thenReturn(privateAuxKey);
    when(bot.getPrivateElgamalKey()).thenReturn(new BigInteger(privateElGamalKey, 16));
    when(bot.getTrusteeId()).thenReturn(3L);
    talliesOfElection = generateTallies(result.get("encryptedTally").getAsJsonObject());
    Map<Long, OptionEncrypted> ballot1Map = new HashMap<>();
    Map<Long, OptionEncrypted> ballot2Map = new HashMap<>();
    talliesOfSpoiledBallots1 = generateOptions(
        result.get("encryptedSpoiledBallotQuestions").getAsJsonObject().get("0")
            .getAsJsonObject(), ballot1Map);
    talliesOfSpoiledBallots2 = generateOptions(
        result.get("encryptedSpoiledBallotQuestions").getAsJsonObject().get("1")
            .getAsJsonObject(), ballot2Map);
    optionMap.put(0L, ballot1Map);
    optionMap.put(1L, ballot2Map);
    when(tallyService.getAllTalliesOfElection(electionId)).thenReturn(talliesOfElection);
    when(ballotService.getAllSpoiledBallotsOfElection(electionId)).thenReturn(
        List.of(ballot1, ballot2));
    when(ballotService.getAllOptionsEncryptedOfBallot(0L)).thenReturn(talliesOfSpoiledBallots1);
    when(ballotService.getAllOptionsEncryptedOfBallot(1L)).thenReturn(talliesOfSpoiledBallots2);
    when(ballot1.getBallotId()).thenReturn(0L);
    when(ballot2.getBallotId()).thenReturn(1L);
    String qbar = readInput(INPUT_PATH + "manifest.json").get("qBar").getAsString();
    ReflectionTestUtils.setField(context, "cryptoExtendedBaseHash", Group.hex_to_q_unchecked(qbar));
    when(wrapper.generateCryptoContext(electionId)).thenReturn(context);
    when(election.getElectionId()).thenReturn(electionId);

    List<Trustee> trusteeList = new ArrayList<>();
    trusteeList.addAll(List.of(trustee1, trustee2, botTrustee));
    when(trusteeService.getAllTrustees(electionId)).thenReturn(trusteeList);
    when(election.getElectionId()).thenReturn(electionId);
    Contest contest1 = new Contest(electionId, "A", 1, 0, List.of("a", "b", "c"));
    Contest contest2 = new Contest(electionId, "B", 1, 1, List.of("a", "b"));
    List<Contest> contestList = new ArrayList<>();
    contestList.addAll(List.of(contest2, contest1));
    when(electionService.getAllContestsOfElection(electionId)).thenReturn(contestList);
    when(trustee1.getTrusteeId()).thenReturn(1L);
    when(trustee2.getTrusteeId()).thenReturn(2L);
    when(trustee1.getIndex()).thenReturn(1);
    when(trustee2.getIndex()).thenReturn(2);
    when(botTrustee.getTrusteeId()).thenReturn(3L);
    trusteeMap = Map.of("1", trustee1.getTrusteeId(), "2", trustee2.getTrusteeId(),
        "3", botTrustee.getTrusteeId(), "0", 100L);
  }

  void decryptionSetup(String missingTrustee) {
    Mockito.doAnswer(new Answer<List<PartialDecryption>>() {
      @Override
      public List<PartialDecryption> answer(InvocationOnMock invocation) throws Throwable {
        int questionId = invocation.getArgument(1);
        int optionId = invocation.getArgument(2);
        List<PartialDecryption> decryptions = new ArrayList<>();
        for (PartialDecryption decryption : pDecryptionTally.get(questionId).get(optionId)) {
          decryptions.add(decryption);
        }
        return decryptions;
      }
    }).when(decryptionService).getAllPartialDecryptionOfTally(eq(electionId), anyInt(), anyInt());
    Mockito.doAnswer(new Answer<List<PartialPartialDecryption>>() {
          @Override
          public List<PartialPartialDecryption> answer(InvocationOnMock invocation) throws Throwable {
            int questionId = invocation.getArgument(1);
            int optionId = invocation.getArgument(2);
            List<PartialPartialDecryption> decryptions = new ArrayList<>();
            for (PartialPartialDecryption decryption : ppDecryptionTally.get(questionId)
                .get(optionId)) {
              decryptions.add(decryption);
            }
            return decryptions;
          }
        }).when(decryptionService)
        .getAllPartialPartialDecryptionOfTally(eq(electionId), anyInt(), anyInt());
    Mockito.doAnswer(new Answer<Tally>() {
      @Override
      public Tally answer(InvocationOnMock invocation) throws Throwable {
        int contest = invocation.getArgument(1);
        int option = invocation.getArgument(2);
        for (Tally tally : talliesOfElection) {
          if (tally.getContestIndex() == contest && tally.getOptionIndex() == option) {
            return tally;
          }
        }
        return null;
      }
    }).when(tallyService).getSpecificTally(eq(electionId), anyInt(), anyInt());

    Mockito.doAnswer(new Answer<List<PartialDecryption>>() {
          @Override
          public List<PartialDecryption> answer(InvocationOnMock invocation) throws Throwable {
            Long ballotId = invocation.getArgument(0);
            int questionId = invocation.getArgument(1);
            int optionId = invocation.getArgument(2);
            List<PartialDecryption> decryptions = new ArrayList<>();
            for (PartialDecryption decryption : pDecryptionSpoiled.get(ballotId.intValue())
                .get(questionId).get(optionId)) {
              decryptions.add(decryption);
            }
            return decryptions;
          }
        }).when(decryptionService)
        .getAllPartialDecryptionOfSpoiledBallotOption(anyLong(), anyInt(), anyInt());
    Mockito.doAnswer(new Answer<List<PartialPartialDecryption>>() {
          @Override
          public List<PartialPartialDecryption> answer(InvocationOnMock invocation) throws Throwable {
            Long ballotId = invocation.getArgument(0);
            int questionId = invocation.getArgument(1);
            int optionId = invocation.getArgument(2);
            List<PartialPartialDecryption> decryptions = new ArrayList<>();
            for (PartialPartialDecryption decryption : ppDecryptionSpoiled.get(ballotId.intValue())
                .get(questionId).get(optionId)) {
              decryptions.add(decryption);
            }
            return decryptions;
          }
        }).when(decryptionService)
        .getAllPartialPartialDecryptionOfSpoiledBallotOption(anyLong(), anyInt(), anyInt());
    Mockito.doAnswer(new Answer<OptionEncrypted>() {
      @Override
      public OptionEncrypted answer(InvocationOnMock invocation) throws Throwable {
        int contest = invocation.getArgument(1);
        int option = invocation.getArgument(2);
        long ballotId = invocation.getArgument(0);
        List<OptionEncrypted> options =
            ballotId == 0L ? talliesOfSpoiledBallots1 : talliesOfSpoiledBallots2;
        for (OptionEncrypted optionEnc : options) {
          if (optionEnc.getContestIndex() == contest && optionEnc.getOptionIndex() == option) {
            return optionEnc;
          }
        }
        return null;
      }
    }).when(ballotService).getSpecificOptionOfBallot(anyLong(), anyInt(), anyInt());

    Mockito.doAnswer(new Answer<List<PartialDecryption>>() {
      @Override
      public List<PartialDecryption> answer(InvocationOnMock invocation) throws Throwable {
        Long trusteeId = invocation.getArgument(0);
        if (trusteeId.intValue() == Integer.valueOf(missingTrustee)) {
          return List.of();
        } else {
          return List.of(new PartialDecryption());
        }
      }
    }).when(decryptionService).getAllPartialDecryptionByTrustee(anyLong());

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
    }).when(botTrustee).setLagrangeCoefficient(anyString());

    decryptionFacade.computeLagrangeCoefficients(election);
    when(trustee1.getLagrangeCoefficient()).thenReturn(lagrange[0]);
    when(trustee2.getLagrangeCoefficient()).thenReturn(lagrange[1]);
    when(botTrustee.getLagrangeCoefficient()).thenReturn(lagrange[2]);
  }

  void pDecryptionTrustee(Trustee trustee, String path) {
    DecryptionHandler handler = new DecryptionHandler();
    handler.initialize(new JsonObject[]{readInput(path + "encrypted_tallies.json"),
        readInput(path + "ceremony_private.Json")}, new String[0]);
    JsonObject decryption = handler.execute()[0];
    JsonObject tallyDecryption = decryption.get("partialDecryptedTally").getAsJsonObject().get("0")
        .getAsJsonObject().get("partialDecryption").getAsJsonObject();
    for (String question : tallyDecryption.keySet()) {
      JsonArray decryptionOfQuestion = tallyDecryption.get(question).getAsJsonArray();
      for (int i = 0; i < decryptionOfQuestion.size(); i++) {
        pDecryptionTally.get(Integer.valueOf(question)).get(i).add(new PartialDecryption(
            0L, trustee.getTrusteeId(), -1L, decryptionOfQuestion.get(i).getAsString(), ""
        ));
      }
    }
    JsonArray ballots = decryption.get("partialDecryptedSpoiledBallots").getAsJsonObject().get("0")
        .getAsJsonArray();
    for (JsonElement ballotDecryption : ballots) {
      JsonObject spoiledDecryption = ballotDecryption.getAsJsonObject().get("partialDecryption")
          .getAsJsonObject();
      for (String question : spoiledDecryption.keySet()) {
        JsonArray decryptionOfQuestion = spoiledDecryption.get(question).getAsJsonArray();
        for (int i = 0; i < decryptionOfQuestion.size(); i++) {
          pDecryptionSpoiled.get(
                  Integer.valueOf(ballotDecryption.getAsJsonObject().get("ballotId").getAsString()))
              .get(Integer.valueOf(question)).get(i).add(new PartialDecryption(
                  -1L, trustee.getTrusteeId(), 0L, decryptionOfQuestion.get(i).getAsString(), ""
              ));
        }
      }
    }
    when(trustee.isAvailable()).thenReturn(true);
  }

  void ppDecryptionTrustee(Trustee trustee, String path, List<Integer> missingTrustees) {
    CompensationHandler handler = new CompensationHandler();
    handler.initialize(new JsonObject[]{readInput(path + "encrypted_tallies.json"),
        readInput(path + "aux_private.Json")}, new String[0]);
    JsonObject decryption = handler.execute()[0];
    for (String otherTrustee : decryption.get("partialDecryptedTally").getAsJsonObject().keySet()) {
      if (missingTrustees.contains(Integer.valueOf(otherTrustee))) {
        JsonObject tallyDecryption = decryption.get("partialDecryptedTally").getAsJsonObject()
                .get(otherTrustee)
                .getAsJsonObject().get("partialDecryption").getAsJsonObject();
        for (String question : tallyDecryption.keySet()) {
          JsonArray decryptionOfQuestion = tallyDecryption.get(question).getAsJsonArray();
          for (int i = 0; i < decryptionOfQuestion.size(); i++) {
            ppDecryptionTally.get(Integer.valueOf(question)).get(i).add(new PartialPartialDecryption(
                    trustee.getTrusteeId(), trusteeMap.get(otherTrustee),
                    0L, -1L, decryptionOfQuestion.get(i).getAsString(), ""
            ));
          }
        }
        JsonArray ballots = decryption.get("partialDecryptedSpoiledBallots").getAsJsonObject()
                .get(otherTrustee).getAsJsonArray();
        for (JsonElement ballotDecryption : ballots) {
          JsonObject spoiledDecryption = ballotDecryption.getAsJsonObject().get("partialDecryption")
                  .getAsJsonObject();
          for (String question : spoiledDecryption.keySet()) {
            JsonArray decryptionOfQuestion = spoiledDecryption.get(question).getAsJsonArray();
            for (int i = 0; i < decryptionOfQuestion.size(); i++) {
              ppDecryptionSpoiled.get(
                              Integer.valueOf(ballotDecryption.getAsJsonObject().get("ballotId").getAsString()))
                      .get(Integer.valueOf(question)).get(i).add(new PartialPartialDecryption(
                              trustee.getTrusteeId(), trusteeMap.get(otherTrustee),
                              -1L, 0L, decryptionOfQuestion.get(i).getAsString(), ""
                      ));
            }
          }
        }
      }
    }
  }

  void addBotDecryption() {
    Map<Integer, String> backups = new HashMap<>();
    JsonObject backupObj = readInput(INPUT_PATH + "trustee3/encrypted_tallies.json");
    for (String trusteeId : backupObj.get("keyBackups").getAsJsonObject().keySet()) {
      backups.put(Integer.valueOf(trusteeId),
          backupObj.get("keyBackups").getAsJsonObject().get(trusteeId).getAsString());
    }
    when(trusteeService.getBackups(botMail, electionId, true)).thenReturn(backups);
    Mockito.doAnswer(new Answer() {
          @Override
          public Void answer(InvocationOnMock invocation) throws Throwable {

            Map<Long, DecryptionFacade.Decryption> tallyDecryptions = invocation.getArgument(0);
            Map<Long, Map<Long, DecryptionFacade.Decryption>> ballotDecryption = invocation.getArgument(
                1);
            int forWhichTrustee = invocation.getArgument(2);
            Long forWhichTrusteeId = trusteeMap.get(String.valueOf(forWhichTrustee));
            for (long tallyId : tallyDecryptions.keySet()) {
              Tally tally = tallyMap.get(tallyId);
              ppDecryptionTally.get(tally.getContestIndex()).get(tally.getOptionIndex()).add(
                  new PartialPartialDecryption(3L, forWhichTrusteeId, tallyId, -1L,
                      tallyDecryptions.get(tallyId).decryption().base16(), ""));
            }

            for (Map.Entry<Long, Map<Long, DecryptionFacade.Decryption>> ballot : ballotDecryption.entrySet()) {
              for (Map.Entry<Long, DecryptionFacade.Decryption> decryption : ballot.getValue()
                  .entrySet()) {
                OptionEncrypted option = optionMap.get(ballot.getKey()).get(decryption.getKey());
                ppDecryptionSpoiled.get(ballot.getKey().intValue()).get(option.getContestIndex())
                    .get(option.getOptionIndex()).add(
                        new PartialPartialDecryption(3L, forWhichTrusteeId, -1L,
                            option.getOptionEncryptedId(),
                            decryption.getValue().decryption().base16(), ""));
              }
            }
            return null;
          }
        }).when(decryptionService)
        .addPartialPartialDecryptionBotTrustee(any(), any(), anyInt(), eq(election));
    Mockito.doAnswer(new Answer() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        Map<Long, DecryptionFacade.Decryption> tallyDecryptions = invocation.getArgument(0);
        Map<Long, Map<Long, DecryptionFacade.Decryption>> ballotDecryption = invocation.getArgument(
            1);
        for (long tallyId : tallyDecryptions.keySet()) {
          Tally tally = tallyMap.get(tallyId);
          pDecryptionTally.get(tally.getContestIndex()).get(tally.getOptionIndex()).add(
              new PartialDecryption(tallyId, 3L, -1L,
                  tallyDecryptions.get(tallyId).decryption().base16(), ""));
        }

        for (Map.Entry<Long, Map<Long, DecryptionFacade.Decryption>> ballot : ballotDecryption.entrySet()) {
          for (Map.Entry<Long, DecryptionFacade.Decryption> decryption : ballot.getValue()
              .entrySet()) {
            OptionEncrypted option = optionMap.get(ballot.getKey()).get(decryption.getKey());
            pDecryptionSpoiled.get(ballot.getKey().intValue()).get(option.getContestIndex())
                .get(option.getOptionIndex()).add(
                    new PartialDecryption(-1L, 3L, option.getOptionEncryptedId(),
                        decryption.getValue().decryption().base16(), ""));
          }
        }
        botTrustee.setAvailable();
        return null;
      }
    }).when(decryptionService).addPartialDecryptionBotTrustee(any(), any(), eq(election));
  }

  void setupDecryptionMaps() {
    pDecryptionTally = new HashMap<>();
    ppDecryptionTally = new HashMap<>();
    pDecryptionSpoiled = new HashMap<>();
    ppDecryptionSpoiled = new HashMap<>();
    for (int ballot = 0; ballot < 2; ballot++) {
      Map<Integer, Map<Integer, List<PartialDecryption>>> pDecBallot = new HashMap<>();
      Map<Integer, Map<Integer, List<PartialPartialDecryption>>> ppDecBallot = new HashMap<>();
      int[] optionCount = new int[]{4, 3};
      for (int contest = 0; contest < 2; contest++) {
        Map<Integer, List<PartialDecryption>> pDecContest = new HashMap<>();
        Map<Integer, List<PartialPartialDecryption>> ppDecContest = new HashMap<>();
        for (int option = 0; option < optionCount[contest]; option++) {
          pDecContest.put(option, new ArrayList<>());
          ppDecContest.put(option, new ArrayList<>());
        }
        pDecBallot.put(contest, pDecContest);
        ppDecBallot.put(contest, ppDecContest);
      }
      pDecryptionSpoiled.put(ballot, pDecBallot);
      ppDecryptionSpoiled.put(ballot, ppDecBallot);
    }
    int[] optionCount = new int[]{4, 3};
    for (int contest = 0; contest < 2; contest++) {
      Map<Integer, List<PartialDecryption>> pDecContest = new HashMap<>();
      Map<Integer, List<PartialPartialDecryption>> ppDecContest = new HashMap<>();
      for (int option = 0; option < optionCount[contest]; option++) {
        pDecContest.put(option, new ArrayList<>());
        ppDecContest.put(option, new ArrayList<>());
      }
      pDecryptionTally.put(contest, pDecContest);
      ppDecryptionTally.put(contest, ppDecContest);
    }
  }

  @Test
  void testBotDecryptionAllTrustees() {
    setupDecryptionMaps();
    addBotDecryption();
    assertDoesNotThrow(() -> botFacadeUnderTest.partialDecryption(election));
    pDecryptionTrustee(trustee1, INPUT_PATH + "trustee1/");
    pDecryptionTrustee(trustee2, INPUT_PATH + "trustee2/");
    when(botTrustee.isAvailable()).thenReturn(true);
    decryptionSetup("0");
    decryptionFacade.evaluateResult(election);
    assertDoesNotThrow(() -> decryptionFacade.evaluateResult(election));
  }

  @Test
  void testBotDecryptionMissingTrustee() {
    setupDecryptionMaps();
    pDecryptionTrustee(trustee2, INPUT_PATH + "trustee2/");
    when(trustee1.isAvailable()).thenReturn(false);
    ppDecryptionTrustee(trustee2, INPUT_PATH + "trustee2/", List.of(trustee1.getIndex()));
    when(botTrustee.isAvailable()).thenReturn(true);
    addBotDecryption();
    botFacadeUnderTest.partialDecryption(election);
    botFacadeUnderTest.partialPartialDecryption(election);
    decryptionSetup("1");
    decryptionFacade.evaluateResult(election);
  }

  /**
   * Assures that error is actually with the bot if error occurs
   */
  @Test
  void testWithoutBot() {
    setupDecryptionMaps();
    pDecryptionTrustee(trustee1, INPUT_PATH + "trustee1/");
    pDecryptionTrustee(trustee2, INPUT_PATH + "trustee2/");
    pDecryptionTrustee(botTrustee, INPUT_PATH + "trustee3/");
    decryptionSetup("0");
    assertDoesNotThrow(() -> decryptionFacade.evaluateResult(election));
  }

  @Test
  void testMissingWithoutBot() {
    setupDecryptionMaps();
    pDecryptionTrustee(trustee2, INPUT_PATH + "trustee2/");
    pDecryptionTrustee(botTrustee, INPUT_PATH + "trustee3/");
    ppDecryptionTrustee(trustee2, INPUT_PATH + "trustee2/", List.of(1));
    ppDecryptionTrustee(botTrustee, INPUT_PATH + "trustee3/", List.of(1));
    decryptionSetup("1");
    decryptionFacade.evaluateResult(election);
    assertDoesNotThrow(() -> decryptionFacade.evaluateResult(election));
  }

  private List<Tally> generateTallies(JsonObject talliesObject) {
    List<Tally> tallies = new ArrayList<>();
    long tallyCount = 0;
    for (String question : talliesObject.keySet()) {
      for (int i = 0; i < talliesObject.get(question).getAsJsonArray().size(); i++) {
        JsonObject tally = talliesObject.get(question).getAsJsonArray().get(i).getAsJsonObject();
        Tally mockedTally = mock(Tally.class);
        when(mockedTally.getCiphertextPAD()).thenReturn(
            new BigInteger(tally.get("pad").getAsString(), 16));
        when(mockedTally.getCiphertextDATA()).thenReturn(
            new BigInteger(tally.get("data").getAsString(), 16));
        when(mockedTally.getTallyId()).thenReturn(tallyCount);
        when(mockedTally.getContestIndex()).thenReturn(Integer.valueOf(question));
        when(mockedTally.getOptionIndex()).thenReturn(i);
        tallyMap.put(tallyCount, mockedTally);
        tallies.add(mockedTally);
        tallyCount++;
      }
    }
    return tallies;
  }

  private List<OptionEncrypted> generateOptions(JsonObject talliesObject,
      Map<Long, OptionEncrypted> ballotOptionMap) {
    List<OptionEncrypted> tallies = new ArrayList<>();
    long optionCount = 0;
    for (String question : talliesObject.keySet()) {
      for (int i = 0; i < talliesObject.get(question).getAsJsonArray().size(); i++) {
        JsonObject tally = talliesObject.get(question).getAsJsonArray().get(i).getAsJsonObject();
        OptionEncrypted mockedTally = mock(OptionEncrypted.class);
        when(mockedTally.getCiphertextPAD()).thenReturn(
            new BigInteger(tally.get("pad").getAsString(), 16));
        when(mockedTally.getCiphertextDATA()).thenReturn(
            new BigInteger(tally.get("data").getAsString(), 16));
        when(mockedTally.getOptionEncryptedId()).thenReturn(optionCount);
        when(mockedTally.getContestIndex()).thenReturn(Integer.valueOf(question));
        when(mockedTally.getOptionIndex()).thenReturn(i);
        when(mockedTally.getBallotId()).thenThrow(new IllegalArgumentException("need ballotId"));
        ballotOptionMap.put(optionCount, mockedTally);
        tallies.add(mockedTally);
        optionCount++;
      }
    }
    return tallies;
  }

  private JsonObject readInput(String path) {
    File inputFile = new File(path);
    JsonObject data;
    try {
      Reader reader = new FileReader(inputFile);
      data = JsonParser.parseReader(reader).getAsJsonObject();
      reader.close();
    } catch (IOException e) {
      return null;
    }
    return data;
  }
}
