package com.rsp.server;

import com.lmax.disruptor.dsl.Disruptor;
import com.rsp.protocol.ClientEvent;
import com.rsp.protocol.ProtocolConfiguration;
import com.rsp.protocol.ServerOutEvent;
import com.rsp.protocol.ShutdownServerEvent;
import com.rsp.protocol.exceptions.InvalidUDPPacketException;
import com.rsp.protocol.exceptions.ProtocolValidationException;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Objects;


class UDPDisruptorBasedServer implements RSPServer {
    private static final Logger logger = LogManager.getLogger(UDPDisruptorBasedServer.class);


    //RingBuffer for incoming messages
    private Disruptor<ClientEvent> incomeMessagesDisruptor;
    private ClientEventProducer clientEventProducer;

    //RingBuffer for outcoming messages
    private ServerOutEventProducer serverOutEventProducer;
    private Disruptor<ServerOutEvent> outcomeMessagesDisruptor;


    //UDP channel specifics
    private DatagramChannel incomeChannel;
    private DatagramChannel outcomeChannel;

    private static UDPDisruptorBasedServer server;

    private static UDPDisruptorBasedServer getInstance() {
        if (server == null) {
            server = new UDPDisruptorBasedServer();
        }
        return server;
    }

    private UDPDisruptorBasedServer() {
    }

    @Override
    public int run() {
        validate();
        startOutcomeMessagePartDisruptor();
        startIncomeMessagePartDisruptor();
        return runMainLoop();

    }

    private void startOutcomeMessagePartDisruptor() {
        outcomeMessagesDisruptor.start();
    }

    private void startIncomeMessagePartDisruptor() {
        incomeMessagesDisruptor.start();
    }

    private int runMainLoop() {
        logger.info("RSP game server started.");
        ByteBuffer udpMessageBuffer = ByteBuffer.allocate(ProtocolConfiguration.getClientEventSizeInBytes());
        InetSocketAddress inetSocketAddress = null;
        while (true) {
            try {
                udpMessageBuffer.clear();
                inetSocketAddress = (InetSocketAddress) incomeChannel.receive(udpMessageBuffer);
                if (stopEventReceived(udpMessageBuffer)) {
                    silentlyShutdownServer();
                    return 0;
                }

                if (udpMessageBuffer.position() != ProtocolConfiguration.getInEventSizeInBytes()) {
                    throw new InvalidUDPPacketException(wrongSizeOfUDPPacket(udpMessageBuffer));
                }
                clientEventProducer.onCorrectDataReceived(udpMessageBuffer, inetSocketAddress);
            } catch (IOException e) {
                logger.error("Issue happened with network I/O, error is {}. Will try to stop server silently.", e.getMessage());
                return -1;
            } catch (InvalidUDPPacketException e) {
                serverOutEventProducer.onIncorrectUDPPacket(inetSocketAddress, ProtocolConfiguration.INVALID_UPD_PACKET_RECEIVED);
            } catch (ProtocolValidationException e) {
                logger.error("Looks like there was attempt tp stop server but secret code provided is incorrect, so keeps running....");
                return -1;
            }
        }
    }

    private boolean stopEventReceived(ByteBuffer udpMessageBuffer) throws ProtocolValidationException {
        if (udpMessageBuffer.position() == ProtocolConfiguration.getShutdownServerEventSizeInBytes()) {
            ShutdownServerEvent shutdownServerEvent = new ShutdownServerEvent();
            udpMessageBuffer.flip();
            shutdownServerEvent.unMarshallFrom(udpMessageBuffer);
            shutdownServerEvent.validate();
            logger.info("Shutdown server event received.");
            return true;
        }
        return false;
    }

    private void silentlyShutdownServer() {
        logger.info("Shutting down sockets.");
        try {
            incomeChannel.close();
            outcomeChannel.close();
        } catch (IOException e) {
            logger.error("Something terrible happened while stopping socket channels, error is {}", e.getMessage());
        }

        logger.info("Shutting down disruptors.");
        incomeMessagesDisruptor.shutdown();
        outcomeMessagesDisruptor.shutdown();

        logger.info("Server is down.");

    }

    private String wrongSizeOfUDPPacket(ByteBuffer udpMessageBuffer) {
        return new StringBuilder()
                .append("Size of packet is ")
                .append(udpMessageBuffer.position())
                .append(" bytes while expecting ")
                .append(ProtocolConfiguration.getInEventSizeInBytes())
                .append(" bytes.").toString();
    }
    private void validate() {
        Objects.requireNonNull(outcomeChannel, "OutcomeChannel should not be null");
        Validate.isTrue(outcomeChannel.socket().isBound(), "OutcomeChannel should be bounded to port before used.");

        Objects.requireNonNull(incomeChannel, "IncomeChannel should not be null");
        Validate.isTrue(outcomeChannel.socket().isBound(), "IncomeChannel should be bounded to port before used.");

        Objects.requireNonNull(incomeMessagesDisruptor, "Please set IncomeMessagesDisruptor before use of server");
        Objects.requireNonNull(outcomeMessagesDisruptor, "Please set IncomeMessagesDisruptor before use of server");

        Objects.requireNonNull(serverOutEventProducer, "Please set ServerOutEventProducer before use of server");
        Objects.requireNonNull(clientEventProducer, "Please set ClientEventProducer before use of server");
    }

    public static Builder getBuilder() {
        return getInstance().new Builder();
    }

    class Builder {

        Builder withIncomingChannel(final DatagramChannel channel) {
            getInstance().incomeChannel = channel;
            return this;
        }

        Builder withOutComingChannel(final DatagramChannel channel) {
            getInstance().outcomeChannel = channel;
            return this;
        }

        Builder withIncomeMessageDisruptor(final Disruptor disruptor) {
            getInstance().incomeMessagesDisruptor = disruptor;
            return this;
        }

        Builder withOutcomeMessageDisruptor(final Disruptor disruptor) {
            getInstance().outcomeMessagesDisruptor = disruptor;
            return this;
        }

        Builder withServerOutEventProducer(ServerOutEventProducer producer) {
            getInstance().serverOutEventProducer = producer;
            return this;
        }

        Builder withClientEventProducer(ClientEventProducer producer) {
            getInstance().clientEventProducer = producer;
            return this;
        }


        UDPDisruptorBasedServer buildServer() {

            return getInstance();
        }


    }

}
