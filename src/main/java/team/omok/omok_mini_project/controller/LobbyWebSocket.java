package team.omok.omok_mini_project.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import team.omok.omok_mini_project.domain.Room;
import team.omok.omok_mini_project.domain.dto.RankingDTO;
import team.omok.omok_mini_project.domain.vo.UserVO;
import team.omok.omok_mini_project.manager.RoomManager;
import team.omok.omok_mini_project.repository.RecordDAO;
import team.omok.omok_mini_project.service.UserService;
import team.omok.omok_mini_project.util.HttpSessionConfigurator;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 통합된 로비 WebSocket 엔드포인트
 * (기존 LobbyManager 기능 통합)
 * <p>
 * 기능:
 * - 로비 접속/퇴장 처리
 * - 방 목록 실시간 업데이트
 * - 랭킹 실시간 업데이트
 * - 로비 채팅 메시지 송수신
 * - 유저 프로필 실시간 변경
 * <p>
 * URL: ws://localhost:8080/omok/ws/lobby
 */
@ServerEndpoint(
        configurator = HttpSessionConfigurator.class,
        value = "/ws/lobby"
)
public class LobbyWebSocket {

    // ========== Static 필드: 전체 로비 세션 관리 ==========

    // 로비에 접속한 모든 WebSocket 세션 (thread-safe)
    private static final Set<Session> lobbySessions = ConcurrentHashMap.newKeySet();

    // JSON 변환용 ObjectMapper
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // RoomManager 참조 (방 목록 가져오기 위해)
    private static final RoomManager roomManager = RoomManager.getInstance();

    // UserService 참조 (유저 정보 조회)
    private final UserService userService = new UserService();

    // ========== WebSocket 생명주기 메서드 ==========

