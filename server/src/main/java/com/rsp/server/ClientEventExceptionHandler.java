package com.rsp.server;

import com.lmax.disruptor.ExceptionHandler;
import com.rsp.protocol.ClientEvent;

public class ClientEventExceptionHandler implements ExceptionHandler<ClientEvent> {

    private final ServerOutEventProducer serverOutEventProducer;



    public ClientEventExceptionHandler(final ServerOutEventProducer serverOutEventProducer) {
        this.serverOutEventProducer = serverOutEventProducer;
    }


    @Override
    public void handleEventException(Throwable ex, long sequence, ClientEvent event) {
        serverOutEventProducer.onIvalidData(event, ex.getMessage().toCharArray());
    }

    @Override
    public void handleOnStartException(Throwable ex) {
        // nothing to do here so far
    }

    @Override
    public void handleOnShutdownException(Throwable ex) {
        // nothing to do here so far
    }
}
