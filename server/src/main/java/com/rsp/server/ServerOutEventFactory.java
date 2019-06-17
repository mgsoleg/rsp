package com.rsp.server;

import com.lmax.disruptor.EventFactory;
import com.rsp.protocol.ServerOutEvent;

class ServerOutEventFactory implements EventFactory<ServerOutEvent> {
    @Override
    public ServerOutEvent newInstance() {
        return new ServerOutEvent();
    }
}
