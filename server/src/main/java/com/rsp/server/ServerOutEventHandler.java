package com.rsp.server;

import com.lmax.disruptor.EventHandler;
import com.rsp.protocol.ServerOutEvent;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

class ServerOutEventHandler implements EventHandler<ServerOutEvent> {


    private final DatagramChannel outcomeChannel;
    private final ByteBuffer outcomeBuffer;

    ServerOutEventHandler(DatagramChannel outcomeChannel, ByteBuffer outcomeBuffer) {
        this.outcomeChannel = outcomeChannel;
        this.outcomeBuffer = outcomeBuffer;
    }


    @Override
    public void onEvent(ServerOutEvent event, long sequence, boolean endOfBatch) throws Exception {
        outcomeBuffer.rewind();
        outcomeBuffer.clear();

        event.marshallTo(outcomeBuffer);
        outcomeBuffer.flip();
        outcomeChannel.send(outcomeBuffer, new InetSocketAddress(InetAddress.getByAddress(event.getClientAddress()), event.getClientPort()));
    }
}
