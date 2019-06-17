package com.rsp.server;

import com.lmax.disruptor.EventHandler;
import com.rsp.game.RSP;
import com.rsp.game.RSPGame;
import com.rsp.protocol.ClientEvent;

import java.util.Map;
import java.util.Optional;

class ClientEventHandler implements EventHandler<ClientEvent> {

    private final Map<String, RSPGame> usersGame;
    private final ServerOutEventProducer serverOutEventProducer;


    ClientEventHandler(Map usersGame, ServerOutEventProducer serverOutEventProducer) {
        this.usersGame = usersGame;
        this.serverOutEventProducer = serverOutEventProducer;
    }

    @Override
    public void onEvent(ClientEvent clientEvent, long l, boolean b) throws Exception {
        clientEvent.validate();
        playAndPublishForSending(clientEvent);
    }

    private void playAndPublishForSending(ClientEvent clientEvent) {
        final String userName = String.valueOf(clientEvent.getUserName());
        final Optional<RSP> lastUserChoice = RSP.valueOf(clientEvent.getChoice());

        RSPGame rspGame = usersGame.get(userName);
        if (rspGame != null) {
            rspGame.play(lastUserChoice);
        } else {
            rspGame = new RSPGame();
            rspGame.play(Optional.empty());
            usersGame.put(String.valueOf(clientEvent.getUserName()), rspGame);
        }
        serverOutEventProducer.onSuccess(clientEvent, rspGame);
    }
}
