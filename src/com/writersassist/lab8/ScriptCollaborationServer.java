package com.writersassist.lab8;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

public class ScriptCollaborationServer extends UnicastRemoteObject implements CollaborationService {
    private List<ScriptUpdateCallback> clients = new ArrayList<>();

    public ScriptCollaborationServer() throws RemoteException {
        super();
    }

    @Override
    public synchronized void registerClient(String username, ScriptUpdateCallback client) throws RemoteException {
        clients.add(client);
        System.out.println("User connected: " + username);
        broadcastUpdate("System: " + username + " has joined the collaboration session.");
    }

    @Override
    public synchronized void broadcastUpdate(String message) throws RemoteException {
        System.out.println("Broadcasting: " + message);
        List<ScriptUpdateCallback> disconnectedClients = new ArrayList<>();

        for (ScriptUpdateCallback client : clients) {
            try {
                client.onScriptUpdated(message);
            } catch (RemoteException e) {
                disconnectedClients.add(client);
            }
        }

        clients.removeAll(disconnectedClients);
    }
}
