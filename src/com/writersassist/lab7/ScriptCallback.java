package com.writersassist.lab7;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ScriptCallback extends Remote {
    void onScriptUpdated(String scriptId, String message) throws RemoteException;

    void onUserActivity(String message) throws RemoteException;
}
