package org.example;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

import java.util.Scanner;

public class ClientApp {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter your name: ");
        String name = scanner.nextLine();

        ActorSystem system = ActorSystem.create("ChatClient");
        ActorRef serverActor = system.actorSelection("akka://ChatServer@127.0.0.1:25520/user/serverActor")
                .resolveOne(java.time.Duration.ofSeconds(5)).toCompletableFuture().join();

        ActorRef clientActor = system.actorOf(ClientActor.props(serverActor, name), "clientActor");

        while (true) {
            System.out.println("Options: [1] Find Clients [2] Send Message [3] Exit");
            int choice = getUserChoice(scanner);

            if (choice == 1) {
                serverActor.tell(new ServerActor.FindClients(name), clientActor);
            } else if (choice == 2) {
                System.out.print("Enter recipient name: ");
                String recipient = scanner.nextLine();
                System.out.print("Enter message: ");
                String message = scanner.nextLine();
                serverActor.tell(new ServerActor.SendMessage(name, recipient, message), clientActor);
            } else if (choice == 3) {
                system.terminate();
                break;
            }
        }
    }

    // Helper method to get a valid user choice
    private static int getUserChoice(Scanner scanner) {
        while (true) {
            try {
                String input = scanner.nextLine(); // Use nextLine to capture the full input
                int choice = Integer.parseInt(input); // Try to parse the input as an integer

                if (choice >= 1 && choice <= 3) { // Check if the choice is valid
                    return choice;
                } else {
                    System.out.println("Invalid choice. Please select 1, 2, or 3.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number (1, 2, or 3).");
            }
        }
    }
}
