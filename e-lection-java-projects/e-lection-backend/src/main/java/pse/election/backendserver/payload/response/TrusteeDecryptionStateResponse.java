package pse.election.backendserver.payload.response;

/**
 * This class is the decryption state response.
 * */
public record TrusteeDecryptionStateResponse(String state, boolean waiting) {

}
