// ClientApp.java
package org.example;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

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

            if (input.isEmpty()) {
                continue;
            }

            if (input.startsWith("/")) {
                handleCommand(input);
            } else {
                if (chatUI.isInChatMode()) {
                    serverActor.tell(
                            new ServerActor.SendMessage(username, chatUI.getCurrentChatPartner(), input, Message.MessageType.DIRECT, null),
                            clientActor
                    );
                } else if (chatUI.isInGroupChat()) {
                    serverActor.tell(
                            new ServerActor.SendMessage(username, null, input, Message.MessageType.GROUP, chatUI.getCurrentGroup()),
                            clientActor
                    );
                } else {
                    handleMainMenuInput(input);
                }
            }
        }
    }

    private void handleCommand(String input) {
        String[] parts = input.split("\\s+", 2);
        String command = parts[0].toLowerCase();
        String args = parts.length > 1 ? parts[1] : "";

        switch (command) {
            case "/exit":
                if (chatUI.isInChatMode() || chatUI.isInGroupChat()) {
                    chatUI.exitChatMode();
                    chatUI.exitGroupChatMode();
                } else {
                    system.terminate();
                    System.exit(0);
                }
                break;

            case "/create":
                if (!args.isEmpty()) {
                    serverActor.tell(new ServerActor.CreateGroup(args, username), clientActor);
                } else {
                    chatUI.displayError("Usage: /create <group_name>");
                }
                break;

            case "/join":
                if (!args.isEmpty()) {
                    serverActor.tell(new ServerActor.JoinGroup(args, username), clientActor);
                } else {
                    chatUI.displayError("Usage: /join <group_name>");
                }
                break;

            case "/leave":
                if (chatUI.isInGroupChat()) {
                    serverActor.tell(new ServerActor.LeaveGroup(chatUI.getCurrentGroup(), username), clientActor);
                } else {
                    chatUI.displayError("You must be in a group chat to use this command");
                }
                break;

            case "/disband":
                if (chatUI.isInGroupChat()) {
                    serverActor.tell(new ServerActor.DisbandGroup(chatUI.getCurrentGroup(), username), clientActor);
                } else {
                    chatUI.displayError("You must be in a group chat to use this command");
                }
                break;

            case "/invite":
                if (chatUI.isInGroupChat() && !args.isEmpty()) {
                    serverActor.tell(new ServerActor.InviteToGroup(chatUI.getCurrentGroup(), username, args), clientActor);
                } else {
                    chatUI.displayError("Usage: /invite <username> (must be in a group chat)");
                }
                break;

            case "/members":
                if (chatUI.isInGroupChat()) {
                    serverActor.tell(new ServerActor.GetGroupInfo(chatUI.getCurrentGroup(), username), clientActor);
                } else {
                    chatUI.displayError("You must be in a group chat to use this command");
                }
                break;

            case "/groups":
                serverActor.tell(new ServerActor.GetGroupList(username), clientActor);
                break;

            case "/help":
                displayHelp();
                break;

            default:
                chatUI.displayError("Unknown command. Type /help for available commands");
        }
    }

    private void handleMainMenuInput(String input) {
        try {
            int choice = Integer.parseInt(input);
            switch (choice) {
                case 1:
                    serverActor.tell(new ServerActor.FindClients(username), clientActor);
                    break;

                case 2:
                    System.out.print("Enter username to chat with: ");
                    String chatPartner = chatUI.readLine().trim();
                    serverActor.tell(new ServerActor.GetChatHistory(username, chatPartner), clientActor);
                    chatUI.enterChatMode(chatPartner);
                    break;

                case 3:
                    chatUI.displayGroupCreationMenu();
                    String groupName = chatUI.readLine().trim();
                    if (!groupName.isEmpty()) {
                        serverActor.tell(new ServerActor.CreateGroup(groupName, username), clientActor);
                    }
                    break;

                case 4:
                    serverActor.tell(new ServerActor.GetGroupList(username), clientActor);
                    break;

                case 5:
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

    private void displayHelp() {
        StringBuilder help = new StringBuilder("\nAvailable Commands:\n");
        help.append("/exit - Exit chat or application\n");
        help.append("/create <group_name> - Create a new group\n");
        help.append("/join <group_name> - Join a group (requires invitation)\n");
        help.append("/leave - Leave current group\n");
        help.append("/disband - Disband group (admin only)\n");
        help.append("/invite <username> - Invite user to current group\n");
        help.append("/members - List group members\n");
        help.append("/groups - List available groups\n");
        help.append("/help - Show this help message\n");
        chatUI.displaySystemMessage(help.toString());
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter your username: ");
        String username = scanner.nextLine().trim();

        if (username.isEmpty()) {
            System.out.println("Username cannot be empty!");
            return;
        }

        try {
            ClientApp client = new ClientApp(username);
            client.start();
        } catch (Exception e) {
            System.out.println("Failed to connect to server: " + e.getMessage());
        }
    }
}