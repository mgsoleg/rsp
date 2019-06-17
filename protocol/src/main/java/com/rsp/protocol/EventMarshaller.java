package com.rsp.protocol;

import java.nio.ByteBuffer;

public interface EventMarshaller {

    default void unMarshallFrom(ByteBuffer buffer) {
        throw new IllegalAccessError("This should be used only with Protocol annotation.");
    }


    default void marshallTo(ByteBuffer buffer) {
        throw new IllegalAccessError("This should be used only with Protocol annotation.");
    }

    default int messageSizeInBytes() {
        throw new IllegalAccessError("This should be used only with Protocol annotation.");
    }


}
