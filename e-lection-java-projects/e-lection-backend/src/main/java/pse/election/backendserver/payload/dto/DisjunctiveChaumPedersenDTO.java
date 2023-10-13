package pse.election.backendserver.payload.dto;

import java.math.BigInteger;

/**
 * This class is the disjunctive chaum pederson dto.
 * */
public record DisjunctiveChaumPedersenDTO(ChaumPedersenProofDTO proof0,
                                          ChaumPedersenProofDTO proof1,
                                          BigInteger challenge) {


}
