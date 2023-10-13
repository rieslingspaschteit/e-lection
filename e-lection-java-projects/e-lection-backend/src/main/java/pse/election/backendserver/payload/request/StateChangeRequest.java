package pse.election.backendserver.payload.request;

/**
 * This class is the election state change request.
 * */
public class StateChangeRequest {

  private String state;

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }
}
