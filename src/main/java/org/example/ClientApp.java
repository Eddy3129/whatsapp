package org.example;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

import java.util.Scanner;

public class ClientApp {
    private static String name;  // Store the name globally once entered
    private static ActorRef serverActor;
    private static ActorRef clientActor;
    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        // Initial login (only once)
        System.out.print("Enter your name: ");
        name = scanner.nextLine();  // Store name after initial login

        ActorSystem system = ActorSystem.create("ChatClient");
        try {
            serverActor = system.actorSelection("akka://ChatServer@127.0.0.1:25520/user/serverActor")
                    .resolveOne(java.time.Duration.ofSeconds(5)).toCompletableFuture().join();
            clientActor = system.actorOf(ClientActor.props(serverActor, name), "clientActor");

            // Start listening for messages in a continuous loop
            while (true) {
                showMenu();
                int choice = scanner.nextInt();
                scanner.nextLine(); // Consume newline

                switch (choice) {
                    case 1:  // Find clients
                        serverActor.tell(new ServerActor.FindClients(), clientActor);
                        break;
                    case 2:  // Send message
                        sendMessage();
                        break;
                    case 3:  // Exit
                        system.terminate();
                        return;
                    default:
                        System.out.println("Invalid choice, try again.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Display menu options
    private static void showMenu() {
        System.out.println("\nOptions: ");
        System.out.println("[1] Find Clients");
        System.out.println("[2] Send Message");
        System.out.println("[3] Exit");
        System.out.print("Enter choice: ");
    }

    // Handle sending a message
    private static void sendMessage() {
        System.out.print("Enter recipient name: ");
        String recipient = scanner.nextLine();
        System.out.print("Enter message: ");
        String message = scanner.nextLine();

        serverActor.tell(new ServerActor.SendMessage(name, recipient, message), clientActor);
    }

    // Handle opening a communication channel
    public static void openChatSession(ActorRef recipient) {
        System.out.println("You are now chatting with " + recipient.path().name());
        while (true) {
            System.out.println("Type 'exit' to return to the menu.");
            System.out.print("Your message: ");
            String message = scanner.nextLine();
            if (message.equalsIgnoreCase("exit")) {
                break;
            }
            recipient.tell(new ServerActor.SendMessage(name, recipient.path().name(), message), clientActor);
        }
    }
}
