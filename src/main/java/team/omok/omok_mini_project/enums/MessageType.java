package team.omok.omok_mini_project.enums;

public enum MessageType {
    JOIN,
    LEAVE,
    ROOM_WAIT,
    ROOM_READY,
    ROOM_END,
    COUNTDOWN,
    GAME_START,
    MOVE,       // 클라이언트 -> 서버
    MOVE_OK,    // 서버 -> 클라이언트
    GAME_END,
    CHAT,
    ERROR,
    BOARD_SNAPSHOT,
    ROOM_MEMBERS,
}
