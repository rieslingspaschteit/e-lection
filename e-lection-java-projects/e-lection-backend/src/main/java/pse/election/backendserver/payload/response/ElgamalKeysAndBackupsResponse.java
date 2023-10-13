package pse.election.backendserver.payload.response;

import java.math.BigInteger;
import java.util.Map;

/**
 * This class is the elgamal keys and backups response.
 * */
public record ElgamalKeysAndBackupsResponse(Map<Integer, String> backups, Map<Integer, String[]> publicKeys,
                                            int id, String auxKeyType) {

}
