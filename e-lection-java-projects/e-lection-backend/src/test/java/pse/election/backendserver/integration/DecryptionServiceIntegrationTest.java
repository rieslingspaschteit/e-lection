package pse.election.backendserver.integration;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import pse.election.backendserver.security.user.UserPrincipal;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.time.Duration;
import java.time.Instant;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest(args = {"--oidcClients=./fakeOIDC.toml",
        "--authorityConfig=./fakeAuthorities.txt"}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class DecryptionServiceIntegrationTest {

  @Autowired
  private WebApplicationContext context;

  @Mock
  private UserPrincipal authorityPrincipal;
  @Mock
  private UserPrincipal trusteeOnePrincipal;
  @Mock
  private UserPrincipal trusteeTwoPrincipal;
  @Mock
  private UserPrincipal trusteeThreePrincipal;
  @Mock
  private UserPrincipal voterOnePrincipal;
  @Mock
  private UserPrincipal voterTwoPrincipal;

  private MockMvc mockMvc;
  private MvcResult result;
  private JsonObject response;
  private String electionId;
  private final int electionDuration = 10;

  @BeforeEach
  public void setUp() throws Exception {
    when(authorityPrincipal.getEmail()).thenReturn("no@valid.email.com");
    when(authorityPrincipal.getName()).thenReturn("no@valid.email.com");
    when(voterOnePrincipal.getEmail()).thenReturn("voter1@example.com");
    when(voterOnePrincipal.getName()).thenReturn("voter1@example.com");
    when(voterTwoPrincipal.getEmail()).thenReturn("voter2@example.com");
    when(voterTwoPrincipal.getName()).thenReturn("voter2@example.com");
    when(trusteeOnePrincipal.getEmail()).thenReturn("ujxik@student.kit.edu");
    when(trusteeOnePrincipal.getName()).thenReturn("ujxik@student.kit.edu");
    when(trusteeTwoPrincipal.getEmail()).thenReturn("penciu@online.de");
    when(trusteeTwoPrincipal.getName()).thenReturn("penciu@online.de");
    when(trusteeThreePrincipal.getEmail()).thenReturn("maximu31415926@gmail.com");
    when(trusteeThreePrincipal.getName()).thenReturn("maximu31415926@gmail.com");

    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

    // Election creation
    JsonObject electionJson = readFile("./TestingSet2/election.json");
    electionJson.remove("start");
    electionJson.remove("end");
    String startTime = Instant.now().toString();
    String endTime = Instant.now().plus(Duration.ofSeconds(electionDuration)).toString();
    electionJson.addProperty("start", startTime);
    electionJson.addProperty("end", endTime);
    result = mockMvc.perform(post("/api/authority/elections/create")
              .contentType(MediaType.APPLICATION_JSON)
              .content(new Gson().toJson(readFile("./TestingSet2/election.json")))
              .with(oidcLogin().oidcUser(authorityPrincipal)))
              .andReturn();
    response = new Gson().fromJson(result.getResponse().getContentAsString(), JsonObject.class);
    electionId = response.get("electionId").getAsString();

    // Key Ceremony Phase One
    mockMvc.perform(post("/api/trustee/elections/" + electionId + "/auxkeys")
            .contentType(MediaType.APPLICATION_JSON)
            .content(new Gson().toJson(readFile("./TestingSet2/trustee1/aux_public.json")))
            .with(oidcLogin().oidcUser(trusteeOnePrincipal)));
    mockMvc.perform(post("/api/trustee/elections/" + electionId + "/auxkeys")
            .contentType(MediaType.APPLICATION_JSON)
            .content(new Gson().toJson(readFile("./TestingSet2/trustee1/aux_public.json")))
            .with(oidcLogin().oidcUser(trusteeTwoPrincipal)));
    mockMvc.perform(post("/api/trustee/elections/" + electionId + "/auxkeys")
            .contentType(MediaType.APPLICATION_JSON)
            .content(new Gson().toJson(readFile("./TestingSet2/trustee1/aux_public.json")))
            .with(oidcLogin().oidcUser(trusteeThreePrincipal)));

    // Key Ceremony Phase Two
    mockMvc.perform(post("/api/trustee/elections/" + electionId + "/keys-and-backups")
            .contentType(MediaType.APPLICATION_JSON)
            .content(new Gson().toJson(readFile("./TestingSet2/trustee1/ceremony_public.json")))
            .with(oidcLogin().oidcUser(trusteeOnePrincipal)));
    mockMvc.perform(post("/api/trustee/elections/" + electionId + "/keys-and-backups")
            .contentType(MediaType.APPLICATION_JSON)
            .content(new Gson().toJson(readFile("./TestingSet2/trustee2/ceremony_public.json")))
            .with(oidcLogin().oidcUser(trusteeTwoPrincipal)));
    mockMvc.perform(post("/api/trustee/elections/" + electionId + "/keys-and-backups")
            .contentType(MediaType.APPLICATION_JSON)
            .content(new Gson().toJson(readFile("./TestingSet2/trustee3/ceremony_public.json")))
            .with(oidcLogin().oidcUser(trusteeThreePrincipal)));

    // Ballot import
    JsonObject stateChange = new JsonObject();
    stateChange.addProperty("state", "OPEN");
    mockMvc.perform(patch("/api/authority/elections/" + electionId)
            .contentType(MediaType.APPLICATION_JSON).content(new Gson().toJson(stateChange))
            .with(oidcLogin().oidcUser(authorityPrincipal)));

    mockMvc.perform(post("/api/voter/" + electionId + "/")
            .contentType(MediaType.APPLICATION_JSON)
            .content(new Gson().toJson(readFile("./TestingSet2/ballots/ballot1.json")))
            .with(oidcLogin().oidcUser(voterOnePrincipal)));
    mockMvc.perform(post("/api/voter/" + electionId + "/")
            .contentType(MediaType.APPLICATION_JSON)
            .content(new Gson().toJson(readFile("./TestingSet2/ballots/ballot2.json")))
            .with(oidcLogin().oidcUser(voterTwoPrincipal)));

  }

  @Test
  @Disabled
  public void addDecryptionPhaseOne_Test() throws Exception {
    Instant end = Instant.now().plus(Duration.ofSeconds(electionDuration));
    while (Instant.now().isBefore(end)) { }

    // TODO Does not work because somehow the election is still not in P_DECRYPTION
    result = mockMvc.perform(post("/api/trustee/elections/" + electionId + "/result")
              .contentType(MediaType.APPLICATION_JSON)
              .content(new Gson().toJson(readFile("./TestingSet2/trustee1/partial_decryption.json")))
              .with(oidcLogin().oidcUser(trusteeOnePrincipal)))
              .andReturn();

    response = new Gson().fromJson(result.getResponse().getContentAsString(), JsonObject.class);
    System.out.println(response.get("state").getAsString());
    assertTrue(response.get("state").getAsString().equals("P_DECRYPTION"));

  }

  private JsonObject readFile(String path) {
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

}
