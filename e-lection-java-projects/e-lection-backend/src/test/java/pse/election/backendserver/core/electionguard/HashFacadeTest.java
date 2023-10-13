package pse.election.backendserver.core.electionguard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sunya.electionguard.ElectionCryptoContext;
import com.sunya.electionguard.Group;
import com.sunya.electionguard.Hash;
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import pse.election.backendserver.core.service.ElectionService;
import pse.election.backendserver.core.service.TrusteeService;
import pse.election.backendserver.core.service.VoterService;
import pse.election.backendserver.entity.Ballot;
import pse.election.backendserver.entity.Contest;
import pse.election.backendserver.entity.Election;
import pse.election.backendserver.entity.OptionEncrypted;
import pse.election.backendserver.entity.Trustee;
import pse.election.backendserver.payload.dto.ElectionDescriptionHashesDTO;
import pse.election.backendserver.payload.response.record.ElectionGuardInitializedWrapper;
import pse.election.backendserver.payload.response.record.ElectionGuardManifestWrapper;

@ExtendWith(MockitoExtension.class)
class HashFacadeTest {

  static final String OPTION_ENCRYPTED_PAD = "013E131D6567DCAD3D4833B28182F42843312AB7573CEEC140F78F7FE4BA160BA7ADB4B33EC6941C453359DD5F86F116B869470F6E75CC50F5027BBFDF03F9DD670E53EFA4B1E62BCC01AA5EA6CD044AC64626272DE5F96E4D029C054D04D83EED3DF4B54CF97C26ECADF75F33F9990B2E003A1BABF3CFEE941BE1A5C5574A7F158E047B1368C5C0C405D370530C1B256ED25567BDDF4821F90D62D9D23972954D158A63D489D573CC1F27BA588D0FAB6DE5EB5C3A85A5CB30907A6CCD465ED0D5DD649E83865955108243179778DEB6487E2DC6407C23C2A0D75F32A32A7299DE2C2B964D9F98406C9245C1C2A85D04EE2977F6AE42A1BAD6F5980C0DD461C36B6EE1F75A52BB4A7D48ADE6C5B6CE8560559248D0AEF051CE47919BA0A18113280D6F2E62E0D1EF36C2CC9A99FE0743E95D02D5C5BB5AC03F8DD9D635D66797DAE177246B4C0D21AE2D833BD740850FFA02581CF667A7C37D7A2016B78467D902A4938B027C615265593FC51C7747D1A9AAABF0FC6B7136968414420C71C84475D1FB50B452DB9CCE8190E120DD4B5E51F88142D250E2D738DCA56FDBC591D08BE7391E7C61174C30D4471ABF3C857D7468BFE31C6C1758E9698B58028F27C3B2042C9D4E0A4103CED34A25AED5FF71E188E036571FEB051A118A0D99C5514CC1CAD3B9E18FF4084B5CEA902428B8717CE8A500EB839CED8DF31403034178EC";
  static final String OPTION_ENCRYPTED_DATA = "09BC589669CFD9BCC52F0F655056F4CCED952A02D6E4B80F3A8CD29140F0CA29405BC461C1DD949E146B32D2676370362C7A91CD20921C5F095B0045E613A33C017BD297C275283F22B8598AF99F7050054306DE8DAC7C2A93ED970505F83E1FE19179D1204A9B51B6AB3A60828D5544373628A21E5B9ECD6D6045296DDEB38DF37C4CA97DE2B15D717CB6B13ECA8C04378BC471BF4BB464244C455B68A02406B8CB9F825EDE30549438137CC3C65688A6B6DD3B19D3D745902971AC606E2DE6156A7853695F104FB6F92B560BCD3E62AFEAD498ED2AE73789602CAB79581A3376D85F84AFC084CB971182ADA9D1B9FE08E50606E3A5339AC3A6416CB492836DA0DE52AE75794B0E5DA3E33D1E0E02B96847168A83076AE4D2BAF58706A65C9FECDBC3A5C20532DD031D82DC32DEA958DCE5851C21034421E76479A0AA9CA11DCBE0049844A5E45D330AA81F46F7D499F3D113AFFCDC60A51131E451B5296FA8F0D480AC424F9E0A96B1D5451491C0D311FA595345EE9C5F463322572DE0B480A031D0CF8E4E277CFFDFA12C88CF684DB13DBBD02907FEC4B743040C1F2A0F4641F46CC70387C639A67EAF89311D25D13C7B7510A9DF085CED5EDF709039004BD0D8EF70F076784135F89125B69DC9FA513995DCC9445AA967F30A19595D4172CFD5335AA06168087FFD1708AC32844DC86140B0089DB6E7B7FCB3EAA75EEDD2";
  private static final String INPUT_PATH = "./src/test/resources";
  @Mock
  TrusteeService trusteeService;
  @Mock
  VoterService voterService;
  List<OptionEncrypted> optionEncryptedToBallot;
  Ballot ballot;
  @Mock
  Election election;
  @InjectMocks
  ElectionGuardManifestWrapper manifestWrapper;
  ElectionDescriptionHashesDTO electionDescriptionHashesDTO;
  @Mock
  private ElectionService mockElectionService;
  @Mock
  private ElectionGuardManifestWrapper mockElectionGuardManifestWrapper;
  @Mock
  private ElectionGuardInitializedWrapper electionGuardInitializedWrapper;
  @Spy
  @InjectMocks
  private HashFacade hashFacadeUnderTest;

