package com.rsp;

import com.rsp.server.RSPServer;
import com.rsp.server.ServerFactory;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class Main {
    private static final Logger logger = LogManager.getLogger(Main.class);

    static final String MSG_POOL_SIZE = "poolSize";
    static final String IN_PORT = "inPort";
    static final String OUT_PORT = "outPort";

    public static void main(String[] args) {

        Options options = new Options();
        options.addOption(getRequiredOption(MSG_POOL_SIZE, true, "size of messages pool, should be more that 1 and power of 2 "));
        options.addOption(getRequiredOption(IN_PORT, true, "port where messages from clients being received"));
        options.addOption(getRequiredOption(OUT_PORT, true, "port where messages to clients being send"));


        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("Error while parsing command line arguments: " + e.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("RSP server", options);
            System.exit(-1);
        }


        try {
            RSPServer server = ServerFactory.getUDPServerForPoolSize(
                    Integer.valueOf(cmd.getOptionValue(MSG_POOL_SIZE)),
                    Integer.valueOf(cmd.getOptionValue(IN_PORT)),
                    Integer.valueOf(cmd.getOptionValue(OUT_PORT)));
            System.exit(server.run());
        } catch (IOException e) {
            logger.error("Error happened while server set up: <{}>", e.getMessage());
        }
    }

    private static Option getRequiredOption(String key, boolean hasArg, String msg) {
        final Option option = new Option(key, hasArg, msg);
        option.setRequired(true);
        return option;
    }
}
