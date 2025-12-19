package team.omok.omok_mini_project.domain;

import java.util.List;

import team.omok.omok_mini_project.enums.GameStatus;
import team.omok.omok_mini_project.enums.Stone;

// 오목 규칙 엔진.
// 1. 착수 가능 여부
// 2. 4방향 승리 판정
// 3. (흑) 쌍삼 금지 판정(간소화 패턴 기반)
//
// 실패 사유(reason) 코드:
// - GAME_ALREADY_ENDED
// - OUT_OF_BOUNDS
// - CELL_NOT_EMPTY
// - FORBIDDEN_DOUBLE_THREE
// - PLAYER_IDS_NOT_SET (추가)
public class OmokRule {

    private static final int[][] DIRS = {
            {1, 0}, {0, 1}, {1, 1}, {1, -1}
    };

    // 상태(state)의 현재 턴을 기준으로 (x,y)에 착수를 시도한다.
    // - 성공 시: 보드 반영 + (승리면 게임 종료) + (승리 아니면 턴 교대)
    public MoveResult placeStone(GameState state, int x, int y) {

        // 유저 id 세팅 체크
        if (state.getBlackUserId() < 0 || state.getWhiteUserId() < 0) {
            return MoveResult.invalidTurn("PLAYER_IDS_NOT_SET");
        }

        // 기본 검증
        if (state.getStatus() != GameStatus.IN_PROGRESS) {
            return MoveResult.invalidTurn("GAME_ALREADY_ENDED");
        }
        if (!state.inBounds(x, y)) {
            return MoveResult.invalidPosition("OUT_OF_BOUNDS");
        }
        if (!state.isEmpty(x, y)) {
            return MoveResult.invalidPosition("CELL_NOT_EMPTY");
        }

        Stone color = state.getTurn();

        // 임시 착수(금수/승리 판정을 위해)
        state.setStone(x, y, color);

        //흑돌 쌍삼 금지
        if (color == Stone.BLACK) {
            int openThreeCount = countOpenThreesCreatedByMove(state, x, y, Stone.BLACK);
            if (openThreeCount >= 2) {
                state.setStone(x, y, Stone.EMPTY); // 롤백
                return MoveResult.invalidPosition("FORBIDDEN_DOUBLE_THREE");
            }
        }

        // 승리 판정
        if (isWin(state, x, y, color)) {
            int winnerId = state.getUserIdByStone(color); // Users.user_id
            state.endGame(winnerId);
            return MoveResult.win(x, y, winnerId);
        }

        // 무승부 판정 (보드가 꽉 찼는데 승리가 아니면 DRAW)
        if (isDraw(state)) {
            state.endGame();
            return MoveResult.draw("DRAW");
        }

        // 5) 턴 교대
        state.switchTurn();
        return MoveResult.moveOk(x, y);
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
    
    // 무승부 판정: 보드에 EMPTY가 하나도 없으면 true
    private boolean isDraw(GameState state) {
        for (int y = 0; y < GameState.SIZE; y++) {
            for (int x = 0; x < GameState.SIZE; x++) {
                if (state.getStone(x, y) == Stone.EMPTY) {
                    return false;
                }
            }
        }
        return true;
    }

    /*
     * 쌍삼 판정용: 이번 수로 인해 생성된 "열린 3" 개수(방향 수)
     *
     * 열린 3(간소화) 패턴:
     *   .BBB.
     *   .BB.B.
     *   .B.BB.
     *
     * '.' : EMPTY
     * 'B' : 검사 대상 돌
     * 'O' : 상대/벽(막힘)
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

    /*
     * 중심(x,y) 기준으로 -4..+4 총 9칸을 문자열로 구성
     * 범위 밖/상대돌은 'O'(막힘)로 처리해서 "열림" 판정에서 제외
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

    // 열린 3 포함 여부 판단
    private boolean containsOpenThree(String line) {
        List<String> patterns = List.of(".BBB.", ".BB.B.", ".B.BB.");
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
