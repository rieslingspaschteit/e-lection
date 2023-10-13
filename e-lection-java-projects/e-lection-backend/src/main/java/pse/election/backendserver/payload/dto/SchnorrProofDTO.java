package pse.election.backendserver.payload.dto;

import java.math.BigInteger;

/**
 * This class is the schnorr proof dto.
 * */
public record SchnorrProofDTO(BigInteger publicKey,
                              BigInteger commitment,
                              BigInteger challenge,
                              BigInteger response) {

}
