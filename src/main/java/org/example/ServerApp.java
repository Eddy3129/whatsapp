// ServerApp.java
package org.example;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

public class ServerApp {
    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create("ChatServer");
        ActorRef serverActor = system.actorOf(ServerActor.props(), "serverActor");
        System.out.println("Chat server started on port 25520");
        System.out.println("Waiting for clients...");
    }
}