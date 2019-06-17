package com.rsp.server;


import com.rsp.game.RSPGame;
import com.rsp.protocol.ClientEvent;
import com.rsp.protocol.ProtocolConfiguration;
import com.rsp.protocol.ServerInEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ClientEventHandlerTest {


    private char [] TEST_USER = new char[ProtocolConfiguration.getMaximumAllowedUserNameLength()];
    private ClientEventHandler clientEventHandler;
    private InetSocketAddress inetAddress = new InetSocketAddress("localhost", 23);


    @Mock
    private ServerOutEventProducer serverOutEventProducer;

    @Mock
    private Map<String, RSPGame> gamesMap;

    @Before
    public void setUp() {

        clientEventHandler = new ClientEventHandler(gamesMap, serverOutEventProducer);
        final String user = "TEST_USER";
        System.arraycopy(user.toCharArray(), 0, TEST_USER, 0, user.length());
    }

    @Test
    public void testCorrectMessageFirstTime() throws Exception {

        ServerInEvent inEvent = ServerInEvent.getBuilder()
                .appendUserName(TEST_USER)
                .appendEventTime(System.currentTimeMillis()).build();

        final ByteBuffer buffer = ByteBuffer.allocate(ProtocolConfiguration.getInEventSizeInBytes());
        inEvent.marshallTo(buffer);

        buffer.flip();
        final ClientEvent correctMessage = new ClientEvent();
        correctMessage.updateClientInetAddressFrom(inetAddress);
        correctMessage.unMarshallFrom(buffer);

        clientEventHandler.onEvent(correctMessage, 0, false);

        ArgumentCaptor<ClientEvent> clientMessage = ArgumentCaptor.forClass(ClientEvent.class);
        verify(serverOutEventProducer).onSuccess(clientMessage.capture(),any());
        final ClientEvent actualEvent = clientMessage.getValue();
        assertEquals("Client message should match ", correctMessage, actualEvent);

        ArgumentCaptor<String> userName = ArgumentCaptor.forClass(String.class);
        verify(gamesMap).put(userName.capture(), any());
        final String actualUsername = userName.getValue();
        assertTrue("User names should match ", String.valueOf(TEST_USER).equals(actualUsername));


    }


    @Before
    public void tearDown() {
        // nothing to do here
    }

}