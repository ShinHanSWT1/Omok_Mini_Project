package team.omok.omok_mini_project.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import team.omok.omok_mini_project.domain.Room;

import javax.websocket.Session;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 로비에 접속한 모든 클라이언트의 WebSocket 세션을 관리하는 싱글톤 매니저
 *
 * 주요 기능:
 * - 로비 세션 추가/제거
 * - 방 목록 변경 시 모든 로비 유저에게 실시간 브로드캐스트
 * - 채팅 메시지를 모든 로비 유저에게 실시간 브로드캐스트
 */
public class LobbyManager {

    // 싱글톤 인스턴스
    private static final LobbyManager instance = new LobbyManager();

    // 로비에 접속한 모든 WebSocket 세션 (thread-safe)
    private final Set<Session> lobbySessions = ConcurrentHashMap.newKeySet();

    // JSON 변환용 ObjectMapper
    private final ObjectMapper objectMapper = new ObjectMapper();

    // RoomManager 참조 (방 목록 가져오기 위해)
    private final RoomManager roomManager = RoomManager.getInstance();

    // 싱글톤 패턴: 외부에서 new 생성 불가
    private LobbyManager() {}

    /**
     * LobbyManager 싱글톤 인스턴스 반환
     */
    public static LobbyManager getInstance() {
        return instance;
    }

    /**
     * 로비에 새로운 세션 추가
     * @param session WebSocket 세션
     */
    public void addSession(Session session) {
        lobbySessions.add(session);
        System.out.println("[LobbyManager] 세션 추가: " + session.getId()
                + " | 현재 로비 인원: " + lobbySessions.size());
    }

    /**
     * 로비에서 세션 제거
     * @param session WebSocket 세션
     */
    public void removeSession(Session session) {
        lobbySessions.remove(session);
        System.out.println("[LobbyManager] 세션 제거: " + session.getId()
                + " | 현재 로비 인원: " + lobbySessions.size());
    }

    /**
     * 모든 로비 유저에게 현재 방 목록을 전송
     * 방 생성/삭제/상태 변경 시 호출됨
     */
    public void broadcastRoomList() {
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

            // JSON 문자열로 변환
            String jsonMessage = objectMapper.writeValueAsString(message);

            // 모든 로비 세션에게 전송
            broadcast(jsonMessage);

            System.out.println("[LobbyManager] 방 목록 브로드캐스트: " + waitingRooms.size() + "개");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 모든 로비 유저에게 채팅 메시지 전송
     * @param nickname 메시지 보낸 유저 닉네임
     * @param chatMessage 채팅 내용
     */
    public void broadcastChat(String nickname, String chatMessage) {
        try {
            // JSON 메시지 구성
            Map<String, Object> message = new HashMap<>();
            message.put("type", "CHAT");
            message.put("nickname", nickname);
            message.put("message", chatMessage);

            // JSON 문자열로 변환
            String jsonMessage = objectMapper.writeValueAsString(message);

            // 모든 로비 세션에게 전송
            broadcast(jsonMessage);

            System.out.println("[LobbyManager] 채팅 브로드캐스트: " + nickname + " - " + chatMessage);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 모든 로비 세션에게 메시지 전송 (내부 유틸 메서드)
     * @param message 전송할 JSON 문자열
     */
    private void broadcast(String message) {
        for (Session session : lobbySessions) {
            try {
                // 세션이 열려있는지 확인
                if (session.isOpen()) {
                    session.getBasicRemote().sendText(message);
                }
            } catch (Exception e) {
                System.err.println("[LobbyManager] 메시지 전송 실패: " + session.getId());
                e.printStackTrace();
            }
        }
    }

    /**
     * 현재 로비에 접속한 인원 수 반환
     */
    public int getLobbyUserCount() {
        return lobbySessions.size();
    }
}

// pom.xml - jackson 라이브러리 추가
