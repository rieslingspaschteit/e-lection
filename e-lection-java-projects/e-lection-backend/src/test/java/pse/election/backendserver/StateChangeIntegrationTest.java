package pse.election.backendserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import electioncli.handle.decryption.CompensationHandler;
import electioncli.handle.decryption.DecryptionHandler;
import electioncli.handle.key_generation.AuxKeyHandler;
import electioncli.handle.key_generation.ElectionKeyHandler;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import pse.election.backendserver.repository.BotTrusteeRepository;
import pse.election.backendserver.repository.ElectionRepository;
import pse.election.backendserver.security.user.UserPrincipal;

//FIXME Tests sometimes unstable?

/**
 * Tests the state changes of an election when using a bot trustee Shall test for: Completion of key
 * ceremony with a bot and external trustees Failure of upload of EPKB when trustee tries to upload
 * an invalid backup for the bot Completion of decryption when using a bot trustee Completion of
 * partial partial decryption when using a bot trustee Partial partial decryption only ends when
 * every trustee present in decryption has uploaded partial partial decryption Completion of key
 * ceremony and decryption when only using bot trustee Shall not test for: Decryption, Verification
 * and Decryption of ballots as this would require to include frontend
 */
@SpringBootTest(args = {"--oidcClients=./fakeOIDC.toml",
    "--authorityConfig=./fakeAuthorities.txt"}, webEnvironment = WebEnvironment.RANDOM_PORT)
public class StateChangeIntegrationTest {

  private static final String INPUT_PATH = "./TestingSet3/";
  String authorityEmail = "no@valid.email.com";
  int electionDuration = 5;
  @Autowired
  ElectionRepository electionRepository;
  @Autowired
  BotTrusteeRepository botTrusteeRepository;
  @Mock
  UserPrincipal authorityPrincipal;
  @Autowired
  private WebApplicationContext context;
  private MockMvc mockMvc;

  @BeforeEach
  public void setup() {
    mockMvc = MockMvcBuilders
        .webAppContextSetup(context)
        .apply(springSecurity())
        .build();
    when(authorityPrincipal.getEmail()).thenReturn(authorityEmail);
    when(authorityPrincipal.getName()).thenReturn(authorityEmail);
    for (String repository : context.getBeanNamesForAnnotation(Repository.class)) {
      if (!repository.matches("authorityRepository")) {
        System.out.println(repository);
        CrudRepository<Object, String> repo = (CrudRepository<Object, String>) context.getBean(
            repository);
        repo.deleteAll();
      }
    }
  }

