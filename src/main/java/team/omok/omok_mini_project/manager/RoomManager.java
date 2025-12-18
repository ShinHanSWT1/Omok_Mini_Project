package team.omok.omok_mini_project.manager;

import team.omok.omok_mini_project.domain.Room;
import team.omok.omok_mini_project.domain.UserVO;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RoomManager {
    // 단 하나의 인스턴스만 존재
    private static final RoomManager instance = new RoomManager();
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();


    public static RoomManager getInstance() {
        return instance;
    }

    public Room getRoom(String roomId) {
        return rooms.get(roomId);
    }

    public Room createRoom(String login_id) {
        String roomId = UUID.randomUUID().toString();
        Room room = new Room(roomId, String.valueOf(login_id));
        rooms.put(roomId, room);

        return room;
    }

    // 기존 코드 (FIFO 적용 전)
    // public List<Room> getWaitingRooms() {
    //     return rooms.values().stream()
    //             .filter(room -> !room.isFull())
    //             .toList();
    // }

    // 새 코드 (FIFO 적용: 생성 시간 순 정렬)
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

    public void enterRoom(String roomId, UserVO user) {
        Room room = rooms.get(roomId);
        if (room == null) {
            throw new IllegalArgumentException("방 없음");
        }
        room.addPlayer(String.valueOf(user.getUserId()));
    }
}
