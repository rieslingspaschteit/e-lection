package pse.election.backendserver.payload.response;

import java.util.List;

/**
 * This class is the ballot board response.
 * */
public record ElectionBallotBoardResponse(List<String> trackingCodes) {

}
