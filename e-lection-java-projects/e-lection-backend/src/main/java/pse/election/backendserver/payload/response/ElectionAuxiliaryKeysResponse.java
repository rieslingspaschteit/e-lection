package pse.election.backendserver.payload.response;

import java.util.Map;

/**
 * This class is the election auxiliary keys response.
 * */
public record ElectionAuxiliaryKeysResponse(Map<Integer, AuxiliaryKey> auxKeys, int threshold) {

  /**
   * This class wrappes the auxiliary key.
   * */
  public record AuxiliaryKey(String publicKey, String keyType) {

  }
}
