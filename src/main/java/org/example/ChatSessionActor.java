package org.example;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class ChatSessionActor extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private final ActorRef sender;
    private final ActorRef recipient;

    public ChatSessionActor(ActorRef sender, ActorRef recipient) {
        this.sender = sender;
        this.recipient = recipient;
    }

    public static Props props(ActorRef sender, ActorRef recipient) {
        return Props.create(ChatSessionActor.class, () -> new ChatSessionActor(sender, recipient));
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ServerActor.Message.class, message -> {
                    if (message.getSender().equals(sender.path().name())) {
                        System.out.println("You: " + message.getContent());
                    } else {
                        System.out.println(message.getSender() + ": " + message.getContent());
                    }
                })
                .build();
    }
}
