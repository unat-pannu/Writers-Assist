package com.writersassist.lab8;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.function.Consumer;

public class ScriptCollaborationClient extends UnicastRemoteObject implements ScriptUpdateCallback {

    private Consumer<String> onUpdateConsumer;

    public ScriptCollaborationClient(Consumer<String> onUpdateConsumer) throws RemoteException {
        super();
        this.onUpdateConsumer = onUpdateConsumer;
    }

    @Override
    public void onScriptUpdated(String message) throws RemoteException {
        if (onUpdateConsumer != null) {
            onUpdateConsumer.accept(message);
        }
    }
}
