// ClientApp.java
package org.example;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletionStage;

public class ClientApp {
    private final ActorSystem system;
    private final ActorRef clientActor;
    private final ActorRef serverActor;
    private final ChatUI chatUI;
    private final String username;

    public ClientApp(String username) {
        this.username = username;
        this.system = ActorSystem.create("ChatClient");
        this.chatUI = new ChatUI(username);

        // Connect to server
        CompletionStage<ActorRef> serverFuture = system.actorSelection("akka://ChatServer@127.0.0.1:25520/user/serverActor")
                .resolveOne(java.time.Duration.ofSeconds(5));

        this.serverActor = serverFuture.toCompletableFuture().join();
        this.clientActor = system.actorOf(ClientActor.props(serverActor, username, chatUI), "clientActor");
    }

    public void start() {
        chatUI.start();
        processUserInput();
    }

    private void processUserInput() {
        while (true) {
            String input = chatUI.readLine().trim();

            if (chatUI.isInChatMode()) {
                handleChatModeInput(input);
            } else {
                handleMainMenuInput(input);
            }
        }
    }

    private void handleChatModeInput(String input) {
        if ("/exit".equalsIgnoreCase(input)) {
            chatUI.exitChatMode();
            return;
        }

        if (!input.isEmpty()) {
            serverActor.tell(
                    new ServerActor.SendMessage(username, chatUI.getCurrentChatPartner(), input),
                    clientActor
            );
        }
    }

    private void handleMainMenuInput(String input) {
        try {
            int choice = Integer.parseInt(input);
            switch (choice) {
                case 1:
                    // Show online users
                    serverActor.tell(new ServerActor.FindClients(username), clientActor);
                    break;

                case 2:
                    // Start chat
                    System.out.print("Enter username to chat with: ");
                    String chatPartner = chatUI.readLine().trim();

                    // Get chat history and enter chat mode
                    serverActor.tell(new ServerActor.GetChatHistory(username, chatPartner), clientActor);
                    chatUI.enterChatMode(chatPartner);
                    break;

                case 3:
                    // Exit
                    system.terminate();
                    System.exit(0);
                    break;

                default:
                    chatUI.displayError("Invalid option. Please try again.");
                    chatUI.displayMainMenu();
            }
        } catch (NumberFormatException e) {
            chatUI.displayError("Please enter a number.");
            chatUI.displayMainMenu();
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter your username: ");
        String username = scanner.nextLine().trim();

        ClientApp client = new ClientApp(username);
        client.start();
    }
}