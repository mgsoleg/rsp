package com.rsp.protocol;

import com.rsp.protocol.exceptions.ProtocolValidationException;

public interface EventValidator {
    void validate() throws ProtocolValidationException;
}
