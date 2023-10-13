package pse.election.backendserver.payload.dto;

import java.util.Map;

/**
 * This class is the description hashes dto.
 * */
public record ElectionDescriptionHashesDTO(Map<Integer, String[]> optionDescriptionHashes,
                                           Map<Integer, String> contestDescriptionHashes,
                                           Map<Integer, String[]> optionIds,
                                           Map<Integer, String> contestIds,
                                           String manifestHash, String commitments,
                                           String voterHash) {

}