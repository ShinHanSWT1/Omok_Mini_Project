package team.omok.omok_mini_project.domain;

import lombok.Data;
import team.omok.omok_mini_project.domain.dto.WsMessage;
import team.omok.omok_mini_project.enums.JoinResult;
import team.omok.omok_mini_project.enums.LeaveResult;
import team.omok.omok_mini_project.enums.MessageType;
import team.omok.omok_mini_project.enums.RoomStatus;
import team.omok.omok_mini_project.manager.RoomManager;
import team.omok.omok_mini_project.repository.RecordDAO;
import team.omok.omok_mini_project.service.RoomBroadcaster;
import team.omok.omok_mini_project.service.UserService;

import javax.websocket.Session;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/*
 * Room (도메인 엔티티)
 * - 방의 상태
 * - 플레이어 정보
 * - 게임 시작 조건
 * - 게임 로직 실행
 * - 게임 상태 전이
 * - Session, WebSocket, JSON, Thread 없음
 */
@Data
public class Room {
    UserService userService = new UserService();

    private static final int MAX_PLAYER = 2;

    private final String roomId;
    private final int ownerId;
    private final long createdAt;                   // 방 생성 시간
    private final RecordDAO recordDAO = new RecordDAO();

    // 플레이어(user_id 저장 -> HTTP로 /enter 통해 들어오는 플레이어 아이디 저장 필요)
    private final List<Integer> players = new ArrayList<>(MAX_PLAYER);

    // 플레이어 세션 (userId -> session)
    private final Map<Integer, Session> playerSessionMap = new ConcurrentHashMap<>();

    // 관전자 세션
    private final Set<Session> spectatorSessions = ConcurrentHashMap.newKeySet();

    // 방 상태: WAITING, READY, COUNTDOWN, PLAYING, END
    private RoomStatus status = RoomStatus.WAIT;

    // 게임
    private Game game;

    public Room(String roomId, int ownerId) {
        this.roomId = roomId;
        this.ownerId = ownerId;
        this.players.add(ownerId);              // 방장은 자동 입장
        this.createdAt = System.currentTimeMillis();
    }

