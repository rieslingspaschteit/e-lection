package pse.election.backendserver.payload.response;

import java.util.List;

/**
 * Is the election trustee response.
 * */
public record ElectionTrusteeResponse(List<String> trustees, boolean isBotEnabled) {

}
