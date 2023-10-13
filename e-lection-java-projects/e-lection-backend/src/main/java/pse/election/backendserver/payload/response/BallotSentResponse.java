package pse.election.backendserver.payload.response;

/**
 * This class is the ballot sent response.
 * */
public record BallotSentResponse(String trackingCode, String lastTrackingCode) {

}
