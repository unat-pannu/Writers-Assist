package com.writersassist.lab8;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ScriptUpdateCallback extends Remote {
    void onScriptUpdated(String message) throws RemoteException;
}
