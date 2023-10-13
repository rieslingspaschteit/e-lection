package pse.election.backendserver.payload.response;

/**
 * This class is a response to the authority decryption state request.
 * */
public record AuthorityDecryptionStateResponse(int decCount, String decState) {

}
