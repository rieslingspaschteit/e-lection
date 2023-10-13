/*
 * This Java source file was generated by the Gradle 'init' task.
 */

package electioncli.core;

import java.io.IOException;

/**
 * This class is the main class for the app.
 * */
public class App {
    /**
     * This is the main method.
     * */
    @SuppressWarnings("checkstyle:Indentation")
    public static void main(String[] args) {
        CommandParser parser;
        try {
            parser = new CommandParser(args);
            parser.parse();
            parser.execute();
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

}