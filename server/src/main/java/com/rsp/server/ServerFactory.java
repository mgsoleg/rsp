package com.rsp.server;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ExceptionHandlerSetting;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.DaemonThreadFactory;
import com.rsp.game.RSPGame;
import com.rsp.protocol.ClientEvent;
import com.rsp.protocol.ProtocolConfiguration;
import com.rsp.protocol.ServerOutEvent;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.HashMap;

public class ServerFactory {

    private ServerFactory(){}

    public static RSPServer getUDPServerForPoolSize(final int poolSize, final int inPort, final int outPort) throws IOException {
        final DatagramChannel inComingChannel = getChannelAndBindTo(inPort);

        final DatagramChannel outComingChannel = getChannelAndBindTo(outPort);

        Disruptor<ServerOutEvent> outcomeMessagesDisruptor = getServerOutEventDisruptor(poolSize, outComingChannel);

        ServerOutEventProducer serverOutEventProducer = getServerOutEventProducer(outcomeMessagesDisruptor);

        Disruptor<ClientEvent> incomeMessagesDisruptor = getClientEventDisruptor(poolSize, serverOutEventProducer);

        ClientEventProducer clientEventProducer = getClientEventProducer(incomeMessagesDisruptor);


        return UDPDisruptorBasedServer.getBuilder()
                .withIncomingChannel(inComingChannel)
                .withOutComingChannel(outComingChannel)
                .withIncomeMessageDisruptor(incomeMessagesDisruptor)
                .withOutcomeMessageDisruptor(outcomeMessagesDisruptor)
                .withClientEventProducer(clientEventProducer)
                .withServerOutEventProducer(serverOutEventProducer)
                .buildServer();
    }

    static DatagramChannel getChannelAndBindTo(int port) throws IOException {
        final DatagramChannel channel = DatagramChannel.open();
        channel.socket().bind(new InetSocketAddress(port));
        return channel;
    }

    static ClientEventProducer getClientEventProducer(Disruptor<ClientEvent> incomeMessagesDisruptor) {
        return new ClientEventProducer(incomeMessagesDisruptor.getRingBuffer());
    }

    static ServerOutEventProducer getServerOutEventProducer(Disruptor<ServerOutEvent> outcomeMessagesDisruptor) {
        return new ServerOutEventProducer(outcomeMessagesDisruptor.getRingBuffer());
    }

    static Disruptor<ServerOutEvent> getServerOutEventDisruptor(int poolSize, DatagramChannel outcomingChannel) {
        Disruptor<ServerOutEvent> outcomeMessagesDisruptor = new Disruptor<>(new ServerOutEventFactory(), poolSize, DaemonThreadFactory.INSTANCE, ProducerType.MULTI, new BlockingWaitStrategy());
        outcomeMessagesDisruptor.handleEventsWith(new ServerOutEventHandler(outcomingChannel, ByteBuffer.allocate(ProtocolConfiguration.getServerOutEventSizeInBytes())));
        return outcomeMessagesDisruptor;
    }

    static Disruptor<ClientEvent> getClientEventDisruptor(int poolSize, ServerOutEventProducer serverOutEventProducer) {
        Disruptor<ClientEvent> incomeMessagesDisruptor = new Disruptor<>(new ClientEventFactory(), poolSize, DaemonThreadFactory.INSTANCE, ProducerType.SINGLE, new BlockingWaitStrategy());
        final ClientEventHandler clientEventHandler = new ClientEventHandler(new HashMap<String, RSPGame>(poolSize), serverOutEventProducer);
        incomeMessagesDisruptor.handleEventsWith(clientEventHandler);
        final ExceptionHandlerSetting<ClientEvent> clientEventExceptionHandlerSetting = incomeMessagesDisruptor.handleExceptionsFor(clientEventHandler);
        clientEventExceptionHandlerSetting.with(new ClientEventExceptionHandler(serverOutEventProducer));
        return incomeMessagesDisruptor;
    }
}
