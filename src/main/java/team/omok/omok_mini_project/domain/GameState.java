package team.omok.omok_mini_project.domain;

import java.util.Arrays;

import team.omok.omok_mini_project.enums.GameStatus;
import team.omok.omok_mini_project.enums.Stone;

/*
 * 게임 상태 저장소(서버 authoritative).
 * - 보드(15x15)
 * - 현재 턴
 * - 게임 상태(진행/종료)
 * - 흑/백 유저 id (Users.user_id)
 * - 승자 id (종료 시 기록)
 *
 * 검증/승리판정/금수는 OmokRule에서만 한다.
 */
public class GameState {
    public static final int SIZE = 15;
    private static final int NONE = -1;

    private final Stone[][] board = new Stone[SIZE][SIZE];

    private Stone turn;
    private GameStatus status;

    // 유저 매핑 (DB Users.user_id)
    private int blackUserId;
    private int whiteUserId;

    // 승자 (Users.user_id), 없으면 -1
    private int winnerId;

    public GameState() {
        reset();
    }

    public void reset() {
        for (int y = 0; y < SIZE; y++) {
            Arrays.fill(board[y], Stone.EMPTY);
        }
        this.turn = Stone.BLACK;
        this.status = GameStatus.READY;

        this.blackUserId = NONE;
        this.whiteUserId = NONE;
        this.winnerId = NONE;
    }

    public static Stone opposite(Stone s) {
        if (s == Stone.BLACK) return Stone.WHITE;
        if (s == Stone.WHITE) return Stone.BLACK;
        return Stone.EMPTY;
    }

    public static boolean isPlayerStone(Stone s) {
        return s == Stone.BLACK || s == Stone.WHITE;
    }

    public boolean inBounds(int x, int y) {
        return x >= 0 && x < SIZE && y >= 0 && y < SIZE;
    }

    public Stone getStone(int x, int y) {
        return board[y][x];
    }

    public void setStone(int x, int y, Stone stone) {
        board[y][x] = stone;
    }

    public boolean isEmpty(int x, int y) {
        return getStone(x, y) == Stone.EMPTY;
    }

    public Stone getTurn() {
        return turn;
    }

    public void setTurn(Stone turn) {
        if (turn == null || !isPlayerStone(turn)) {
            throw new IllegalArgumentException("turn must be BLACK or WHITE");
        }
        this.turn = turn;
    }

    public void switchTurn() {
        this.turn = opposite(this.turn);
    }

    public GameStatus getStatus() {
        return status;
    }

    public void startGame() {
        this.status = GameStatus.IN_PROGRESS;
        this.winnerId = NONE;
    }

    // 종료(승자 없음)
    public void endGame() {
        this.status = GameStatus.FINISHED;
    }

    // 종료(승자 기록)
    public void endGame(int winnerId) {
        this.status = GameStatus.FINISHED;
        this.winnerId = winnerId;
    }

    public void setStatus(GameStatus status) {
        this.status = status;
        if (status != GameStatus.FINISHED) {
            this.winnerId = NONE;
        }
    }

    // 유저(Players) 매핑

    public int getBlackUserId() {
        return blackUserId;
    }

    public int getWhiteUserId() {
        return whiteUserId;
    }

    public void setBlackUserId(int userId) {
        this.blackUserId = userId;
    }

    public void setWhiteUserId(int userId) {
        this.whiteUserId = userId;
    }

    // Stone(색) -> Users.user_id 변환
    public int getUserIdByStone(Stone stone) {
        if (stone == Stone.BLACK) return blackUserId;
        if (stone == Stone.WHITE) return whiteUserId;
        return NONE;
    }

    public int getWinnerId() {
        return winnerId;
    }

    public void setWinnerId(int winnerId) {
        this.winnerId = winnerId;
    }
}
