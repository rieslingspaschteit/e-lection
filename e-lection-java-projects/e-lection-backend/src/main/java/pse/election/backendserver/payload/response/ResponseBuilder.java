package pse.election.backendserver.payload.response;

import com.sunya.electionguard.ElectionCryptoContext;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pse.election.backendserver.core.electionguard.HashFacade;
import pse.election.backendserver.core.service.AuthorityService;
import pse.election.backendserver.core.service.BallotService;
import pse.election.backendserver.core.service.ElectionService;
import pse.election.backendserver.core.service.TallyService;
import pse.election.backendserver.core.service.TrusteeService;
import pse.election.backendserver.core.service.VoterService;
import pse.election.backendserver.core.state.ElectionState;
import pse.election.backendserver.core.state.handler.ElectionStateHandler;
import pse.election.backendserver.entity.Ballot;
import pse.election.backendserver.entity.Contest;
import pse.election.backendserver.entity.Election;
import pse.election.backendserver.entity.OptionEncrypted;
import pse.election.backendserver.entity.Tally;
import pse.election.backendserver.entity.Trustee;
import pse.election.backendserver.payload.dto.ElectionDescriptionHashesDTO;
import pse.election.backendserver.payload.response.record.ElectionGuardInitializedWrapper;
import pse.election.backendserver.payload.response.record.ElectionGuardRecord;
import pse.election.backendserver.payload.response.record.ElectionRecord;
import pse.election.backendserver.payload.response.state.DecryptionStateFormatter;
import pse.election.backendserver.payload.response.state.DefaultStateFormatter;
import pse.election.backendserver.payload.response.state.KeyCeremonyStateFormatter;
import pse.election.backendserver.payload.response.state.StateFormatter;
import pse.election.backendserver.security.user.UserRole;

/**
 * This class is responsible for building the responses.
 * */
@Component
public class ResponseBuilder {

  private static final int BIGINTEGER_RADIX = 16;

  private StateFormatter stateFormatter;
  @Autowired
  private ElectionService electionService;
  @Autowired
  private TrusteeService trusteeService;

  @Autowired
  private BallotService ballotService;

  @Autowired
  private ElectionGuardInitializedWrapper electionGuardInitializedWrapper;

  @Autowired
  private HashFacade hashFacade;

  @Autowired
  private AuthorityService authorityService;
  @Autowired
  private VoterService voterService;
  @Autowired
  private TallyService tallyService;

  @Autowired
  private ElectionGuardRecord electionGuardRecord;
  @Autowired
  private ElectionStateHandler electionStateHandler;

  /**
   * Building the authority decryption state response.
   * */
  public AuthorityDecryptionStateResponse buildAuthorityDecryptionStateResponse(long electionId) {
    this.stateFormatter = new DecryptionStateFormatter();

    if (electionService.getState(electionId) == ElectionState.OPEN) {
      //checking if the election end date has been reached
      electionStateHandler.testAndSet(electionService.getElection(electionId),
          ElectionState.P_DECRYPTION);
    }

    return new AuthorityDecryptionStateResponse(
        this.trusteeService.getTrusteesWaitingCount(electionId),
        stateFormatter.formatElectionState(this.electionService.getState(electionId))
    );
  }

  /**
   * Building the authority key ceremony state response.
   * */
  public AuthorityKeyCeremonyStateResponse buildAuthorityKeyCeremonyStateResponse(long electionId) {
    this.stateFormatter = new KeyCeremonyStateFormatter();
    return new AuthorityKeyCeremonyStateResponse(
        this.trusteeService.getTrusteesWaitingCount(electionId),
        stateFormatter.formatElectionState(this.electionService.getState(electionId))
    );
  }

  /**
   * Building the election ballot response.
   * */
  public BallotResponse buildElectionBallotResponse(long electionId) {

    List<Contest> contestList = electionService.getAllContestsOfElection(electionId);
    Map<Integer, BallotResponse.Question> questions = new HashMap<>();

    for (int i = 0; i < contestList.size(); i++) {
      Contest contest = contestList.get(i);
      BallotResponse.Question question = new BallotResponse.Question(
          contest.getName(),
          contest.getOptions(),
          contest.getMax()
      );
      questions.put(i, question);
    }
    return new BallotResponse(questions);
  }

