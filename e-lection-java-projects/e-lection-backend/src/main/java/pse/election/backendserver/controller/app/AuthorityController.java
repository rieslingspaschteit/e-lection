package pse.election.backendserver.controller.app;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import pse.election.backendserver.controller.ElectionAuthorityAPI;
import pse.election.backendserver.core.bot.BotFacade;
import pse.election.backendserver.core.electionguard.DecryptionFacade;
import pse.election.backendserver.core.service.ElectionService;
import pse.election.backendserver.core.service.TrusteeService;
import pse.election.backendserver.core.service.VoterService;
import pse.election.backendserver.core.state.ElectionState;
import pse.election.backendserver.entity.Contest;
import pse.election.backendserver.entity.Election;
import pse.election.backendserver.entity.Trustee;
import pse.election.backendserver.entity.Voter;
import pse.election.backendserver.payload.error.exception.InvalidConfigurationException;
import pse.election.backendserver.payload.request.ElectionCreationRequest;
import pse.election.backendserver.payload.response.AuthorityDecryptionStateResponse;
import pse.election.backendserver.payload.response.AuthorityKeyCeremonyStateResponse;
import pse.election.backendserver.payload.response.ElectionCreationResponse;
import pse.election.backendserver.payload.response.EmptyResponse;
import pse.election.backendserver.payload.response.ResponseBuilder;
import pse.election.backendserver.payload.response.state.StateParser;

/**
 * This class is used as an api controller and handles authority functionalities. It implements the
 * {@link ElectionAuthorityAPI} that is controlled by the {@link FrontController}. This includes
 * election creation and key ceremony state switches.
 *
 * @version 1.0
 */
@Component
@Qualifier("AuthorityController")
public class AuthorityController implements ElectionAuthorityAPI {

  private static final String NO_TRUSTEES = "Election must have at least one trustee";
  private static final String INVALID_THRESHOLD = "Threshold must not exceed number of trustees";
  private static final String NO_VOTERS = "Election must have at least one voter";
  private static final String INVALID_CONTEST
      = "Election must have at least one contest and one option for each contest";
  private static final String EMPTY_TEXT
      = "Election, contests and options must all have non-empty titles";
  private static final String INVALID_MAX
      = "Maximum number of selections must be at least 1 and must not exceed the total"
      + " number of selections for each contest";
  private static final String VOTER_LIMIT
      = "Number of voters must not exceed " + DecryptionFacade.MAX_ACCUMULATION;
  private static final String INVALID_END_DATE
      = "End date of election must be after election creation";
  private static final String INVALID_EMAIL
      = "A provided email address did not match the required format";

  private static final String EMAIL_REGEX
      = "^[a-zA-Z0-9]+(?:\\.[a-zA-Z0-9]+)*@[a-zA-Z0-9]+(?:\\.[a-zA-Z0-9]+)*$";
  private static final String STATE_NOT_FOUND_MESSAGE
      = "The requested state could not be found.";
  private static final int STARTING_TRUSTEE_INDEX = 1;
  private static final int STARTING_CONTEST_INDEX = 0;

  private final ElectionService electionService;

  private final TrusteeService trusteeService;

  private final VoterService voterService;

  private final ResponseBuilder responseBuilder;

  private final BotFacade botFacade;

  /**
   * Constructor of new AuthorityController.
   *
   * @param electionService is the election service
   * @param botFacade is the bot facade
   * @param responseBuilder is the response builder
   * @param trusteeService is the trustee service
   * @param voterService is the voter service
   * */
  public AuthorityController(ElectionService electionService, TrusteeService trusteeService,
      VoterService voterService, ResponseBuilder responseBuilder, BotFacade botFacade) {
    this.electionService = electionService;
    this.trusteeService = trusteeService;
    this.voterService = voterService;
    this.responseBuilder = responseBuilder;
    this.botFacade = botFacade;
  }


  @Override
  public ElectionCreationResponse createElection(String authorityEmail,
      ElectionCreationRequest electionCreationRequest) {
    checkCreationRequest(electionCreationRequest, authorityEmail);
    Election createdElection = new Election(
        electionCreationRequest.getElectionMeta().getEnd(),
        electionCreationRequest.getElectionMeta().getTitle(),
        electionCreationRequest.getElectionMeta().getDescription(),
        authorityEmail,
        electionCreationRequest.hasBot(),
        electionCreationRequest.getElectionMeta().getThreshold()
    );

    Election savedElection = this.electionService.initialSave(createdElection);
    List<Trustee> trusteesOfElection = parseTrusteesFromRequest(
        electionCreationRequest.getTrustees(), savedElection.getElectionId());
    List<Voter> votersOfElection = parseVotersFromRequest(electionCreationRequest.getVoters(),
        savedElection.getElectionId());
    List<Contest> contestsOfElection = parseContestFromRequest(
        electionCreationRequest.getQuestions(), savedElection.getElectionId());

    this.trusteeService.addTrustees(trusteesOfElection);
    this.voterService.addVoters(votersOfElection);
    this.electionService.addContest(contestsOfElection);

    if (electionCreationRequest.hasBot()) {
      this.botFacade.createBot(savedElection.getElectionId(), trusteesOfElection.size());
      electionService.tryUpdateState(savedElection.getElectionId(), ElectionState.EPKB);
    }

    return this.responseBuilder.buildElectionCreationResponse(savedElection.getElectionId());
  }

