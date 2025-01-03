package org.example;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class ClientActor extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private final ActorRef serverActor;
    private final String name;

    public ClientActor(ActorRef serverActor, String name) {
        this.serverActor = serverActor;
        this.name = name;
    }

    public static Props props(ActorRef serverActor, String name) {
        return Props.create(ClientActor.class, () -> new ClientActor(serverActor, name));
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
                })
                .match(ServerActor.ClientList.class, clientList -> {
                    // Exclude the client itself from the list
                    log.info("Available clients: {}", clientList.getClients());
                })
                .match(ServerActor.Message.class, message -> {
                    log.info("Message from {}: {}", message.getSender(), message.getContent());
                })
                .match(ServerActor.ErrorMessage.class, error -> {
                    log.warning("Error: {}", error.getError());
                })
                .match(ServerActor.FindClients.class, findClients -> {
                    // Pass the current client's name to avoid including it in the list
                    serverActor.tell(new ServerActor.FindClients(name), getSelf());
                })
                .match(ServerActor.SendMessage.class, sendMessage -> {
                    log.info("Received a SendMessage to deliver: {} -> {}: {}",
                            sendMessage.getSender(), sendMessage.getRecipient(), sendMessage.getContent());
                })
                .build();
    }


}