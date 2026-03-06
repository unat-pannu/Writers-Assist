package com.writersassist.lab7;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface ScriptService extends Remote {
    void saveScript(String title, String content) throws RemoteException;

    String loadScript(String title) throws RemoteException;

    List<String> listScripts() throws RemoteException;
}
