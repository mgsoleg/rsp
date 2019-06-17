package com.rsp.protocol;

import java.util.Arrays;

public class ProtocolConfiguration {
    private static ClientEvent clientEvent = new ClientEvent();
    private static ServerInEvent serverInEvent = new ServerInEvent();
    private static ServerOutEvent serverOutEvent = new ServerOutEvent();
    private static ShutdownServerEvent shutdownServerEvent = new ShutdownServerEvent();


    static final byte IPv4Flag = 0;
    static final byte IPv6Flag = 1;

    static final int IPv4BufferLength = 4;
    public static final int IPv6BufferLength = 16;

    public static final byte invalidUDPMessage = 0;
    public static final byte validUDPMessage = 1;

    static final int PORT_RANGE_MAX = 65535;
    static final int PORT_RANGE_MIN = 0;

    static final long SHUT_DOWN_SECRET_CODE = 563454575;

    public static final char[] INVALID_UPD_PACKET_RECEIVED = "Invalid UDP packet received.".toCharArray();


    public static int getClientEventSizeInBytes() {
        return clientEvent.messageSizeInBytes();
    }

    public static int getInEventSizeInBytes() {
        return serverInEvent.messageSizeInBytes();
    }

    public static int getServerOutEventSizeInBytes() {
        return serverOutEvent.messageSizeInBytes();
    }

    public static int getShutdownServerEventSizeInBytes() {
        return shutdownServerEvent.messageSizeInBytes();
    }

    public static int getMaximumAllowedUserNameLength() {
        return serverInEvent.getUserName().length;
    }


    public enum ControlStatus {
        OK((byte) 0),
        ERROR((byte) 1);

        private byte state;

        ControlStatus(byte b) {
            this.state = b;
        }

        public byte byteValue() {
            return state;
        }

        public static ControlStatus valueOf(byte b) {
            return Arrays.stream(values()).filter(e -> e.state == b).findFirst().get();
        }
    }


}
