package team.omok.omok_mini_project.domain;


/*한 수(착수) 처리 결과.
* ok: 착수 성공 여부
* reason: 실패 사유 코드(성공이면 null)
* win: 이번 수로 승리했는지 여부
*/
public class MoveResult {
	private final boolean ok;
    private final String reason;
    private final boolean win;
    
    private MoveResult(boolean ok, String reason, boolean win) {
    	this.ok = ok;
    	this.reason = reason;
    	this.win = win;
    }
    
    public static MoveResult ok(boolean win) {
    	return new MoveResult(true, null, win);
    }
    
    public static MoveResult fail(String reason) {
    	return new MoveResult(false, reason, false);
    }
    
    public boolean isOk() {
        return ok;
    }

    public String getReason() {
        return reason;
    }

    public boolean isWin() {
        return win;
    }

    @Override
    public String toString() {
        return "MoveResult{ok=" + ok + ", reason='" + reason + "', win=" + win + "}";
    }
}