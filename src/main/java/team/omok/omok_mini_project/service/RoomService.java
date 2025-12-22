package team.omok.omok_mini_project.service;

import team.omok.omok_mini_project.domain.GameState;
import team.omok.omok_mini_project.domain.MoveResult;
import team.omok.omok_mini_project.domain.Room;
import team.omok.omok_mini_project.domain.dto.WsMessage;
import team.omok.omok_mini_project.domain.vo.UserVO;
import team.omok.omok_mini_project.enums.JoinResult;
import team.omok.omok_mini_project.enums.LeaveResult;
import team.omok.omok_mini_project.enums.MessageType;
import team.omok.omok_mini_project.manager.RoomManager;
import team.omok.omok_mini_project.repository.RecordDAO;

import javax.websocket.Session;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 방에서 일어나는 행위를 처리하는 서비스 클래스
public class RoomService {

    private final RoomManager roomManager = RoomManager.getInstance();
    private final RoomBroadcaster broadcaster = new RoomBroadcaster();
    private final RecordDAO recordDAO = new RecordDAO();
    private final UserService userService = new UserService();

    /// //////////////// 방 접근 함수 /////////////////////

    // 방 생성
    public Room createRoom(int userId) {
        return roomManager.createRoom(userId);
    }

    // 방 입장
    public void enterRoom(String roomId, UserVO user) {
        Room room = roomManager.getRoomById(roomId);

        if (room == null) {
            throw new IllegalArgumentException("방이 존재하지 않습니다");
        } else if (room.isFull()) {
            throw new IllegalStateException("방이 꽉 찼습니다");
            // TODO: 우회?
        }
        room.tryAddPlayer(user.getUserId());

        // TODO: LobbyWebSocket에서 방 목록 실시간 갱신을 위한 broadcast 추가
        // 현재는 단순화를 위해 RoomManager에서 직접 LobbyWebSocket을 호출한다
        // 추후 이벤트 기반 구조로 변경 시 제거 대상이 된다
    }

    // 관전자로 방 입장
    public void enterRoomAsSpectator(String roomId, Session session) {
        roomManager.enterRoomAsSpectator(roomId, session);
    }

    // 대기 중인 방 목록 가져오기
    public List<Room> getWaitingRooms() {
        return roomManager.getWaitingRooms();
    }

    // 빠른 입장: 가장 먼저 생성된 대기 방 반환
    public Room getFirstWaitingRoom() {
        return roomManager.getFirstWaitingRoom();
    }

    // 모든 방 목록 가져오기
    public List<Room> getAllRooms() {
        return roomManager.getAllRooms();
    }

    // 방 하나만 가져오기
    public Room getRoom(String roomId) {
        return getRoomOrThrow(roomId);
    }

    /// ////////////// 웹 소켓 통신 핸들러 ////////////////////

    // 소켓 onOpen
    // 게임 시작할 때 서로의 프로필 사진, 닉네임 브로드캐스트
    public void onJoin(String roomId, int userId, Session session, boolean spectator) {
        Room room = getRoomOrThrow(roomId);
        JoinResult result = room.addSession(userId, session, spectator);

        // 유저 정보 생성 (회원/비회원 공통)
        Map<String, Object> joinInfo = createUserInfoMap(userId, spectator);

        // 신규 입장 알림 (모든 사람에게)
        broadcaster.broadcastAll(room, new WsMessage<>(MessageType.JOIN, joinInfo));

        // 신규 입장자(나)에게만 현재 방의 '플레이어 리스트' 전송
        List<Map<String, Object>> memberList = room.getPlayers().stream()
                .map(id -> createUserInfoMap(id, false))
                .toList();
        broadcaster.broadcastToSession(session, new WsMessage<>(MessageType.ROOM_MEMBERS, memberList));

        // 관전자 혹은 재접속 플레이어에게 '현재 바둑판 상태' 전송
        if (room.getGame() != null && room.getGame().getState() != null) {
            GameState state = room.getGame().getState();
            broadcaster.broadcastToSession(session, new WsMessage<>(MessageType.BOARD_SNAPSHOT,
                    Map.of(
                            "board", state.getBoard(),
                            "turn", state.getTurn().toString(),
                            "remainingTime", state.getRemainingTimeMs()
                    )
            ));
        }

        switch (result) {
            case ROOM_READY -> {
                broadcaster.broadcastAll(room,
                        new WsMessage<>(
                                MessageType.ROOM_READY, "게임이 곧 시작됩니다!"
                        )
                );

                room.tryStartGame();

            }
        }
    }

