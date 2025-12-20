package team.omok.omok_mini_project.controller;

import team.omok.omok_mini_project.domain.Room;
import team.omok.omok_mini_project.domain.dto.MovePayload;
import team.omok.omok_mini_project.domain.dto.WsMessage;
import team.omok.omok_mini_project.enums.MessageType;
import team.omok.omok_mini_project.manager.RoomManager;
import team.omok.omok_mini_project.util.HttpSessionConfigurator;
import team.omok.omok_mini_project.util.JsonUtil;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.PathParam;
import java.io.IOException;
import java.util.Map;

@ServerEndpoint(
        configurator = HttpSessionConfigurator.class,
        value = "/ws/game/{roomId}"
)
public class GameWebSocket {

    private static RoomManager roomManager = RoomManager.getInstance();
//    private String roomId;

    @OnOpen
    public void onOpen(Session session,
                       @PathParam("roomId") String roomId) throws IOException {

        System.out.printf("[WS OPEN] roomId=%s, userId=%d, sessionId=%s%n", roomId, getUserId(session), session.getId());

        //  방 존재 여부 확인 및 검증
        Room room = roomManager.getRoomById(roomId);
        if (room == null){
            try {
                session.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        // 방에 유저 등록 및 연결
        if(room != null){
            room.addSession(getUserId(session), session);
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        System.out.println("[WS MESSAGE] " + message);
        try {
            Room room = roomManager.getRoomById(getRoomId(session));
            if (room == null) return;

            int userId = getUserId(session);

            WsMessage<?> wsMessage =
                    JsonUtil.MAPPER.readValue(message, WsMessage.class);

            switch (wsMessage.getType()) {
                case MOVE -> {
                    MovePayload payload =
                            JsonUtil.MAPPER.convertValue(
                                    wsMessage.getPayload(),
                                    MovePayload.class
                            );

                    room.handleMove(userId, payload.getX(), payload.getY());
                }

                case CHAT -> {
                    String msg = (String) wsMessage.getPayload();
                    room.handleChat(userId, msg);
                }

                default -> {
                    sendError(session, "UNSUPPORTED_MESSAGE");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendError(session, "INVALID_MESSAGE_FORMAT");
        }

    }

    @OnClose
    public void onClose(Session session) {
        try{
            Room room = roomManager.getRoomById(getRoomId(session));
            if(room == null) return;

            int userId = getUserId(session);

            System.out.printf(
                    "[WS CLOSE] roomId=%s, userId=%d, sessionId=%s%n",
                    getRoomId(session), userId, session.getId()
            );
            room.removeSession(userId, session);

        }catch (Exception ignored) {}

        System.out.println("[WS] 연결 종료");
    }


    @OnError
    public void onError(Session session, Throwable error) {
        error.printStackTrace();
    }

    private void sendError(Session session, String message) {
        try {
            session.getBasicRemote().sendText(JsonUtil.MAPPER.writeValueAsString(
                    new WsMessage<>(MessageType.ERROR, message)));
        } catch (IOException ignored) {}
    }

    private String getRoomId(Session session) {
        return session.getPathParameters().get("roomId");
    }


    private int getUserId(Session session) {
        return Integer.parseInt(
                String.valueOf(session.getUserProperties().get("user_id"))
        );
    }


}
