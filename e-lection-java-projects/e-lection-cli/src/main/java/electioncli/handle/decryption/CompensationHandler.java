package electioncli.handle.decryption;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sunya.electionguard.ElGamal;
import com.sunya.electionguard.Group;
import electioncli.core.Commands;
import electioncli.core.Identifiers;
import electioncli.utils.RSA;

import java.security.PrivateKey;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CompensationHandler extends DecryptionHandler {
    private static final Commands COMMAND = Commands.COMPENSATION;
    private static final String[] TARGET_FILE_NAMES = new String[] {"partial_partial_decryption"};
    private static final String[] REQUIRED_FILE_NAMES = new String[]{"encrypted_tallies", "aux_private"};
    private static final String EXIT_MESSAGE = "";

    protected Map<Integer, Group.ElementModQ> backupKeys = new HashMap<>();

    @Override
    public void initialize(JsonObject[] input, String[] args) {
        if (input.length < 2) {
            throw new IllegalArgumentException();
        }
        parseInput(input);
        JsonObject encryptedBackups = input[0].get(Identifiers.KEY_BACKUPS.getName()).getAsJsonObject();
        String function = input[1].get(Identifiers.ENCRYPTION_TYPE.getName()).getAsString();
        PrivateKey secretKey = electioncli.utils.RSA.stringToPrivateKey(input[1].get(Identifiers.PRIVATE_KEY.getName()).getAsString());
        for (String trusteeIndex : encryptedBackups.keySet()) {
            String backupKey = RSA.decode(secretKey, encryptedBackups.get(trusteeIndex).getAsString(), function);
            backupKeys.put(Integer.valueOf(trusteeIndex), Group.hex_to_q_unchecked(backupKey));
        }
    }

    @Override
    public JsonObject[] execute() {
        JsonObject allTallyDecryptions = new JsonObject();
        JsonObject allSpoiledDecryptions = new JsonObject();
        for (int trusteeId : backupKeys.keySet()) {
            int count = 0;
            JsonArray spoiledBallotDecryptions = new JsonArray();
            for (Map<Integer, List<ElGamal.Ciphertext>> spoiledBallot : spoiledBallots) {
                spoiledBallotDecryptions.add(decryptTally(spoiledBallot, backupKeys.get(trusteeId), count++));
            }
            allSpoiledDecryptions.add(String.valueOf(trusteeId), spoiledBallotDecryptions);
            allTallyDecryptions.add(String.valueOf(trusteeId), decryptTally(tally, backupKeys.get(trusteeId), null));
        }
        JsonObject decryption = new JsonObject();
        JsonObject allSpoiledBallots = new JsonObject();
        decryption.add(Identifiers.DECRYPTED_SPOILED_BALLOT.getName(), allSpoiledDecryptions);
        decryption.add(Identifiers.DECRYPTED_TALLY.getName(), allTallyDecryptions);
        JsonObject[] out = {decryption};
        return out;
    }

    @Override
    public boolean matchesCommand(String command) {
        return command.matches(this.COMMAND.getRegex());
    }

    @Override
    public String[] getRequiredFiles() {
        return REQUIRED_FILE_NAMES;
    }

    @Override
    public String[] getTargetFiles() {
        return TARGET_FILE_NAMES;
    }

    @Override
    public String message() {
        return EXIT_MESSAGE;
    }
}
