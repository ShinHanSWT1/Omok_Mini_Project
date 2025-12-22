package team.omok.omok_mini_project.domain;

import team.omok.omok_mini_project.enums.GameStatus;
import team.omok.omok_mini_project.enums.Stone;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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

    // 턴 제한 시간
    private static final long TURN_LIMIT_MS = 30_000L;

    // 전체 게임에서 공유하는 스케줄러(방/게임마다 스레드 안 늘리기)
    private static final ScheduledExecutorService TURN_TIMER =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "omok-turn-timer");
                t.setDaemon(true);
                return t;
            });

    // MoveResult 쪽에서 TIMEOUT reason으로 바꿔치기 위해 쓰는 ThreadLocal
    private static final ThreadLocal<String> REASON_OVERRIDE = new ThreadLocal<>();
    private final Stone[][] board = new Stone[SIZE][SIZE];
    private Stone turn;
    private GameStatus status;
    // 유저 매핑 (DB Users.user_id)
    private int blackUserId;
    private int whiteUserId;
    // 승자 (Users.user_id), 없으면 -1
    private int winnerId;
    // 타이머 상태
    private long turnDeadlineMs;          // 현재 턴 마감 시각(epoch ms)
    private int turnSeq;                  // 스케줄 레이스 방지용 시퀀스
    private ScheduledFuture<?> timeoutFuture;
    private String endReason;
    public GameState() {
        reset();
    }

    static void setReasonOverride(String reason) {
        REASON_OVERRIDE.set(reason);
    }

    static String consumeReasonOverride() {
        String v = REASON_OVERRIDE.get();
        if (v != null) REASON_OVERRIDE.remove();
        return v;
    }

    public static Stone opposite(Stone s) {
        if (s == Stone.BLACK) return Stone.WHITE;
        if (s == Stone.WHITE) return Stone.BLACK;
        return Stone.EMPTY;
    }

    public static boolean isPlayerStone(Stone s) {
        return s == Stone.BLACK || s == Stone.WHITE;
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

        this.turnDeadlineMs = 0L;
        this.turnSeq = 0;
        this.endReason = null;

        cancelTimeoutLocked();
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

    /**
     * 정상 착수 후 OmokRule이 호출함
     * -> 여기서 턴을 바꾸면서 다음 턴 30초를 "자동"으로 리셋한다.
     */
    public synchronized void switchTurn() {
        this.turn = opposite(this.turn);

        // 게임 진행중이면 다음 턴 타이머 재시작
        if (this.status == GameStatus.IN_PROGRESS) {
            scheduleTurnTimeoutLocked();
        }
    }

    /**
     * OmokRule이 가장 먼저 호출하는 곳
     * - TIMEOUT으로 이미 종료된 게임이면, MoveResult reason을 TIMEOUT으로 바꿔치기하도록 ThreadLocal 설정
     * - 혹시 스케줄이 밀렸더라도 deadline 지난 경우 여기서 즉시 timeout 처리(안전망)
     */
    public synchronized GameStatus getStatus() {
        if (this.status == GameStatus.IN_PROGRESS) {
            long now = System.currentTimeMillis();
            if (this.turnDeadlineMs > 0 && now > this.turnDeadlineMs) {
                // 스케줄이 늦었거나 edge 케이스일 때 즉시 timeout 처리
                forceTimeoutLocked();
            }
        }

        if (this.status != GameStatus.IN_PROGRESS && "TIMEOUT".equals(this.endReason)) {
            setReasonOverride("TIMEOUT");
        }

        return status;
    }

    public synchronized void startGame() {
        this.status = GameStatus.IN_PROGRESS;
        this.winnerId = NONE;
        this.endReason = null;

        // 게임 시작 = 흑 턴 시작 -> 30초 타이머 시작
        scheduleTurnTimeoutLocked();
    }

    public synchronized void endGame() {
        this.status = GameStatus.FINISHED;
        cancelTimeoutLocked();
    }

    // 종료(승자 기록)
    public synchronized void endGame(int winnerId) {
        this.status = GameStatus.FINISHED;
        this.winnerId = winnerId;
        cancelTimeoutLocked();
    }

    public synchronized int getBlackUserId() {
        return blackUserId;
    }

    public synchronized void setBlackUserId(int userId) {
        this.blackUserId = userId;
    }

    public synchronized int getWhiteUserId() {
        return whiteUserId;
    }

    public synchronized void setWhiteUserId(int userId) {
        this.whiteUserId = userId;
    }

    public synchronized int getUserIdByStone(Stone stone) {
        if (stone == Stone.BLACK) return blackUserId;
        if (stone == Stone.WHITE) return whiteUserId;
        return NONE;
    }

    public synchronized int getWinnerId() {
        return winnerId;
    }

    // 타이머 내부 로직
    private void scheduleTurnTimeoutLocked() {
        cancelTimeoutLocked();

        long now = System.currentTimeMillis();
        this.turnDeadlineMs = now + TURN_LIMIT_MS;
        int seqSnapshot = ++this.turnSeq;

        this.timeoutFuture = TURN_TIMER.schedule(() -> {
            synchronized (GameState.this) {
                // 이미 게임이 끝났으면 무시
                if (status != GameStatus.IN_PROGRESS) return;
                // 턴이 넘어가서 seq가 바뀌었으면(늦게 실행된 타이머) 무시
                if (turnSeq != seqSnapshot) return;
                forceTimeoutLocked();
            }
        }, TURN_LIMIT_MS, TimeUnit.MILLISECONDS);
    }

    private void forceTimeoutLocked() {
        if (this.status != GameStatus.IN_PROGRESS) return;

        // 현재 턴 플레이어가 시간초과 패배
        Stone loserStone = this.turn;
        int winner = getUserIdByStone(opposite(loserStone));

        this.winnerId = winner;
        this.status = GameStatus.FINISHED;
        this.endReason = "TIMEOUT";

        cancelTimeoutLocked();
    }

    private void cancelTimeoutLocked() {
        if (this.timeoutFuture != null) {
            this.timeoutFuture.cancel(false);
            this.timeoutFuture = null;
        }
    }

    public synchronized long getTurnDeadlineMs() {
        return turnDeadlineMs;
    }

    public synchronized String getEndReason() {
        return endReason;
    }

    // 보드판 상태 전달(관전자 용)
    public synchronized Stone[][] getBoard() {
        Stone[][] copy = new Stone[SIZE][SIZE];
        for (int i = 0; i < SIZE; i++) {
            copy[i] = board[i].clone();
        }
        return copy;
    }

    public synchronized long getRemainingTimeMs() {
        if (status != GameStatus.IN_PROGRESS) return 0;
        return Math.max(0, turnDeadlineMs - System.currentTimeMillis());
    }
}