  private void checkCreationRequest(ElectionCreationRequest electionCreationRequest,
      String authorityEmail) {
    if (electionCreationRequest.getElectionMeta().getTitle().length() == 0) {
      throw new InvalidConfigurationException(EMPTY_TEXT);
    } else if (electionCreationRequest.getTrustees().isEmpty()
        && !electionCreationRequest.hasBot()) {
      throw new InvalidConfigurationException(NO_TRUSTEES);
    } else if (electionCreationRequest.getVoters().isEmpty()) {
      throw new InvalidConfigurationException(NO_VOTERS);
    } else if (electionCreationRequest.getVoters().size() > DecryptionFacade.MAX_ACCUMULATION) {
      throw new InvalidConfigurationException(VOTER_LIMIT);
    } else if (electionCreationRequest.getElectionMeta().getThreshold() < 1) {
      throw new InvalidConfigurationException(INVALID_THRESHOLD);
    } else if (electionCreationRequest.getElectionMeta().getThreshold()
        > electionCreationRequest.getTrustees().size()
        + (electionCreationRequest.hasBot() ? 1 : 0)) {
      throw new InvalidConfigurationException(INVALID_THRESHOLD);
    } else if (electionCreationRequest.getElectionMeta().getEnd()
        .before(Date.from(Instant.now()))) {
      throw new InvalidConfigurationException(INVALID_END_DATE);
    } else if (electionCreationRequest.getQuestions().isEmpty()) {
      throw new InvalidConfigurationException(INVALID_CONTEST);
    }
    for (ElectionCreationRequest.Question question : electionCreationRequest.getQuestions()
        .values()) {
      if (question.getQuestionText().length() == 0 || question.getOptions().contains("")) {
        throw new InvalidConfigurationException(EMPTY_TEXT);
      } else if (question.getOptions().isEmpty()) {
        throw new InvalidConfigurationException(INVALID_CONTEST);
      } else if (question.getOptions().size() < question.getMaxSelections()
          || 1 > question.getMaxSelections()) {
        throw new InvalidConfigurationException(INVALID_MAX);
      }
    }
    if (!authorityEmail.matches(EMAIL_REGEX)) {
      throw new InvalidConfigurationException(INVALID_EMAIL + ": " + authorityEmail);
    }
    for (String voterEmail : electionCreationRequest.getVoters()) {
      if (!voterEmail.matches(EMAIL_REGEX)) {
        throw new InvalidConfigurationException(INVALID_EMAIL  + ": " + voterEmail);
      }
    }
    for (String trusteeEmail : electionCreationRequest.getTrustees()) {
      if (!trusteeEmail.matches(EMAIL_REGEX)) {
        throw new InvalidConfigurationException(INVALID_EMAIL + ": " + trusteeEmail);
      }
    }
  }

  @Override
  public EmptyResponse nextState(Long electionId, String state) throws IllegalArgumentException {
    ElectionState electionState;
    StateParser parser = new StateParser();
    if ((electionState = parser.parse(state)) == null) {
      throw new IllegalArgumentException(STATE_NOT_FOUND_MESSAGE);
    }

    electionService.tryUpdateState(electionId, electionState);
    return this.responseBuilder.buildEmptyResponse();
  }

  @Override
  public AuthorityKeyCeremonyStateResponse getElectionKeyCeremonyState(Long electionId) {
    return this.responseBuilder.buildAuthorityKeyCeremonyStateResponse(electionId);
  }

  @Override
  public AuthorityDecryptionStateResponse getElectionDecryptionState(Long electionId) {
    return this.responseBuilder.buildAuthorityDecryptionStateResponse(electionId);
  }

  private List<Contest> parseContestFromRequest(
      Map<String, ElectionCreationRequest.Question> questionMap, long electionId) {
    List<Contest> contestList = new ArrayList<>();
    int contestIndex = STARTING_CONTEST_INDEX;
    for (ElectionCreationRequest.Question question : questionMap.values()) {
      List<String> contestOptions = new ArrayList<>(question.getOptions());
      Contest contest = new Contest(
          electionId,
          question.getQuestionText(),
          question.getMaxSelections(),
          contestIndex++,
          contestOptions
      );
      contestList.add(contest);
    }

    return contestList;
  }

  private List<Voter> parseVotersFromRequest(Collection<String> voters, long electionId) {
    return voters.stream().map(email -> new Voter(electionId, email)).collect(Collectors.toList());
  }

  private List<Trustee> parseTrusteesFromRequest(Collection<String> trustees, long electionId) {
    List<Trustee> trusteeList = new ArrayList<>();
    int trusteeIndex = STARTING_TRUSTEE_INDEX;
    for (String email : trustees) {
      trusteeList.add(new Trustee(electionId, email, trusteeIndex));
      trusteeIndex++;
    }
    return trusteeList;
  }
}
