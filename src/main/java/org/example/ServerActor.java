package org.example;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerActor extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private final Map<String, ActorRef> clients = new HashMap<>();

    public static Props props() {
        return Props.create(ServerActor.class, ServerActor::new);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(RegisterClient.class, register -> {
                    clients.put(register.getName(), getSender());
                    log.info("Client registered: {}", register.getName());
                    getSender().tell(new RegistrationSuccess(register.getName()), getSelf());
                })
                .match(FindClients.class, find -> {
                    log.info("Responding with client list to {}", getSender());

                    // Exclude the client making the request
                    List<String> availableClients = new ArrayList<>();
                    for (String client : clients.keySet()) {
                        if (!client.equals(find.getRequesterName())) {
                            availableClients.add(client);
                        }
                    }

                    getSender().tell(new ClientList(availableClients), getSelf());
                })
                .match(SendMessage.class, message -> {
                    ActorRef recipient = clients.get(message.getRecipient());
                    if (recipient != null) {
                        recipient.tell(new Message(message.getSender(), message.getContent()), getSelf());
                    } else {
                        getSender().tell(new ErrorMessage("Recipient not found"), getSelf());
                    }
                })
                .build();
    }

    // Message Classes

    public static class RegisterClient implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String name;

        public RegisterClient(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public static class RegistrationSuccess implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String name;

        public RegistrationSuccess(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public static class FindClients implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String requesterName;

        public FindClients(String requesterName) {
            this.requesterName = requesterName;
        }

        public String getRequesterName() {
            return requesterName;
        }
    }


    public static class ClientList implements Serializable {
        private static final long serialVersionUID = 1L;
        private final List<String> clients;

        public ClientList(Iterable<String> clients) {
            this.clients = new ArrayList<>();
            clients.forEach(this.clients::add); // Convert the Iterable to a List
        }

        public List<String> getClients() {
            return clients;
        }
    }


    public static class SendMessage implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String sender;
        private final String recipient;
        private final String content;

        public SendMessage(String sender, String recipient, String content) {
            this.sender = sender;
            this.recipient = recipient;
            this.content = content;
        }

        public String getSender() {
            return sender;
        }

        public String getRecipient() {
            return recipient;
        }

        public String getContent() {
            return content;
        }
    }

    public static class ErrorMessage implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String error;

        public ErrorMessage(String error) {
            this.error = error;
        }

        public String getError() {
            return error;
        }
    }

    public static class Message implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String sender;
        private final String content;

        public Message(String sender, String content) {
            this.sender = sender;
            this.content = content;
        }

        public String getSender() {
            return sender;
        }

        public String getContent() {
            return content;
        }
    }
}