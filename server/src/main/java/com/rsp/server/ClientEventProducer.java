package com.rsp.server;

import com.lmax.disruptor.EventTranslatorTwoArg;
import com.lmax.disruptor.RingBuffer;
import com.rsp.protocol.ClientEvent;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

class ClientEventProducer {
    private final RingBuffer<ClientEvent> ringBuffer;

    ClientEventProducer(RingBuffer<ClientEvent> ringBuffer) {
        this.ringBuffer = ringBuffer;
    }

    private static final EventTranslatorTwoArg<ClientEvent, ByteBuffer, InetSocketAddress> ON_CORRECT_DATA_TRANSLATOR =
            (event, sequence, udpMessageBuffer, inetAddress) -> {
                event.unMarshallFrom(udpMessageBuffer);
                event.updateClientInetAddressFrom(inetAddress);
            };

    void onCorrectDataReceived(ByteBuffer udpMessageBuffer, InetSocketAddress inetAddress) {
        udpMessageBuffer.flip();
        ringBuffer.publishEvent(ON_CORRECT_DATA_TRANSLATOR, udpMessageBuffer, inetAddress);
    }

}
