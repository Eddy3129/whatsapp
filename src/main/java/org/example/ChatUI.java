package org.example;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ChatUI {
    private final String username;
    private final BlockingQueue<String> messageQueue;
    private volatile boolean isInChatMode = false;
    private String currentChatPartner = null;
    private final Scanner scanner;
    private final Set<String> chatPartners = new HashSet<>();
    private boolean isInGroupChat = false;
    private String currentGroup = null;

    private static final String CLEAR_CONSOLE = "\033[H\033[2J";
    private static final String SYSTEM_COLOR = "\u001B[33m";  // Yellow
    private static final String ERROR_COLOR = "\u001B[31m";   // Red
    private static final String HEADER_COLOR = "\u001B[36m";  // Cyan
    private static final String SENDER_COLOR = "\u001B[32m";  // Green
    private static final String TIMESTAMP_COLOR = "\u001B[90m"; // Gray
    private static final String NEW_MSG_COLOR = "\u001B[35m";  // Purple
    private static final String RESET_COLOR = "\u001B[0m";
    private static final String DIVIDER = createDivider('─', 50);
    private static final String DOUBLE_DIVIDER = createDivider('═', 50);
    private static final String GROUP_COLOR = "\u001B[34m";

    public ChatUI(String username) {
        this.username = username;
        this.messageQueue = new LinkedBlockingQueue<>();
        this.scanner = new Scanner(System.in);
    }

    private static String createDivider(char c, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(c);
        }
        return sb.toString();
    }

    private static String createPadding(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(' ');
        }
        return sb.toString();
    }

    public void start() {
        new Thread(this::processMessageQueue).start();
        displayWelcomeMessage();
    }

    private void processMessageQueue() {
        while (true) {
            try {
                String message = messageQueue.take();
                if (isInChatMode) {
                    System.out.println("\n" + message);
                    System.out.print("You: ");
                } else {
                    System.out.println(message);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void displayWelcomeMessage() {
        clearScreen();
        System.out.println(HEADER_COLOR + "┌" + DOUBLE_DIVIDER + "┐" + RESET_COLOR);
        System.out.println(HEADER_COLOR + "│" + SYSTEM_COLOR + " Welcome to the Chat System, " + username + "!" +
                createPadding(Math.max(0, 48 - username.length())) + HEADER_COLOR + "│" + RESET_COLOR);
        System.out.println(HEADER_COLOR + "└" + DOUBLE_DIVIDER + "┘" + RESET_COLOR + "\n");
        displayMainMenu();
    }

    public void displayMainMenu() {
        System.out.println(HEADER_COLOR + "MAIN MENU" + RESET_COLOR);
        System.out.println(DIVIDER);
        System.out.println("1. 👥 Show Online Users");
        System.out.println("2. 💬 Start Chat");
        System.out.println("3. 👥 Create Group");
        System.out.println("4. 📱 View Groups");
        System.out.println("5. 🚪 Exit");
        System.out.println(DIVIDER);
        System.out.print("\nSelect an option: ");
    }

    public void displayGroupCreationMenu() {
        System.out.println(HEADER_COLOR + "\nCREATE GROUP" + RESET_COLOR);
        System.out.println(DIVIDER);
        System.out.print("Enter group name: ");
    }

    public void displayGroupList(List<ServerActor.GroupInfo> groups) {
        if (groups.isEmpty()) {
            displaySystemMessage("You are not a member of any groups");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(HEADER_COLOR + "\nYOUR GROUPS" + RESET_COLOR + "\n");
        sb.append(DIVIDER + "\n");

        for (int i = 0; i < groups.size(); i++) {
            ServerActor.GroupInfo group = groups.get(i);
            String status = group.isMember() ? "Member" :
                    group.hasInvite() ? "Invited" : "";

            sb.append(String.format("%d. 👥 %s (%d members) - %s\n",
                    i + 1,
                    group.getName(),
                    group.getMemberCount(),
                    status
            ));
        }

        sb.append(DIVIDER);
        messageQueue.offer(sb.toString());
    }

    // Tweaked method to remove new message bubble for group messages
    public void displayGroupChatHistory(Group group, List<Message> messages) {
        clearScreen();
        System.out.println(HEADER_COLOR + "┌" + DOUBLE_DIVIDER + "┐" + RESET_COLOR);
        System.out.println(HEADER_COLOR + "│" + GROUP_COLOR + " Group Chat: " + group.getName() +
                createPadding(Math.max(0, 38 - group.getName().length())) + HEADER_COLOR + "│" + RESET_COLOR);
        System.out.println(HEADER_COLOR + "└" + DOUBLE_DIVIDER + "┘" + RESET_COLOR + "\n");

        // Display members
        System.out.println(GROUP_COLOR + "Members: " + String.join(", ", group.getMembers()) + RESET_COLOR);
        System.out.println(DIVIDER);

        // Display messages or no-messages prompt
        if (messages.isEmpty()) {
            System.out.println(SYSTEM_COLOR + "No messages in this group yet. Start the conversation!" + RESET_COLOR);
        } else {
            messages.forEach(message -> {
                String timestamp = TIMESTAMP_COLOR + "[" +
                        message.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "]" + RESET_COLOR;

                // Formatting the group messages like system messages, showing sender and group name
                if (message.getType() == Message.MessageType.SYSTEM) {
                    System.out.printf("%s %s%s%s\n",
                            timestamp,
                            SYSTEM_COLOR,
                            message.getContent(),
                            RESET_COLOR);
                } else {
                    String sender = message.getSender().equals(username) ?
                            "You" :
                            SENDER_COLOR + message.getSender() + RESET_COLOR;

                    String groupInfo = GROUP_COLOR + group.getName() + RESET_COLOR;
                    System.out.printf("%s %s (%s): %s\n", timestamp, sender, groupInfo, message.getContent());
                }
            });
        }

        // Display help information prominently
        System.out.println("\n" + HEADER_COLOR + "AVAILABLE COMMANDS" + RESET_COLOR);
        System.out.println(DIVIDER);
        System.out.println("• Type your message and press Enter to send");
        System.out.println("• /invite <username> - Invite someone to the group");
        System.out.println("• /members - View current group members");
        System.out.println("• /leave - Leave the group");
        System.out.println("• /exit - Return to main menu");
        System.out.println("• /help - Show these commands again");
        System.out.println(DIVIDER);
        System.out.print("\nYou: ");
    }

    public void displayGroupInvitation(String groupName, String inviter) {
        String notification = String.format("\n%s ┌─────────────────────────────────┐%s",
                GROUP_COLOR,
                RESET_COLOR);

        String messageLine = String.format("%s │ Group Invitation: %s%s",
                GROUP_COLOR,
                groupName,
                RESET_COLOR);

        String inviterLine = String.format("%s │ From: %s%s",
                GROUP_COLOR,
                inviter,
                RESET_COLOR);

        String commandLine = String.format("%s │ Type '/join %s' to accept%s",
                GROUP_COLOR,
                groupName,
                RESET_COLOR);

        String bottomBorder = String.format("%s └─────────────────────────────────┘%s",
                GROUP_COLOR,
                RESET_COLOR);

        messageQueue.offer(notification);
        messageQueue.offer(messageLine);
        messageQueue.offer(inviterLine);
        messageQueue.offer(commandLine);
        messageQueue.offer(bottomBorder);
    }

    public void enterGroupChatMode(String groupName) {
        isInGroupChat = true;
        currentGroup = groupName;
        isInChatMode = false;
        currentChatPartner = null;
        clearScreen();
        displaySystemMessage("📱 Entering group chat: " + groupName);
    }

    public void exitGroupChatMode() {
        isInGroupChat = false;
        currentGroup = null;
        clearScreen();
        displayMainMenu();
    }

    // Getters for group chat state
    public boolean isInGroupChat() { return isInGroupChat; }
    public String getCurrentGroup() { return currentGroup; }

    public void displayMessage(Message message) {
        // Skip blank messages entirely
        if (message.getContent() == null || message.getContent().trim().isEmpty()) {
            return;
        }

        String timestamp = TIMESTAMP_COLOR + "[" +
                message.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "]" + RESET_COLOR;

        if (message.getType() == Message.MessageType.GROUP) {
            // Handle group messages
            String sender = message.getSender().equals(username) ?
                    "You" :
                    SENDER_COLOR + message.getSender() + RESET_COLOR;

            String formattedMessage = String.format("%s %s (%s): %s",
                    timestamp,
                    sender,
                    GROUP_COLOR + message.getTargetGroup() + RESET_COLOR,
                    message.getContent()
            );

            // Queue the group message
            messageQueue.offer(formattedMessage);

        } else if (!message.getSender().equals(username) &&
                !message.getSender().equals("SYSTEM") &&
                (!isInChatMode || !message.getSender().equals(currentChatPartner)) &&
                !chatPartners.contains(message.getSender())) {

            // Show new message notification only for direct messages
            displayNewMessageNotification(message, timestamp);

        } else {
            // Regular message display for direct messages
            String sender = message.getSender().equals(username) ?
                    "You" :
                    SENDER_COLOR + message.getSender() + RESET_COLOR;

            String formattedMessage = String.format("%s %s: %s",
                    timestamp,
                    sender,
                    message.getContent()
            );

            // Queue the direct message
            messageQueue.offer(formattedMessage);
        }
    }

    private void displayNewMessageNotification(Message message, String timestamp) {
        String notification = String.format("\n%s %s ┌─────────────────────────────────┐%s",
                timestamp,
                NEW_MSG_COLOR,
                RESET_COLOR);

        String messageLine = String.format("%s %s │ New message from %s%s",
                createPadding(11),
                NEW_MSG_COLOR,
                message.getSender(),
                RESET_COLOR);

        String contentLine = String.format("%s %s │ %s%s",
                createPadding(11),
                NEW_MSG_COLOR,
                message.getContent(),
                RESET_COLOR);

        String bottomBorder = String.format("%s %s └─────────────────────────────────┘%s",
                createPadding(11),
                NEW_MSG_COLOR,
                RESET_COLOR);

        messageQueue.offer(notification);
        messageQueue.offer(messageLine);
        messageQueue.offer(contentLine);
        messageQueue.offer(bottomBorder);
    }

    public void displaySystemMessage(String message) {
        messageQueue.offer("\n" + SYSTEM_COLOR + "💬 " + message + RESET_COLOR);
    }

    public void displayError(String error) {
        messageQueue.offer("\n" + ERROR_COLOR + "❌ Error: " + error + RESET_COLOR);
    }

    public void displayAvailableClients(List<String> clients) {
        if (clients.isEmpty()) {
            displaySystemMessage("No other users are currently online");
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(HEADER_COLOR + "\nONLINE USERS" + RESET_COLOR + "\n");
            sb.append(DIVIDER + "\n");
            for (int i = 0; i < clients.size(); i++) {
                sb.append(String.format("%d. 👤 %s\n", i + 1, clients.get(i)));
            }
            sb.append(DIVIDER);
            messageQueue.offer(sb.toString());
        }
    }

    public void displayChatHistory(List<Message> messages) {
        if (currentChatPartner != null) {
            chatPartners.add(currentChatPartner);
        }

        clearScreen();
        System.out.println(HEADER_COLOR + "┌" + DOUBLE_DIVIDER + "┐" + RESET_COLOR);
        System.out.println(HEADER_COLOR + "│" + SYSTEM_COLOR + " Chat with " + currentChatPartner +
                createPadding(Math.max(0, 40 - currentChatPartner.length())) + HEADER_COLOR + "│" + RESET_COLOR);
        System.out.println(HEADER_COLOR + "└" + DOUBLE_DIVIDER + "┘" + RESET_COLOR + "\n");

        if (messages.isEmpty()) {
            System.out.println(SYSTEM_COLOR + "No previous messages. Start your conversation!" + RESET_COLOR);
        } else {
            System.out.println(HEADER_COLOR + "CHAT HISTORY" + RESET_COLOR);
            System.out.println(DIVIDER);
            messages.forEach(message -> {
                String timestamp = TIMESTAMP_COLOR + "[" + message.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "]" + RESET_COLOR;
                String sender = message.getSender().equals(username) ?
                        "You" :
                        SENDER_COLOR + message.getSender() + RESET_COLOR;
                System.out.printf("%s %s: %s\n", timestamp, sender, message.getContent());
            });
            System.out.println(DIVIDER);
        }

        System.out.println("\n" + SYSTEM_COLOR + "Commands:" + RESET_COLOR);
        System.out.println("• Type your message and press Enter to send");
        System.out.println("• Type '/exit' to return to main menu");
        System.out.println("• Type '/clear' to clear chat history");
        System.out.println(DIVIDER);
        System.out.print("\nYou: ");
    }

    public void enterChatMode(String partner) {
        isInChatMode = true;
        currentChatPartner = partner;
        chatPartners.add(partner);
        clearScreen();
        displaySystemMessage("📱 Starting chat with " + partner);
    }

    public void exitChatMode() {
        isInChatMode = false;
        currentChatPartner = null;
        clearScreen();
        displayMainMenu();
    }

    public boolean isInChatMode() {
        return isInChatMode;
    }

    public String getCurrentChatPartner() {
        return currentChatPartner;
    }

    public String readLine() {
        return scanner.nextLine();
    }

    private void clearScreen() {
        System.out.print(CLEAR_CONSOLE);
        System.out.flush();
    }
}