  /**
   * Building the ballot sent response.
   * */
  public BallotSentResponse buildBallotSentResponse(long ballotId) {
    Ballot sentBallot = ballotService.getBallot(ballotId);
    return new BallotSentResponse(
        sentBallot.getLatestTrackingCode(),
        sentBallot.getPreviousTrackingCode()
    );
  }

  /**
   * Building the election auxiliary keys response.
   * */
  public ElectionAuxiliaryKeysResponse buildElectionAuxiliaryKeysResponse(long electionId,
      String email) {
    Map<Integer, ElectionAuxiliaryKeysResponse.AuxiliaryKey> auxKeys = new HashMap<>();
    List<Trustee> trusteeList = trusteeService.getAllTrustees(electionId);
    int threshold = electionService.getElection(electionId).getTrusteeThreshold();
    for (Trustee trustee : trusteeList) {

      if (trustee.getEmail().equals(email)) {
        continue;
      }

      ElectionAuxiliaryKeysResponse.AuxiliaryKey auxiliaryKey = new ElectionAuxiliaryKeysResponse.AuxiliaryKey(
          trustee.getAuxkey(),
          trustee.getAuxkeyType()
      );
      auxKeys.put(trustee.getIndex(), auxiliaryKey);
    }

    return new ElectionAuxiliaryKeysResponse(auxKeys, threshold);
  }

  /**
   * Building the election ballot board response.
   * */
  public ElectionBallotBoardResponse buildElectionBallotBoardResponse(long electionId) {
    List<Ballot> submittedBallots = ballotService.getAllSubmittedBallotsOfElection(electionId);
    return new ElectionBallotBoardResponse(
        submittedBallots.stream()
            .map(Ballot::getLatestTrackingCode)
            .toList()
    );
  }

  /**
   * Building the election guard record.
   * */
  public ElectionRecord buildElectionGuardRecord(long electionId) throws IOException {
    return electionGuardRecord.buildElectionRecord(this.electionService.getElection(electionId));
  }

  /**
   * Builoding the election creation response.
   * */
  public ElectionCreationResponse buildElectionCreationResponse(long electionId) {
    this.stateFormatter = new DefaultStateFormatter();
    Election election = electionService.getElection(electionId);
    return new ElectionCreationResponse(election.getElectionId(),
        stateFormatter.formatElectionState(election.getState()));
  }

  /**
   * Builds a response for the decrypted result of the election without dummy options.
   *
   * @param electionId id of the election
   */
  public ElectionDecryptedResultResponse buildElectionDecryptedResultResponse(long electionId) {
    Map<Integer, Integer[]> resultWithDummies = electionService.getDecryptedResult(electionId);
    Map<Integer, Integer[]> resultWithoutDummies = new HashMap<>();
    for (Contest contest : electionService.getAllContestsOfElection(electionId)) {
      resultWithoutDummies.put(contest.getIndex(), new Integer[contest.getOptions().size()]);
      for (int i = 0; i < contest.getOptions().size(); i++) {
        resultWithoutDummies.get(contest.getIndex())[i] = resultWithDummies.get(
            contest.getIndex())[i];
      }
    }
    return new ElectionDecryptedResultResponse(resultWithoutDummies);
  }

  /**
   * Building the election encrypted result response.
   * */
  public ElectionEncryptedResultResponse buildElectionEncryptedResultResponse(long electionId,
      String trusteeEmail) {

    if (electionService.getState(electionId) == ElectionState.OPEN) {
      //checking if the election has finished
      electionService.tryUpdateState(electionId, ElectionState.P_DECRYPTION);
    }

    Map<Long, Map<Integer, List<EncryptedOptionResponse>>> encryptedSpoiledBallotQuestions =
            createEncryptedSpoiledBallotResponse(electionId);
    Map<Integer, List<EncryptedOptionResponse>> encryptedTally = createEncryptedTallyResponse(
        electionId);
    String extendedBaseHash = this.electionGuardInitializedWrapper.generateCryptoContext(
        electionId).cryptoExtendedBaseHash.base16();

    if (electionService.getState(electionId) == ElectionState.PP_DECRYPTION) {
      return new ElectionEncryptedResultResponse(
          encryptedSpoiledBallotQuestions,
          encryptedTally,
          trusteeService.getBackups(trusteeEmail, electionId, true),
          extendedBaseHash
      );
    }

    return new ElectionEncryptedResultResponse(
        encryptedSpoiledBallotQuestions,
        encryptedTally,
        extendedBaseHash
    );
  }

