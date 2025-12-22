package team.omok.omok_mini_project.service;

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
    // TODO: GAME_START 일 때, myColor, firstTurn, userId 브로드캐스트
    public void onJoin(String roomId, int userId, Session session, boolean spectator) {
        Room room = getRoomOrThrow(roomId);
        System.out.println("[onJoin] boolean spectator=" + spectator);
        JoinResult result = room.addSession(userId, session, spectator);

        System.out.println("[onJoin] JoinResult=" + result);
        String nickname = null;
        String profileImg = null;

        switch (result) {

            case PLAYER_JOINED -> {
                // 플레이어 정보 브로드캐스트
                try {
                    UserVO userVO = userService.getUserById(userId);
                    System.out.println("[onJoin] UserVO=" + userVO);

                    if (userVO == null) {
                        nickname = "Guest_" + userId;
                        profileImg = "/omok/static/img/profiles/p1.png";
                    } else {
                        nickname = userVO.getNickname();
                        profileImg = userVO.getProfileImg();
                        if(profileImg == null) profileImg = "/omok/static/img/profiles/p1.png";
                    }
                    broadcaster.broadcastAll(room,
                            new WsMessage<>(
                                    MessageType.JOIN,
                                    Map.of(
                                            // "userId", userVO.getUserId(),  // 굳이 전달해야하나?
                                            "nickname", nickname,
                                            "profileImg", profileImg,
                                            "role", "PLAYER"
                                    )
                            )
                    );
                } catch (Exception e) {
                }
            }

            // 관전자 입장
            case SPECTATOR_JOINED -> {
                // 관전자의 닉네임 전달
                try {
                    UserVO userVO = userService.getUserById(userId);
                    System.out.println("[onJoin] UserVO=" + userVO);

                    // TODO: 관전자가 비회원인 경우 -> 임시 사용
                    if (userVO == null) {
                        nickname = "Guest_" + userId;
                    } else {
                        nickname = userVO.getNickname();
                    }
                    broadcaster.broadcastAll(room,
                            new WsMessage<>(
                                    MessageType.JOIN,
                                    Map.of(
                                            "nickname", nickname,
                                            "role", "SPECTATOR"
                                    )
                            )
                    );
                } catch (Exception e) { }
            }

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
    public void onLeave(String roomId, int userId, Session session) {
        Room room = getRoomOrThrow(roomId);

        LeaveResult result = room.removeSession(userId, session);

        switch (result) {
            case PLAYER_LEFT_DURING_GAME -> {
                broadcaster.broadcastToPlayers(room,
                        new WsMessage<>(MessageType.LEAVE,
                                Map.of("reason", "PLAYER GG"))
                );
                handleGameEnd(room);
            }

            case PLAYER_LEFT_BEFORE_START -> {
                broadcaster.broadcastToPlayers(room,
                        new WsMessage<>(MessageType.LEAVE,
                                Map.of("reason", "PLAYER_LEFT"))
                );
            }

            case ROOM_EMPTY -> {
                broadcaster.broadcastAll(room,
                        new WsMessage<>(MessageType.GAME_END,
                                Map.of("reason", "ROOM EMPTY"))
                );
                handleGameEnd(room);
            }

            case SPECTATOR_LEFT -> {
                // TODO: 채팅창에 "[]님이 나갔습니다." 출력하기
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

        broadcaster.broadcastAll(room,
                new WsMessage<>(
                        MessageType.CHAT,
                        Map.of(
                                "senderId", userId,     // 보내는 사람
                                "message", message
                        )
                ));
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
}