    // 소켓 onClose
    // RoomService.java

    public void onLeave(String roomId, int userId, Session session) {
        Room room = getRoomOrThrow(roomId);

        // 1. 나가려는 사람의 닉네임 미리 확보 (세션 끊기기 전/후 DB 조회)
        String nickname = getNicknameSafe(userId);

        LeaveResult result = room.removeSession(userId, session);

        // 2. 공통으로 보낼 메시지 페이로드 구성
        Map<String, Object> leavePayload = new HashMap<>();
        leavePayload.put("userId", userId);
        leavePayload.put("nickname", nickname);

        switch (result) {
            case PLAYER_LEFT_DURING_GAME -> {
                leavePayload.put("reason", "PLAYER_GG"); // 기권 패
                // 플레이어 GG는 게임 종료 사유이므로 모두에게 알림
                broadcaster.broadcastAll(room, new WsMessage<>(MessageType.LEAVE, leavePayload));
                handleGameEnd(room);
            }

            case PLAYER_LEFT_BEFORE_START -> {
                leavePayload.put("reason", "PLAYER_LEFT"); // 단순 퇴장
                broadcaster.broadcastAll(room, new WsMessage<>(MessageType.LEAVE, leavePayload));
            }

            case ROOM_EMPTY -> {
                // 방이 비었으므로 남은 사람에게 알릴 필요 없음 (이미 다 나감)
                // 방 제거 로직만 수행
                handleGameEnd(room);
            }

            case SPECTATOR_LEFT -> {
                leavePayload.put("reason", "SPECTATOR_LEFT");
                // 관전자가 나간 건 모두에게 알림 (채팅창 출력용)
                broadcaster.broadcastAll(room, new WsMessage<>(MessageType.LEAVE, leavePayload));
            }
        }
    }


    // 착수 처리
    public void handleMove(String roomId, int userId, int x, int y) {
        Room room = getRoomOrThrow(roomId);

        MoveResult result = room.handleMove(userId, x, y);
        if (result == null) return;

        switch (result.getType()) {

            case MOVE_OK -> {
                broadcaster.broadcastAll(room, new WsMessage<>(
                        MessageType.MOVE_OK,
                        Map.of(
                                "x", result.getX(),
                                "y", result.getY(),
                                "color", room.getGame().getState().getStone(result.getX(), result.getY())
                        )
                ));

            }

            case INVALID_POSITION -> {
                broadcaster.broadcastToSession(
                        room.getPlayerSessionMap().get(userId),
                        new WsMessage<>(
                                MessageType.ERROR,
                                Map.of(
                                        "code", result.getType().name(),
                                        "message", result.getReason()
                                )
                        ));
            }

            case INVALID_TURN -> {
                if ("TIMEOUT".equals(result.getReason())) {
                    broadcaster.broadcastAll(room,
                            new WsMessage<>(
                                    MessageType.GAME_END,
                                    Map.of(
                                            "reason", "TIMEOUT",
                                            "winner", room.getGame().getState().getWinnerId()
                                    )
                            ));

                    handleGameEnd(room);
                }

            }

            case WIN -> {
                // 마지막 착수
                broadcaster.broadcastAll(room,
                        new WsMessage<>(MessageType.MOVE_OK, Map.of(
                                "x", result.getX(),
                                "y", result.getY(),
                                "color", room.getGame().getState().getStone(
                                        result.getX(), result.getY()
                                )
                        )
                        ));

                // 승자, 게임 종료
                broadcaster.broadcastAll(room,
                        new WsMessage<>(
                                MessageType.GAME_END,
                                Map.of("winner", result.getWinnerId())
                        ));

                handleGameEnd(room);
                // 필요하면 cleanUp() 호출 정책 결정
                // cleanUp();
            }

            case DRAW -> {
                broadcaster.broadcastAll(room,
                        new WsMessage<>(MessageType.GAME_END,
                                Map.of("reason", "DRAW"))
                );
                handleGameEnd(room);
            }
        }
    }

