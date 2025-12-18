package team.omok.omok_mini_project.domain;

import java.util.List;

import team.omok.omok_mini_project.enums.GameStatus;
import team.omok.omok_mini_project.enums.Stone;

// 오목 규칙 엔진.
// 1. 착수 가능 여부
// 2. 4방향 승리 판정
// 3. (흑) 쌍삼 금지 판정(간소화 패턴 기반)

// 실패 사유(reason) 코드:
// 1. GAME_ALREADY_ENDED
// 2. OUT_OF_BOUNDS
// 3. CELL_NOT_EMPTY
// 4. FORBIDDEN_DOUBLE_THREE

public class OmokRule {
	private static final int[][] DIRS = {
			{1, 0}, {0, 1}, {1, 1}, {1, -1}
	};
	
	// 상태(state)의 현재 턴을 기준으로 (x,y)에 착수를 시도한다.
	// - 성공 시: 보드 반영 + (승리면 게임 종료) + (승리 아니면 턴 교대)
	public MoveResult placeStone(GameState state, int x, int y) {
		// 1) 기본 검증
	    if (state.getStatus() != GameStatus.IN_PROGRESS) {
	        return MoveResult.fail("GAME_ALREADY_ENDED");
	    }
	    if (!state.inBounds(x, y)) {
	        return MoveResult.fail("OUT_OF_BOUNDS");
	    }
	    if (!state.isEmpty(x, y)) {
	        return MoveResult.fail("CELL_NOT_EMPTY");
	    }

	    Stone color = state.getTurn();
	    
	    // 2) 임시로 착수(금수/승리 판정을 위해)
        state.setStone(x, y, color);

        // 3) 흑돌 쌍삼 금지
        if (color == Stone.BLACK) {
            int openThreeCount = countOpenThreesCreatedByMove(state, x, y, Stone.BLACK);
            if (openThreeCount >= 2) {
                state.setStone(x, y, Stone.EMPTY); // 롤백
                return MoveResult.fail("FORBIDDEN_DOUBLE_THREE");
            }
        }

        // 4) 승리 판정
        boolean win = isWin(state, x, y, color);
        if (win) {
            state.endGame();
            return MoveResult.ok(true);
        }
        
        // 5) 턴 교대
        state.switchTurn();
        return MoveResult.ok(false);
    }

	// 마지막에 둔 돌(x,y)을 기준으로 4방향 오목(>=5) 승리 판정
	public boolean isWin(GameState state, int x, int y, Stone color) {
	    for (int[] d : DIRS) {
	        int count = 1;
	        count += countDir(state, x, y, d[0], d[1], color);
	        count += countDir(state, x, y, -d[0], -d[1], color);
	        if (count >= 5) return true;
	    }
	    return false;
	}
	
	private int countDir(GameState state, int x, int y, int dx, int dy, Stone color) {
        int cx = x + dx, cy = y + dy;
        int cnt = 0;
        while (state.inBounds(cx, cy) && state.getStone(cx, cy) == color) {
            cnt++;
            cx += dx;
            cy += dy;
        }
        return cnt;
    }

	/* 쌍삼 판정용: 이번 수로 인해 생성된 쌍삼 개수
    
     쌍삼(간소화) 패턴:
     .BBB.
     .BB.B.
     .B.BB.
    
     '.' : EMPTY
     'B' : 흑(검사 대상)
     'O' : 상대/벽(막힘)
     */
	
	private int countOpenThreesCreatedByMove(GameState state, int x, int y, Stone color) {
        int count = 0;
        for (int[] d : DIRS) {
            String line = buildLineString(state, x, y, d[0], d[1], color);
            if (containsOpenThree(line)) {
                count++;
            }
        }
        return count;
    }
	
	/* 중심(x,y) 기준으로 -4..+4 총 9칸을 문자열로 구성
	범위 밖은 'O'(벽)로 처리해서 "열림" 판정에서 제외
	*/
	private String buildLineString(GameState state, int x, int y, int dx, int dy, Stone color) {
        StringBuilder sb = new StringBuilder(9);
        for (int k = -4; k <= 4; k++) {
            int cx = x + dx * k;
            int cy = y + dy * k;

            if (!state.inBounds(cx, cy)) {
                sb.append('O');
                continue;
            }

            Stone cell = state.getStone(cx, cy);
            if (cell == Stone.EMPTY) sb.append('.');
            else if (cell == color) sb.append('B');
            else sb.append('O');
        }
        return sb.toString();
    }
	
	// 쌍삼 포함 여부 판단
	private boolean containsOpenThree(String line) {
        // 대표 쌍삼 패턴
        List<String> patterns = List.of(".BBB.", ".BB.B.", ".B.BB.");

        // 오탐 방지(간소화): 이미 4 이상이 강하게 포함되면 쌍삼으로 보지 X
        List<String> disqualify = List.of(".BBBB.", "BBBBB", "BBBB");

        for (String p : patterns) {
            if (line.contains(p)) {
                for (String dq : disqualify) {
                    if (line.contains(dq)) return false;
                }
                return true;
            }
        }
        return false;
    }
}
