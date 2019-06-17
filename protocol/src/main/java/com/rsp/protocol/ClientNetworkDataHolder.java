package com.rsp.protocol;

import com.rsp.protocol.exceptions.InvalidClientAddressException;
import com.rsp.protocol.exceptions.ProtocolValidationException;

import java.net.InetSocketAddress;

abstract class ClientNetworkDataHolder extends BaseEvent {

    byte ipProtocol = -1;
    byte[] clientAddress = new byte[16];
    int clientPort = -1;

    public void updateClientInetAddressFrom(InetSocketAddress socketAddress) {
        if (socketAddress == null) {
            throw new NullPointerException("Socket address cannot be null for this operation.");
        }
        byte[] address = socketAddress.getAddress().getAddress();
        this.ipProtocol = (address.length == ProtocolConfiguration.IPv4BufferLength ? ProtocolConfiguration.IPv4Flag : ProtocolConfiguration.IPv6Flag);

        System.arraycopy(address, 0, this.clientAddress, 0, address.length);
        this.clientPort = socketAddress.getPort();
    }

    public int getClientPort() {
        return clientPort;
    }

    public byte[] getClientAddress() {
        if (ipProtocol == ProtocolConfiguration.IPv6Flag) {
            return clientAddress;
        }
        byte[] IPv4 = new byte[4];
        System.arraycopy(clientAddress, 0, IPv4, 0, 4);
        return IPv4;
    }


    public void updateClientInetAddressFrom(ClientNetworkDataHolder networkDataHolder) {
        this.ipProtocol = networkDataHolder.ipProtocol;
        System.arraycopy(networkDataHolder.clientAddress, 0, this.clientAddress, 0, networkDataHolder.clientAddress.length);
        this.clientPort = networkDataHolder.clientPort;
    }

    @Override
    public void validate() throws ProtocolValidationException {
        super.validate();
        if (ipProtocol != ProtocolConfiguration.IPv6Flag && ipProtocol != ProtocolConfiguration.IPv4Flag) {
            throw new InvalidClientAddressException("Invalid client address data: wrong protocol specified.");
        }
        if (!(this.clientPort >= ProtocolConfiguration.PORT_RANGE_MIN && this.clientPort <= ProtocolConfiguration.PORT_RANGE_MAX)) {
            throw new InvalidClientAddressException("Invalid client address data: port out of range.");
        }

    }
}
