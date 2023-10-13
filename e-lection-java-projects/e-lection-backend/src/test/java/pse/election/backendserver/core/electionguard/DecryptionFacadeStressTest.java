package pse.election.backendserver.core.electionguard;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import electioncli.handle.decryption.CompensationHandler;
import electioncli.handle.decryption.DecryptionHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import pse.election.backendserver.core.service.*;
import pse.election.backendserver.entity.*;

import java.io.*;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

public class DecryptionFacadeStressTest {
    private static final String INPUT_PATH = "./TestingSet2";
    private static final int MAX_DURATION = 5000;
    long electionId = 1L;
    Contest contest1;
    Contest contest2;
    Map<Integer, Map<Integer, Tally>> allTallies;
    Map<Integer, Trustee> trustees;
    static Map<Integer, Long> accumulationCost;
    static Map<Integer, Long> decryptionCost;
    static Instant startingTime;
    @Mock
    Ballot ballot1;
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
    @BeforeEach
    void generalSetup() {
        MockitoAnnotations.openMocks(this);
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
        allTallies = new HashMap<>();
        allTallies.put(0, new HashMap<>());
        allTallies.put(1, new HashMap<>());
    }

    JsonObject accumulation(int count) {
        JsonObject ballot =readInput(INPUT_PATH + "/ballots/ballot1.json");
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocation) {
                Tally tally = invocation.getArgument(0);
                allTallies.get(tally.getContestIndex()).put(tally.getOptionIndex(), tally);
                return null;
            }
        }).when(tallyService).addTally(any(Tally.class));
        Mockito.doAnswer(new Answer<List<OptionEncrypted>>() {
                    @Override
                    public List<OptionEncrypted> answer(final InvocationOnMock invocation) {
                        int contestId = invocation.getArgument(1);
                        int optionId = invocation.getArgument(2);
                        String pad = ballot.get("cipherText").getAsJsonObject().get(String.valueOf(contestId))
                                .getAsJsonArray().get(optionId).getAsJsonObject().get("pad").getAsString();
                        String data = ballot.get("cipherText").getAsJsonObject().get(String.valueOf(contestId))
                                .getAsJsonArray().get(optionId).getAsJsonObject().get("data").getAsString();
                        List<OptionEncrypted> options = new ArrayList<>();
                        for (int i = 0; i < count; i++) {
                            options.add(new OptionEncrypted(i, electionId, optionId, contestId, pad, data, "", ""));
                        }
                        return options;
                    }
                }).when(ballotService)
                .getAllOptionEncryptedOfContestAndOption(eq(electionId), anyInt(), anyInt());
        decryptionFacade.combineOptionsEncryptedToTallies(election);
        JsonObject result = new JsonObject();
        result.add("encryptedSpoiledBallotQuestions", new JsonObject());
        JsonObject tally = new JsonObject();
        for (Map.Entry<Integer, Map<Integer, Tally>> contest : allTallies.entrySet()) {
            JsonArray contestTally = new JsonArray();
            for (Tally option : contest.getValue().values()) {
                JsonObject optionTally = new JsonObject();
                optionTally.addProperty("pad", allTallies.get(option.getContestIndex())
                        .get(option.getOptionIndex()).getCiphertextPAD().toString(16));
                optionTally.addProperty("data", allTallies.get(option.getContestIndex())
                        .get(option.getOptionIndex()).getCiphertextDATA().toString(16));
                contestTally.add(optionTally);
            }
            tally.add(String.valueOf(contest.getKey()), contestTally);
        }
        result.add("encryptedTally", tally);
        return result;
    }

    void decryptionSetup(JsonObject encryption, List<Trustee> availableTrustees, List<Trustee> missingTrustees) {
        encryption.addProperty("baseHash", "ABC");
        Map<Integer, JsonObject> pdecryption = new HashMap<>();
        Map<Integer, JsonObject> ppdecryption = new HashMap<>();
        Map<Integer, Map<Integer, String>> backups = new HashMap<>();
        DecryptionHandler decryptionHandler = new DecryptionHandler();
        CompensationHandler compensationHandler = new CompensationHandler();
        for (Trustee trustee : availableTrustees) {
            JsonObject privateElection = readInput(INPUT_PATH + "/trustee" + trustee.getIndex() + "/ceremony_private.Json");
            Instant start = Instant.now();
            decryptionHandler.initialize(new JsonObject[]{encryption, privateElection}, new String[0]);
            pdecryption.put(trustee.getIndex(), decryptionHandler.execute()[0]);
            when(trustee.isAvailable()).thenReturn(true);
        }
        for (Trustee missingTrustee : missingTrustees) {
            JsonObject allBackups = readInput(INPUT_PATH + "/trustee" + missingTrustee.getTrusteeId() +
                    "/ceremony_private.Json");
            backups.put(missingTrustee.getIndex(), new HashMap<>());
            for (String trusteeIndex : allBackups.keySet()) {
                backups.get(trusteeIndex).put(Integer.valueOf(trusteeIndex), allBackups.get(trusteeIndex).getAsString());
            }
            when(missingTrustee.isAvailable()).thenReturn(false);

        }
        if (!missingTrustees.isEmpty()) {
            for (Trustee trustee : availableTrustees) {
                JsonObject privateAux = readInput(INPUT_PATH + "/trustee" + trustee.getIndex() + "/aux_private.Json");
                JsonObject allBackups = new JsonObject();
                JsonObject encryption2 = encryption.deepCopy();
                for (Trustee other : missingTrustees) {
                    allBackups.addProperty(String.valueOf(other.getIndex()),
                            backups.get(other.getIndex()).get(trustee.getIndex()));
                }
                encryption2.add("keyBackups", allBackups);
                compensationHandler.initialize(new JsonObject[]{privateAux, encryption2}, new String[0]);
                ppdecryption.put(trustee.getIndex(), compensationHandler.execute()[0]);
            }
        }
        when(ballotService.getAllSpoiledBallotsOfElection(electionId)).thenReturn(new ArrayList<>());
        Mockito.doAnswer(new Answer<List<PartialDecryption>>() {
            @Override
            public List<PartialDecryption> answer(InvocationOnMock invocation) throws Throwable {
                List<PartialDecryption> allDec = new ArrayList<>();
                int contestId = invocation.getArgument(1);
                int optionId = invocation.getArgument(2);
                for (Trustee trustee : availableTrustees) {
                    String decryption = pdecryption.get(trustee.getIndex())
                            .getAsJsonObject().get("partialDecryptedTally").getAsJsonObject().get("0")
                            .getAsJsonObject().get("partialDecryption").getAsJsonObject()
                            .get(String.valueOf(contestId))
                            .getAsJsonArray().get(optionId).getAsString();
                    allDec.add(new PartialDecryption(0L, trustee.getTrusteeId(), -1L, decryption, ""));
                }
                return allDec;
            }
        }).when(decryptionService).getAllPartialDecryptionOfTally(eq(electionId), anyInt(), anyInt());
        Mockito.doAnswer(new Answer<List<PartialPartialDecryption>>() {
                    @Override
                    public List<PartialPartialDecryption> answer(InvocationOnMock invocation) throws Throwable {
                        List<PartialPartialDecryption> allDec = new ArrayList<>();
                        int contest = invocation.getArgument(1);
                        int option = invocation.getArgument(2);
                        for (Trustee trustee : availableTrustees) {
                            for (Trustee missingTrustee : missingTrustees) {
                                String decryption = ppdecryption.get(trustee.getIndex()).
                                        getAsJsonObject().get("partialDecryptedTally").getAsJsonObject()
                                        .get(String.valueOf(missingTrustee.getIndex()))
                                        .getAsJsonObject().get("partialDecryption").getAsJsonObject()
                                        .get(String.valueOf(contest))
                                        .getAsJsonArray().get(option).getAsString();
                                allDec.add(new PartialPartialDecryption(trustee.getTrusteeId(),
                                        missingTrustee.getIndex(), 0L, -1L, decryption, ""));
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
                String data = encryption.get("encryptedTally").getAsJsonObject().get(String.valueOf(contest))
                        .getAsJsonArray().get(option).getAsJsonObject().get("data").getAsString();
                return new Tally(electionId, contest, option, "", data);
            }
        }).when(tallyService).getSpecificTally(eq(electionId), anyInt(), anyInt());
        Mockito.doAnswer(new Answer<List<PartialDecryption>>() {
            @Override
            public List<PartialDecryption> answer(InvocationOnMock invocation) throws Throwable {
                Long trusteeId = invocation.getArgument(0);
                if (missingTrustees.contains(trusteeId.intValue())) {
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
        }).when(trustee3).setLagrangeCoefficient(anyString());
        decryptionFacade.computeLagrangeCoefficients(election);
        when(trustee1.getLagrangeCoefficient()).thenReturn(lagrange[0]);
        when(trustee2.getLagrangeCoefficient()).thenReturn(lagrange[1]);
        when(trustee3.getLagrangeCoefficient()).thenReturn(lagrange[2]);
    }

    void setupResultCheck(int count) {
        Map<Integer, Integer[]> realResult = Map.of(0, new Integer[]{count, 0, 0, 0}, 1, new Integer[]{0, count, 0});
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Map<Integer, Integer[]> result = (Map<Integer, Integer[]>) invocation.getArgument(1);
                for (int contest : result.keySet()) {
                    for (int option = 0; option < result.get(contest).length; option++) {
                        assertEquals(result.get(contest)[option], realResult.get(contest)[option]);
                    }
                }
                return null;
            }
        }).when(electionService).setResult(eq(electionId), any(Map.class));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 10, 100, 1000, 10000, 50000})
    @Disabled
    void testDecryptionLimit(int count) {
        System.out.println("Initiating test");
        System.out.println(count);
        Instant start = Instant.now();
        JsonObject encryption = accumulation(count);
        Instant end = Instant.now();
        accumulationCost.put(count, Timestamp.from(end).getTime() - Timestamp.from(start).getTime());
        decryptionSetup(encryption, List.of(trustee1, trustee2, trustee3), List.of());
        setupResultCheck(count);
        start = Instant.now();
        decryptionFacade.evaluateResult(election);
        end = Instant.now();
        decryptionCost.put(count, Timestamp.from(end).getTime() - Timestamp.from(start).getTime());
    }

    JsonObject readInput(String path) {
        try {
            File file = new File(path);
            Reader reader = new FileReader(file);
            JsonObject data = JsonParser.parseReader(reader).getAsJsonObject();
            reader.close();
            return data;
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @BeforeAll
    static void start() {
        startingTime = Instant.now();
        accumulationCost =new HashMap<>();
        decryptionCost = new HashMap<>();
    }

    /**
    @AfterAll
    @Disabled
    //Log doesnt work in CI
    static void log() throws IOException {
        File file = new File("./src/test/resources/out/log.txt");
        if (!file.exists()) {
            file.createNewFile();
        }
        Writer writer = new FileWriter(file, true);
        writer.write("Initiating test at: " + startingTime.toString() + System.lineSeparator());
        List<Integer> keys = new ArrayList<>();
        keys.addAll(accumulationCost.keySet());
        Collections.sort(keys);
        for (Integer count : keys) {
            writer.write(count + ": " + accumulationCost.get(count) + ", " + decryptionCost.get(count) + System.lineSeparator());
        }
        writer.flush();
        writer.close();
    }
    */
}
