package pse.election.backendserver.payload.dto;

/**
 * This class is the chaum pederson proof dto.
 * */
public record ConstantChaumPedersenDTO(ChaumPedersenProofDTO pedersenProofDTO, int constant) {

}
