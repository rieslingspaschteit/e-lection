package pse.election.backendserver.payload.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * This class is used for translation of an election creation request into the election entity. It
 * is used in the controller layer where the translation takes place.
 *
 * @version 1.0
 * @see pse.election.backendserver.entity.Election
 */
public class ElectionCreationRequest {

  @JsonProperty("electionMeta")
  @NotNull
  private ElectionCreationBody electionMeta;
  @JsonProperty("questions")
  @NotNull
  private Map<String, Question> questions;

  @JsonProperty("trustees")
  @NotNull
  private Collection<String> trustees;

  @JsonProperty("voters")
  @NotNull
  private Collection<String> voters;

  @JsonProperty("isBotEnabled")
  @NotNull
  private boolean bot;

  /**
   * Getter if the election has a system bot as a trustee.
   *
   * @return if the election has a bot
   */
  public boolean hasBot() {
    return bot;
  }

  /**
   * Setter if the election uses a system bot for the election.
   *
   * @param bot is true if a bot is used in the election
   */
  public void setBot(boolean bot) {
    this.bot = bot;
  }

  /**
   * Getter for the election metadata. Those have been translated from the request.
   *
   * @return election metadata translated from the request
   */
  public ElectionCreationBody getElectionMeta() {
    return electionMeta;
  }

  /**
   * Setter for the election metadata from the request. This contains information that is publicly
   * for every user that is assigned to the election.
   *
   * @param electionMeta is the data from the request
   */
  public void setElectionMeta(ElectionCreationBody electionMeta) {
    this.electionMeta = electionMeta;
  }

  public Map<String, Question> getQuestions() {
    return questions;
  }

  public void setQuestions(Map<String, Question> questions) {
    this.questions = questions;
  }


  /**
   * Getter for the emails of the trustees assigned to the requested election.
   *
   * @return collection of emails of the trustees that have been assigned to the election
   */
  public Collection<String> getTrustees() {
    return trustees;
  }

  /**
   * Setter for the trustees of the requested election. This is extracted from the request and only
   * contains the emails of the trustees.
   *
   * @param trustees is the collection of trustee emails
   */
  public void setTrustees(Collection<String> trustees) {
    this.trustees = trustees;
  }

  /**
   * Getter for the voters that have been assigned to the election. This only contains their
   * emails.
   *
   * @return collection of emails of the voters that have been assigned to the election
   */
  public Collection<String> getVoters() {
    return voters;
  }

  /**
   * Setter for the voters of the requested election. This is extracted from the request and only
   * contains the emails of the voters.
   *
   * @param voters is the collection of voter emails
   */
  public void setVoters(Collection<String> voters) {
    this.voters = voters;
  }

  /**
   * This class represents a Question in the request. Therefore, it is needed for translation of a
   * ballot.
   *
   * @version 1.0
   */
  public static class Question {

    @JsonProperty("questionText")
    private String questionText;
    @JsonProperty("options")
    private Collection<String> options;
    @JsonProperty("max")
    private int maxSelections;

    /**
     * Getter for the question text. This contains the question.
     *
     * @return text of the question
     */
    public String getQuestionText() {
      return questionText;
    }

    /**
     * Setter for the question text.
     *
     * @param questionText is the text of the question from the request
     */
    public void setQuestionText(String questionText) {
      this.questionText = questionText;
    }

    /**
     * Getter for the options of a question.
     *
     * @return collection of the options of a question
     */
    public Collection<String> getOptions() {
      return options;
    }

    /**
     * Setter for the options of a question.
     *
     * @param options are the options of a question
     */
    public void setOptions(Collection<String> options) {
      this.options = options;
    }

    public int getMaxSelections() {
      return maxSelections;
    }

    public void setMaxSelections(int maxSelections) {
      this.maxSelections = maxSelections;
    }

  }

  /**
   * This class contains the election meta information.
   * */
  public static class ElectionCreationBody {

    @JsonProperty("title")
    private String title;

    @JsonProperty("description")
    private String description;

    @JsonProperty("end")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Date end;

    @JsonProperty("threshold")
    private int threshold;

    public int getThreshold() {
      return threshold;
    }

    public void setThreshold(int threshold) {
      this.threshold = threshold;
    }

    public String getTitle() {
      return title;
    }

    public void setTitle(String title) {
      this.title = title;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public Date getEnd() {
      return end;
    }

    public void setEnd(Date end) {
      this.end = end;
    }
  }
}
