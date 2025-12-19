package team.omok.omok_mini_project.domain;

import team.omok.omok_mini_project.enums.Stone;

public class GameLogicTest {

    public static void main(String[] args) {
        GameState state = new GameState();
        OmokRule rule = new OmokRule();
        
        state.setBlackUserId(1);
        state.setWhiteUserId(2);

        // 게임 시작
        state.startGame();
//
//        // Test 시, 해당 파트 주석 풀어서 게산
        
        
//        // Test 1 (일반적인 오목 테스트) --> 통과
        System.out.println(rule.placeStone(state, 7, 7)); // BLACK
        System.out.println(rule.placeStone(state, 7, 8)); // WHITE
        System.out.println(rule.placeStone(state, 8, 7)); // BLACK
        System.out.println(rule.placeStone(state, 8, 8)); // WHITE
        System.out.println(rule.placeStone(state, 9, 7)); // BLACK
        System.out.println(rule.placeStone(state, 9, 8)); // WHITE
        System.out.println(rule.placeStone(state,10, 7)); // BLACK
        System.out.println(rule.placeStone(state,10, 8)); // WHITE
        // 마지막 한 수 → 흑 승리
        System.out.println(rule.placeStone(state,11, 7)); // BLACK WIN
        
        
//        //Test 2 (이미 돌 있는 곳에 착수 에러 테스트) --> 통과
//        System.out.println(rule.placeStone(state, 7, 7)); // BLACK
//        System.out.println(rule.placeStone(state, 7, 8)); // WHITE
//        System.out.println(rule.placeStone(state, 8, 7)); // BLACK
//        System.out.println(rule.placeStone(state, 8, 8)); // WHITE
//        System.out.println(rule.placeStone(state, 9, 7)); // BLACK
//        System.out.println(rule.placeStone(state, 9, 8)); // WHITE
//        System.out.println(rule.placeStone(state,10, 7)); // BLACK
//        System.out.println(rule.placeStone(state,10, 8)); // WHITE
//        // 중복 팍수
//        System.out.println(rule.placeStone(state,10, 7)); // BLACK
        
        
//        //Test 3 (쌍삼 착수 에러 테스트) --> 통과
//        System.out.println(rule.placeStone(state, 7, 7)); // BLACK
//        System.out.println(rule.placeStone(state, 7, 8)); // WHITE
//        System.out.println(rule.placeStone(state, 8, 7)); // BLACK
//        System.out.println(rule.placeStone(state, 8, 8)); // WHITE
//        System.out.println(rule.placeStone(state, 10, 8)); // BLACK
//        System.out.println(rule.placeStone(state, 9, 8)); // WHITE
//        System.out.println(rule.placeStone(state,11, 9)); // BLACK
//        System.out.println(rule.placeStone(state,11, 8)); // WHITE
//        System.out.println(rule.placeStone(state, 9, 7)); // BLACK
        
        
//        //Test 4 (범위 밖 착수 에러 테스트) --> 통과
//        System.out.println(rule.placeStone(state, 7, 7)); // BLACK
//        System.out.println(rule.placeStone(state, 7, 17)); // WHITE
    }
}
