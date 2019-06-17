package com.rsp.game;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum RSP {

    ROCK((byte) 0),
    PAPER((byte) 1),
    SCISSORS((byte) 2),
    LIZARD((byte) 3),
    SPOCK((byte) 4);


    List<RSP> looseTo;


    private byte byteRepresenation;

    RSP(byte b) {
        this.byteRepresenation = b;
    }

    public byte byteValue() {
        return byteRepresenation;
    }

    public static Optional<RSP> valueOf(byte b) {
        return Arrays.stream(values()).filter(v -> v.byteRepresenation == b).findFirst();
    }

    public List<RSP> getLooseTo() {
        return looseTo;
    }

    public boolean losesTo(RSP other) {
        return looseTo.contains(other);
    }

    static {
        SCISSORS.looseTo = Arrays.asList(ROCK, SPOCK);
        ROCK.looseTo = Arrays.asList(PAPER, SPOCK);
        PAPER.looseTo = Arrays.asList(SCISSORS, LIZARD);
        SPOCK.looseTo = Arrays.asList(PAPER, LIZARD);
        LIZARD.looseTo = Arrays.asList(SCISSORS, ROCK);
    }


}
