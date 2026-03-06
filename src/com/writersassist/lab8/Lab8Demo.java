package com.writersassist.lab8;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Lab 8: Comprehensive RMI, Callback, and JavaBean Demonstration
 */
public class Lab8Demo {

    public static void main(String[] args) {
        System.out.println("=== Lab 8: Advanced Java Networking & Beans Demo ===");

        try {
            // 1. DEMONSTRATING JAVABEANS
            System.out.println("\n[1] Demonstrating JavaBean (CharacterBean)");
            CharacterBean hero = new CharacterBean();

            // Using public setter methods to modify private data members
            hero.setName("Aria Vance");
            hero.setArchetype("Rebel Leader");
            hero.setMotivation("Liberty");
            hero.setDescription("A fierce strategist in the underground resistance.");

            // Using public getter methods to retrieve data
            System.out.println("Character Created: " + hero.getName() + " (" + hero.getArchetype() + ")");
            System.out.println("Motivation: " + hero.getMotivation());

            // 2. SETTING UP RMI SERVER
            System.out.println("\n[2] Setting up RMI Server...");
            ScriptCollaborationServer server = new ScriptCollaborationServer();
            Registry registry = LocateRegistry.createRegistry(1888); // Using unique port for demo
            registry.rebind("CollabService", server);
            System.out.println("RMI Server bound to 'CollabService' on port 1888.");

            // 3. SETTING UP RMI CLIENTS WITH CALLBACKS
            System.out.println("\n[3] Setting up RMI Clients with Callbacks...");

            // Client 1
            CollaborationService service = (CollaborationService) registry.lookup("CollabService");

            ScriptCollaborationClient client1 = new ScriptCollaborationClient(msg -> {
                System.out.println(">>> Client 1 Received Update: " + msg);
            });
            service.registerClient("Writer_One", client1);

            // Client 2
            ScriptCollaborationClient client2 = new ScriptCollaborationClient(msg -> {
                System.out.println(">>> Client 2 Received Update: " + msg);
            });
            service.registerClient("Writer_Two", client2);

            // 4. DEMONSTRATING REAL-TIME BROADCAST (RMI CALLBACK)
            System.out.println("\n[4] Demonstrating RMI Callback (Broadcast)...");
            System.out.println("Writer_One is sending a message...");
            service.broadcastUpdate("Aria Vance just entered the Scene Heading!");

            System.out.println(
                    "\nCheck the console output above to see how Client 1 and Client 2 received the message via Callbacks.");

            // Shutdown demo after a brief delay
            Thread.sleep(2000);
            System.out.println("\nDemo completed. Shutting down...");
            System.exit(0);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