  private static JsonElement readDecryptionData(String path) {
    File inputFile = new File(path);
    JsonElement data;
    try {
      Reader reader = new FileReader(inputFile);
      data = JsonParser.parseReader(reader);
      reader.close();
    } catch (IOException e) {
      throw new IllegalArgumentException(e.getMessage());
    }
    return data;
  }

  /**
   * Random Generation for all Information necessary to generate a TrackingCode
   */

  void setUpTrackingCode() {

    String cryptoHash = "crpytoHash";

    ballot = new Ballot(0L, "0", "PC", new Date(), true);

    Map<Integer, String[]> optionDescriptionHashes = new HashMap<>();
    String[] optionString = new String[1];
    optionString[0] = Hash.hash_elems(0, "value").toString();
    optionDescriptionHashes.put(0, optionString);

    Map<Integer, String> contestHahses = new HashMap<>();
    contestHahses.put(0, Hash.hash_elems("Wer wird Cheef").toString());

    Map<Integer, String[]> optionIds = new HashMap<>();
    String[] option = new String[1];
    optionString[0] = Hash.hash_elems(0, "0").toString();
    optionIds.put(0, option);

    Map<Integer, String> contestIds = new HashMap<>();
    contestIds.put(0, Hash.hash_elems("0").toString());

    String manifestHash = Hash.hash_elems("manifest").toString();
    //TODO fix this test
    String voterHash = "ABC";
    String commitments = "123";
    electionDescriptionHashesDTO =
        new ElectionDescriptionHashesDTO(optionDescriptionHashes, contestHahses, optionIds,
            contestIds, manifestHash,
            commitments, voterHash);

    List<OptionEncrypted> optionEncryptedList = new ArrayList<>();
    optionEncryptedList.add(
        new OptionEncrypted(0L, 0L, 0, 0, OPTION_ENCRYPTED_PAD, OPTION_ENCRYPTED_DATA, "", ""));

    doReturn(electionDescriptionHashesDTO).when(hashFacadeUnderTest)
        .generateElectionHashes(election);
    doReturn(cryptoHash).when(hashFacadeUnderTest)
        .generateCryptoHash(electionDescriptionHashesDTO, optionEncryptedToBallot, election,
            ballot);

    Map<String, String> input = new HashMap<>();

    when(electionGuardInitializedWrapper.generateCryptoContext(0L)).thenReturn(
        new ElectionCryptoContext(1, 2, Group.int_to_p_unchecked(new BigInteger("123")),
            Group.int_to_q_unchecked(new BigInteger("123")),
            Group.int_to_q_unchecked(new BigInteger("123")),
            Group.int_to_q_unchecked(new BigInteger("123")),
            Group.int_to_q_unchecked(new BigInteger("123")), input));
  }

  /**
   * Checks if the generation of Hashes is deterministic.
   */
  @Test
  void trackingCodeDeterministic() {
    setUpTrackingCode();

    hashFacadeUnderTest.generateTrackingCode(optionEncryptedToBallot, election, ballot);
    String firstTrackingCode = ballot.getLatestTrackingCode();

    // TrackingCode of SecondRun
    hashFacadeUnderTest.generateTrackingCode(optionEncryptedToBallot, election, ballot);
    String secondTrackingCode = ballot.getLatestTrackingCode();

    Assertions.assertEquals(firstTrackingCode, secondTrackingCode);
  }

