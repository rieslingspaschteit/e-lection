package pse.election.backendserver.payload.dto;

import java.math.BigInteger;
import java.util.Map;

/**
 * This class is the decryption dto.
 * */
public record DecryptionDTO(Map<Integer, PartialDecryptionDTO[]> partialDecryptedSpoiledBallots,
                            Map<Integer, PartialDecryptionDTO> partialDecryptedTalliedBallots) {

  /**
   * Record for a partial decryption.
   *
   * @param partialDecryptedOptions partial decryptions
   * @param chaumPedersonProofs     proofs
   * @param ballotId                id of the ballot, will be -1 for decryption of tallied result
   */
  public record PartialDecryptionDTO(Map<Integer, BigInteger[]> partialDecryptedOptions,
                                     Map<Integer, ChaumPedersenProofDTO[]> chaumPedersonProofs,
                                     long ballotId) {

  }
}
