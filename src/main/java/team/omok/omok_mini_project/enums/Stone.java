package team.omok.omok_mini_project.enums;


/* 오목 돌/칸 상태 표현
* EMPTY: 빈칸
* BLACK: 흑돌
* WHITE: 백돌
*/
public enum Stone {
	BLACK, WHITE, EMPTY;
	
	public Stone opposite() {
		if (this == BLACK) return WHITE;
		if (this == WHITE) return BLACK;
		return EMPTY;
	}
	
	public boolean isPlayerStone() {
		return this == BLACK || this == WHITE;
	}
}