  void setupFingerprint(JsonObject manifest) {
    JsonObject meta = manifest.get("electionMeta").getAsJsonObject();
    when(election.getTitle()).thenReturn(meta.get("title").getAsString());
    when(election.getAuthorityEmail()).thenReturn(meta.get("authority").getAsString());
    Date start = Date.from(Instant.parse(meta.get("start").getAsString()));
    Date end = Date.from(Instant.parse(meta.get("end").getAsString()));
    when(election.getStartTime()).thenReturn(start);
    when(election.getEndTime()).thenReturn(end);
    when(election.getElectionId()).thenReturn(0L);
    List<Trustee> allTrustees = new ArrayList<>();
    for (JsonElement trustee : manifest.get("trustees").getAsJsonArray()) {
      allTrustees.add(new Trustee(0L, trustee.getAsString(), 0));
    }
        /*
        List<Voter> allVoters = new ArrayList<>();
        for (JsonElement voter :  manifest.get("voters").getAsJsonArray()) {
            allVoters.add(new Voter(0L, voter.getAsString()));
        }
        */
    when(trusteeService.getAllTrustees(0L)).thenReturn(allTrustees);
    when(election.hasBot()).thenReturn(manifest.get("isBotEnabled").getAsBoolean());
    when(election.getTrusteeThreshold()).thenReturn(meta.get("threshold").getAsInt());
    when(election.getPublicKey()).thenReturn(new BigInteger(meta.get("key").getAsString(), 16));
    List<Contest> allContests = readAllContests(manifest.get("questions").getAsJsonObject());
    when(mockElectionService.getAllContestsOfElection(0L)).thenReturn(allContests);
  }

  @Test
  void testElectionFingerprint() {
    String path = INPUT_PATH + "/manifest.json";
    JsonArray manifests = readDecryptionData(path).getAsJsonArray();
    //ElectionDescriptionHashesDTO dto = parseHashesDTO(manifest.getAsJsonObject().get("hashes").getAsJsonObject());
    for (JsonElement manifest : manifests) {
      HashFacade partiallyMockedFacade = mock(HashFacade.class);
      ReflectionTestUtils.setField(partiallyMockedFacade, "electionService", mockElectionService);
      ReflectionTestUtils.setField(partiallyMockedFacade, "trusteeService", trusteeService);
      when(partiallyMockedFacade.generateElectionFingerprint(election)).thenCallRealMethod();
      setupFingerprint(manifest.getAsJsonObject());
      ElectionDescriptionHashesDTO dto = parseHashesDTO(
          manifest.getAsJsonObject().get("hashes").getAsJsonObject());
      when(partiallyMockedFacade.generateElectionHashes(election)).thenReturn(dto);
      String fingerprint = manifest.getAsJsonObject().get("fingerprint").getAsString();
      assertEquals(fingerprint, partiallyMockedFacade.generateElectionFingerprint(election));
    }
  }

  private ElectionDescriptionHashesDTO parseHashesDTO(JsonObject hashes) {
    Map<Integer, String> contestHashes = new HashMap<>();
    Map<Integer, String> contestIds = new HashMap<>();
    Map<Integer, String[]> selectionHashes = new HashMap<>();
    Map<Integer, String[]> selectionIds = new HashMap<>();
    for (String questionIndex : hashes.get("contestHashes").getAsJsonObject().keySet()) {
      String contestHash = hashes.get("contestHashes").getAsJsonObject().get(questionIndex)
          .getAsString();
      String contestId = hashes.get("contestIds").getAsJsonObject().get(questionIndex)
          .getAsString();
      JsonArray selectionHash = hashes.get("selectionHashes").getAsJsonObject().get(questionIndex)
          .getAsJsonArray();
      JsonArray selectionId = hashes.get("selectionIds").getAsJsonObject().get(questionIndex)
          .getAsJsonArray();
      contestHashes.put(Integer.valueOf(questionIndex), contestHash);
      contestIds.put(Integer.valueOf(questionIndex), contestId);
      List<String> optionHashes = new ArrayList<>();
      List<String> optionIds = new ArrayList<>();
      for (JsonElement hash : selectionHash) {
        optionHashes.add(hash.getAsString());
      }
      for (JsonElement id : selectionId) {
        optionIds.add(id.getAsString());
      }
      selectionHashes.put(Integer.valueOf(questionIndex), optionHashes.toArray(new String[0]));
      selectionIds.put(Integer.valueOf(questionIndex), optionIds.toArray(new String[0]));
    }
    return new ElectionDescriptionHashesDTO(selectionHashes, contestHashes, selectionIds,
        contestIds,
        hashes.get("manifestHash").getAsString(), hashes.get("commitments").getAsString(),
        hashes.get("voterHash").getAsString());
  }

  private List<Contest> readAllContests(JsonObject questions) {
    List<Contest> allContests = new ArrayList<>();
    for (String questionIndex : questions.keySet()) {
      JsonObject question = questions.get(questionIndex).getAsJsonObject();
      List<String> options = new ArrayList<>();
      for (JsonElement option : question.get("options").getAsJsonArray()) {
        options.add(option.getAsString());
      }
      allContests.add(new Contest(0L, question.get("questionText").getAsString(),
          question.get("max").getAsInt(),
          Integer.valueOf(questionIndex), options));
    }
    return allContests;
  }
}
