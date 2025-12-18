package team.omok.omok_mini_project.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.websocket.Session;

public class Room {
    private final String roomId;
    private final String ownerId;
    private final List<String> players = new ArrayList<>();

    private final Set<Session> sessions = ConcurrentHashMap.newKeySet();
    private boolean gameStarted = false;

    public Room(String roomId, String ownerId) {
        this.roomId = roomId;
        this.ownerId = ownerId;
        this.players.add(ownerId); // 방장은 자동 입장
    }

    public String getRoomId() {
        return roomId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public synchronized void addPlayer(String userId) {
        if (players.size() >= 2) {
            throw new IllegalStateException("방이 가득 참");
        }
        players.add(userId);
    }

    public boolean isFull() {
        return players.size() == 2;
    }

    public void addSession(Session session) {
        System.out.println("[INFO]Room-addSession: "+ session);
        sessions.add(session);
    }

    public void removeSession(Session session) {
        sessions.remove(session);
    }

    public void broadcast(String message) {
        for (Session s : sessions) {
            try {
                System.out.println("[INFO]Room-broadcast: "+ message);
                s.getBasicRemote().sendText(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isReady() {
        return players.size() == 2 && sessions.size() == 2;
    }

    public synchronized void startCountdown() {
        if (gameStarted) return;
        gameStarted = true;
        System.out.println("[INFO]Room-startCountdown");

        new Thread(() -> {
            try {
                for (int i = 5; i >= 1; i--) {
                    broadcast("{\"type\":\"COUNTDOWN\",\"sec\":" + i + "}");
                    Thread.sleep(1000);
                }
                broadcast("{\"type\":\"GAME_START\"}");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
