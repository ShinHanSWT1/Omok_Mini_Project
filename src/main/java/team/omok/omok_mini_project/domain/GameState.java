package team.omok.omok_mini_project.domain;

import java.util.Arrays;

import team.omok.omok_mini_project.enums.GameStatus;
import team.omok.omok_mini_project.enums.Stone;

/* 게임 상태 저장소(서버 authoritative).
* 보드(15x15)
* 현재 턴
* 게임 상태(진행/종료)

* 검증/승리판정/금수는 OmokRule에서만 한다.
*/

public class GameState {
	public static final int SIZE = 15;
	private final Stone[][] board = new Stone[SIZE][SIZE];
	
	private Stone turn;
	private GameStatus status;
	
	public GameState() {
		reset();
	}
	
	public void reset() {
		for (int y=0; y<SIZE; y++) {
			Arrays.fill(board[y], Stone.EMPTY);
		}
		this.turn = Stone.BLACK;
        this.status = GameStatus.IN_PROGRESS;
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
        if (turn == null || !turn.isPlayerStone()) {
            throw new IllegalArgumentException("turn must be BLACK or WHITE");
        }
        this.turn = turn;
    }

    public void switchTurn() {
        this.turn = this.turn.opposite();
    }

    public GameStatus getStatus() {
        return status;
    }

    public void endGame() {
        this.status = GameStatus.FINISHED;
    }

    public void setStatus(GameStatus status) {
        this.status = status;
    }
}