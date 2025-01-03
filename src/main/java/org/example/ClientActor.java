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

    public ClientActor(ActorRef serverActor, String name) {
        this.serverActor = serverActor;
        this.name = name;
    }

    public static Props props(ActorRef serverActor, String name) {
        return Props.create(ClientActor.class, () -> new ClientActor(serverActor, name));
    }

    @Override
    public void preStart() {
        // Register with the server only once when the client starts
        serverActor.tell(new ServerActor.RegisterClient(name), getSelf());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ServerActor.RegistrationSuccess.class, success -> {
                    log.info("Registered with server as {}", success.getName());
                })
                .match(ServerActor.ClientList.class, clientList -> {
                    // Show available clients, excluding self
                    log.info("Available clients: {}", clientList.getClients());
                    System.out.println("\nAvailable clients:");
                    for (String clientName : clientList.getClients()) {
                        System.out.println(clientName);
                    }
                })
                .match(ServerActor.Message.class, message -> {
                    // Handle incoming chat messages (received continuously)
                    System.out.println(message.getSender() + ": " + message.getContent());
                })
                .match(ServerActor.ErrorMessage.class, error -> {
                    System.out.println("Error: " + error.getError());
                })
                .match(ServerActor.SendMessage.class, sendMessage -> {
                    // Handle outgoing message when received by ClientActor
                    log.info("Received a SendMessage to deliver: {} -> {}: {}",
                            sendMessage.getSender(), sendMessage.getRecipient(), sendMessage.getContent());
                })
                .matchEquals("startChat", msg -> {
                    // Logic to initiate chat with another client
                    String recipient = (String) msg;
                    ActorRef recipientActor = serverActor;
                    getContext().actorOf(ChatSessionActor.props(getSelf(), recipientActor));
                })
                .build();
    }
}
