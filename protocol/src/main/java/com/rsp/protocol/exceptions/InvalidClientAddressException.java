package com.rsp.protocol.exceptions;

public class InvalidClientAddressException extends ProtocolValidationException {
    public InvalidClientAddressException(String message) {
        super(message);
    }
}