  JsonObject readFile(String path) {
    File inputFile = new File(path);
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

  long setupElectionExternal(JsonObject config) throws Exception {
    MvcResult result = mockMvc.perform(post("/api/authority/elections/create").
        contentType(MediaType.APPLICATION_JSON).content(new Gson().toJson(config)).
        with(oidcLogin().oidcUser(authorityPrincipal))).andReturn();
    System.out.println(result.getResponse().getContentAsString());
    assertTrue(result.getResponse().getStatus() < 400);
    JsonObject response = new Gson().fromJson(result.getResponse().getContentAsString(),
        JsonObject.class);
    assertEquals(response.get("state").getAsString(), "KEY_CEREMONY");
    return response.get("electionId").getAsLong();
  }

  void auxKeysExternals(List<ExternalTrustee> trustees, long electionId) throws Exception {
    int count = 1;
    MvcResult result;
    for (ExternalTrustee trustee : trustees) {
      String addedAuxKey = addAuxKey(trustee);
      result = mockMvc.perform(get("/api/authority/elections/" + electionId + "/key-cer")
          .with(oidcLogin().oidcUser(authorityPrincipal))).andReturn();
      JsonObject ceremonyState = new Gson().fromJson(result.getResponse().getContentAsString(),
          JsonObject.class);
      System.out.println(
          result.getResponse().getContentAsString() + " " + result.getResponse().getStatus());
      assertTrue(result.getResponse().getStatus() < 400);
      assertEquals(ceremonyState.get("keyCerState").getAsString(), "AUX_KEYS");
      assertEquals(ceremonyState.get("keyCerCount").getAsInt(), count++);
      System.out.println(trustee.mockedPrincipal.getName());
      System.out.println(addedAuxKey);
      result = mockMvc.perform(post("/api/trustee/elections/" + electionId + "/auxkeys").
          contentType(MediaType.APPLICATION_JSON).content(addedAuxKey).
          with(oidcLogin().oidcUser(trustee.mockedPrincipal))).andReturn();
      System.out.println(result.getResponse().getContentAsString());
    }
    result = mockMvc.perform(get("/api/authority/elections/" + electionId + "/key-cer")
        .with(oidcLogin().oidcUser(authorityPrincipal))).andReturn();
    JsonObject ceremonyState = new Gson().fromJson(result.getResponse().getContentAsString(),
        JsonObject.class);
    System.out.println(
        result.getResponse().getContentAsString() + " " + result.getResponse().getStatus());
    assertTrue(result.getResponse().getStatus() < 400);
    assertEquals(ceremonyState.get("keyCerState").getAsString(), "EPKB");
    assertEquals(ceremonyState.get("keyCerCount").getAsInt(), 1);
  }

  void epkbExternals(long electionId, List<ExternalTrustee> trustees) throws Exception {
    int count = 1;
    MvcResult result;
    for (ExternalTrustee trustee : trustees) {
      result = mockMvc.perform(get("/api/authority/elections/" + electionId + "/key-cer")
          .with(oidcLogin().oidcUser(authorityPrincipal))).andReturn();
      JsonObject ceremonyState = new Gson().fromJson(result.getResponse().getContentAsString(),
          JsonObject.class);
      System.out.println(
          result.getResponse().getContentAsString() + " " + result.getResponse().getStatus());
      assertTrue(result.getResponse().getStatus() < 400);
      assertEquals(ceremonyState.get("keyCerState").getAsString(), "EPKB");
      assertEquals(ceremonyState.get("keyCerCount").getAsInt(), count++);

      result = mockMvc.perform(get("/api/trustee/elections/" + electionId + "/auxkeys")
          .with(oidcLogin().oidcUser(trustee.mockedPrincipal))).andReturn();
      String auxKeys = result.getResponse().getContentAsString();
      assertTrue(result.getResponse().getStatus() < 400);
      String invalidEPKB = addEPKB(trustee,
          new Gson().fromJson(result.getResponse().getContentAsString(), JsonObject.class), true);
      result = mockMvc.perform(post("/api/trustee/elections/" + electionId + "/keys-and-backups").
          contentType(MediaType.APPLICATION_JSON).content(invalidEPKB).
          with(oidcLogin().oidcUser(trustee.mockedPrincipal))).andReturn();
      assertFalse(result.getResponse().getStatus() < 400);
      System.out.println(result.getResponse().getContentAsString());
      String validEPKB = addEPKB(trustee, new Gson().fromJson(auxKeys, JsonObject.class), false);
      System.out.println(validEPKB);
      result = mockMvc.perform(post("/api/trustee/elections/" + electionId + "/keys-and-backups").
          contentType(MediaType.APPLICATION_JSON).content(validEPKB).
          with(oidcLogin().oidcUser(trustee.mockedPrincipal))).andReturn();
      assertTrue(result.getResponse().getStatus() < 400);

    }
    result = mockMvc.perform(get("/api/authority/elections/" + electionId + "/key-cer")
        .with(oidcLogin().oidcUser(authorityPrincipal))).andReturn();
    JsonObject ceremonyState = new Gson().fromJson(result.getResponse().getContentAsString(),
        JsonObject.class);
    System.out.println(
        result.getResponse().getContentAsString() + " " + result.getResponse().getStatus());
    assertTrue(result.getResponse().getStatus() < 400);
    assertEquals(ceremonyState.get("keyCerState").getAsString(), "FINISHED");
    //Todo: im Frontend irreführende Anzeige entfernen
    assertEquals(ceremonyState.get("keyCerCount").getAsInt(), 0);
  }

  void uploadDecryption(int decryptionCount, ExternalTrustee trustee, long electionId)
      throws Exception {
    MvcResult result = mockMvc.perform(get("/api/trustee/elections/" + electionId + "/result")
        .with(oidcLogin().oidcUser(trustee.mockedPrincipal))).andReturn();
    assertTrue(result.getResponse().getStatus() < 400);
    JsonObject encryptedResult = new Gson().fromJson(result.getResponse().getContentAsString(),
        JsonObject.class);
    String decryption = decrypt(trustee, encryptedResult);
    result = mockMvc.perform(get("/api/authority/elections/" + electionId + "/decryption")
        .with(oidcLogin().oidcUser(authorityPrincipal))).andReturn();
    assertTrue(result.getResponse().getStatus() < 400);
    JsonObject decryptionStatus = new Gson().fromJson(result.getResponse().getContentAsString(),
        JsonObject.class);
    assertEquals("P_DECRYPTION", decryptionStatus.get("decState").getAsString());
    assertEquals(decryptionCount, decryptionStatus.get("decCount").getAsInt());
    result = mockMvc.perform(post("/api/trustee/elections/" + electionId + "/result")
        .with(oidcLogin().oidcUser(trustee.mockedPrincipal)).contentType(MediaType.APPLICATION_JSON)
        .content(decryption)).andReturn();
    System.out.println(result.getResponse().getContentAsString() +" " + result.getResponse().getStatus());
    assertTrue(result.getResponse().getStatus() < 400);
  }

  void uploadPPDecryption(int decryptionCount, ExternalTrustee trustee, long electionId)
      throws Exception {
    MvcResult result = mockMvc.perform(get("/api/trustee/elections/" + electionId + "/result")
        .with(oidcLogin().oidcUser(trustee.mockedPrincipal))).andReturn();
    assertTrue(result.getResponse().getStatus() < 400);
    JsonObject encryptedResult = new Gson().fromJson(result.getResponse().getContentAsString(),
        JsonObject.class);
    String ppdecryption = ppdecrypt(trustee, encryptedResult);
    result = mockMvc.perform(get("/api/authority/elections/" + electionId + "/decryption")
        .with(oidcLogin().oidcUser(authorityPrincipal))).andReturn();
    assertTrue(result.getResponse().getStatus() < 400);
    JsonObject decryptionStatus = new Gson().fromJson(result.getResponse().getContentAsString(),
        JsonObject.class);
    assertEquals("PP_DECRYPTION", decryptionStatus.get("decState").getAsString());
    assertEquals(decryptionCount, decryptionStatus.get("decCount").getAsInt());
    result = mockMvc.perform(post("/api/trustee/elections/" + electionId + "/result")
        .with(oidcLogin().oidcUser(trustee.mockedPrincipal)).contentType(MediaType.APPLICATION_JSON)
        .content(ppdecryption)).andReturn();
    assertTrue(result.getResponse().getStatus() < 400);
  }

  @ParameterizedTest
  @ValueSource(strings = {"threshold1.json", "threshold2.json"})
  @Disabled
  void testPartialDecryptionExternalTrustees(String path) throws Exception {
    JsonObject config = readFile(INPUT_PATH + path);
    List<ExternalTrustee> trustees = new ArrayList<>();
    for (JsonElement email : config.get("trustees").getAsJsonArray()) {
      trustees.add(new ExternalTrustee(email.getAsString()));
    }
    Instant end = Instant.now().plus(Duration.ofSeconds(electionDuration))
        .truncatedTo(ChronoUnit.SECONDS);
    JsonObject meta = config.getAsJsonObject("electionMeta").getAsJsonObject();
    meta.addProperty("end", end.toString());
    config.add("electionMeta", meta);
    long electionId = setupElectionExternal(config);
    auxKeysExternals(trustees, electionId);
    epkbExternals(electionId, trustees);
    System.out.println(Duration.between(Instant.now(), end));
    JsonObject stateChange = new JsonObject();
    stateChange.addProperty("state", "OPEN");
    MvcResult result = mockMvc.perform(patch("/api/authority/elections/" + electionId)
        .contentType(MediaType.APPLICATION_JSON).content(new Gson().toJson(stateChange))
        .with(oidcLogin().oidcUser(authorityPrincipal))).andReturn();
    assertTrue(result.getResponse().getStatus() < 400);
    while (Instant.now().isBefore(end)) {
    }
    int count = 1;
    for (ExternalTrustee trustee : trustees) {
      uploadDecryption(count++, trustee, electionId);
    }
    result = mockMvc.perform(get("/api/elections").param("electionId", String.valueOf(electionId))
        .with(oidcLogin().oidcUser(authorityPrincipal))).andReturn();
    assertTrue(result.getResponse().getStatus() < 400);
    JsonObject election = new Gson().fromJson(result.getResponse().getContentAsString(),
        JsonObject.class);
    assertEquals("FINISHED", election.get("state").getAsString());
  }

  /**
   * Decrypting with k trustees
   *
   * @param path
   * @throws Exception
   */
  @ParameterizedTest
  @ValueSource(strings = {"threshold1.json", "threshold2.json"})
  @Disabled
  void testPartialPartialDecryption(String path) throws Exception {
    JsonObject config = readFile(INPUT_PATH + path);
    List<ExternalTrustee> trustees = new ArrayList<>();
    for (JsonElement email : config.get("trustees").getAsJsonArray()) {
      trustees.add(new ExternalTrustee(email.getAsString()));
    }
    Instant end = Instant.now().plus(Duration.ofSeconds(electionDuration))
        .truncatedTo(ChronoUnit.SECONDS);
    JsonObject meta = config.getAsJsonObject("electionMeta").getAsJsonObject();
    meta.addProperty("end", end.toString());
    config.add("electionMeta", meta);
    long electionId = setupElectionExternal(config);
    auxKeysExternals(trustees, electionId);
    epkbExternals(electionId, trustees);
    System.out.println(Duration.between(Instant.now(), end));
    JsonObject stateChange = new JsonObject();
    stateChange.addProperty("state", "OPEN");
    MvcResult result = mockMvc.perform(patch("/api/authority/elections/" + electionId)
        .contentType(MediaType.APPLICATION_JSON).content(new Gson().toJson(stateChange))
        .with(oidcLogin().oidcUser(authorityPrincipal))).andReturn();
    assertTrue(result.getResponse().getStatus() < 400);
    while (Instant.now().isBefore(end)) {
    }
    for (int i = 0;
        i < config.get("electionMeta").getAsJsonObject().get("threshold").getAsInt() - 1; i++) {
      uploadDecryption(i + 1, trustees.get(i), electionId);
    }
    stateChange = new JsonObject();
    stateChange.addProperty("state", "PP_DECRYPTION");
    System.out.println(stateChange);
    result = mockMvc.perform(patch("/api/authority/elections/" + electionId)
        .contentType(MediaType.APPLICATION_JSON).content(new Gson().toJson(stateChange))
        .with(oidcLogin().oidcUser(authorityPrincipal))).andReturn();
    assertTrue(result.getResponse().getStatus() < 400);
    int threshold = config.get("electionMeta").getAsJsonObject().get("threshold").getAsInt();
    for (int i = 0; i < threshold - 1; i++) {
      uploadPPDecryption(i + trustees.size() - threshold + 2, trustees.get(i), electionId);
    }
    result = mockMvc.perform(get("/api/elections").param("electionId", String.valueOf(electionId))
        .with(oidcLogin().oidcUser(authorityPrincipal))).andReturn();
    System.out.println(
        result.getResponse().getContentAsString() + "" + result.getResponse().getStatus());
    assertTrue(result.getResponse().getStatus() < 400);
    JsonObject election = new Gson().fromJson(result.getResponse().getContentAsString(),
        JsonObject.class);
    assertEquals("FINISHED", election.get("state").getAsString());
  }

  @ParameterizedTest
  @ValueSource(strings = "threshold1.json")
  void testPartialPartialDecryptionGreaterThreshold(String path) throws Exception {
    JsonObject config = readFile(INPUT_PATH + path);
    List<ExternalTrustee> trustees = new ArrayList<>();
    for (JsonElement email : config.get("trustees").getAsJsonArray()) {
      trustees.add(new ExternalTrustee(email.getAsString()));
    }
    Instant end = Instant.now().plus(Duration.ofSeconds(electionDuration))
        .truncatedTo(ChronoUnit.SECONDS);
    JsonObject meta = config.getAsJsonObject("electionMeta").getAsJsonObject();
    meta.addProperty("end", end.toString());
    config.add("electionMeta", meta);
    long electionId = setupElectionExternal(config);
    auxKeysExternals(trustees, electionId);
    epkbExternals(electionId, trustees);
    System.out.println(Duration.between(Instant.now(), end));
    JsonObject stateChange = new JsonObject();
    stateChange.addProperty("state", "OPEN");
    MvcResult result = mockMvc.perform(patch("/api/authority/elections/" + electionId)
        .contentType(MediaType.APPLICATION_JSON).content(new Gson().toJson(stateChange))
        .with(oidcLogin().oidcUser(authorityPrincipal))).andReturn();
    assertTrue(result.getResponse().getStatus() < 400);
    while (Instant.now().isBefore(end)) {
    }
    for (int i = 0; i < trustees.size() - 1; i++) {
      uploadDecryption(i + 1, trustees.get(i), electionId);
    }
    stateChange = new JsonObject();
    stateChange.addProperty("state", "PP_DECRYPTION");
    System.out.println(stateChange);
    result = mockMvc.perform(patch("/api/authority/elections/" + electionId)
        .contentType(MediaType.APPLICATION_JSON).content(new Gson().toJson(stateChange))
        .with(oidcLogin().oidcUser(authorityPrincipal))).andReturn();
    assertTrue(result.getResponse().getStatus() < 400);
    for (int i = 0; i < trustees.size() - 1; i++) {
      uploadPPDecryption(i + 2, trustees.get(i), electionId);
    }
    result = mockMvc.perform(get("/api/elections").param("electionId", String.valueOf(electionId))
        .with(oidcLogin().oidcUser(authorityPrincipal))).andReturn();
    System.out.println(
        result.getResponse().getContentAsString() + "" + result.getResponse().getStatus());
    assertTrue(result.getResponse().getStatus() < 400);
    JsonObject election = new Gson().fromJson(result.getResponse().getContentAsString(),
        JsonObject.class);
    assertEquals("FINISHED", election.get("state").getAsString());
    result = mockMvc.perform(get("/api/elections/" + electionId + "/election-record/electionguard")
            .with(oidcLogin().oidcUser(authorityPrincipal))).andReturn();
    assertTrue(result.getResponse().getStatus() < 400);
  }

  @ParameterizedTest
  @ValueSource(strings = "onlyBot.json")
  void testBotOnlyElection(String path) throws Exception {
    JsonObject config = readFile(INPUT_PATH + path);
    Instant end = Instant.now().plus(Duration.ofSeconds(electionDuration))
        .truncatedTo(ChronoUnit.SECONDS);
    JsonObject meta = config.getAsJsonObject("electionMeta").getAsJsonObject();
    meta.addProperty("end", end.toString());
    config.add("electionMeta", meta);
    MvcResult result = mockMvc.perform(post("/api/authority/elections/create").
        contentType(MediaType.APPLICATION_JSON).content(new Gson().toJson(config)).
        with(oidcLogin().oidcUser(authorityPrincipal))).andReturn();
    System.out.println(result.getResponse().getContentAsString());
    assertTrue(result.getResponse().getStatus() < 400);
    JsonObject response = new Gson().fromJson(result.getResponse().getContentAsString(),
        JsonObject.class);
    assertEquals(response.get("state").getAsString(), "KEY_CEREMONY");
    long electionId = response.get("electionId").getAsLong();

    System.out.println(Duration.between(Instant.now(), end));
    JsonObject stateChange = new JsonObject();
    stateChange.addProperty("state", "OPEN");
    result = mockMvc.perform(patch("/api/authority/elections/" + electionId)
        .contentType(MediaType.APPLICATION_JSON).content(new Gson().toJson(stateChange))
        .with(oidcLogin().oidcUser(authorityPrincipal))).andReturn();
    assertTrue(result.getResponse().getStatus() < 400);
    while (Instant.now().isBefore(end)) {
    }

    result = mockMvc.perform(get("/api/elections").param("electionId", String.valueOf(electionId))
        .with(oidcLogin().oidcUser(authorityPrincipal))).andReturn();
    System.out.println(
        result.getResponse().getContentAsString() + "" + result.getResponse().getStatus());
    assertTrue(result.getResponse().getStatus() < 400);
    JsonObject election = new Gson().fromJson(result.getResponse().getContentAsString(),
        JsonObject.class);
    assertEquals("FINISHED", election.get("state").getAsString());
  }

  String addAuxKey(ExternalTrustee trustee) {
    AuxKeyHandler handler = new AuxKeyHandler();
    handler.initialize(new JsonObject[0], new String[0]);
    JsonObject[] keys = handler.execute();
    trustee.auxPrivate = keys[0];
    return new Gson().toJson(keys[1]);
  }

  String addEPKB(ExternalTrustee trustee, JsonObject auxKeys, boolean invalidKey) {
    if (invalidKey) {
      String ownKey = trustee.auxPrivate.get("publicKey").getAsString();
      for (String trusteeId : auxKeys.get("auxKeys").getAsJsonObject().keySet()) {
        JsonObject keyObj = auxKeys.get("auxKeys").getAsJsonObject().get(trusteeId)
            .getAsJsonObject();
        keyObj.addProperty("publicKey", ownKey);
      }
    }
    ElectionKeyHandler handler = new ElectionKeyHandler();
    handler.initialize(new JsonObject[]{auxKeys}, new String[0]);
    JsonObject[] keys = handler.execute();
    trustee.ceremonyPrivate = keys[0];
    return new Gson().toJson(keys[1]);
  }

  String decrypt(ExternalTrustee trustee, JsonObject encryption) {
    DecryptionHandler handler = new DecryptionHandler();
    handler.initialize(new JsonObject[]{encryption, trustee.ceremonyPrivate}, new String[0]);
    JsonObject[] decryption = handler.execute();
    return new Gson().toJson(decryption[0]);
  }


  String ppdecrypt(ExternalTrustee trustee, JsonObject encryption) {
    CompensationHandler handler = new CompensationHandler();
    handler.initialize(new JsonObject[]{encryption, trustee.auxPrivate}, new String[0]);
    JsonObject[] decryption = handler.execute();
    return new Gson().toJson(decryption[0]);
  }

  private class ExternalTrustee {

    UserPrincipal mockedPrincipal;
    String email;
    JsonObject auxPrivate;
    JsonObject ceremonyPrivate;
    ExternalTrustee(String email) {
      this.email = email;
      mockedPrincipal = mock(UserPrincipal.class);
      when(mockedPrincipal.getName()).thenReturn(email);
      when(mockedPrincipal.getEmail()).thenReturn(email);
    }
  }

}
