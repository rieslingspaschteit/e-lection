package electioncli.handle.key_generation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sunya.electionguard.Group;
import electioncli.core.Commands;
import electioncli.core.ICommand;
import electioncli.core.Identifiers;
import electioncli.handle.CommandHandler;
import electioncli.utils.AdditionalVerifiers;
import electioncli.utils.RSA;

import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BackupHandler extends CommandHandler {
    private static final ICommand COMMAND = Commands.BACKUP_CHECK;
    private static final String[] REQUIRED_FILE_NAMES = new String[] {"backup_keys", "aux_private"};
    private static final String[] TARGET_FILE_NAMES = new String[0];

    private static final String MISSING_INPUT = "Error, missing input files";

    private static final String INVALID_ENCRYPTION = "Error, requested encryption is not supported";
    private static final String INVALID_KEY = "Error, provided key could not be resolved";
    private static final String SUCCESS = "Backups are correct" + System.lineSeparator();
    private static final String FAILURE = "Backups are incorrect" + System.lineSeparator();

    private Map<Integer, List<Group.ElementModP>> publicKeys;
    private Map<Integer, String> backups;
    private int id;
    private PrivateKey key;
    private String func;
    private Map<Integer, Boolean> results;
    @Override
    public void initialize(JsonObject[] input, String[] args) {
        if (input.length < 2) {
            throw new IllegalArgumentException(MISSING_INPUT);
        }
        JsonObject rawBackups = input[0].get(Identifiers.BACKUP.getName()).getAsJsonObject();
        this.backups = new HashMap<>();
        for (String trustee : rawBackups.keySet()) {
            this.backups.put(Integer.valueOf(trustee), rawBackups.get(trustee).getAsString());
        }
        this.publicKeys = new HashMap<>();
        JsonObject rawKeys = input[0].get(Identifiers.PUBLIC_KEYS.getName()).getAsJsonObject();
        for (String trusteeId : rawKeys.keySet()) {
            List<Group.ElementModP> keys = new ArrayList<>();
            for (JsonElement rawKey : rawKeys.get(trusteeId).getAsJsonArray()) {
                keys.add(Group.hex_to_p_unchecked(rawKey.getAsString()));
            }
            this.publicKeys.put(Integer.valueOf(trusteeId), keys);
        }
        this.id = input[0].get(Identifiers.ID.getName()).getAsInt();
        this.key = RSA.stringToPrivateKey(input[1].get(Identifiers.PRIVATE_KEY.getName()).getAsString());
        this.func = input[1].get(Identifiers.ENCRYPTION_TYPE.getName()).getAsString();
    }

    @Override
    public JsonObject[] execute() {
        this.results = new HashMap<>();
        for (int trusteeId : publicKeys.keySet()) {
            String pointString;
            pointString = RSA.decode(key, backups.get(trusteeId), func);
            Group.ElementModQ point = Group.hex_to_q_unchecked(pointString);
            results.put(trusteeId, AdditionalVerifiers.verifyEPKB(publicKeys.get(trusteeId), point, id));
        }
        return new JsonObject[0];
    }

    @Override
    public boolean matchesCommand(String command) {
        return command.matches(COMMAND.getRegex());
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
        String message = FAILURE;
        boolean result = true;
        for (int trusteeId : results.keySet()) {
            if (!results.get(trusteeId)) {
                message += trusteeId + ",";
                result = false;
            }
        }
        if (result) {
            return SUCCESS;
        } else {
            return message;
        }
    }
}
