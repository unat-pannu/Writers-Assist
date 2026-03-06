package com.writersassist.lab8;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface CollaborationService extends Remote {
    void registerClient(String username, ScriptUpdateCallback client) throws RemoteException;

    void broadcastUpdate(String message) throws RemoteException;
}