  private Map<Integer, List<EncryptedOptionResponse>> createEncryptedTallyResponse(
      long electionId) {
    Map<Integer, List<EncryptedOptionResponse>> encryptedTally = new LinkedHashMap<>();

    //iterate threw all tallied ballots
    for (Tally tally : tallyService.getAllTalliesOfElection(electionId)) {
      encryptedTally.computeIfAbsent(tally.getContestIndex(), arg -> new ArrayList<>());
      encryptedTally.get(tally.getContestIndex()).add(
          new EncryptedOptionResponse(tally.getCiphertextPAD().toString(BIGINTEGER_RADIX),
              tally.getCiphertextDATA().toString(BIGINTEGER_RADIX))
      );
    }
    return encryptedTally;
  }

  private Map<Long, Map<Integer, List<EncryptedOptionResponse>>> createEncryptedSpoiledBallotResponse(
      long electionId) {
    Map<Long, Map<Integer, List<EncryptedOptionResponse>>> encryptedSpoiledBallotQuestions = new LinkedHashMap<>();

    //iterating threw all spoiled ballots
    for (Ballot ballot : this.ballotService.getAllSpoiledBallotsOfElection(electionId)) {
      Map<Integer, List<EncryptedOptionResponse>> encryptedOptions = new LinkedHashMap<>();

      List<OptionEncrypted> allOptionsEncryptedOfBallot = ballotService.getAllOptionsEncryptedOfBallot(
          ballot.getBallotId());

      //per ballot, store to every question his encrypted options
      for (OptionEncrypted optionEncrypted : allOptionsEncryptedOfBallot) {
        encryptedOptions.computeIfAbsent(optionEncrypted.getContestIndex(),
            contestId -> new ArrayList<>());
        encryptedOptions.get(optionEncrypted.getContestIndex())
            .add(new EncryptedOptionResponse(
                optionEncrypted.getCiphertextPAD().toString(BIGINTEGER_RADIX),
                optionEncrypted.getCiphertextDATA().toString(BIGINTEGER_RADIX))
            );
      }
      encryptedSpoiledBallotQuestions.put(ballot.getBallotId(), encryptedOptions);
    }

    return encryptedSpoiledBallotQuestions;
  }

  /**
   * Building the election meta response.
   * */
  public ElectionMetaResponse buildElectionMetaResponse(long electionId) {
    Election election = electionService.getElection(electionId);
    ElectionMetaResponse.ElectionMeta electionMeta = createElectionMetaBody(election);
    this.stateFormatter = new DefaultStateFormatter();

    if (election.getState() == ElectionState.OPEN) {
      electionStateHandler.testAndSet(election, ElectionState.P_DECRYPTION);
    }

    return new ElectionMetaResponse(
        election.getElectionId(),
        electionMeta,
        this.stateFormatter.formatElectionState(election.getState()),
        election.getFingerprint()
    );
  }

  @SuppressWarnings("checkstyle:EmptyCatchBlock")
  private ElectionMetaResponse.ElectionMeta createElectionMetaBody(Election election) {

    ElectionCryptoContext electionCryptoContext = null;

    try {
      electionCryptoContext = this.electionGuardInitializedWrapper.generateCryptoContext(
          election.getElectionId());
    } catch (Exception ignored) {

    }

    if (electionCryptoContext == null) {
      return new ElectionMetaResponse.ElectionMeta(
          election.getTitle(),
          election.getDescription(),
          election.getStartTime() != null ? election.getStartTime().toString() : null,
          election.getEndTime().toString(),
          election.getAuthorityEmail(),
          election.getTrusteeThreshold(),
          null,
          election.getFingerprint()
      );
    }

    return new ElectionMetaResponse.ElectionMeta(
        election.getTitle(),
        election.getDescription(),
        election.getStartTime() != null ? election.getStartTime().toString() : null,
        election.getEndTime().toString(),
        election.getAuthorityEmail(),
        election.getTrusteeThreshold(),
        election.getPublicKey().toString(BIGINTEGER_RADIX),
        election.getFingerprint()
    );

  }

