package team.omok.omok_mini_project.manager;

import team.omok.omok_mini_project.controller.LobbyWebSocket;
import team.omok.omok_mini_project.domain.Room;
import team.omok.omok_mini_project.domain.vo.UserVO;

import javax.websocket.Session;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 서버 전체에 존재하는 모든 room을 관리하며, 싱글톤으로 구현된다.
 * RoomManager는 Room 상태를 알지 못하며,
 * 오직 조회/생성/삭제만 수행한다.
 *
 * @function RoomManager.getInstance()
 * @function public Room getRoomById(String roomId)
 * @function public List<Room> getWaitingRooms()
 * @function public Room createRoom(String userId)
 * @function public boolean removeRoom(int roomId)
 * @see Room
 *
 */
public class RoomManager {
    private static final RoomManager instance = new RoomManager();          // 싱글톤 인스턴스
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();      // 서버에 존재하는 방 리스트(roomId, Room)

    public static RoomManager getInstance() {
        return instance;
    }


    public Room createRoom(int userId) {
        String roomId = UUID.randomUUID().toString();
        Room room = new Room(roomId, userId);
        rooms.put(roomId, room);
        System.out.println("[INFO]RoomManager - createRoom:" + roomId);

        // TODO:
        // 현재는 단순화를 위해 RoomManager에서 직접 LobbyWebSocket을 호출한다.
        // 추후 이벤트 기반 구조로 변경 시 제거 대상.
        //LobbyWebSocket.broadcastRoomList();

        return room;
    }

    public void enterRoom(String roomId, UserVO user) {
        Room room = rooms.get(roomId);
        if (room == null) {
            throw new IllegalArgumentException("방이 존재하지 않습니다");
        }
        System.out.println("[INFO]RoomManager - enterRoom:" + roomId);
        room.tryAddPlayer(user.getUserId());

        // TODO:
        // 현재는 단순화를 위해 RoomManager에서 직접 LobbyWebSocket을 호출한다.
        // 추후 이벤트 기반 구조로 변경 시 제거 대상.
        //LobbyWebSocket.broadcastRoomList();
    }

    public void enterRoomAsSpectator(String roomId, Session session){
        Room room = rooms.get(roomId);
        if (room == null) {
            throw new IllegalArgumentException("방이 존재하지 않습니다");
        }
        room.addSpectatorSession(session);
    }

    public boolean removeRoom(String roomId){
        boolean removed = rooms.remove(roomId) != null;

        // 로비에 방 목록 업데이트 전송 (실시간으로 방이 사라짐)
        if (removed) {
            LobbyManager.getInstance().broadcastRoomList();
        }

        return removed;
    }

    public Room getRoomById(String roomId) {
        return rooms.get(roomId);
    }

    public List<Room> getAllRooms() {
        return rooms.values().stream().toList();
    }

    public List<Room> getWaitingRooms() {
        return rooms.values().stream()
                .filter(room -> !room.isFull())
                .sorted(Comparator.comparingLong(Room::getCreatedAt))  // 생성 시간 오름차순
                .toList();
    }

    // 빠른 입장: 가장 먼저 생성된 대기 방 1개 반환
    public Room getFirstWaitingRoom() {
        return rooms.values().stream()
                .filter(room -> !room.isFull())
                .min(Comparator.comparingLong(Room::getCreatedAt))  // 가장 오래된 방
                .orElse(null);  // 대기 중인 방이 없으면 null
    }

}
