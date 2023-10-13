package pse.election.backendserver.payload.response;

/**
 * This class is the encrypted option response.
 * */
public record EncryptedOptionResponse(String pad, String data) {

}
