package com.rsp.client;

import com.rsp.protocol.ProtocolConfiguration;
import org.apache.commons.cli.*;

public class Main {

    static final String USER_NAME = "userName";
    static final String SERVER_HOST = "serverHost";
    static final String SERVER_PORT = "serverPort";
    static final String USER_PORT = "userPort";

    public static void main(String[] args) {

        Options options = new Options();
        options.addOption(getRequiredOption(USER_NAME, true, "name of the user, no more than " + ProtocolConfiguration.getMaximumAllowedUserNameLength() + " length."));
        options.addOption(getRequiredOption(SERVER_PORT, true, "port of game server"));
        options.addOption(getRequiredOption(SERVER_HOST, true, "host of game server"));
        options.addOption(getRequiredOption(USER_PORT, true, "port of user part to communicate with server"));


        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("Error while parsing command line arguments: " + e.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("RSP client", options);
            System.exit(-1);
        }


        final ClientGameManager game = ClientGameManager.getInstance(
                cmd.getOptionValue(USER_NAME),
                cmd.getOptionValue(SERVER_HOST),
                Integer.valueOf(cmd.getOptionValue(SERVER_PORT)),
                Integer.valueOf(cmd.getOptionValue(USER_PORT)));
        System.exit(game.playGame());
    }

    private static Option getRequiredOption(String key, boolean hasArg, String msg) {
        final Option option = new Option(key, hasArg, msg);
        option.setRequired(true);
        return option;
    }
}
