package org.example;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.io.Serializable;
import java.util.*;

public class ServerActor extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private final Map<String, ActorRef> clients = new HashMap<>();
    private final Map<String, List<Message>> messageHistory = new HashMap<>();

    public static Props props() {
        return Props.create(ServerActor.class, ServerActor::new);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(RegisterClient.class, this::handleRegistration)
                .match(FindClients.class, this::handleFindClients)
                .match(SendMessage.class, this::handleSendMessage)
                .match(GetChatHistory.class, this::handleGetChatHistory)
                .build();
    }

    private void handleRegistration(RegisterClient register) {
        clients.put(register.getName(), getSender());
        messageHistory.put(register.getName(), new ArrayList<>());
        log.info("Client registered: {}", register.getName());
        getSender().tell(new RegistrationSuccess(register.getName()), getSelf());
    }

    private void handleFindClients(FindClients find) {
        List<String> availableClients = new ArrayList<>();
        for (String client : clients.keySet()) {
            if (!client.equals(find.getRequesterName())) {
                availableClients.add(client);
            }
        }
        getSender().tell(new ClientList(availableClients), getSelf());
    }

    private void handleSendMessage(SendMessage sendMsg) {
        Message message = new Message(sendMsg.getSender(), sendMsg.getContent());
        ActorRef recipient = clients.get(sendMsg.getRecipient());

        if (recipient != null) {
            // Store message in history for both sender and recipient
            String chatId = getChatId(sendMsg.getSender(), sendMsg.getRecipient());
            messageHistory.computeIfAbsent(chatId, k -> new ArrayList<>()).add(message);

            // Send to recipient
            recipient.tell(message, getSelf());
        } else {
            getSender().tell(new ErrorMessage("Recipient not found"), getSelf());
        }
    }

    private void handleGetChatHistory(GetChatHistory request) {
        String chatId = getChatId(request.getUser1(), request.getUser2());
        List<Message> history = messageHistory.getOrDefault(chatId, new ArrayList<>());
        getSender().tell(new ChatHistory(history), getSelf());
    }

    private String getChatId(String user1, String user2) {
        return user1.compareTo(user2) < 0 ?
                user1 + ":" + user2 :
                user2 + ":" + user1;
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

        public ClientList(List<String> clients) {
            this.clients = new ArrayList<>(clients);
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

    public static class GetChatHistory implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String user1;
        private final String user2;

        public GetChatHistory(String user1, String user2) {
            this.user1 = user1;
            this.user2 = user2;
        }

        public String getUser1() {
            return user1;
        }

        public String getUser2() {
            return user2;
        }
    }

    public static class ChatHistory implements Serializable {
        private static final long serialVersionUID = 1L;
        private final List<Message> messages;

        public ChatHistory(List<Message> messages) {
            this.messages = new ArrayList<>(messages);
        }

        public List<Message> getMessages() {
            return messages;
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
}