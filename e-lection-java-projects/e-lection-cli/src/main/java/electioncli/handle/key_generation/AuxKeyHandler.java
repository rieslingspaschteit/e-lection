package electioncli.handle.key_generation;

import com.google.gson.JsonObject;
import electioncli.core.Commands;
import electioncli.core.Identifiers;
import electioncli.handle.CommandHandler;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

@SuppressWarnings("checkstyle:MissingJavadocType")
public class AuxKeyHandler extends CommandHandler {
    @SuppressWarnings("checkstyle:Indentation")
    private static final Commands COMMAND = Commands.AUXKEY;
    @SuppressWarnings("checkstyle:Indentation")
    private static final String[] REQUIRED_FILE_NAMES = new String[0];
    @SuppressWarnings("checkstyle:Indentation")
    private static final String[] TARGET_FILE_NAMES = new String[]{"aux_private", "aux_public"};
    @SuppressWarnings("checkstyle:Indentation")
    private static final String EXIT_MESSAGE = "";
    @SuppressWarnings("checkstyle:Indentation")
    private static final String RSA = "RSA";
    @SuppressWarnings("checkstyle:Indentation")
    private static final String INVALID_ENCRYPTION = "Error, requested encryption is not supported";

    @SuppressWarnings("checkstyle:Indentation")
    private String encryptionType;

    @SuppressWarnings("checkstyle:Indentation")
    @Override
    public void initialize(JsonObject[] input, String[] args) {
        encryptionType = null;
        for (String arg : args) {
            System.out.println(arg);
            if (arg.matches(Commands.OPTION_ENCRYPTION.getRegex())) {
                encryptionType = arg.split(Commands.COMMAND_SEPARATOR.getRegex())[1].toUpperCase();
            }
        }
        if (encryptionType == null) {
            encryptionType = RSA;
        }
    }

    @SuppressWarnings("checkstyle:Indentation")
    @Override
    public JsonObject[] execute() {
        JsonObject publicKey = new JsonObject();
        JsonObject privateKey = new JsonObject();
        publicKey.addProperty(Identifiers.ENCRYPTION_TYPE.getName(), encryptionType);
        privateKey.addProperty(Identifiers.ENCRYPTION_TYPE.getName(), encryptionType);
        KeyPairGenerator generator;
        try {
            generator = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(INVALID_ENCRYPTION);
        }
        KeyPair keys = generator.generateKeyPair();
        publicKey.addProperty(Identifiers.PUBLIC_KEY.getName(), electioncli.utils.RSA.keyToString(keys.getPublic()));
        privateKey.addProperty(Identifiers.PRIVATE_KEY.getName(), electioncli.utils.RSA.keyToString(keys.getPrivate()));
        privateKey.addProperty(Identifiers.PUBLIC_KEY.getName(), electioncli.utils.RSA.keyToString(keys.getPublic()));
        JsonObject[] out = {privateKey, publicKey};
        return out;
    }

    @SuppressWarnings("checkstyle:Indentation")
    @Override
    public boolean matchesCommand(String command) {
        return command.matches(this.COMMAND.getRegex());
    }

    @SuppressWarnings("checkstyle:Indentation")
    @Override
    public String[] getRequiredFiles() {
        return REQUIRED_FILE_NAMES;
    }

    @SuppressWarnings("checkstyle:Indentation")
    @Override
    public String[] getTargetFiles() {
        return TARGET_FILE_NAMES;
    }

    @SuppressWarnings("checkstyle:Indentation")
    @Override
    public String message() {
        return EXIT_MESSAGE;
    }
}
