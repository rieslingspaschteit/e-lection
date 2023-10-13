package pse.election.backendserver.payload.dto;

/**
 * This class contains the open id connect provider information.
 * */
public record OidcProvider(
    String providerName,
    String authenticationUrl
) {

}
