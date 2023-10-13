package pse.election.backendserver.payload.dto;

import java.util.Date;
import java.util.Map;

/**
 * This class is the ballot proof dto.
 * */
public record BallotProofDTO(Map<Integer, EncryptedOptionDTO[]> cipherText,
                             Map<Integer, DisjunctiveChaumPedersenDTO[]> individualProofs,
                             Map<Integer, ConstantChaumPedersenDTO> accumulatedProofs,
                             String deviceInformation,
                             Date date) {

}
