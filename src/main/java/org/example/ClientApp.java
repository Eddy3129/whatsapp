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
            int choice = scanner.nextInt();
            scanner.nextLine(); // consume newline

            if (choice == 1) {
                serverActor.tell(new ServerActor.FindClients(), clientActor);
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
}