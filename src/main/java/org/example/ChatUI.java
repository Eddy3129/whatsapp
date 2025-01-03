package org.example;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ChatUI {
    private final String username;
    private final BlockingQueue<String> messageQueue;
    private volatile boolean isInChatMode = false;
    private String currentChatPartner = null;
    private final Scanner scanner;

    private static final String CLEAR_CONSOLE = "\033[H\033[2J";
    private static final String SYSTEM_COLOR = "\u001B[33m";  // Yellow
    private static final String ERROR_COLOR = "\u001B[31m";   // Red
    private static final String HEADER_COLOR = "\u001B[36m";  // Cyan
    private static final String SENDER_COLOR = "\u001B[32m";  // Green
    private static final String TIMESTAMP_COLOR = "\u001B[90m"; // Gray
    private static final String RESET_COLOR = "\u001B[0m";
    private static final String DIVIDER = createDivider('─', 50);
    private static final String DOUBLE_DIVIDER = createDivider('═', 50);

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
        System.out.println("3. 🚪 Exit");
        System.out.println(DIVIDER);
        System.out.print("\nSelect an option: ");
    }

    public void displayMessage(Message message) {
        String timestamp = TIMESTAMP_COLOR + "[" + message.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "]" + RESET_COLOR;
        String sender = message.getSender().equals(username) ?
                "You" :
                SENDER_COLOR + message.getSender() + RESET_COLOR;
        String formattedMessage = String.format("%s %s: %s",
                timestamp,
                sender,
                message.getContent()
        );
        messageQueue.offer(formattedMessage);
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