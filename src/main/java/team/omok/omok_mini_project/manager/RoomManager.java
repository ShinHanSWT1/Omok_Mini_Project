package team.omok.omok_mini_project.manager;

import team.omok.omok_mini_project.domain.Room;
import team.omok.omok_mini_project.domain.UserVO;

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
 */
public class RoomManager {
    private static final RoomManager instance = new RoomManager();          // 싱글톤 인스턴스
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();      // 서버에 존재하는 방 리스트(roomId, Room)

    public static RoomManager getInstance() {
        return instance;
    }

    public Room createRoom(String userId) {
        String roomId = UUID.randomUUID().toString();
        Room room = new Room(roomId, userId);
        rooms.put(roomId, room);

        return room;
    }

    public void enterRoom(String roomId, UserVO user) {
        Room room = rooms.get(roomId);
        if (room == null) {
            throw new IllegalArgumentException("방이 존재하지 않습니다");
        }
        room.tryAddPlayer(user.getId());
    }

    public boolean removeRoom(String roomId){
        return rooms.remove(roomId) != null;
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
                .toList();
    }

}