    /// /////////// 상태 전이 ///////////////

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
            default -> {
            }
        }
    }

    //////////////// 세션 관리 ////////////////

    /**
     * 기본값은 "플레이어로 시도" (players에 없으면 관전자로 들어감)
     */
    public synchronized void addSession(int userId, Session session) {
        addSession(userId, session, true);
    }

    /**
     * 역할 포함 세션 등록
     *
     * @param isSpectator true면 무조건 관전자로 등록
     */
    public synchronized JoinResult addSession(int userId, Session session, boolean isSpectator) {
        System.out.println("[INFO] Room-addSession: roomId=" + roomId
                + ", userId=" + userId
                + ", sessionId=" + session.getId()
                + ", isSpectator=" + isSpectator);

        try {
            // 1) 관전자면 관전자 세션으로만 등록
            if (isSpectator) {
                this.spectatorSessions.add(session);
                return JoinResult.SPECTATOR_JOINED;
            }

            // 2) 플레이어로 들어왔더라도, players에 포함된 유저만 "플레이어"로 인정
            if (this.players.contains(userId)) {

                // 플레이어 세션 등록 (재접속이면 덮어쓰기)
                this.playerSessionMap.put(userId, session);

            } else {
                // players에 없는 유저는 관전자로 처리(권한 안전)
                this.spectatorSessions.add(session);
                return JoinResult.SPECTATOR_JOINED;
            }

        } catch (Exception e) {
            System.out.println("[WARN] Room-addSession exception: " + e.getMessage());
        }

        // READY 조건 달성 시 상태 변경
        if (isReady() && this.status == RoomStatus.WAIT) {
            updateStatus(RoomStatus.READY);
            return JoinResult.ROOM_READY;
        }

        return JoinResult.PLAYER_JOINED;
    }

    /**
     * 세션에서 유저 혹은 관전자 삭제
     * - 관전자는 players를 건드리면 안 됨
     * - 플레이어 세션일 때만 players.remove 및 게임 종료 로직을 탄다
     */
    public synchronized LeaveResult removeSession(int userId, Session session) {

        boolean wasPlayer = this.playerSessionMap.containsKey(userId);

        // 세션 제거
        this.playerSessionMap.remove(userId);
        this.spectatorSessions.remove(session);

        // 관전자였다면 여기서 종료 (게임 상태 영향 X)
        if (!wasPlayer) {
            return LeaveResult.SPECTATOR_LEFT;
        }

        // 플레이어였던 경우만 players에서도 제거
        this.players.remove(Integer.valueOf(userId));

        // 게임 도중 방 나간 경우
        if (!isReady() && this.status == RoomStatus.PLAYING) {
            updateStatus(RoomStatus.END);

            return LeaveResult.PLAYER_LEFT_DURING_GAME;
        }

        // 게임 시작 전에 방 나간 경우
        if (!isReady() && (this.status == RoomStatus.READY || this.status == RoomStatus.COUNTDOWN)) {
            updateStatus(RoomStatus.WAIT);

            return LeaveResult.PLAYER_LEFT_BEFORE_START;
        }

        // 아예 방이 비어버린 경우
        if (this.playerSessionMap.isEmpty() && this.players.isEmpty()) {
            updateStatus(RoomStatus.END);
            return LeaveResult.ROOM_EMPTY;

        }

        return LeaveResult.SPECTATOR_LEFT;
    }

    public synchronized void tryAddPlayer(int userId) {
        if (isFull()) {
            throw new IllegalStateException("방이 가득 찼습니다");
        }
        this.players.add(userId);
    }

    public synchronized void addSpectatorSession(Session session) {
        this.spectatorSessions.add(session);
    }


    /// /////////// 게임 흐름 제어 ///////////////

    public synchronized void tryStartGame() {
        if (this.status != RoomStatus.READY) {
            return;
        }
        updateStatus(RoomStatus.COUNTDOWN);
    }

    // 게임 시작 전 카운트다운
    private void startCountdown() {
        System.out.println("[INFO]Room-startCountdown");

        new Thread(() -> {
            try {
                RoomBroadcaster broadcaster = new RoomBroadcaster();
                for (int i = 5; i >= 1; i--) {
                    if (this.status != RoomStatus.COUNTDOWN) {
                        return;
                    }

                    broadcaster.broadcastAll(this, new WsMessage<>(
                            MessageType.COUNTDOWN,
                            Map.of("sec", i)
                    ));

                    Thread.sleep(1000);
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
        System.out.println("플레이어: " + players + " / 플레이어 세션: " + playerSessionMap);
        if (!isReady()) return;

        // 게임 초기화
        this.game = new Game(players.get(0), players.get(1));
        this.game.startGame();
        System.out.println("게임시작");

        RoomBroadcaster broadcaster = new RoomBroadcaster();

        for (Session s : playerSessionMap.values()) {
            int userId = (int) s.getUserProperties().get("user_id");
            String myStone =
                    (userId == game.state.getBlackUserId()) ? "BLACK" : "WHITE";

            broadcaster.broadcastToSession(s, new WsMessage<>(
                    MessageType.GAME_START,
                    Map.of(
                            "myColor", myStone,
                            "myUserId", userId,
                            "blackPlayerId", game.state.getBlackUserId(),
                            "whitePlayerId", game.state.getWhiteUserId(),
                            "firstTurn", "BLACK"

                    )
            ));
            System.out.println(userId + ": " + myStone);
        }
    }

    // 게임 종료 함수
    public synchronized void endGame() {
        // TODO: 게임 결과 저장 및 유저 전적 업데이트
        // 게임 상태에서 승자 ID 가져오기
        int winnerId = this.game.state.getWinnerId();

        // 승자가 있을 경우 (무승부가 아님) DB 업데이트
        if (winnerId != -1) {
            for (Integer playerId : players) {
                boolean isWin = (playerId == winnerId);
                // DAO 호출: 이긴 사람은 true, 진 사람은 false 전달
                recordDAO.updateRating(playerId, isWin);
            }
        }

        updateStatus(RoomStatus.END);
    }

    // 게임 방 정리
    private synchronized void cleanUp() {
        RoomManager.getInstance().removeRoom(roomId);
    }

    public synchronized MoveResult handleMove(int userId, int x, int y) {
        if (this.status != RoomStatus.PLAYING) {
            return null;
        }

        return this.game.rule.placeStone(this.game.state, x, y);
    }


    /// /////////// 유틸 ///////////////

    public synchronized boolean isFull() {
        return this.players.size() >= MAX_PLAYER;
    }

    // 게임 시작 가능 조건
    private boolean isReady() {
        return this.players.size() == MAX_PLAYER && this.playerSessionMap.size() == MAX_PLAYER;
    }

}
