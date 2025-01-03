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
    private final Map<String, Group> groups = new HashMap<>();

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
                .match(CreateGroup.class, this::handleCreateGroup)
                .match(InviteToGroup.class, this::handleGroupInvite)
                .match(JoinGroup.class, this::handleJoinGroup)
                .match(GetGroupList.class, this::handleGetGroupList)
                .match(GetGroupInfo.class, this::handleGetGroupInfo)
                .match(LeaveGroup.class, this::handleLeaveGroup)
                .match(DisbandGroup.class, this::handleDisbandGroup)
                .build();
    }

    private void handleRegistration(RegisterClient register) {
        if (clients.containsKey(register.getName())) {
            getSender().tell(new ErrorMessage("Username already taken"), getSelf());
            return;
        }

        clients.put(register.getName(), getSender());
        messageHistory.put(register.getName(), new ArrayList<>());
        log.info("Client registered: {}", register.getName());
        getSender().tell(new RegistrationSuccess(register.getName()), getSelf());

        // Notify all clients about new user
        broadcastSystemMessage(String.format("%s has joined the chat", register.getName()));
    }

    private void handleFindClients(FindClients find) {
        List<String> availableClients = new ArrayList<>(clients.keySet());
        availableClients.remove(find.getRequesterName());
        getSender().tell(new ClientList(availableClients), getSelf());
    }

    private void handleSendMessage(SendMessage sendMsg) {
        Message message = new Message(sendMsg.getSender(), sendMsg.getContent(),
                sendMsg.getType(), sendMsg.getTargetGroup());

        switch (message.getType()) {
            case GROUP:
                handleGroupMessage(message);
                break;
            case DIRECT:
                handleDirectMessage(message, sendMsg.getRecipient());
                break;
            case SYSTEM:
                broadcastSystemMessage(message.getContent());
                break;
        }
    }

    private void handleGroupMessage(Message message) {
        Group group = groups.get(message.getTargetGroup());
        if (group == null || !group.isMember(message.getSender())) {
            getSender().tell(new ErrorMessage("Cannot send message to this group"), getSelf());
            return;
        }

        storeGroupMessage(message.getTargetGroup(), message);
        broadcastToGroup(group, message);

        // Send confirmation to sender
        getSender().tell(message, getSelf());
    }

    private void handleDirectMessage(Message message, String recipient) {
        ActorRef recipientActor = clients.get(recipient);
        if (recipientActor == null) {
            getSender().tell(new ErrorMessage("Recipient not found"), getSelf());
            return;
        }

        String chatId = getChatId(message.getSender(), recipient);
        messageHistory.computeIfAbsent(chatId, k -> new ArrayList<>()).add(message);

        // Send to recipient
        recipientActor.tell(message, getSelf());
        // Send confirmation to sender
        getSender().tell(message, getSelf());
    }

    public static class CreateGroup implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String groupName;
        private final String admin;

        public CreateGroup(String groupName, String admin) {
            this.groupName = groupName;
            this.admin = admin;
        }

        public String getGroupName() { return groupName; }
        public String getAdmin() { return admin; }
    }

    private void handleCreateGroup(CreateGroup cmd) {
        if (groups.containsKey(cmd.getGroupName())) {
            getSender().tell(new ErrorMessage("Group already exists"), getSelf());
            return;
        }

        Group group = new Group(cmd.getGroupName(), cmd.getAdmin());
        groups.put(cmd.getGroupName(), group);

        String message = String.format("Group '%s' created by %s", cmd.getGroupName(), cmd.getAdmin());
        Message systemMsg = new Message("SYSTEM", message, Message.MessageType.SYSTEM, cmd.getGroupName());
        storeGroupMessage(cmd.getGroupName(), systemMsg);

        getSender().tell(new GroupCreated(group), getSelf());
        broadcastSystemMessage(message);
    }

    public static class InviteToGroup implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String groupName;
        private final String inviter;
        private final String invitee;

        public InviteToGroup(String groupName, String inviter, String invitee) {
            this.groupName = groupName;
            this.inviter = inviter;
            this.invitee = invitee;
        }

        public String getGroupName() { return groupName; }
        public String getInviter() { return inviter; }
        public String getInvitee() { return invitee; }
    }

    private void handleGroupInvite(InviteToGroup cmd) {
        Group group = groups.get(cmd.getGroupName());
        if (group == null) {
            getSender().tell(new ErrorMessage("Group not found"), getSelf());
            return;
        }

        if (!group.isMember(cmd.getInviter())) {
            getSender().tell(new ErrorMessage("You are not a member of this group"), getSelf());
            return;
        }

        if (group.isMember(cmd.getInvitee())) {
            getSender().tell(new ErrorMessage("User is already a member"), getSelf());
            return;
        }

        group.addInvite(cmd.getInvitee());
        ActorRef invitee = clients.get(cmd.getInvitee());
        if (invitee != null) {
            invitee.tell(new GroupInvitation(cmd.getGroupName(), cmd.getInviter()), getSelf());
            getSender().tell(new SystemMessage(String.format("Invitation sent to %s", cmd.getInvitee())), getSelf());
        }
    }

    public static class JoinGroup implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String groupName;
        private final String username;

        public JoinGroup(String groupName, String username) {
            this.groupName = groupName;
            this.username = username;
        }

        public String getGroupName() { return groupName; }
        public String getUsername() { return username; }
    }


    private void handleJoinGroup(JoinGroup cmd) {
        Group group = groups.get(cmd.getGroupName());
        if (group == null) {
            getSender().tell(new ErrorMessage("Group not found"), getSelf());
            return;
        }

        if (!group.hasPendingInvite(cmd.getUsername()) && !group.isMember(cmd.getUsername())) {
            getSender().tell(new ErrorMessage("No pending invitation found"), getSelf());
            return;
        }

        group.addMember(cmd.getUsername());

        String joinMessage = String.format("%s joined the group", cmd.getUsername());
        Message systemMsg = new Message("SYSTEM", joinMessage, Message.MessageType.SYSTEM, cmd.getGroupName());
        broadcastToGroup(group, systemMsg);
        storeGroupMessage(cmd.getGroupName(), systemMsg);

        // Send group info to new member
        getSender().tell(new JoinedGroup(group), getSelf());

        // Send chat history to new member
        String historyKey = "group:" + cmd.getGroupName();
        List<Message> history = messageHistory.getOrDefault(historyKey, new ArrayList<>());
        getSender().tell(new GroupChatHistory(group, history), getSelf());
    }

    private void handleLeaveGroup(LeaveGroup cmd) {
        Group group = groups.get(cmd.getGroupName());
        if (group == null) {
            getSender().tell(new ErrorMessage("Group not found"), getSelf());
            return;
        }

        if (!group.isMember(cmd.getUsername())) {
            getSender().tell(new ErrorMessage("You are not a member of this group"), getSelf());
            return;
        }

        if (group.getAdmin().equals(cmd.getUsername())) {
            getSender().tell(new ErrorMessage("Admin cannot leave the group. Use /disband to delete the group."), getSelf());
            return;
        }

        group.removeMember(cmd.getUsername());
        String leaveMessage = String.format("%s left the group", cmd.getUsername());
        Message systemMsg = new Message("SYSTEM", leaveMessage, Message.MessageType.SYSTEM, cmd.getGroupName());
        broadcastToGroup(group, systemMsg);
        storeGroupMessage(cmd.getGroupName(), systemMsg);

        getSender().tell(new LeftGroup(cmd.getGroupName()), getSelf());
    }

    private void handleDisbandGroup(DisbandGroup cmd) {
        Group group = groups.get(cmd.getGroupName());
        if (group == null) {
            getSender().tell(new ErrorMessage("Group not found"), getSelf());
            return;
        }

        if (!group.getAdmin().equals(cmd.getUsername())) {
            getSender().tell(new ErrorMessage("Only the admin can disband the group"), getSelf());
            return;
        }

        // Notify all members
        String disbandMessage = String.format("Group '%s' has been disbanded by admin", cmd.getGroupName());
        Message systemMsg = new Message("SYSTEM", disbandMessage, Message.MessageType.SYSTEM, cmd.getGroupName());
        broadcastToGroup(group, systemMsg);

        // Remove group
        groups.remove(cmd.getGroupName());
        messageHistory.remove("group:" + cmd.getGroupName());

        getSender().tell(new GroupDisbanded(cmd.getGroupName()), getSelf());
    }

    public static class GetGroupInfo implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String groupName;
        private final String username;

        public GetGroupInfo(String groupName, String username) {
            this.groupName = groupName;
            this.username = username;
        }

        public String getGroupName() { return groupName; }
        public String getUsername() { return username; }
    }

    public static class GetGroupList implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String username;

        public GetGroupList(String username) {
            this.username = username;
        }

        public String getUsername() { return username; }
    }

    private void handleGetGroupList(GetGroupList cmd) {
        List<GroupInfo> groupInfos = new ArrayList<>();
        for (Group group : groups.values()) {
            boolean isMember = group.isMember(cmd.getUsername());
            boolean hasInvite = group.hasPendingInvite(cmd.getUsername());
            if (isMember || hasInvite) {
                groupInfos.add(new GroupInfo(group.getName(), group.getAdmin(),
                        group.getMembers().size(), isMember, hasInvite));
            }
        }
        getSender().tell(new GroupList(groupInfos), getSelf());
    }

    private void handleGetGroupInfo(GetGroupInfo cmd) {
        Group group = groups.get(cmd.getGroupName());
        if (group == null) {
            getSender().tell(new ErrorMessage("Group not found"), getSelf());
            return;
        }

        if (!group.isMember(cmd.getUsername())) {
            getSender().tell(new ErrorMessage("You are not a member of this group"), getSelf());
            return;
        }

        String historyKey = "group:" + cmd.getGroupName();
        List<Message> history = messageHistory.getOrDefault(historyKey, new ArrayList<>());
        getSender().tell(new GroupChatHistory(group, history), getSelf());
    }

    private void handleGetChatHistory(GetChatHistory request) {
        if (request.isGroupChat()) {
            String historyKey = "group:" + request.getGroupName();
            List<Message> history = messageHistory.getOrDefault(historyKey, new ArrayList<>());
            getSender().tell(new ChatHistory(history), getSelf());
        } else {
            String chatId = getChatId(request.getUser1(), request.getUser2());
            List<Message> history = messageHistory.getOrDefault(chatId, new ArrayList<>());
            getSender().tell(new ChatHistory(history), getSelf());
        }
    }

    private void storeGroupMessage(String groupName, Message message) {
        String historyKey = "group:" + groupName;
        messageHistory.computeIfAbsent(historyKey, k -> new ArrayList<>()).add(message);
    }

    private void broadcastToGroup(Group group, Message message) {
        for (String member : group.getMembers()) {
            // Skip sending the message back to the sender
            if (member.equals(message.getSender())) {
                continue;
            }

            ActorRef memberActor = clients.get(member);
            if (memberActor != null) {
                memberActor.tell(message, getSelf());
            }
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

    private void broadcastSystemMessage(String content) {
        Message systemMsg = new Message("SYSTEM", content, Message.MessageType.SYSTEM, null);
        for (ActorRef client : clients.values()) {
            client.tell(systemMsg, getSelf());
        }
    }

    private String getChatId(String user1, String user2) {
        return user1.compareTo(user2) < 0 ?
                user1 + ":" + user2 :
                user2 + ":" + user1;
    }

    // Additional Message Classes
    public static class LeaveGroup implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String groupName;
        private final String username;

        public LeaveGroup(String groupName, String username) {
            this.groupName = groupName;
            this.username = username;
        }

        public String getGroupName() { return groupName; }
        public String getUsername() { return username; }
    }

    public static class LeftGroup implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String groupName;

        public LeftGroup(String groupName) {
            this.groupName = groupName;
        }

        public String getGroupName() { return groupName; }
    }

    public static class DisbandGroup implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String groupName;
        private final String username;

        public DisbandGroup(String groupName, String username) {
            this.groupName = groupName;
            this.username = username;
        }

        public String getGroupName() { return groupName; }
        public String getUsername() { return username; }
    }

    public static class GroupDisbanded implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String groupName;

        public GroupDisbanded(String groupName) {
            this.groupName = groupName;
        }

        public String getGroupName() { return groupName; }
    }

    public static class SystemMessage implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String message;

        public SystemMessage(String message) {
            this.message = message;
        }

        public String getMessage() { return message; }
    }

    public static class RegistrationSuccess implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String name;

        public RegistrationSuccess(String name) {
            this.name = name;
        }

        public String getName() { return name; }
    }

    public static class ClientList implements Serializable {
        private static final long serialVersionUID = 1L;
        private final List<String> clients;

        public ClientList(List<String> clients) {
            this.clients = new ArrayList<>(clients);
        }

        public List<String> getClients() { return clients; }
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

    // Note: This is the only GetChatHistory class that should be in your code
    public static class GetChatHistory implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String user1;
        private final String user2;
        private final String groupName;
        private final boolean isGroupChat;

        // Constructor for direct messages
        public GetChatHistory(String user1, String user2) {
            this.user1 = user1;
            this.user2 = user2;
            this.groupName = null;
            this.isGroupChat = false;
        }

        // Constructor for group messages
        public GetChatHistory(String groupName) {
            this.user1 = null;
            this.user2 = null;
            this.groupName = groupName;
            this.isGroupChat = true;
        }

        public String getUser1() { return user1; }
        public String getUser2() { return user2; }
        public String getGroupName() { return groupName; }
        public boolean isGroupChat() { return isGroupChat; }
    }

    public static class SendMessage implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String sender;
        private final String recipient;
        private final String content;
        private final Message.MessageType type;
        private final String targetGroup;

        public SendMessage(String sender, String recipient, String content, Message.MessageType type, String targetGroup) {
            this.sender = sender;
            this.recipient = recipient;
            this.content = content;
            this.type = type;
            this.targetGroup = targetGroup;
        }

        public String getSender() { return sender; }
        public String getRecipient() { return recipient; }
        public String getContent() { return content; }
        public Message.MessageType getType() { return type; }
        public String getTargetGroup() { return targetGroup; }
    }

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

    public static class GroupInfo implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String name;
        private final String admin;
        private final int memberCount;
        private final boolean isMember;
        private final boolean hasInvite;

        public GroupInfo(String name, String admin, int memberCount,
                         boolean isMember, boolean hasInvite) {
            this.name = name;
            this.admin = admin;
            this.memberCount = memberCount;
            this.isMember = isMember;
            this.hasInvite = hasInvite;
        }

        public String getName() { return name; }
        public String getAdmin() { return admin; }
        public int getMemberCount() { return memberCount; }
        public boolean isMember() { return isMember; }
        public boolean hasInvite() { return hasInvite; }
    }

    // 3. Missing Message Classes in ServerActor
    public static class GroupList implements Serializable {
        private static final long serialVersionUID = 1L;
        private final List<GroupInfo> groups;

        public GroupList(List<GroupInfo> groups) {
            this.groups = new ArrayList<>(groups);
        }

        public List<GroupInfo> getGroups() { return groups; }
    }

    public static class GroupCreated implements Serializable {
        private static final long serialVersionUID = 1L;
        private final Group group;

        public GroupCreated(Group group) {
            this.group = group;
        }

        public Group getGroup() { return group; }
    }

    public static class GroupChatHistory implements Serializable {
        private static final long serialVersionUID = 1L;
        private final Group group;
        private final List<Message> messages;

        public GroupChatHistory(Group group, List<Message> messages) {
            this.group = group;
            this.messages = new ArrayList<>(messages);
        }

        public Group getGroup() { return group; }
        public List<Message> getMessages() { return messages; }
    }

    public static class GroupInvitation implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String groupName;
        private final String inviter;

        public GroupInvitation(String groupName, String inviter) {
            this.groupName = groupName;
            this.inviter = inviter;
        }

        public String getGroupName() { return groupName; }
        public String getInviter() { return inviter; }
    }

    public static class JoinedGroup implements Serializable {
        private static final long serialVersionUID = 1L;
        private final Group group;

        public JoinedGroup(Group group) {
            this.group = group;
        }

        public Group getGroup() { return group; }
    }


}
