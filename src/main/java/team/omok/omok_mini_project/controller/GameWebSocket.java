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

        Room room = roomManager.getRoomById(roomId);
        if (room == null){
            try {
                session.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        String user_id = (String) session.getUserProperties().get("user_id");
        Objects.requireNonNull(room).addSession(user_id, session);
        room.tryStartGame();

        session.getBasicRemote().sendText("CONNECTED");
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
        if (room != null) {
            room.removeSession(session);
        }
        System.out.println("[WS] 연결 종료");
    }


    @OnError
    public void onError(Session session, Throwable error) {
        error.printStackTrace();
    }
}