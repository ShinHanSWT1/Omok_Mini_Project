package team.omok.omok_mini_project.domain;

import lombok.Data;
import team.omok.omok_mini_project.enums.RoomStatus;
import team.omok.omok_mini_project.game.Game;
import team.omok.omok_mini_project.manager.RoomManager;

import javax.websocket.Session;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 하나의 게임 방에 대해 모든 상태 저장
 *
 * 방 ID
 * 참가자 목록
 * 관전자 목록
 * WebSocket 세션들
 * 현재 게임(Game) 참조
 * @see RoomManager
 */
@Data
public class Room {
    private static final int MAX_PLAYER = 2;

    private final String roomId;
    private final String ownerId;
    private final List<String> players = new ArrayList<>(MAX_PLAYER);           // 플레이어
    private final Set<Session> sessions = ConcurrentHashMap.newKeySet();        // 플레이어 세션
    private final Set<Session> spectators = ConcurrentHashMap.newKeySet();      // 관전자 세션
    private Game game;                          // 게임
    private RoomStatus status;                  // 방 상태: WAITING, COUNTDOWN, PLAYING, END
    private boolean gameStarted = false;


    public Room(String roomId, String ownerId) {
        this.roomId = roomId;
        this.ownerId = ownerId;
        this.players.add(ownerId);              // 방장은 자동 입장
    }

    public void addSession(Session session) {
        System.out.println("[INFO]Room-addSession: " + session);
        sessions.add(session);
    }

    public void removeSession(Session session) {
        sessions.remove(session);
    }

    public synchronized void tryAddPlayer(String userId) {
        if (isFull()) {
            throw new IllegalStateException("방이 가득 찼습니다");
        }
        players.add(userId);
    }

    public synchronized void tryStartGame() {
        if (gameStarted) return;
        if (!isReady()) return;

        startCountdown();
    }


    public boolean isFull() {
        return players.size() >= MAX_PLAYER;
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
        return players.size() == MAX_PLAYER && sessions.size() == MAX_PLAYER;
    }

    public void startCountdown() {
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