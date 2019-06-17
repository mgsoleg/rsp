package com.rsp.protocol;

import com.rsp.protocol.exceptions.ProtocolValidationException;
import com.rsp.protocol.gen.Protocol;
import com.rsp.protocol.gen.ProtocolChunk;

@Protocol(chunks = 1)
public class ShutdownServerEvent implements EventMarshaller, EventValidator {
    @ProtocolChunk(order = 0)
    private long shutdownCode = ProtocolConfiguration.SHUT_DOWN_SECRET_CODE;

    @Override
    public void validate() throws ProtocolValidationException {
        if (shutdownCode != ProtocolConfiguration.SHUT_DOWN_SECRET_CODE) {
            throw new ProtocolValidationException("Incorrect shutdown server secret code");
        }
    }
}
