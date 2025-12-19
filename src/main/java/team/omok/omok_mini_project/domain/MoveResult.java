package team.omok.omok_mini_project.domain;

import team.omok.omok_mini_project.enums.GameResultType;

/* 한 수(착수) 처리 결과.
 - type: 결과 타입 (MOVE_OK / INVALID_TURN / INVALID_POSITION / WIN / DRAW)
 - x, y: 착수 좌표 (좌표가 의미 없으면 -1)
 - winnerId: WIN일 때만 유효, 그 외 -1
 - reason: 실패/종료 사유(없으면 null)
 */
public class MoveResult {

    private static final int NONE = -1;

    // 결과 타입
    private final GameResultType type;

    // 착수 정보
    private final int x;
    private final int y;

    // 승자 (WIN일 때만)
    private final int winnerId;

    // 실패/종료 사유
    private final String reason;

    private MoveResult(GameResultType type, int x, int y, int winnerId, String reason) {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        // WIN이면 winnerId는 반드시 필요
        if (type == GameResultType.WIN && winnerId == NONE) {
            throw new IllegalArgumentException("winnerId must be set when type is WIN");
        }
        // WIN이 아닌데 winnerId가 있으면 혼란 방지
        if (type != GameResultType.WIN && winnerId != NONE) {
            throw new IllegalArgumentException("winnerId must be -1 unless type is WIN");
        }

        this.type = type;
        this.x = x;
        this.y = y;
        this.winnerId = winnerId;
        this.reason = reason;
    }

    // Factory methods

    // 정상 착수
    public static MoveResult moveOk(int x, int y) {
        return new MoveResult(GameResultType.MOVE_OK, x, y, NONE, null);
    }

    // 턴 아님
    public static MoveResult invalidTurn(String reason) {
        return new MoveResult(GameResultType.INVALID_TURN, NONE, NONE, NONE, reason);
    }

    // 범위 밖 / 이미 둔 자리
    public static MoveResult invalidPosition(String reason) {
        return new MoveResult(GameResultType.INVALID_POSITION, NONE, NONE, NONE, reason);
    }

    // 승리
    public static MoveResult win(int x, int y, int winnerId) {
        return new MoveResult(GameResultType.WIN, x, y, winnerId, null);
    }

    // 무승부(선택)
    public static MoveResult draw(String reason) {
        String msg = (reason == null || reason.isBlank()) ? "draw" : reason;
        return new MoveResult(GameResultType.DRAW, NONE, NONE, NONE, msg);
    }

    // Getters
    public GameResultType getType() {
        return type;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWinnerId() {
        return winnerId;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return "MoveResult{type=" + type +
                ", x=" + x +
                ", y=" + y +
                ", winnerId=" + winnerId +
                ", reason='" + reason + '\'' +
                '}';
    }
}
