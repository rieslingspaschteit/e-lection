package pse.election.backendserver.payload.dto;

import java.math.BigInteger;

/**
 * This class is the encrypted option dto.
 * */
public record EncryptedOptionDTO(BigInteger pad,
                                 BigInteger data) {

}
