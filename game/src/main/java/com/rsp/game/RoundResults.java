package com.rsp.game;

import java.util.Arrays;
import java.util.Optional;

public enum RoundResults {
    WON((byte)0 ),
    TIE((byte)1 ),
    LOOSE((byte)2 );


    private byte byteRepresenation;

    RoundResults(byte b) {
        this.byteRepresenation = b;
    }

    public byte byteValue() {
        return byteRepresenation;
    }

    public static Optional<RoundResults> valueOf(byte b) {
        return Arrays.stream(values()).filter(v -> v.byteRepresenation == b).findFirst();
    }
}
