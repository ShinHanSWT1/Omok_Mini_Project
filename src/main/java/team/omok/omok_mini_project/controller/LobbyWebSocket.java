package team.omok.omok_mini_project.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import team.omok.omok_mini_project.domain.UserVO;
import team.omok.omok_mini_project.manager.LobbyManager;
import team.omok.omok_mini_project.util.HttpSessionConfigurator;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Map;

/**
 * 로비 실시간 통신을 위한 WebSocket 엔드포인트
 *
 * 기능:
 * - 로비 접속/퇴장 처리
 * - 방 목록 실시간 업데이트
 * - 로비 채팅 메시지 송수신
 *
 * URL: ws://localhost:8080/omok/ws/lobby
 */
@ServerEndpoint(
        configurator = HttpSessionConfigurator.class,
        value = "/ws/lobby"
)
public class LobbyWebSocket {

    // LobbyManager 싱글톤 인스턴스
    private static final LobbyManager lobbyManager = LobbyManager.getInstance();

    // JSON 파싱용 ObjectMapper
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * WebSocket 연결 시 호출
     * - 세션을 LobbyManager에 등록
     * - 현재 방 목록을 클라이언트에게 전송
     */
    @OnOpen
    public void onOpen(Session session) throws IOException {
        System.out.println("[LobbyWebSocket] 연결 성공: sessionId=" + session.getId());

        // 로비 세션 등록
        lobbyManager.addSession(session);

        // 연결 확인 메시지 전송
        session.getBasicRemote().sendText("{\"type\":\"CONNECTED\",\"message\":\"로비 접속 성공\"}");

        // 현재 방 목록 전송 (최초 접속 시)
        lobbyManager.broadcastRoomList();
    }

    /**
     * 클라이언트로부터 메시지 수신 시 호출
     *
     * 메시지 타입:
     * - CHAT: 채팅 메시지 → 모든 로비 유저에게 전송
     * - REQUEST_ROOM_LIST: 방 목록 요청 → 방 목록 전송
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        try {
            System.out.println("[LobbyWebSocket] 메시지 수신: " + message);

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
                    lobbyManager.broadcastRoomList();
                    break;

                default:
                    System.out.println("[LobbyWebSocket] 알 수 없는 메시지 타입: " + type);
            }

        } catch (Exception e) {
            System.err.println("[LobbyWebSocket] 메시지 처리 중 오류 발생");
            e.printStackTrace();
        }
    }

    /**
     * WebSocket 연결 종료 시 호출
     * - 세션을 LobbyManager에서 제거
     */
    @OnClose
    public void onClose(Session session) {
        System.out.println("[LobbyWebSocket] 연결 종료: sessionId=" + session.getId());
        lobbyManager.removeSession(session);
    }

    /**
     * 에러 발생 시 호출
     */
    @OnError
    public void onError(Session session, Throwable error) {
        System.err.println("[LobbyWebSocket] 에러 발생: sessionId=" + session.getId());
        error.printStackTrace();
    }

    /**
     * 채팅 메시지 처리 (private 헬퍼 메서드)
     * - 세션에서 유저 정보 가져오기
     * - LobbyManager를 통해 모든 로비 유저에게 전송
     */
    private void handleChatMessage(Session session, Map<String, Object> data) {
        try {
            // 세션에서 유저 정보 가져오기 (HttpSessionConfigurator가 설정함)
            Integer userId = (Integer) session.getUserProperties().get("user_id");

            if (userId == null) {
                System.err.println("[LobbyWebSocket] 유저 정보 없음 - 로그인 필요");
                return;
            }

            // 채팅 메시지 내용 추출
            String chatMessage = (String) data.get("message");

            // 닉네임 처리 (TODO: 나중에 실제 닉네임으로 변경)
            String nickname = "유저" + userId;

            // 모든 로비 유저에게 채팅 브로드캐스트
            lobbyManager.broadcastChat(nickname, chatMessage);

        } catch (Exception e) {
            System.err.println("[LobbyWebSocket] 채팅 메시지 처리 중 오류");
            e.printStackTrace();
        }
    }
}
