package electioncli.handle;

import com.google.gson.JsonObject;

public abstract class CommandHandler {
    public abstract void initialize(JsonObject[] input, String args[]);
    public abstract JsonObject[] execute();
    public abstract boolean matchesCommand(String command);
    public abstract String[] getRequiredFiles();
    public abstract String[] getTargetFiles();
    public abstract String message();
}
