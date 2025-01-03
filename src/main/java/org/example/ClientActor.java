// ClientActor.java
package org.example;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class ClientActor extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private final ActorRef serverActor;
    private final String name;
    private final ChatUI chatUI;

    public ClientActor(ActorRef serverActor, String name, ChatUI chatUI) {
        this.serverActor = serverActor;
        this.name = name;
        this.chatUI = chatUI;
    }

    public static Props props(ActorRef serverActor, String name, ChatUI chatUI) {
        return Props.create(ClientActor.class, () -> new ClientActor(serverActor, name, chatUI));
    }

    @Override
    public void preStart() {
        serverActor.tell(new ServerActor.RegisterClient(name), getSelf());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ServerActor.RegistrationSuccess.class, success -> {
                    log.info("Registered with server as {}", success.getName());
                    chatUI.displaySystemMessage("Successfully connected to chat server");
                })
                .match(ServerActor.ClientList.class, clientList -> {
                    chatUI.displayAvailableClients(clientList.getClients());
                })
                .match(Message.class, message -> {
                    chatUI.displayMessage(message);
                })
                .match(ServerActor.ChatHistory.class, history -> {
                    chatUI.displayChatHistory(history.getMessages());
                })
                .match(ServerActor.ErrorMessage.class, error -> {
                    chatUI.displayError(error.getError());
                })
                .build();
    }
}