  /**
   * Building multiple election meta responses.
   * */
  public Collection<ElectionMetaResponse> buildMultipleElectionMetaResponse(
      List<Long> electionIds) {
    this.stateFormatter = new DefaultStateFormatter();
    List<ElectionMetaResponse> electionMetaResponseList = new ArrayList<>();

    for (Long electionId : electionIds) {
      Election election = electionService.getElection(electionId);
      ElectionMetaResponse.ElectionMeta electionMeta = createElectionMetaBody(election);

      electionMetaResponseList.add(new ElectionMetaResponse(
          election.getElectionId(),
          electionMeta,
          this.stateFormatter.formatElectionState(election.getState()),
          election.getFingerprint())
      );
    }

    return electionMetaResponseList;
  }

  /**
   * Building the voter information response.
   * */
  public ElectionsVoterResponse buildElectionVoterInformationResponse(long electionId) {
    return new ElectionsVoterResponse(
        hashFacade.generateVoterHash(voterService.getAllVoters(electionId))
    );
  }

  /**
   * Building the election trustee response.
   * */
  public ElectionTrusteeResponse buildElectionTrusteeResponse(long electionId) {
    List<Trustee> trusteeList = trusteeService.getAllTrustees(electionId);
    return new ElectionTrusteeResponse(
        trusteeList.stream().map(Trustee::getEmail).toList(),
        this.electionService.hasBot(electionId)
    );
  }

  /**
   * Building the elgamal keys and backups response.
   * */
  public ElgamalKeysAndBackupsResponse buildElgamalKeysAndBackupsResponse(long electionId,
      String trusteeEmail) {
    Trustee desiredTrustee = trusteeService.getTrustee(trusteeEmail, electionId);
    return new ElgamalKeysAndBackupsResponse(
            this.trusteeService.getBackups(trusteeEmail, electionId, false),
            this.trusteeService.getElgamalKeys(trusteeEmail, electionId),
            desiredTrustee.getIndex(),
            desiredTrustee.getAuxkeyType()
    );
  }

  public EmptyResponse buildEmptyResponse() {
    return new EmptyResponse();
  }

  /**
   * Building the Trustee Decryption State response.
   * */
  public TrusteeDecryptionStateResponse buildTrusteeDecryptionStateResponse(long electionId,
      String trusteeEmail) {
    this.stateFormatter = new DecryptionStateFormatter();

    return new TrusteeDecryptionStateResponse(
        this.stateFormatter.formatElectionState(this.electionService.getState(electionId)),
        this.trusteeService.getTrustee(trusteeEmail, electionId).isWaiting()
    );
  }

  /**
   * Building the trustee key ceremony state response.
   * */
  public TrusteeKeyCeremonyStateResponse buildTrusteeKeyCeremonyStateResponse(long electionId,
      String trusteeEmail) {
    this.stateFormatter = new KeyCeremonyStateFormatter();
    return new TrusteeKeyCeremonyStateResponse(
        this.stateFormatter.formatElectionState(this.electionService.getState(electionId)),
        this.trusteeService.getTrustee(trusteeEmail, electionId).isWaiting()
    );
  }

  /**
   * Building the user information response.
   * */
  public UserInformationResponse buildUserInformationResponse(String email) {
    List<UserRole> roles = new ArrayList<>();

    if (authorityService.existsByEmail(email)) {
      roles.add(UserRole.AUTHORITY);
    }
    if (trusteeService.existsByEmail(email)) {
      roles.add(UserRole.TRUSTEE);
    }
    if (voterService.existsByEmail(email)) {
      roles.add(UserRole.VOTER);
    }
    return new UserInformationResponse(roles, email);
  }

  /**
   * Building the voter information response.
   * */
  public VoterInformationResponse buildVoterInformationResponse(long electionId, String email) {
    return new VoterInformationResponse(
        !this.voterService.getVoter(email, electionId).isHasVoted()
    );
  }

  /**
   * Building the election hashes response.
   * */
  public ElectionHashesResponse buildElectionHashesResponse(long electionId) {
    ElectionDescriptionHashesDTO hashesDTO = hashFacade.generateElectionHashes(
        electionService.getElection(electionId));
    return new ElectionHashesResponse(
        hashesDTO.manifestHash(),
        hashesDTO.contestDescriptionHashes(),
        hashesDTO.optionDescriptionHashes(),
        hashesDTO.optionIds(),
        hashesDTO.contestIds(),
        hashesDTO.commitments(),
        hashesDTO.voterHash()
    );
  }

}
