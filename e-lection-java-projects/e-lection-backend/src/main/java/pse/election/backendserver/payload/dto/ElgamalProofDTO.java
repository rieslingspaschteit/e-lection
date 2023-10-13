package pse.election.backendserver.payload.dto;

import java.util.Map;

/**
 * This class is the elgamal proof dto.
 * */
public record ElgamalProofDTO(SchnorrProofDTO[] proofs, Map<Integer, String> backups) {

}
