package pse.election.backendserver.payload.dto;

import java.math.BigInteger;

/**
 * This is the chaum pederson dto.
 * */
public record ChaumPedersenProofDTO(BigInteger pad,
                                    BigInteger data,
                                    BigInteger challenge,
                                    BigInteger response) {

}
