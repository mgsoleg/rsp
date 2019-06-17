package com.rsp.protocol.exceptions;

public class InvalidMessageException extends ProtocolValidationException {
    public InvalidMessageException(String message) {
        super(message);
    }
}
