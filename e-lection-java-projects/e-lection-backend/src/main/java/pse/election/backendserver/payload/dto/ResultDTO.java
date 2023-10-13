package pse.election.backendserver.payload.dto;

import java.util.Map;

/**
 * This class is the result dto.
 * */
public record ResultDTO(Map<Integer, Integer[]> resultElection,
                        Map<Long, Map<Integer, Integer[]>> resultSpoiledBallot) {

}
