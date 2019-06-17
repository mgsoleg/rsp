package com.rsp.server;

import com.lmax.disruptor.EventTranslatorTwoArg;
import com.lmax.disruptor.RingBuffer;
import com.rsp.game.RSPGame;
import com.rsp.protocol.ClientEvent;
import com.rsp.protocol.ServerOutEvent;

import java.net.InetSocketAddress;

class ServerOutEventProducer {


    private final RingBuffer<ServerOutEvent> ringBuffer;

    ServerOutEventProducer(RingBuffer<ServerOutEvent> ringBuffer) {
        this.ringBuffer = ringBuffer;
    }

    private static final EventTranslatorTwoArg<ServerOutEvent, ClientEvent, RSPGame> SUCCESS =
            (event, sequence, clientEvent, rspGame) -> {
                event.updateClientInetAddressFrom(clientEvent);
                event.consumeAsSuccessFrom(rspGame);
            };

    private static final EventTranslatorTwoArg<ServerOutEvent, ClientEvent, char[]> ERROR =
            (event, sequence, clientEvent, message) -> {
                event.updateClientInetAddressFrom(clientEvent);
                event.consumeErroredFrom(message);

            };

    private static final EventTranslatorTwoArg<ServerOutEvent, InetSocketAddress, char[]> INVALID_UDP_PACKET =
            (event, sequence, inetSocketAddress, message) -> {
                event.updateClientInetAddressFrom(inetSocketAddress);
                event.consumeErroredFrom(message);
            };


    void onSuccess(ClientEvent clientEvent, RSPGame rspGame) {
        ringBuffer.publishEvent(SUCCESS, clientEvent, rspGame);
    }

    void onIvalidData(ClientEvent clientEvent, char[] message) {
        ringBuffer.publishEvent(ERROR, clientEvent, message);
    }

    void onIncorrectUDPPacket(InetSocketAddress inetSocketAddress, char[] message) {
        ringBuffer.publishEvent(INVALID_UDP_PACKET, inetSocketAddress, message);
    }


    }
