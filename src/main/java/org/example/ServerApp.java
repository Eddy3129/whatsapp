package org.example;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

public class ServerApp {
    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create("ChatServer");
        ActorRef serverActor = system.actorOf(ServerActor.props(), "serverActor");
        System.out.println("Server started. Waiting for clients...");
    }
}