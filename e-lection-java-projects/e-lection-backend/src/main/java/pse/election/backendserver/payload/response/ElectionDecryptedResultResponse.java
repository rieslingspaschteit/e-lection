package pse.election.backendserver.payload.response;

import java.util.Map;

/**
 * This class is the decryption result response.
 * */
public record ElectionDecryptedResultResponse(Map<Integer, Integer[]> result) {

}
