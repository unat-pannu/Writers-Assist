package com.writersassist.lab7;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import com.writersassist.lab8.ScriptCollaborationServer;

public class ScriptServerLauncher {
    public static void main(String[] args) {
        try {
            // Lab 7 Server
            ScriptServerImpl scriptServer = new ScriptServerImpl();

            // Lab 8 Collaboration Server
            ScriptCollaborationServer collaborationServer = new ScriptCollaborationServer();

            Registry registry = LocateRegistry.createRegistry(1099);
            registry.rebind("ScriptService", scriptServer);
            registry.rebind("CollaborationService", collaborationServer);

            System.out.println("WritersAssist Cloud Server is ready on port 1099.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
