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
                .match(ServerActor.RegistrationSuccess.class, msg -> {
                    log.info("Registered with server as {}", msg.getName());
                    chatUI.displaySystemMessage("Successfully connected to chat server");
                })
                .match(ServerActor.ClientList.class, msg -> {
                    chatUI.displayAvailableClients(msg.getClients());
                })
                .match(Message.class, msg -> {
                    chatUI.displayMessage(msg);
                })
                .match(ServerActor.ChatHistory.class, msg -> {
                    chatUI.displayChatHistory(msg.getMessages());
                })
                .match(ServerActor.GroupCreated.class, msg -> {
                    chatUI.displaySystemMessage("Group created: " + msg.getGroup().getName());
                    chatUI.enterGroupChatMode(msg.getGroup().getName());
                })
                .match(ServerActor.GroupInvitation.class, msg -> {
                    chatUI.displayGroupInvitation(msg.getGroupName(), msg.getInviter());
                })
                .match(ServerActor.JoinedGroup.class, msg -> {
                    chatUI.displaySystemMessage("Successfully joined group: " + msg.getGroup().getName());
                    chatUI.enterGroupChatMode(msg.getGroup().getName());
                })
                .match(ServerActor.GroupList.class, msg -> {
                    chatUI.displayGroupList(msg.getGroups());
                })
                .match(ServerActor.GroupChatHistory.class, msg -> {
                    chatUI.displayGroupChatHistory(msg.getGroup(), msg.getMessages());
                })
                .match(ServerActor.LeftGroup.class, msg -> {
                    chatUI.displaySystemMessage("Left group: " + msg.getGroupName());
                    chatUI.exitGroupChatMode();
                })
                .match(ServerActor.GroupDisbanded.class, msg -> {
                    chatUI.displaySystemMessage("Group disbanded: " + msg.getGroupName());
                    chatUI.exitGroupChatMode();
                })
                .match(ServerActor.SystemMessage.class, msg -> {
                    chatUI.displaySystemMessage(msg.getMessage());
                })
                .match(ServerActor.ErrorMessage.class, msg -> {
                    chatUI.displayError(msg.getError());
                })
                .build();
    }
}
