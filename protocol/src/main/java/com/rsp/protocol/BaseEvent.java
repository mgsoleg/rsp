package com.rsp.protocol;

import com.rsp.protocol.exceptions.InvalidMessageException;
import com.rsp.protocol.exceptions.ProtocolValidationException;
import com.rsp.protocol.gen.Protocol;
import com.rsp.protocol.gen.ProtocolChunk;

@Protocol(chunks = 3)
abstract class BaseEvent implements EventMarshaller, EventValidator {

    @ProtocolChunk(order = 0)
    long eventTime = -1;

    @ProtocolChunk(order = 1)
    byte choice = -1;

    @ProtocolChunk(order = 2, elementsNumber = 10)
    char[] userName = new char[10];

    public long getEventTime() {
        return eventTime;
    }

    public char[] getUserName() {
        return userName;
    }

    public byte getChoice() {
        return choice;
    }


    @Override
    public void validate() throws ProtocolValidationException {

        if (eventTime == -1) {
            throw new InvalidMessageException("Event time cannot be null.");
        }
        int count = 0;
        while (count < userName.length && userName[count++] == Character.MIN_VALUE) ;
        if (count == userName.length - 1) {
            throw new InvalidMessageException("User name cannot be null.");
        }
    }

}
