package pse.election.backendserver.payload.response.record;

import com.sunya.electionguard.Manifest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pse.election.backendserver.core.service.ElectionService;
import pse.election.backendserver.entity.Contest;
import pse.election.backendserver.entity.Election;

/**
 * This class generates a election manifest compatible with an ElectionGuard verifier.
 */
@Component
public class ElectionGuardManifestWrapper {

  private static final Manifest.GeopoliticalUnit UNIT = new Manifest.GeopoliticalUnit("kit", "KIT",
      Manifest.ReportingUnitType.unknown, null);
  public static final Manifest.BallotStyle style = new Manifest.BallotStyle("0",
      List.of(UNIT.geopoliticalUnitId()), null, null);
  private static final String SPEC_VERSION = "v0.95";
  private static final String SEPARATOR = "-";
  private static final String SEPARATOR_2 = " ";
  private static final String DUMMY = "placeholder";
  @Autowired
  private ElectionService electionService;
  private List<Manifest.Candidate> candidates = new ArrayList<>();

  public ElectionGuardManifestWrapper() {
  }

  /**
   * Generates a full manifest for the election.
   *
   * @return the full manifest
   */
  public Manifest generateElectionGuardManifest(long electionId) {
    Election election = electionService.getElection(electionId);
    Iterable<Contest> contests = electionService.getAllContestsOfElection(electionId);
    List<Manifest.ContestDescription> contestDescriptions = new ArrayList<>();
    this.candidates.clear();
    for (Contest contest : contests) {
      contestDescriptions.add(generateContestDescription(contest));
    }
    //Inconsistency: start time can be either a Date or Timestamp. .toInstant() will return the same result in
    //either case, but .toString() wouldn't
    Instant startTime = election.getStartTime().toInstant();
    Instant endTime = election.getEndTime().toInstant();
    return new Manifest(
        String.valueOf(electionId),
        this.SPEC_VERSION,
        Manifest.ElectionType.general,
        startTime.toString(),
        endTime.toString(),
        List.of(UNIT),
        null,
        candidates,
        contestDescriptions,
        List.of(style),
        null,
        null,
        null
    );
  }

  /**
   * Generates a description for a contest of the election that is compatible with ElectionGuard.
   *
   * @param contest The required contest
   * @return contest description
   */
  public Manifest.ContestDescription generateContestDescription(Contest contest) {
    String contestId = String.valueOf(contest.getContestId());
    List<Manifest.SelectionDescription> selections = new ArrayList<>();
    for (int i = 0; i < contest.getOptions().size(); i++) {
      selections.add(generateSelctionDescription(contest, i));
    }
    return new Manifest.ContestDescription(
        contestId,
        contest.getIndex(),
        this.UNIT.geopoliticalUnitId(),
        Manifest.VoteVariationType.n_of_m,
        //electing max selections
        contest.getMax(),
        contest.getMax(),
        contest.getName(),
        selections,
        null,
        null,
        null,
        null
    );
  }

  /**
   * Generates a description for an option in the election by sequence order an title. If title is
   * null, it will be set as a placeholder option
   *
   * @param contestId     id of the contest the otion is for. It must be the id that will bew set in
   *                      the ElectionGuard manifest.
   * @param sequenceOrder sequence order of the option
   * @param title         titke of the option
   * @return slection description
   */
  public Manifest.SelectionDescription generateSelctionDescription(String contestId,
      int sequenceOrder, String title) {
    String finalTitle = title == null ? DUMMY : title;
    String selectionId =
        contestId + SEPARATOR + sequenceOrder + SEPARATOR + finalTitle.replaceAll(SEPARATOR_2,
            SEPARATOR);
    this.candidates.add(new Manifest.Candidate(selectionId));
    return new Manifest.SelectionDescription(selectionId, sequenceOrder, selectionId, null);
  }

  /**
   * Generates a description for an option in the election by sequence order and title.
   *
   * @param contest       A Contest object for the contest the option is for. It contains the title
   *                      of all its options
   * @param sequenceOrder the sequence order of the option. If it is greater than the number of
   *                      options for the contes, it will be marked as placeholder
   * @return selection description
   */
  public Manifest.SelectionDescription generateSelctionDescription(Contest contest,
      int sequenceOrder) {
    String title = DUMMY;
    if (sequenceOrder < contest.getOptions().size()) {
      title = contest.getOptions().get(sequenceOrder);
    }
    String selectionId =
        contest.getContestId() + SEPARATOR + sequenceOrder + SEPARATOR + title.replaceAll(
            SEPARATOR_2, SEPARATOR);
    this.candidates.add(new Manifest.Candidate(selectionId));
    return new Manifest.SelectionDescription(selectionId, sequenceOrder, selectionId, null);
  }

  /**
   * Returns the list of candidates of the election.
   *
   * @return list of candidates
   */
  public List<Manifest.Candidate> getCandidates() {
    return this.candidates;
  }

}
