package com.rsp.client;

import com.rsp.game.RSP;
import com.rsp.game.RoundResults;
import com.rsp.protocol.ProtocolConfiguration;
import com.rsp.protocol.ProtocolConfiguration.ControlStatus;
import com.rsp.protocol.ServerInEvent;
import com.rsp.protocol.ServerOutEvent;
import com.rsp.protocol.ShutdownServerEvent;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Optional;
import java.util.Scanner;


class ClientGameManager {


    private static final String GAME_END = "END";
    private static final String STOP_SERVER = "STOPSERVER";
    private static final int TIMEOUT = 5000;
    private static ClientGameManager manager;

    private String serverHost;
    private int serverPort;
    private int clientPort;
    private String userName;

    private InetSocketAddress serverSocketAddress;
    private DatagramSocket socket;

    private RSP previousRoundMachineChoice;

    private ByteBuffer outcomeBuffer = ByteBuffer.allocate(ProtocolConfiguration.getInEventSizeInBytes());
    private ByteBuffer incomeBuffer = ByteBuffer.allocate(ProtocolConfiguration.getServerOutEventSizeInBytes());

    private ClientGameManager(String userName, String serverHost, int serverPort, int clientPort) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.clientPort = clientPort;
        this.userName = userName;
    }

    private ClientGameManager() {

    }

    static ClientGameManager getInstance(String userName, String serverHost, int serverPort, int clientPort) {


        if (manager != null) {
            return manager;
        }
        manager = new ClientGameManager(userName, serverHost, serverPort, clientPort);
        return manager;
    }


    int playGame() {
        System.out.println("Starting up game.....");

        if (!initNetwork()) {
            return -1;
        }

        if (!enterInGameWithServer()){
            return -1;
        }
        printIntro();

        final Scanner scanner = new Scanner(System.in);
        String command;

        while (true) {
            System.out.println("Enter your choice ");
            command = scanner.nextLine().trim();

            if (endGameFor(command) || stopServerFor(command)) {
                return 0;
            }

            try {
                if (!successfullProccessGameFor(command)) {
                    System.out.println("Hmmm... this round with Great Machine was unsuccessful. See messages above. Let's try again.");
                }
            } catch (IOException e) {
                System.err.println(new StringBuilder()
                        .append("Error occurred while communication with server: ")
                        .append(e.getMessage())
                        .append("Please contact Great Machine developers(i.e. support ) and make them feel uncomfortable."));
                socket.close();
                System.out.println("Game is finished.");
                return -1;
            }
        }

    }


    private boolean successfullProccessGameFor(String command) throws IOException {
        if (validInputChoice(command)) {
            return proccessValidUserChoice(command);
        } else {
            System.out.println("You have entered incorrect choice, please try again. Kind remind that possible values are ");
            printChoiceOptions();
        }
        return true;
    }

    private boolean proccessValidUserChoice(String command) throws IOException {
        final ServerInEvent choice = ServerInEvent.getBuilder()
                .appendUserName(userName.toCharArray())
                .appendEventTime(System.currentTimeMillis())
                .appendChoice(RSP.valueOf(command.toUpperCase()).byteValue()).build();
        final Optional<ServerOutEvent> serverOutEvent;
        serverOutEvent = playRound(choice);

        if (serverOutEvent.isPresent()) {
            final ControlStatus controlStatus = ControlStatus.valueOf(serverOutEvent.get().getStatusCode());
            switch (controlStatus) {
                case OK:
                    processOKFor(serverOutEvent.get());
                    break;
                case ERROR:
                    processERRORFor(serverOutEvent.get());
                    break;
                default:
                    throw new IllegalStateException("No such return code from server.");
            }
            return true;
        }
        return false;
    }

    private void processERRORFor(ServerOutEvent serverOutEvent) {
        System.out.println(new StringBuilder("Error happened on server:  ")
                .append(serverOutEvent.getMessage()));
    }

    private void processOKFor(ServerOutEvent serverOutEvent) {

        System.out.println(new StringBuilder()
                .append("Great Machine choice is ")
                .append(this.previousRoundMachineChoice)
                .append(". You have ")
                .append(RoundResults.valueOf(serverOutEvent.getRoundState()).get()));
        previousRoundMachineChoice = RSP.valueOf(serverOutEvent.getChoice()).get();
        printStats(serverOutEvent);
    }

    private boolean endGameFor(String move) {
        if (GAME_END.equalsIgnoreCase(move)) {
            System.out.println("See you next time. Please learn some math before attempt to beat Great Machine.");
            return true;
        }
        return false;
    }

    private boolean stopServerFor(String move) {
        if (STOP_SERVER.equalsIgnoreCase(move)) {
            try {
                stopServerFor();
                System.out.println("Apparently you decided to stop server. Bye!");
                return true;
            } catch (IOException e) {
                System.err.println(new StringBuilder("Error while attempting to send stop event to  game server: ")
                        .append(e.getMessage())
                        .append(". We still be playing. "));
            }
        }
        return false;
    }

    private void stopServerFor() throws IOException {
        ShutdownServerEvent shutdownServerEvent = new ShutdownServerEvent();
        final ByteBuffer stopServerBufferEvent = ByteBuffer.allocate(ProtocolConfiguration.getShutdownServerEventSizeInBytes());
        shutdownServerEvent.marshallTo(stopServerBufferEvent);
        makeBufferReadyToRead(stopServerBufferEvent);
        final DatagramPacket datagramPacket = new DatagramPacket(stopServerBufferEvent.array(), stopServerBufferEvent.array().length, serverSocketAddress);
        socket.send(datagramPacket);
    }

    private boolean enterInGameWithServer() {
        try {
            final Optional<ServerOutEvent> firstReply = askServerForHisChoiceFirstTime();
            if (firstReply.isPresent()) {
                if (firstReply.get().getTie() == 0 && firstReply.get().getUserWins() == 0 && firstReply.get().getUserLooses() == 0) {
                    System.out.println("Looks like you first time here.");
                } else {
                    System.out.println(new StringBuilder("Welcome back ").append(userName).append('!'));
                    System.out.println(new StringBuilder("It is you again. Hopefully you get learn some math and ready to beat Great Machine. Am I right ").append(userName).append(" ? :)"));
                    System.out.println("Let me remind where we stopped last time, so stats are: ");
                    printStats(firstReply.get());
                }
                previousRoundMachineChoice = RSP.valueOf(firstReply.get().getChoice()).get();
            }

        } catch (IOException e) {
            System.err.println(new StringBuilder().append("Error occurred while access server: ").append(e.getMessage()));
            return false;
        }
        return true;
    }

    private void printIntro() {
        System.out.println();
        System.out.print("This game is very simple. You suppose to enter one of the values in command line ");
        printChoiceOptions();
        System.out.println("As of the loose win relation, we have following picture:");
        printRules();
        System.out.println();
        System.out.println("Let's playGame. Good luck to you and enjoy! ");
        System.out.println("--------------------------------------------------------------------------------- ");
    }

    private void printRules() {
        Arrays.stream(RSP.values()).forEach(e -> {
            System.out.print(e + " looses to ");
            e.getLooseTo().forEach(w -> System.out.print(w + " "));
            System.out.println();
        });
    }

    private void printChoiceOptions() {
        System.out.print("[");
        Arrays.stream(RSP.values()).forEach(e -> System.out.print(e + " "));
        System.out.print("]. ");
    }

    private boolean validInputChoice(String move) {
        return Arrays.stream(RSP.values()).anyMatch(e -> e.toString().equalsIgnoreCase(move));
    }

    private Optional<ServerOutEvent> askServerForHisChoiceFirstTime() throws IOException {

        final ServerInEvent firstChoice = ServerInEvent.getBuilder().appendEventTime(System.currentTimeMillis()).appendUserName(userName.toCharArray()).build();

        return playRound(firstChoice);
    }

    private Optional<ServerOutEvent> playRound(ServerInEvent userChoice) throws IOException {
        sendRequestToServer(userChoice);

        return serverReply();

    }

    private Optional<ServerOutEvent> serverReply() throws IOException {
        clearBuffer(incomeBuffer);
        final DatagramPacket incomingPacket = new DatagramPacket(incomeBuffer.array(), incomeBuffer.array().length);
        socket.receive(incomingPacket);
        incomeBuffer.put(incomingPacket.getData());
        makeBufferReadyToRead(incomeBuffer);
        ServerOutEvent reply = new ServerOutEvent();
        reply.unMarshallFrom(incomeBuffer);

        if (reply.getStatusCode() == ProtocolConfiguration.ControlStatus.OK.byteValue()) {
            return Optional.of(reply);
        } else {
            System.err.println(new StringBuilder().append("Error from the server: ").append(reply.getMessage()));
            return Optional.empty();
        }
    }

    private void printStats(ServerOutEvent reply) {
        System.out.println(new StringBuilder().append("Your score versus Great Machine is {")
                .append(" wins: ").append(reply.getUserWins())
                .append(" looses: ").append(reply.getUserLooses())
                .append(" ties: ").append(reply.getTie()).append("}"));

    }

    private void clearBuffer(ByteBuffer incomeBuffer) {
        incomeBuffer.clear();
    }

    private void sendRequestToServer(ServerInEvent userChoice) throws IOException {

        clearBuffer(outcomeBuffer);
        userChoice.marshallTo(outcomeBuffer);
        makeBufferReadyToRead(outcomeBuffer);
        final DatagramPacket outPacket = new DatagramPacket(outcomeBuffer.array(), outcomeBuffer.array().length, serverSocketAddress);
        socket.send(outPacket);
    }

    private void makeBufferReadyToRead(ByteBuffer buffer) {
        buffer.flip();
    }

    private boolean initNetwork() {

        try {
            serverSocketAddress = new InetSocketAddress(serverHost, serverPort);
            socket = new DatagramSocket(clientPort);
            socket.setSoTimeout(TIMEOUT);
        } catch (IOException e) {

            System.err.println(new StringBuilder()
                    .append("Issues happened with client network initialization, error is: ")
                    .append(e.getMessage()));
            return false;
        }
        return true;
    }

}
