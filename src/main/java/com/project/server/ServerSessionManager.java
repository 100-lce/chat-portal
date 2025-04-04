package com.project.server;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ServerSessionManager {

    private static final ServerSessionManager INSTANCE = new ServerSessionManager();

    private final ConcurrentMap<String, Room> sessions;

    private ServerSessionManager() {
        sessions = new ConcurrentHashMap<>();
    }

    public static ServerSessionManager getInstance() {
        return INSTANCE;
    }

    public CopyOnWriteArrayList<ServerClientHandler> getSessions(String roomID) {
        Room room = sessions.get(roomID);
        if (room != null) {
            return room.getObservers();
        }
        return new CopyOnWriteArrayList<>();
    }

    public String createSession() {
        return createSession(UUID.randomUUID().toString());
    }

    public String createSession(String roomID) {
        sessions.putIfAbsent(roomID, new Room(roomID));
        return roomID;
    }

    public void removeSession(String roomID) {
        sessions.remove(roomID);
    }

    public boolean addClientToSession(String roomID, ServerClientHandler client) {
        Room room = sessions.computeIfAbsent(roomID, k -> new Room(roomID));
        room.registerObserver(client);
        return true;
    }

    public boolean removeClientFromSession(String roomID, ServerClientHandler client) {
        Room room = sessions.get(roomID);
        if (room != null) {
            room.removeObserver(client);
            if (room.getObserverCount() == 0) {
                removeSession(roomID);
            }
            return true;
        }
        return false;
    }

    public boolean sessionExists(String roomID) {
        return sessions.containsKey(roomID);
    }

    public int getSessionSize(String roomID) {
        Room room = sessions.get(roomID);
        return (room != null) ? room.getObserverCount() : 0;
    }

    public Room getRoom(String roomId) {
        return sessions.get(roomId);
    }
}