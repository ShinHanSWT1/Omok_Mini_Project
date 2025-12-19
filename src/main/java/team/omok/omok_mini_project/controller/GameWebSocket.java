package team.omok.omok_mini_project.controller;

import team.omok.omok_mini_project.domain.Room;
import team.omok.omok_mini_project.manager.RoomManager;
import team.omok.omok_mini_project.util.HttpSessionConfigurator;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.PathParam;
import java.io.IOException;
import java.util.Objects;

@ServerEndpoint(
        configurator = HttpSessionConfigurator.class,
        value = "/ws/game/{roomId}"
)
public class GameWebSocket {

    private static RoomManager roomManager = RoomManager.getInstance();
    private String roomId;

    @OnOpen
    public void onOpen(Session session,
                       @PathParam("roomId") String roomId) throws IOException {

        System.out.println("[WS OPEN] roomId=" + roomId
                + ", sessionId=" + session.getId());

        this.roomId = roomId;

        // 게임 방 확인
        Room room = roomManager.getRoomById(roomId);
        if (room == null){
            try {
                session.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        // 유저 확인
        int userId = Integer.parseInt(String.valueOf(session.getUserProperties().get("user_id")));

        // 방에 유저 등록 및 연결
        if(room != null){
            room.addSession(userId, session);
            System.out.println("[INFO]방 상태: " + room.getStatus());
            room.tryStartGame();
        }

        // 기존 코드 (JSON 파싱 에러 발생)
        // session.getBasicRemote().sendText("CONNECTED");

        // 수정: JSON 형식으로 연결 메시지 전송
        session.getBasicRemote().sendText("{\"type\":\"CONNECTED\",\"message\":\"게임방 연결 성공\"}");
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        System.out.println("[WS MESSAGE] " + message);

        Room room = roomManager.getRoomById(roomId);
        if (room != null) {
            room.broadcast(message);
        }
    }

    @OnClose
    public void onClose(Session session) {
        System.out.println("[WS CLOSE] sessionId=" + session.getId());
        Room room = roomManager.getRoomById(roomId);

        int userId = Integer.parseInt(String.valueOf(session.getUserProperties().get("user_id")));

        if (room != null) {
            room.removeSession(userId, session);
//            roomManager.removeRoom(roomId);
        }
        System.out.println("[WS] 연결 종료");
    }


    @OnError
    public void onError(Session session, Throwable error) {
        error.printStackTrace();
    }
}