    // 채팅 처리
    public void handleChat(String roomId, int userId, String message) {
        Room room = getRoomOrThrow(roomId);

        boolean isPlayer = room.isPlayer(userId);

        Map<String, Object> payload;

        if (isPlayer) {
            int playerIndex = room.getPlayerIndex(userId); // 1 or 2

            payload = Map.of(
                    "senderRole", "PLAYER",
                    "playerIndex", playerIndex,
                    "message", message
            );
        } else {
            payload = Map.of(
                    "senderRole", "SPECTATOR",
                    "message", message
            );
        }

        broadcaster.broadcastAll(
                room,
                new WsMessage<>(MessageType.CHAT, payload)
        );
    }

    // 게임 종료 처리(공통 게임 종료 후 처리용)
    private void handleGameEnd(Room room) {
        room.endGame();

        // DB 저장
        // 게임 상태에서 승자 ID 가져오기
        int winnerId = room.getGame().getState().getWinnerId();

        // 승자가 있을 경우 (무승부가 아님) DB 업데이트
        if (winnerId != -1) {
            for (Integer playerId : room.getPlayers()) {
                boolean isWin = (playerId == winnerId);
                // DAO 호출: 이긴 사람은 true, 진 사람은 false 전달
                recordDAO.updateRating(playerId, isWin);
            }
        }

        // TODO: 방 제거 여부 판단
        // 방 제거 정책. 필요하면 cleanUp() 호출 정책 결정
        // cleanUp();
        roomManager.removeRoom(room.getRoomId());

        // TODO: LobbyWebSocket에서 방 목록 실시간 갱신을 위한 broadcast 추가
    }


    /// ////////////// 유틸 ////////////////////

    private Room getRoomOrThrow(String roomId) {
        Room room = roomManager.getRoomById(roomId);
        if (room == null) {
            throw new IllegalArgumentException("방이 존재하지 않습니다: " + roomId);
        }

        return room;
    }

    private Map<String, Object> buildUserPayload(int userId, String role) {
        UserVO userVO = null;
        try {
            userVO = userService.getUserById(userId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        String nickname = (userVO == null)
                ? "Guest_" + userId
                : userVO.getNickname();

        String profileImg = (userVO == null || userVO.getProfileImg() == null)
                ? "/omok/static/img/profiles/p1.png"
                : "/omok" + userVO.getProfileImg();

        return Map.of(
                "userId", userId,
                "nickname", nickname,
                "profileImg", profileImg,
                "role", role
        );
    }

    // RoomService.java 내부 하단에 추가

    private Map<String, Object> createUserInfoMap(int userId, boolean spectator) {
        String nickname;
        String profileImg;

        try {
            // DB에서 유저 정보 조회
            UserVO userVO = userService.getUserById(userId);

            if (userVO != null) {
                // 회원인 경우: 실제 닉네임과 프로필 사진 사용
                nickname = userVO.getNickname(); // UserVO 필드명에 맞게 수정 (getUserNickname 등)
                profileImg = "/omok" + userVO.getProfileImg(); // UserVO 필드명에 맞게 수정
            } else {
                // 비회원(DB에 없음)인 경우: 기본값 사용
                nickname = "Guest_" + userId;
                profileImg = "/omok/static/img/profile/p1.png"; // 기본 프로필 이미지 경로
            }
        } catch (Exception e) {
            // 에러 발생 시에도 안전하게 기본값 사용
            nickname = "Guest_" + userId;
            profileImg = "/omok/static/img/profile/p1.png";
        }

        // 클라이언트(JS)로 보낼 데이터 맵 구성
        return Map.of(
                "userId", userId,
                "nickname", nickname,
                "profileImg", profileImg,
                "role", spectator ? "SPECTATOR" : "PLAYER"
        );
    }

    // 닉네임 조회 헬퍼 메서드 (onJoin과 로직 공유 가능)
    private String getNicknameSafe(int userId) {
        try {
            UserVO userVO = userService.getUserById(userId);
            return (userVO != null) ? userVO.getNickname() : "Guest_" + userId;
        } catch (Exception e) {
            return "Guest_" + userId;
        }
    }
}
