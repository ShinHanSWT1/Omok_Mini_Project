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
 * 하나의 게임 방에 대해 모든 상태 저장 + 유일하게 상태 변경 할 수 있는 객체
 *
 * 방 ID
 * 참가자 목록
 * 관전자 목록
 * WebSocket 세션들
 * 현재 게임(Game) 참조
 *
 * (참고) 스레드 세이프해야하는 부분
 * * 게임 시작 조건 판단
 * * 플레이어 추가
 * * 게임 상태 변경
 * @see RoomManager
 */
@Data
public class Room {
    private static final int MAX_PLAYER = 2;

    private final String roomId;
    private final String ownerId;
    private final List<String> players = new ArrayList<>(MAX_PLAYER);           // 플레이어
    private final Set<Session> playerSessions = ConcurrentHashMap.newKeySet();        // 플레이어 세션
    private final Set<Session> spectators = ConcurrentHashMap.newKeySet();      // 관전자 세션
    private Game game;                              // 게임
    private RoomStatus status = RoomStatus.WAIT;    // 방 상태: WAITING, READY, COUNTDOWN, PLAYING, END


    public Room(String roomId, String ownerId) {
        this.roomId = roomId;
        this.ownerId = ownerId;
        this.players.add(ownerId);              // 방장은 자동 입장
    }

    public synchronized void addSession(String userId, Session session) {
        System.out.println("[INFO]Room-addSession: " + session);
        if(players.contains(userId)){
            playerSessions.add(session);
        }else{
            spectators.add(session);
        }
        updateRoomStatus();
    }

    public synchronized void removeSession(Session session) {
        playerSessions.remove(session);
    }

    public synchronized void tryAddPlayer(String userId) {
        if (isFull()) {
            throw new IllegalStateException("방이 가득 찼습니다");
        }
        players.add(userId);
    }

    public synchronized void tryStartGame() {
        if(status != RoomStatus.READY) {
            return;
        }
        startCountdown();
    }

    public void broadcast(String message) {
        for (Session s : playerSessions) {
            try {
                System.out.println("[INFO]Room-broadcast: "+ message);
                s.getBasicRemote().sendText(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized boolean isFull() {
        return players.size() >= MAX_PLAYER;
    }

    private boolean isReady() {
        return players.size() == MAX_PLAYER && playerSessions.size() == MAX_PLAYER;
    }

    private void updateRoomStatus(){
        if(isReady() && status == RoomStatus.WAIT){
            status = RoomStatus.READY;
        }
    }

    private void startCountdown() {
        status = RoomStatus.COUNTDOWN;
        System.out.println("[INFO]Room-startCountdown");

        new Thread(() -> {
            try {
                for (int i = 5; i >= 1; i--) {
                    broadcast("{\"type\":\"COUNTDOWN\",\"sec\":" + i + "}");
                    Thread.sleep(1000);
                }

                // 게임 시작
                startGame();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void startGame(){
        broadcast("{\"type\":\"GAME_START\"}");
        this.game = new Game(); // 게임은 추후 추가
        status = RoomStatus.PLAYING;
    }
}