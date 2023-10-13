package pse.election.backendserver.payload.response.record;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.sunya.electionguard.AvailableGuardian;
import com.sunya.electionguard.CiphertextTally;
import com.sunya.electionguard.ElectionConstants;
import com.sunya.electionguard.ElectionCryptoContext;
import com.sunya.electionguard.GuardianRecord;
import com.sunya.electionguard.Manifest;
import com.sunya.electionguard.PlaintextTally;
import com.sunya.electionguard.SubmittedBallot;
import com.sunya.electionguard.json.CiphertextTallyPojo;
import com.sunya.electionguard.json.ElectionConstantsPojo;
import com.sunya.electionguard.json.ElectionContextPojo;
import com.sunya.electionguard.json.GuardianRecordPojo;
import com.sunya.electionguard.json.LagrangeCoefficientsPojo;
import com.sunya.electionguard.json.ManifestPojo;
import com.sunya.electionguard.json.PlaintextTallyPojo;
import com.sunya.electionguard.json.SubmittedBallotPojo;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.stereotype.Component;
import pse.election.backendserver.entity.Election;

/**
 * This class is used to build an election guard verifiable election record.
 *
 * @version 1.0
 */
@Component
public class ElectionGuardRecord {

  private static final String DIR = "records";

  private static final String PREFIX = "record";

  private static final String POSTFIX = ".zip";

  private static final String ENDING = ".json";

  private static final String MANIFEST_PATH = "manifest";

  private static final String CONTEXT_PATH = "context";

  private static final String CONSTANTS_PATH = "constants";

  private static final String SUBMITTED_BALLOT_PATH = "submitted_ballots/";

  private static final String ENCRYPTED_TALLY_PATH = "encrypted_tally";

  private static final String TALLY_PATH = "tally";

  private static final String GUARDIAN_PATH = "guardians/";

  private static final String SPOILED_BALLOT_PATH = "spoiled_ballots/";

  private static final String COEFFICIENTS_PATH = "coefficients";

  private final ElectionGuardInitializedWrapper initializedWrapper;

  private final ElectionGuardManifestWrapper manifestWrapper;

  private final ElectionGuardBallotWrapper ballotWrapper;

  private final ElectionGuardTallyWrapper tallyWrapper;

  /**
   * Constructor of new ElectionGuardRecord.
   * */
  public ElectionGuardRecord(ElectionGuardInitializedWrapper initializedWrapper,
      ElectionGuardManifestWrapper manifestWrapper, ElectionGuardBallotWrapper ballotWrapper,
      ElectionGuardTallyWrapper tallyWrapper) {

    this.initializedWrapper = initializedWrapper;
    this.manifestWrapper = manifestWrapper;
    this.ballotWrapper = ballotWrapper;
    this.tallyWrapper = tallyWrapper;
  }

  /**
   * Assembles the full Election Record using the Wrapper classes in the package.
   *
   * @param election is the election
   * @return record
   * @throws IOException in case of failure
   */
  public ElectionRecord buildElectionRecord(Election election) throws IOException {
    String path = PREFIX + election.getElectionId() + POSTFIX;
    File file = new File(path);
    return createRecord(election, file);
  }

  /**
   * Generates the data structures required for the election record, parses them into json files and
   * writes them to zip compressed stream.
   *
   * @param election is the election
   * @param file is the file
   * @return record
   * @throws IOException in case of failure
   */
  @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
  private ElectionRecord createRecord(Election election, File file) throws IOException {
    Manifest manifest = manifestWrapper.generateElectionGuardManifest(election.getElectionId());
    ElectionCryptoContext context = initializedWrapper.generateCryptoContext(
        election.getElectionId());
    List<SubmittedBallot> ballots = ballotWrapper.generateAllSubmittedBallots(
        election.getElectionId());
    CiphertextTally tally = tallyWrapper.generateCiphertextTally(election.getElectionId());
    PlaintextTally decryptedTally = tallyWrapper.generatePlaintextTally(election.getElectionId());
    List<PlaintextTally> spoiled = tallyWrapper.generateAllSpoiledBallots(election.getElectionId());
    List<GuardianRecord> guardianRecords = initializedWrapper.generateGuardianRecords(
        election.getElectionId());
    List<AvailableGuardian> availableGuardians = tallyWrapper.generateAvailableGuardians(
        election.getElectionId());

    FileOutputStream stream2 = new FileOutputStream(file);
    ZipOutputStream zippedStream = new ZipOutputStream(stream2);

    addZipEntry(zippedStream, ManifestPojo.serialize(manifest), MANIFEST_PATH);
    addZipEntry(zippedStream, ElectionContextPojo.serialize(context), CONTEXT_PATH);
    addZipEntry(zippedStream, ElectionConstantsPojo.serialize(ElectionConstants.STANDARD_CONSTANTS),
        CONSTANTS_PATH);

    for (SubmittedBallot ballot : ballots) {
      addZipEntry(zippedStream, SubmittedBallotPojo.serialize(ballot),
          SUBMITTED_BALLOT_PATH + ballot.ballotId);
    }

    addZipEntry(zippedStream, CiphertextTallyPojo.serialize(tally), ENCRYPTED_TALLY_PATH);
    addZipEntry(zippedStream, PlaintextTallyPojo.serialize(decryptedTally), TALLY_PATH);

    if (guardianRecords != null) {
      for (GuardianRecord guardianRecord : guardianRecords) {
        addZipEntry(zippedStream, GuardianRecordPojo.serialize(guardianRecord),
            GUARDIAN_PATH + guardianRecord.guardianId());
      }
    }

    if (spoiled != null) {
      for (PlaintextTally ballot : spoiled) {
        addZipEntry(zippedStream, PlaintextTallyPojo.serialize(ballot),
            SPOILED_BALLOT_PATH + ballot.tallyId);
      }
    }

    if (availableGuardians != null) {
      LagrangeCoefficientsPojo pojo = new LagrangeCoefficientsPojo(
          StreamSupport.stream(availableGuardians.spliterator(), false)
              .collect(Collectors.toMap(AvailableGuardian::guardianId,
                  AvailableGuardian::lagrangeCoefficient)));
      addZipEntry(zippedStream, LagrangeCoefficientsPojo.serialize(pojo), COEFFICIENTS_PATH);
    }

    zippedStream.close();
    zippedStream.flush();
    return new ElectionRecord(zippedStream, file);
  }

  private void addZipEntry(ZipOutputStream zippedStream, JsonElement object, String path)
      throws IOException {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    String prettyJsonString = gson.toJson(object);
    ZipEntry entry = new ZipEntry(path + ENDING);
    zippedStream.putNextEntry(entry);
    zippedStream.write(prettyJsonString.getBytes(StandardCharsets.UTF_8));
    zippedStream.closeEntry();
  }

}
