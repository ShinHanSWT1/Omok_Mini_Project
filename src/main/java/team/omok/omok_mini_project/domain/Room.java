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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/*
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
    UserService userService = new UserService();

    private static final int MAX_PLAYER = 2;

    private final String roomId;
    private final int ownerId;
    private final long createdAt;                   // 방 생성 시간

    private final List<Integer> players = new ArrayList<>(MAX_PLAYER);                // 플레이어(user_id 저장)
    private final Set<Session> playerSessions = ConcurrentHashMap.newKeySet();        // 플레이어 세션
    private final Set<Session> spectatorSessions = ConcurrentHashMap.newKeySet();     // 관전자 세션

    private RoomStatus status = RoomStatus.WAIT;     // 방 상태: WAITING, READY, COUNTDOWN, PLAYING, END

    // 게임
    private Game game;

    public Room(String roomId, int ownerId) {
        this.roomId = roomId;
        this.ownerId = ownerId;
        this.players.add(ownerId);              // 방장은 자동 입장
        this.createdAt = System.currentTimeMillis();
    }

    ////////////// 상태 관리 ///////////////

    private synchronized void updateStatus(RoomStatus nextStatus){
        if(this.status == nextStatus) return;
        this.status = nextStatus;

        switch (nextStatus){
            case WAIT ->{
            }
            case READY->{
                tryStartGame();
            }
            case COUNTDOWN ->{
                startCountdown();
            }
            case PLAYING->{
                startGame();
            }
            case END->{
                cleanUp();
            }
        }
    }

    ////////////// 세션 관리 ///////////////

    // 세션에 유저 혹은 관전자 추가
    public synchronized void addSession(int userId, Session session) {
        System.out.println("[INFO]Room-addSession: " + session);
        try{
            if(this.players.contains(userId)){
                UserVO vo = userService.getUserById(userId);
                this.playerSessions.add(session);
                broadcastAll(new WsMessage<>(
                        MessageType.JOIN,
                        Map.of(
                                "userId", userId,
                                "userInfo", vo
                        )
                ));

            }else{
                this.spectatorSessions.add(session);
            }

        }catch (Exception e){}

        if(isReady() && this.status == RoomStatus.WAIT){
            System.out.println("플레이어 세션: " + session.getId());
            updateStatus(RoomStatus.READY);
        }
    }


    // 세션에서 유저 혹은 관전자 삭제
    public synchronized void removeSession(int userId, Session session) {
        this.players.remove(Integer.valueOf(userId));
        this.playerSessions.remove(session);
        this.spectatorSessions.remove(session);

        // 게임 도중 방 나간 경우
        if(!isReady() && this.status == RoomStatus.PLAYING){
            updateStatus(RoomStatus.END);
            broadcastToPlayers(new WsMessage<>(
                    MessageType.LEAVE,
                    Map.of("reason", "PLAYER GG")
            ));
            return;
        }

        // 게임 시작 전에 방 나간 경우
        if(!isReady() && (this.status == RoomStatus.READY || this.status == RoomStatus.COUNTDOWN)){
            updateStatus(RoomStatus.WAIT);
            broadcastToPlayers(new WsMessage<>(
                    MessageType.LEAVE,
                    Map.of("reason", "PLAYER_LEFT"))
            );
        }

        // 아예 방이 비어버린 경우
        if(this.playerSessions.isEmpty() && this.players.isEmpty()){
            updateStatus(RoomStatus.END);
            broadcastToPlayers(new WsMessage<>(
                    MessageType.GAME_END,
                    Map.of("reason", "ROOM EMPTY"))
            );
        }
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


    ////////////// 게임 흐름 제어 ///////////////

    public synchronized void tryStartGame() {
        if(this.status != RoomStatus.READY) {
            return;
        }
        updateStatus(RoomStatus.COUNTDOWN);
    }

    // 게임 시작 전 카운트다운
    private void startCountdown() {
        System.out.println("[INFO]Room-startCountdown");

        new Thread(() -> {
            try {
                for (int i = 5; i >= 1; i--) {
                    if(this.status != RoomStatus.COUNTDOWN){
                        return;
                    }
                    Thread.sleep(1000);

                    broadcastAll(new WsMessage<>(
                            MessageType.COUNTDOWN,
                            Map.of("sec", i)
                    ));
                }

                // 게임 시작
                if(isReady()){
                    updateStatus(RoomStatus.PLAYING);
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    // 게임 시작 함수
    private synchronized void startGame(){
        System.out.println("플레이어: " + players + " / 플레이어 세션: " + playerSessions);
        if(!isReady()) return;

        // 게임 초기화
        this.game = new Game(players.get(0), players.get(1));
        this.game.startGame();
        System.out.println("게임시작");
        // 클라이언트에게 자신의 색 전달
//        broadcastToPlayers(new WsMessage<>(
//                MessageType.GAME_START,
//                Map.of(
//                    "blackPlayerId", this.game.state.getBlackUserId(),
//                    "whitePlayerId", this.game.state.getWhiteUserId()
//                )
//        ));
        for (Session s : playerSessions) {
            int userId = (int) s.getUserProperties().get("user_id");
            String myStone =
                    (userId == game.state.getBlackUserId()) ? "BLACK" : "WHITE";
            System.out.println(userId + ": " + myStone);
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
    private synchronized void endGame(){
        // 게임 결과 저장 및 유저 전적 업데이트

        updateStatus(RoomStatus.END);
    }

    // 게임 방 정리
    private synchronized void cleanUp(){
        RoomManager.getInstance().removeRoom(roomId);
    }

    // 게임에 데이터 전달 및 결과 저장
    public synchronized void handleMove(int userId, int x, int y){
        if(this.status != RoomStatus.PLAYING){
            return;
        }

        MoveResult result = this.game.rule.placeStone(game.state, x, y);
        handleMoveResult(result, userId);
    }

    // 게임에서 받은 결과 처리
    private void handleMoveResult(MoveResult result, int userId){
        switch (result.getType()) {
            // 정상 착수 처리
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
            // 유효하지 않은 턴 처리
            case INVALID_TURN, INVALID_POSITION -> {
                sendErrorToUser(userId, result.getType().name(), result.getReason());
            }

            // 승리 처리
            case WIN -> {
                // 정상 착수 결과 전달
                broadcastAll(new WsMessage<>(
                        MessageType.MOVE_OK,
                        Map.of(
                                "x", result.getX(),
                                "y", result.getY(),
                                "color", this.game.state.getStone(result.getX(), result.getY())
                        )
                ));

                // 승자 전달
                broadcastAll(new WsMessage<>(
                        MessageType.GAME_END,
                        Map.of("winner", result.getWinnerId())
                ));

                // 저장
                endGame();
            }

            // 무승부 처리
            case DRAW -> {

            }
        }
    }


    ////////////// 유틸 ///////////////

    public synchronized boolean isFull() {
        return this.players.size() >= MAX_PLAYER;
    }

    // 게임 시작 가능 조건
    private boolean isReady() {
        return this.players.size() == MAX_PLAYER && this.playerSessions.size() == MAX_PLAYER;
    }

    // 해당 방 유저 + 관전자에게 broadcast
    public void broadcastAll(Object message) {
        try {
            broadcastToPlayers(message);
            broadcastToSpectators(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 방 플레이어들에게만 broadcast
    public void broadcastToPlayers(Object message) {
        try {
            String json = JsonUtil.MAPPER.writeValueAsString(message);
            for (Session s : this.playerSessions) {
                s.getBasicRemote().sendText(json);
                System.out.println(json);
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
                s.getBasicRemote().sendText(json);
                System.out.println(json);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 한 명의 유저에게만 메세지 전달
    private void sendToSession(Session session, Object message) {
        try {
            String json = JsonUtil.MAPPER.writeValueAsString(message);
            session.getBasicRemote().sendText(json);
        } catch (Exception ignored) {}
    }

    // 한 명의 유저에게만 에러 메세지 전달
    private void sendErrorToUser(int userId, String error, String msg) {
        for (Session s : this.playerSessions) {
            Object sid = s.getUserProperties().get("user_id");
            if (sid != null && Integer.parseInt(sid.toString()) == userId) {
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

                return;
            }
        }
    }

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
