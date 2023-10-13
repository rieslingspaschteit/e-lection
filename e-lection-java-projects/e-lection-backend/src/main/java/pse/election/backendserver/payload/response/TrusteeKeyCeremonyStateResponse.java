package pse.election.backendserver.payload.response;

/**
 * This class is the key ceremony state response for a trustee.
 * */
public record TrusteeKeyCeremonyStateResponse(String state, boolean waiting) {

}
