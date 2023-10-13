package pse.election.backendserver.payload.response;

import java.util.Map;

/**
 * This class is the election hashes response.
 * */
public record ElectionHashesResponse(String manifestHash, Map<Integer, String> contestHashes,
                                     Map<Integer, String[]> selectionHashes,
                                     Map<Integer, String[]> selectionIds,
                                     Map<Integer, String> contestIds,
                                     String commitments, String voterHash) {

}
