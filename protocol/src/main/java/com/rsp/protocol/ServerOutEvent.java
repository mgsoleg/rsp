package com.rsp.protocol;

import com.rsp.game.RSPGame;
import com.rsp.game.RoundResults;
import com.rsp.protocol.gen.Protocol;
import com.rsp.protocol.gen.ProtocolChunk;

@Protocol(chunks = 8)
public class ServerOutEvent extends ClientNetworkDataHolder {

    @ProtocolChunk(order = 0)
    private long eventTime = -1;

    @ProtocolChunk(order = 1)
    private byte choice = -1;

    @ProtocolChunk(order = 2)
    private int userWins = -1;

    @ProtocolChunk(order = 3)
    private int userLooses = -1;

    @ProtocolChunk(order = 4)
    private int tie = -1;

    @ProtocolChunk(order = 5)
    private byte roundState = -1;

    @ProtocolChunk(order = 6)
    private byte statusCode = -1;

    @ProtocolChunk(order = 7, elementsNumber = 50)
    private char[] message = new char[50];


    @Override
    public long getEventTime() {
        return eventTime;
    }

    @Override
    public byte getChoice() {
        return choice;
    }

    public byte getRoundState() {
        return roundState;
    }

    public int getUserWins() {
        return userWins;
    }

    public int getUserLooses() {
        return userLooses;
    }

    public int getTie() {
        return tie;
    }

    public byte getStatusCode() {
        return statusCode;
    }

    public char[] getMessage() {
        return message;
    }

    public void consumeAsSuccessFrom(RSPGame rspGame) {
        this.eventTime = System.currentTimeMillis();
        this.tie = rspGame.getTie();
        this.choice = rspGame.getLastMachineChoice().byteValue();
        this.userWins = rspGame.getUserWin();
        this.userLooses = rspGame.getUserLoose();
        this.statusCode = ProtocolConfiguration.ControlStatus.OK.byteValue();
        final RoundResults lastUserRoundResult = rspGame.getLastUserRoundResult();
        if (lastUserRoundResult != null) {
            this.roundState = lastUserRoundResult.byteValue();
        }
    }

    public void consumeErroredFrom(char[] errorMsg) {
        this.eventTime = System.currentTimeMillis();
        this.statusCode = ProtocolConfiguration.ControlStatus.ERROR.byteValue();
        if (errorMsg.length > this.message.length) {
            throw new IllegalStateException("Supplied message longer than destination.");
        }
        System.arraycopy(errorMsg, 0, this.message, 0, errorMsg.length);
    }
}
