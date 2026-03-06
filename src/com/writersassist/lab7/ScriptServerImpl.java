package com.writersassist.lab7;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScriptServerImpl extends UnicastRemoteObject implements ScriptService {
    private Map<String, String> scriptStorage = new HashMap<>();

    public ScriptServerImpl() throws RemoteException {
        super();
    }

    @Override
    public synchronized void saveScript(String title, String content) throws RemoteException {
        scriptStorage.put(title, content);
        System.out.println("Saved script to cloud: " + title);
    }

    @Override
    public synchronized String loadScript(String title) throws RemoteException {
        return scriptStorage.getOrDefault(title, "");
    }

    @Override
    public synchronized List<String> listScripts() throws RemoteException {
        return new ArrayList<>(scriptStorage.keySet());
    }
}
