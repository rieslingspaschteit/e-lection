package electioncli.core;

import com.google.gson.*;
import electioncli.handle.decryption.CompensationHandler;
import electioncli.handle.decryption.DecryptionHandler;
import electioncli.handle.key_generation.AuxKeyHandler;
import electioncli.handle.CommandHandler;
import electioncli.handle.key_generation.BackupHandler;
import electioncli.handle.key_generation.ElectionKeyHandler;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class CommandParser {
    private static final List<CommandHandler> PUBLIC_HANDLERS = List.of(new AuxKeyHandler(), new ElectionKeyHandler(), new BackupHandler(),
            new DecryptionHandler(), new CompensationHandler());
    private static final String ERROR_MISSING_ARGUMENTS = "The requested command could not be found.";
    private static final String WROTE_TO = "Success, wrote to ";
    private static final String WRITING_FAILED = "Error: Directory was invalid or could not be accessed. Please try a different directory.";
    private static final String HELP_MESSAGE_1 = "CLI has to be started with at least one argument to specify the required action"
            + System.lineSeparator() + "The following commands are supported:";
    private static final String HELP_MESSAGE_2 = "Additional arguments can be added to specify details of the required action"
            + System.lineSeparator() + "The following arguments are supported:";
    private String commandString;
    private CommandHandler command;
    private String source;
    private String target;
    private String[] specificArgs;
    private JsonObject[] input;
    private List<CommandHandler> usedCollection = PUBLIC_HANDLERS;

    /*
    * Parses and executes the commands of the CLI
    * @param args: parameters passed over the command line, must contain the requested command,
    * can also contain additional options
    * */
    public CommandParser(String[] args) throws IllegalArgumentException {
        if (args.length == 0) {
            System.out.println(HELP_MESSAGE_1);
            for (Commands command : Commands.primaryCommands()) {
                System.out.println(command.getRegex());
                System.out.println(command.getDescription());
            }
            System.out.println();
            System.out.println(HELP_MESSAGE_2);
            for (Commands argument : Commands.arguments()) {
                System.out.println(argument.getRegex());
                System.out.println(argument.getDescription());
            }
            System.exit(0);
        }
        this.commandString = args[0];
        List<String> myArgs = new ArrayList<>();
        for (int i = 1; i < args.length; i++) {
            if (args[i].matches(Commands.OPTION_SOURCE.getRegex())) {
                this.source = args[i].split("=")[1];
            } else if (args[i].matches(Commands.OPTION_TARGET.getRegex())) {
                this.target = args[i].split("=")[1];
            } else {
                myArgs.add(args[i]);
            }
        }
        this.specificArgs = myArgs.toArray(new String[0]);
        if (this.target == null) {
            this.target = "";
        } else {
            this.target = this.target + "/";
        }
        if (this.source == null) {
            this.source = "";
        } else {
            this.source = this.source + "/";
        }
    }

    public void parse() throws IOException {
        for (CommandHandler handler : usedCollection) {
            if (handler.matchesCommand(commandString)) {
                command = handler;
            }
        }

        if (command == null) {
            throw new IllegalArgumentException(ERROR_MISSING_ARGUMENTS);
        }
        input = readFile(source, command.getRequiredFiles());
        command.initialize(input, specificArgs);
    }

    public void execute() throws IOException {
        JsonObject[] results = command.execute();
        for (int i = 0; i < results.length; i++) {
            writeFile(results[i], target, command.getTargetFiles()[i]);
        }
        System.out.print(command.message());
    }

    public static void writeFile(JsonObject content, String target, String name) throws IOException{
        FileWriter file = new FileWriter(target + name + ".json");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String prettyJsonString = gson.toJson(content);
        file.write(prettyJsonString);
        file.close();
        System.out.println(WROTE_TO + target + name);
    }

    /*
    * Returns a Json-Object each for every provided input file
    * */
    public static JsonObject[] readFile(String path, String[] inputFiles) throws IOException {
        JsonObject[] Jsons = new JsonObject[inputFiles.length];
        int i = 0;
        for (String fileName : inputFiles) {
            File f = new File(path + fileName + ".json");
            Reader reader = new FileReader(f);
            JsonElement element = JsonParser.parseReader(reader);
            reader.close();
            Jsons[i++] = element.getAsJsonObject();
        }
        return Jsons;
    }
}