    /**
     * 모든 로비 유저에게 현재 방 목록을 전송
     * 방 생성/삭제/상태 변경 시 호출됨
     * <p>
     * ★ RoomManager에서 직접 호출 (createRoom, enterRoom, removeRoom 후)
     */
    public static void broadcastRoomList() {
        try {
            // RoomManager에서 대기 중인 방 목록 가져오기
            List<Room> waitingRooms = roomManager.getWaitingRooms();

            // Room 객체에서 필요한 정보만 추출 (JSON 직렬화 문제 해결)
            List<Map<String, Object>> roomData = new ArrayList<>();
            for (Room room : waitingRooms) {
                Map<String, Object> roomInfo = new HashMap<>();
                roomInfo.put("roomId", room.getRoomId());
                roomInfo.put("ownerId", room.getOwnerId());
                roomInfo.put("players", room.getPlayers());  // List<Integer>
                roomInfo.put("status", room.getStatus().toString());
                roomData.add(roomInfo);
            }

            // JSON 메시지 구성
            Map<String, Object> message = new HashMap<>();
            message.put("type", "ROOM_LIST");
            message.put("rooms", roomData);

            // 모든 로비 세션에게 전송
            broadcast(message);

            System.out.println("[LobbyWS] 방 목록 브로드캐스트: " + waitingRooms.size() + "개");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 모든 로비 유저에게 현재 랭킹을 전송
     * 게임 종료 후 전적 업데이트 시 호출
     */
    public static void broadcastRanking() {
        try {
            // DB에서 TOP 10 랭킹 조회
            List<RankingDTO> rankingList = RecordDAO.getTopRank();

            // JSON 메시지 구성
            Map<String, Object> message = new HashMap<>();
            message.put("type", "RANKING");
            message.put("rankings", rankingList);

            // 모든 로비 세션에게 전송
            broadcast(message);

            System.out.println("[LobbyWS] 랭킹 브로드캐스트: " + rankingList.size() + "명");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 모든 로비 유저에게 채팅 메시지 전송
     *
     * @param nickname    메시지 보낸 유저 닉네임
     * @param chatMessage 채팅 내용
     */
    public static void broadcastChat(String nickname, String chatMessage) {
        try {
            // JSON 메시지 구성
            Map<String, Object> message = new HashMap<>();
            message.put("type", "CHAT");
            message.put("nickname", nickname);
            message.put("message", chatMessage);

            // 모든 로비 세션에게 전송
            broadcast(message);

            System.out.println("[LobbyWS] 채팅 브로드캐스트: " + nickname + " - " + chatMessage);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 모든 로비 유저에게 프로필 변경 사항 전송
     *
     * @param userId     변경된 유저 ID
     * @param nickname   새 닉네임
     * @param profileImg 새 프로필 이미지 경로
     */
    public static void broadcastProfileUpdate(int userId, String nickname, String profileImg) {
        try {
            // JSON 메시지 구성
            Map<String, Object> message = new HashMap<>();
            message.put("type", "PROFILE_UPDATE");
            message.put("userId", userId);
            message.put("nickname", nickname);
            message.put("profileImg", profileImg);

            // 모든 로비 세션에게 전송
            broadcast(message);

            System.out.println("[LobbyWS] 프로필 변경 브로드캐스트: userId=" + userId);

            // 랭킹도 함께 업데이트 (닉네임 변경 반영)
            broadcastRanking();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ========== 브로드캐스트 메서드 (static - RoomManager에서 호출) ==========

    /**
     * 모든 로비 세션에게 메시지 전송 (내부 유틸 메서드)
     *
     * @param message 전송할 객체 (Map 등)
     */
    private static void broadcast(Object message) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(message);

            for (Session session : lobbySessions) {
                try {
                    // 세션이 열려있는지 확인
                    if (session.isOpen()) {
                        session.getBasicRemote().sendText(jsonMessage);
                    }
                } catch (Exception e) {
                    System.err.println("[LobbyWS] 메시지 전송 실패: " + session.getId());
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 현재 로비에 접속한 인원 수 반환
     */
    public static int getLobbyUserCount() {
        return lobbySessions.size();
    }

    /**
     * WebSocket 연결 시 호출
     * - 세션을 lobbySessions에 등록
     * - 현재 방 목록 + 랭킹을 클라이언트에게 전송
     */
    @OnOpen
    public void onOpen(Session session) throws IOException {
        System.out.println("[LobbyWS] 연결 성공: sessionId=" + session.getId());

        // 로비 세션 등록
        lobbySessions.add(session);
        System.out.println("[LobbyWS] 세션 추가 | 현재 로비 인원: " + lobbySessions.size());

        // 연결 확인 메시지 전송
        sendToSession(session, Map.of(
                "type", "CONNECTED",
                "message", "로비 접속 성공"
        ));

        // 현재 방 목록 전송 (최초 접속 시)
        broadcastRoomList();

        // 현재 랭킹 전송
        broadcastRanking();
    }

    /**
     * 클라이언트로부터 메시지 수신 시 호출
     * <p>
     * 메시지 타입:
     * - CHAT: 채팅 메시지 → 모든 로비 유저에게 전송
     * - REQUEST_ROOM_LIST: 방 목록 요청 → 방 목록 전송
     * - REQUEST_RANKING: 랭킹 요청 → 랭킹 전송
     * - UPDATE_PROFILE: 프로필 변경 → 변경 사항 브로드캐스트
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        try {
            System.out.println("[LobbyWS] 메시지 수신: " + message);

            // JSON 메시지 파싱
            Map<String, Object> data = objectMapper.readValue(message, Map.class);
            String type = (String) data.get("type");

            // 메시지 타입에 따라 처리
            switch (type) {
                case "CHAT":
                    // 채팅 메시지 처리
                    handleChatMessage(session, data);
                    break;

                case "REQUEST_ROOM_LIST":
                    // 방 목록 요청 처리
                    broadcastRoomList();
                    break;

                case "REQUEST_RANKING":
                    // 랭킹 요청 처리
                    broadcastRanking();
                    break;

                case "UPDATE_PROFILE":
                    // 프로필 변경 처리
                    handleProfileUpdate(session, data);
                    break;

                default:
                    System.out.println("[LobbyWS] 알 수 없는 메시지 타입: " + type);
            }

        } catch (Exception e) {
            System.err.println("[LobbyWS] 메시지 처리 중 오류 발생");
            e.printStackTrace();
        }
    }

    /**
     * WebSocket 연결 종료 시 호출
     * - 세션을 lobbySessions에서 제거
     */
    @OnClose
    public void onClose(Session session) {
        System.out.println("[LobbyWS] 연결 종료: sessionId=" + session.getId());
        lobbySessions.remove(session);
        System.out.println("[LobbyWS] 세션 제거 | 현재 로비 인원: " + lobbySessions.size());
    }

    // ========== 핸들러 메서드 ==========

    /**
     * 에러 발생 시 호출
     */
    @OnError
    public void onError(Session session, Throwable error) {
        System.err.println("[LobbyWS] 에러 발생: sessionId=" + session.getId());
        error.printStackTrace();
    }

    /**
     * 채팅 메시지 처리 (private 헬퍼 메서드)
     * - 세션에서 유저 정보 가져오기
     * - 모든 로비 유저에게 전송
     */
    private void handleChatMessage(Session session, Map<String, Object> data) {
        try {
            // 세션에서 유저 정보 가져오기 (HttpSessionConfigurator가 설정함)
            Integer userId = (Integer) session.getUserProperties().get("user_id");

            if (userId == null) {
                System.err.println("[LobbyWS] 유저 정보 없음 - 로그인 필요");
                return;
            }

            // 채팅 메시지 내용 추출
            String chatMessage = (String) data.get("message");

            // 실제 닉네임 조회
            String nickname = "유저" + userId; // 기본값
            try {
                UserVO user = userService.getUserById(userId);
                if (user != null && user.getNickname() != null) {
                    nickname = user.getNickname();
                }
            } catch (Exception e) {
                System.err.println("[LobbyWS] 닉네임 조회 실패, 기본값 사용");
            }

            // 모든 로비 유저에게 채팅 브로드캐스트
            broadcastChat(nickname, chatMessage);

        } catch (Exception e) {
            System.err.println("[LobbyWS] 채팅 메시지 처리 중 오류");
            e.printStackTrace();
        }
    }

    // ========== 유틸리티 메서드 ==========

    /**
     * 프로필 변경 처리
     * - DB 업데이트 후 모든 로비 유저에게 변경 사항 전송
     */
    private void handleProfileUpdate(Session session, Map<String, Object> data) {
        try {
            Integer userId = (Integer) session.getUserProperties().get("user_id");

            if (userId == null) {
                System.err.println("[LobbyWS] 유저 정보 없음 - 로그인 필요");
                return;
            }

            String newNickname = (String) data.get("nickname");
            String newProfileImg = (String) data.get("profileImg");

            // TODO: UserService를 통해 DB 업데이트 로직 추가
            // userService.updateProfile(userId, newNickname, newProfileImg);

            // 변경 사항 브로드캐스트
            broadcastProfileUpdate(userId, newNickname, newProfileImg);

        } catch (Exception e) {
            System.err.println("[LobbyWS] 프로필 변경 처리 중 오류");
            e.printStackTrace();
        }
    }

    /**
     * 특정 세션에만 메시지 전송
     */
    private void sendToSession(Session session, Object message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            if (session.isOpen()) {
                session.getBasicRemote().sendText(json);
            }
        } catch (Exception e) {
            System.err.println("[LobbyWS] 세션 전송 실패: " + session.getId());
            e.printStackTrace();
        }
    }
}
