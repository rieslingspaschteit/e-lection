package electioncli.core;

import java.util.List;

public enum Commands implements ICommand {
    /**
     * Separates an option descriptor from the value of the option
     * */
    COMMAND_SEPARATOR("=",
            "Separates an option descriptor from the value of the option"),
    /**
     * Defines the directory the source files are read from
     * The name of the required source files is defined in the corresponding handler
     */
    OPTION_SOURCE("sourcedir=.*",
            "Defines the directory the source files are read from" + System.lineSeparator()
            + "The name of the required source files is defined in the corresponding handler"),
    /**
     * Defines the directory the output files are written to
     */
    OPTION_TARGET("targetdir=.*",
            "Defines the directory the output files are written to"),
    /**
     * Defines the encryption algorithm for the auxiliary key
     * Right now, only RSA is supported
     */
    OPTION_ENCRYPTION("encryption=.*",
            "Defines the encryption algorithm for the auxiliary key" + System.lineSeparator() +
            "Right now, only RSA is supported" + System.lineSeparator() +
            "This option only works with the command \'aux\'"),
    /**
     * Generates the auxiliary key for the trustee
     */
    AUXKEY("aux",
            "Generates the auxiliary key for the trustee"),
    /**
     * Generates the election key and key backups for other trustees
     * Requires the auxKey file from the e-lection server as aux_keys.json
     */
    ELECTIONKEY("electionkey",
            "Generates the election key and key backups for other trustees" + System.lineSeparator() +
            "Requires the auxKey file from the e-lection server as aux_keys.json"),
    /**
     * Checks the provided key backups.
     * Requires the backup file from the e-lection server as backup_keys.json
     * and the private aux key file as aux_private.json
     */
    BACKUP_CHECK("check",
            "Checks the provided key backups." + System.lineSeparator() +
            "Requires the backup file from the e-lection server as backup_keys.json " +
            "and the private aux key file as aux_private.json"),
    /**
     * Generates a partial decryption of an election result
     * Requires the result file from the e-lection server as encrypted_tallies.json
     * and the private key file for the election key as ceremony_private.json
     */
    DECRYPTION("decrypt",
            "Generates a partial decryption of an election result" + System.lineSeparator() +
            "Requires the result file from the e-lection server as encrypted_tallies.json " +
            "and the private key file for the election key as ceremony_private.json"),
    /**
     * Generates partialPartialDecryptions of an election result
     * requires the result file from the e-lections server as encrypted_tallies.json
     * and the private aux key file as aux_private.json
     */
    COMPENSATION("compensate",
            "Generates partialPartialDecryptions of an election result." + System.lineSeparator() +
            "Requires the result file from the e-lections server as encrypted_tallies.json " +
            "and the private aux key file as aux_private.json"),
    ;
    private final String regex;
    private final String description;
    private Commands(String regex, String description) {
        this.regex = regex;
        this.description = description;
    }
    public String getRegex() {
        return this.regex;
    }
    public String getDescription() {return this.description;}
    public static List<Commands> primaryCommands() {
        return List.of(AUXKEY, ELECTIONKEY, BACKUP_CHECK, DECRYPTION, COMPENSATION);
    }
    public static List<Commands> arguments() {
        return List.of(OPTION_SOURCE, OPTION_TARGET, OPTION_ENCRYPTION);
    }

}

