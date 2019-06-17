package com.rsp.protocol.exceptions;

public class InvalidUDPPacketException extends ProtocolValidationException{

    public InvalidUDPPacketException(String message) {
        super(message);
    }
}
