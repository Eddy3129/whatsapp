package org.example;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.actor.Terminated;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.io.Serializable;
import java.util.*;

public class ServerActor extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private final Map<String, ActorRef> clients = new HashMap<>(); // Registered clients
    private final Map<String, Queue<Message>> offlineMessages = new HashMap<>(); // Offline message queue

    public static Props props() {
        return Props.create(ServerActor.class, ServerActor::new);
    }

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return SupervisorStrategy.stoppingStrategy();  // You can choose stop, restart, etc. here.
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(RegisterClient.class, register -> {
                    log.info("Registering client: {}", register.getName());
                    if (clients.containsKey(register.getName())) {
                        getSender().tell(new ErrorMessage("Client name already taken"), getSelf());
                    } else {
                        clients.put(register.getName(), getSender());
                        getContext().watch(getSender());  // Watch the client for termination
                        getSender().tell(new RegistrationSuccess(register.getName()), getSelf());
                    }
                })
                .match(FindClients.class, find -> {
                    log.info("Responding with client list to {}", getSender());
                    List<String> clientList = new ArrayList<>(clients.keySet());
                    // Exclude self from the client list
                    clientList.remove(getSender().path().name());
                    getSender().tell(new ClientList(clientList), getSelf());
                })
                .match(SendMessage.class, message -> {
                    ActorRef recipient = clients.get(message.getRecipient());
                    if (recipient != null) {
                        log.info("Routing message from {} to {}", message.getSender(), message.getRecipient());
                        recipient.tell(new Message(message.getSender(), message.getContent()), getSelf());
                    } else {
                        log.warning("Recipient {} not found. Storing message for later delivery.", message.getRecipient());
                        // Queue message for offline client
                        offlineMessages.computeIfAbsent(message.getRecipient(), k -> new LinkedList<>())
                                .add(new Message(message.getSender(), message.getContent()));
                        getSender().tell(new ErrorMessage("Recipient not found, message queued"), getSelf());
                    }
                })
                .match(Terminated.class, terminated -> {
                    String disconnectedClient = terminated.getActor().path().name();
                    clients.remove(disconnectedClient);
                    offlineMessages.remove(disconnectedClient);
                    log.info("Client {} disconnected", disconnectedClient);
                })
                .build();
    }

    // Message Classes
    public static class RegisterClient implements Serializable {
        private final String name;

        public RegisterClient(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public static class RegistrationSuccess implements Serializable {
        private final String name;

        public RegistrationSuccess(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public static class FindClients implements Serializable {}

    public static class ClientList implements Serializable {
        private final List<String> clients;

        public ClientList(List<String> clients) {
            this.clients = clients;
        }

        public List<String> getClients() {
            return clients;
        }
    }

    public static class SendMessage implements Serializable {
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
        private final String error;

        public ErrorMessage(String error) {
            this.error = error;
        }

        public String getError() {
            return error;
        }
    }

    public static class Message implements Serializable {
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
