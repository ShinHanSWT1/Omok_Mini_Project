package team.omok.omok_mini_project.controller;

import team.omok.omok_mini_project.domain.Room;
import team.omok.omok_mini_project.domain.dto.MovePayload;
import team.omok.omok_mini_project.domain.dto.WsMessage;
import team.omok.omok_mini_project.enums.MessageType;
import team.omok.omok_mini_project.manager.RoomManager;
import team.omok.omok_mini_project.util.HttpSessionConfigurator;
import team.omok.omok_mini_project.util.JsonUtil;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@ServerEndpoint(
        configurator = HttpSessionConfigurator.class,
        value = "/ws/game/{roomId}"
)
public class GameWebSocket {

    private static final RoomManager roomManager = RoomManager.getInstance();

    // 세션 userProperties 키 (문자열 상수로 고정해두면 실수 줄어듦)
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_WS_ROLE = "ws_role";

    private enum WsRole {
        PLAYER, SPECTATOR
    }

    @OnOpen
    public void onOpen(Session session,
                       @PathParam("roomId") String roomId) throws IOException {

        int userId = getUserId(session);

        // ✅ 1) 쿼리 파라미터로 role 판별: /ws/game/{roomId}?role=spectator
        // 기본값은 spectator로 두는 걸 추천 (명시적으로 player로 들어온 경우만 플레이어 취급)
        String roleParam = getQueryParam(session, "role", "spectator");
        WsRole role = parseRole(roleParam);

        // ✅ 2) 세션에 role 저장 (onMessage에서 MOVE 차단할 때 사용)
        session.getUserProperties().put(KEY_WS_ROLE, role.name());

        System.out.printf("[WS OPEN] roomId=%s, userId=%d, role=%s, sessionId=%s%n",
                roomId, userId, role.name(), session.getId());

        // 방 존재 여부 확인
        Room room = roomManager.getRoomById(roomId);
        if (room == null) {
            session.close();
            return;
        }

        // ✅ 3) Room에 역할 포함 등록 (Room에 오버로드 있으면 그걸 쓰고, 없으면 기존 메서드로 fallback)
        try {
            // Room에 addSession(int, Session, boolean isSpectator) 를 만들었을 경우
            boolean isSpectator = (role == WsRole.SPECTATOR);
            room.addSession(userId, session, isSpectator);
        } catch (NoSuchMethodError e) {
            // 아직 Room이 역할 버전 메서드 없으면 일단 기존 메서드로 등록 (임시)
            room.addSession(userId, session);
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        System.out.println("[WS MESSAGE] " + message);
        try {
            Room room = roomManager.getRoomById(getRoomId(session));
            if (room == null) return;

            int userId = getUserId(session);

            WsMessage<?> wsMessage = JsonUtil.MAPPER.readValue(message, WsMessage.class);

            switch (wsMessage.getType()) {
                case MOVE -> {
                    // ✅ 관전자는 착수 불가
                    if (isSpectator(session)) {
                        sendError(session, "SPECTATOR_CANNOT_MOVE");
                        return;
                    }

                    MovePayload payload = JsonUtil.MAPPER.convertValue(
                            wsMessage.getPayload(),
                            MovePayload.class
                    );

                    room.handleMove(userId, payload.getX(), payload.getY());
                }

                case CHAT -> {
                    String msg = String.valueOf(wsMessage.getPayload());
                    room.handleChat(userId, msg);
                }

                default -> sendError(session, "UNSUPPORTED_MESSAGE");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendError(session, "INVALID_MESSAGE_FORMAT");
        }
    }

    @OnClose
    public void onClose(Session session) {
        try {
            Room room = roomManager.getRoomById(getRoomId(session));
            if (room == null) return;

            int userId = getUserId(session);

            System.out.printf("[WS CLOSE] roomId=%s, userId=%d, role=%s, sessionId=%s%n",
                    getRoomId(session), userId, getRole(session).name(), session.getId()
            );

            room.removeSession(userId, session);
        } catch (Exception ignored) {
        }

        System.out.println("[WS] 연결 종료");
    }

    @OnError
    public void onError(Session session, Throwable error) {
        error.printStackTrace();
    }

    private void sendError(Session session, String message) {
        try {
            session.getBasicRemote().sendText(JsonUtil.MAPPER.writeValueAsString(
                    new WsMessage<>(MessageType.ERROR, message)
            ));
        } catch (IOException ignored) {
        }
    }

    private String getRoomId(Session session) {
        return session.getPathParameters().get("roomId");
    }

    private int getUserId(Session session) {
        Object v = session.getUserProperties().get(KEY_USER_ID);
        if (v == null) {
            // user_id가 없으면 인증/세션 주입이 안 된 상태라 보는 게 안전
            throw new IllegalStateException("user_id not found in session userProperties");
        }
        return Integer.parseInt(String.valueOf(v));
    }

    // ===== 관전자 관련 헬퍼 =====

    private boolean isSpectator(Session session) {
        return getRole(session) == WsRole.SPECTATOR;
    }

    private WsRole getRole(Session session) {
        Object role = session.getUserProperties().get(KEY_WS_ROLE);
        if (role == null) return WsRole.SPECTATOR; // 안전 기본값
        try {
            return WsRole.valueOf(role.toString());
        } catch (Exception e) {
            return WsRole.SPECTATOR;
        }
    }

    private WsRole parseRole(String roleParam) {
        if (roleParam == null) return WsRole.SPECTATOR;
        String v = roleParam.trim().toLowerCase();

        // player로 명시된 경우만 PLAYER, 나머진 전부 SPECTATOR로 안전 처리
        if (v.equals("player") || v.equals("play")) return WsRole.PLAYER;

        // spectator/viewer/watch 등은 관전
        return WsRole.SPECTATOR;
    }

    private String getQueryParam(Session session, String key, String defaultValue) {
        Map<String, List<String>> params = session.getRequestParameterMap();
        if (params == null) return defaultValue;

        List<String> values = params.get(key);
        if (values == null || values.isEmpty()) return defaultValue;

        String v = values.get(0);
        return (v == null || v.isBlank()) ? defaultValue : v;
    }
}
