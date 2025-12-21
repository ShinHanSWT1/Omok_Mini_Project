// 파일 경로: src/main/java/team/omok/omok_mini_project/domain/Room.java
package team.omok.omok_mini_project.domain;

import lombok.Data;
import team.omok.omok_mini_project.domain.dto.WsMessage;
import team.omok.omok_mini_project.domain.vo.UserVO;
import team.omok.omok_mini_project.enums.MessageType;
import team.omok.omok_mini_project.enums.RoomStatus;
import team.omok.omok_mini_project.manager.RoomManager;
import team.omok.omok_mini_project.service.UserService;
import team.omok.omok_mini_project.util.JsonUtil;

import javax.websocket.Session;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class Room {

    private final UserService userService = new UserService();

    private static final int MAX_PLAYER = 2;

    private final String roomId;
    private final int ownerId;
    private final long createdAt; // 방 생성 시간

    // 플레이어(user_id 저장) - 기존 로직(get(0), get(1), indexOf) 유지 위해 List 유지
    private final List<Integer> players = new ArrayList<>(MAX_PLAYER);

    // 플레이어 세션 (userId -> session)
    private final Map<Integer, Session> playerSessions = new ConcurrentHashMap<>();

    // 관전자 세션
    private final Set<Session> spectatorSessions = ConcurrentHashMap.newKeySet();

    // 방 상태
    private RoomStatus status = RoomStatus.WAIT;

    // 게임
    private Game game;

    public Room(String roomId, int ownerId) {
        this.roomId = roomId;
        this.ownerId = ownerId;
        this.players.add(ownerId); // 방장은 자동 입장
        this.createdAt = System.currentTimeMillis();
    }

    //////////////// 상태 전이 ////////////////

    private synchronized void updateStatus(RoomStatus newStatus) {
        if (this.status == newStatus) return;
        this.status = newStatus;

        // 상태별 트리거
        switch (newStatus) {
            case COUNTDOWN -> startCountdown();
            case PLAYING -> startGame();
            case END -> {
                // END 처리 정책은 프로젝트에 따라 다름.
                // 바로 cleanUp() 하면 마지막 메시지 전송 전에 방이 제거될 수 있어,
                // 필요할 때만 호출하도록 분리해둠.
            }
            default -> { }
        }
    }

    //////////////// 세션 관리 ////////////////

    /**
     * (호환용) 기존 호출부 유지
     * 기본값은 "플레이어로 시도" (players에 없으면 관전자로 들어감)
     */
    public synchronized void addSession(int userId, Session session) {
        addSession(userId, session, false);
    }

    /**
     * 역할 포함 세션 등록
     * @param isSpectator true면 무조건 관전자로 등록
     */
    public synchronized void addSession(int userId, Session session, boolean isSpectator) {
        System.out.println("[INFO] Room-addSession: roomId=" + roomId
                + ", userId=" + userId
                + ", sessionId=" + session.getId()
                + ", isSpectator=" + isSpectator);

        try {
            // 1) 관전자면 관전자 세션으로만 등록
            if (isSpectator) {
                this.spectatorSessions.add(session);
                return;
            }

            // 2) 플레이어로 들어왔더라도, players에 포함된 유저만 "플레이어"로 인정
            if (this.players.contains(userId)) {

                // ✅ 플레이어 세션 등록 (재접속이면 덮어쓰기)
                this.playerSessions.put(userId, session);

                // JOIN 브로드캐스트 (기존 로직 유지)
                UserVO vo = userService.getUserById(userId);
                broadcastAll(new WsMessage<>(
                        MessageType.JOIN,
                        Map.of(
                                "userId", userId,
                                "profileImg", vo.getProfileImg()
                        )
                ));

            } else {
                // players에 없는 유저는 관전자로 처리(권한 안전)
                this.spectatorSessions.add(session);
            }

        } catch (Exception e) {
            System.out.println("[WARN] Room-addSession exception: " + e.getMessage());
        }

        // READY 조건 달성 시 상태 변경
        if (isReady() && this.status == RoomStatus.WAIT) {
            updateStatus(RoomStatus.READY);
        }
    }

    /**
     * 세션에서 유저 혹은 관전자 삭제
     * - 관전자는 players를 건드리면 안 됨
     * - 플레이어 세션일 때만 players.remove 및 게임 종료 로직을 탄다
     */
    public synchronized void removeSession(int userId, Session session) {

        boolean wasPlayer = this.playerSessions.containsKey(userId);

        // 세션 제거
        this.playerSessions.remove(userId);
        this.spectatorSessions.remove(session);

        // 관전자였다면 여기서 종료 (게임 상태 영향 X)
        if (!wasPlayer) {
            return;
        }

        // 플레이어였던 경우만 players에서도 제거
        this.players.remove(Integer.valueOf(userId));

        // 게임 도중 방 나간 경우
        if (!isReady() && this.status == RoomStatus.PLAYING) {
            updateStatus(RoomStatus.END);
            broadcastToPlayers(new WsMessage<>(
                    MessageType.LEAVE,
                    Map.of("reason", "PLAYER GG")
            ));
            return;
        }

        // 게임 시작 전에 방 나간 경우
        if (!isReady() && (this.status == RoomStatus.READY || this.status == RoomStatus.COUNTDOWN)) {
            updateStatus(RoomStatus.WAIT);
            broadcastToPlayers(new WsMessage<>(
                    MessageType.LEAVE,
                    Map.of("reason", "PLAYER_LEFT")
            ));
        }

        // 아예 방이 비어버린 경우
        if (this.playerSessions.isEmpty() && this.players.isEmpty()) {
            updateStatus(RoomStatus.END);
            broadcastToPlayers(new WsMessage<>(
                    MessageType.GAME_END,
                    Map.of("reason", "ROOM EMPTY")
            ));

            // 필요하면 방 제거
            // cleanUp();
        }
    }

    // 플레이어로 참가 예약 (HTTP에서 "참가" 누를 때 호출하는 용도)
    public synchronized void tryAddPlayer(int userId) {
        if (isFull()) {
            throw new IllegalStateException("방이 가득 찼습니다");
        }
        if (!this.players.contains(userId)) {
            this.players.add(userId);
        }
    }

    // (호환용) 기존 메서드 유지
    public synchronized void addSpectatorSession(Session session) {
        this.spectatorSessions.add(session);
    }

    //////////////// 게임 흐름 제어 ////////////////

    public synchronized void tryStartGame() {
        if (this.status != RoomStatus.READY) {
            return;
        }
        updateStatus(RoomStatus.COUNTDOWN);
    }

    // 게임 시작 전 카운트다운
    private void startCountdown() {
        System.out.println("[INFO] Room-startCountdown");

        new Thread(() -> {
            try {
                for (int i = 5; i >= 1; i--) {
                    if (this.status != RoomStatus.COUNTDOWN) {
                        return;
                    }
                    Thread.sleep(1000);

                    broadcastAll(new WsMessage<>(
                            MessageType.COUNTDOWN,
                            Map.of("sec", i)
                    ));
                }

                // 게임 시작
                if (isReady()) {
                    updateStatus(RoomStatus.PLAYING);
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    // 게임 시작 함수
    private synchronized void startGame() {
        System.out.println("플레이어: " + players + " / 플레이어 세션: " + playerSessions);
        if (!isReady()) return;

        // 게임 초기화
        this.game = new Game(players.get(0), players.get(1));
        this.game.startGame();

        // 클라이언트에게 자신의 색 전달 (playerSessions: userId -> session)
        for (Map.Entry<Integer, Session> entry : playerSessions.entrySet()) {
            int userId = entry.getKey();
            Session s = entry.getValue();

            String myStone =
                    (userId == game.state.getBlackUserId()) ? "BLACK" : "WHITE";

            sendToSession(s, new WsMessage<>(
                    MessageType.GAME_START,
                    Map.of(
                            "myColor", myStone,
                            "firstTurn", game.state.getTurn().toString()
                    )
            ));
        }
    }

    // 게임 종료 함수
    private synchronized void endGame() {
        updateStatus(RoomStatus.END);
    }

    // 방 제거
    private synchronized void cleanUp() {
        RoomManager.getInstance().removeRoom(roomId);
    }

    // 게임에 데이터 전달
    public synchronized void handleMove(int userId, int x, int y) {
        if (this.status != RoomStatus.PLAYING) {
            return;
        }

        MoveResult result = this.game.rule.placeStone(game.state, x, y);
        handleMoveResult(result, userId);
    }

    // 게임 결과 처리
    private void handleMoveResult(MoveResult result, int userId) {
        switch (result.getType()) {

            case MOVE_OK -> {
                broadcastAll(new WsMessage<>(
                        MessageType.MOVE_OK,
                        Map.of(
                                "x", result.getX(),
                                "y", result.getY(),
                                "color", this.game.state.getStone(result.getX(), result.getY())
                        )
                ));
            }

            case INVALID_TURN, INVALID_POSITION -> {
                sendErrorToUser(userId, result.getType().name(), result.getReason());
            }

            case WIN -> {
                // 마지막 수
                broadcastAll(new WsMessage<>(
                        MessageType.MOVE_OK,
                        Map.of(
                                "x", result.getX(),
                                "y", result.getY(),
                                "color", this.game.state.getStone(result.getX(), result.getY())
                        )
                ));

                // 승자
                broadcastAll(new WsMessage<>(
                        MessageType.GAME_END,
                        Map.of("winner", result.getWinnerId())
                ));

                endGame();
                // 필요하면 cleanUp() 호출 정책 결정
                // cleanUp();
            }

            case DRAW -> {
                // TODO: 무승부 처리 필요 시 구현
            }
        }
    }

    //////////////// 유틸 ////////////////

    public synchronized boolean isFull() {
        return this.players.size() >= MAX_PLAYER;
    }

    // 게임 시작 가능 조건
    private boolean isReady() {
        return this.players.size() == MAX_PLAYER && this.playerSessions.size() == MAX_PLAYER;
    }

    // 해당 방 유저 + 관전자에게 broadcast
    public void broadcastAll(Object message) {
        broadcastToPlayers(message);
        broadcastToSpectators(message);
    }

    // 방 플레이어들에게만 broadcast
    public void broadcastToPlayers(Object message) {
        try {
            String json = JsonUtil.MAPPER.writeValueAsString(message);
            for (Session s : this.playerSessions.values()) {
                if (s != null && s.isOpen()) {
                    s.getBasicRemote().sendText(json);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 방 관전자들에게만 broadcast
    public void broadcastToSpectators(Object message) {
        try {
            String json = JsonUtil.MAPPER.writeValueAsString(message);
            for (Session s : this.spectatorSessions) {
                if (s != null && s.isOpen()) {
                    s.getBasicRemote().sendText(json);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 한 세션에게만 전송
    private void sendToSession(Session session, Object message) {
        try {
            String json = JsonUtil.MAPPER.writeValueAsString(message);
            if (session != null && session.isOpen()) {
                session.getBasicRemote().sendText(json);
            }
        } catch (Exception ignored) {}
    }

    // 특정 유저에게 에러 전송 (Map 기준)
    private void sendErrorToUser(int userId, String error, String msg) {
        Session s = this.playerSessions.get(userId);
        if (s == null || !s.isOpen()) return;

        try {
            s.getBasicRemote().sendText(
                    JsonUtil.MAPPER.writeValueAsString(
                            new WsMessage<>(
                                    MessageType.ERROR,
                                    Map.of(
                                            "code", error,
                                            "message", msg
                                    )
                            )
                    )
            );
        } catch (Exception ignored) {}
    }

    // 채팅 처리
    public void handleChat(int userId, String msg) {
        boolean isPlayer = players.contains(userId);
        int playerIndex = isPlayer ? players.indexOf(userId) + 1 : -1;

        broadcastAll(new WsMessage<>(
                MessageType.CHAT,
                Map.of(
                        "senderRole", isPlayer ? "PLAYER" : "SPECTATOR",
                        "playerIndex", playerIndex,
                        "message", msg
                )
        ));
    }
}
