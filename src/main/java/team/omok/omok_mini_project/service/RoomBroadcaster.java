package team.omok.omok_mini_project.service;

import team.omok.omok_mini_project.domain.Room;
import team.omok.omok_mini_project.domain.dto.WsMessage;
import team.omok.omok_mini_project.util.JsonUtil;

import javax.websocket.Session;
import java.io.IOException;
import java.util.Collection;

public class RoomBroadcaster {

    // 방 플레이어들에게만 브로드캐스트
    public void broadcastToPlayers(Room room, WsMessage<?> message) {
        sendMessage(room.getPlayerSessionMap().values(), message);
    }

    // 방 관전자들에게만 브로드캐스트
    public void broadcastToSpectators(Room room, WsMessage<?> message) {
        sendMessage(room.getSpectatorSessions(), message);
    }

    // 방에 있는 모든 세션에 브로드캐스트
    public void broadcastAll(Room room, WsMessage<?> message) {
        broadcastToPlayers(room, message);
        broadcastToSpectators(room, message);
    }

    // 하나의 세션에만 브로드캐스트
    public void broadcastToSession(Session session, WsMessage<?> message) {
        try {
            String json = JsonUtil.MAPPER.writeValueAsString(message);
            session.getBasicRemote().sendText(json);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // 메세지 전달
    private void sendMessage(Collection<Session> sessions, Object message) {
        try {
            String json = JsonUtil.MAPPER.writeValueAsString(message);
            for (Session s : sessions) {
                if (s != null && s.isOpen()) {
                    s.getBasicRemote().sendText(json);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
