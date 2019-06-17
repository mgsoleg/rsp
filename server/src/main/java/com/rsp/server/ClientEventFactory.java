package com.rsp.server;

import com.lmax.disruptor.EventFactory;
import com.rsp.protocol.ClientEvent;

class ClientEventFactory implements EventFactory<ClientEvent> {
    public ClientEvent newInstance() {
        return new ClientEvent();
    }
}
