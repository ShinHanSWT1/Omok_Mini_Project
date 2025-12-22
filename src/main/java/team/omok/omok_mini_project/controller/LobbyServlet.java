package team.omok.omok_mini_project.controller;

import team.omok.omok_mini_project.domain.Room;
import team.omok.omok_mini_project.domain.dto.RankingDTO;
import team.omok.omok_mini_project.domain.vo.UserVO;
import team.omok.omok_mini_project.repository.RecordDAO;
import team.omok.omok_mini_project.service.RoomService;
import team.omok.omok_mini_project.service.UserService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;


@WebServlet("/lobby/*")
// 방 목록 조회,방 생성, 방입장(방 관련 비즈니스 로직)
public class LobbyServlet extends HttpServlet {
    private final RoomService roomService = new RoomService();
    private final UserService userService = new UserService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        System.out.println("[INFO]lobby-doGet");

        // 로그인 체크
        UserVO user = (UserVO) request.getSession().getAttribute("loginUser");
        if (user == null) {
            response.sendRedirect("/omok/login");
            return;
        }

        // ★ 중요: 세션의 유저 정보를 DB에서 최신 정보로 갱신
        // 이유: 게임이 끝나면 DB의 record 테이블(승/패/레이팅)은 업데이트되지만,
        //       세션에 저장된 loginUser 객체는 옛날 데이터 그대로임
        // 결과: lobby.jsp에서 ${loginUser.record.win_count} 등이 게임 전 데이터로 표시됨
        // 해결: 로비 접속할 때마다 DB에서 최신 정보를 조회해서 세션을 갱신
        try {
            UserVO updatedUser = userService.getUserById(user.getUserId());
            if (updatedUser != null) {
                // 세션에 최신 유저 정보로 덮어쓰기
                request.getSession().setAttribute("loginUser", updatedUser);
                user = updatedUser; // 이후 로직에서 사용할 변수도 갱신
            }
        } catch (Exception e) {
            System.err.println("[LobbyServlet] 유저 정보 갱신 실패: " + e.getMessage());
            e.printStackTrace();
            // 갱신 실패해도 기존 세션 정보로 계속 진행 (에러로 중단하지 않음)
        }

        // 경로 확인 -> enter체크 -> 로그인 정보 가져와
        String path = request.getPathInfo(); // null, /enter, /quick-enter
        if ("/enter".equals(path)) {
            System.out.println("[INFO]lobby-doGet-enter");
            // 방 번호 가져오고
            String roomId = request.getParameter("roomId");
            String role = request.getParameter("role");
            // 해당 방에 유저 입장
            if (role.equals("player")) {
                roomService.enterRoom(roomId, user);
            }
            // 게임 방 화면으로 이동
            response.sendRedirect("/omok/room?roomId=" + roomId + "&role=" + role);
            return;
        }

        // 빠른 입장: 가장 먼저 만들어진 대기 방에 자동 입장
        if ("/quick-enter".equals(path)) {
            System.out.println("[INFO]lobby-doGet-quick-enter");
            // 가장 먼저 생성된 대기 방 가져오기
            Room room = roomService.getFirstWaitingRoom();
            if (room == null) {
                // 대기 중인 방이 없으면 로비로 돌아감
                response.sendRedirect("/omok/lobby");
                return;
            }
            try {
                // 해당 방에 유저 입장
                roomService.enterRoom(room.getRoomId(), user);

                // 게임 방 화면으로 이동
                // 빠른 입장은 무조건 player로 시도
                response.sendRedirect("/omok/room?roomId=" + room.getRoomId() + "&role=player");
            } catch (IllegalStateException e) {
                // 방이 가득 찬 경우 (동시성 이슈 등)
                System.out.println("[WARN] 빠른 입장 실패 - 방이 가득 참: " + e.getMessage());
                // 다시 로비로 이동 (혹은 다른 방 찾기 로직 추가 가능)
                response.sendRedirect("/omok/lobby?error=full");
            } catch (Exception e) {
                e.printStackTrace();
                throw e; // 500 에러 원인 확인을 위해 다시 던지거나, 에러 페이지로 이동
            }
            return;
        }

        // 기본: 로비 화면
        // 대기 중인 방 목록 가져옴
        List<Room> rooms = roomService.getWaitingRooms();
//        List<Room> rooms = roomService.getAllRooms();

        // 랭킹 정보 조회
        List<RankingDTO> rankingList = RecordDAO.getTopRank();
        // JSP에서 쓸수 있도록 request에 저장
        request.setAttribute("rankingList", rankingList); // 랭킹정보
        request.setAttribute("rooms", rooms); // room 정보
        // 실제 화면인 lobby.jsp로 포워딩
        // 빈 리스트 전달 (JSP에서 에러 방지)
//        request.setAttribute("rankings", new java.util.ArrayList<RankingDTO>());
        request.getRequestDispatcher("/WEB-INF/views/lobby.jsp")
                .forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 로그인 체크
        UserVO user = (UserVO) request.getSession().getAttribute("loginUser");
        if (user == null) {
            response.sendRedirect("/omok/login");
            return;
        }

        // URL 경로 확인
        String path = request.getPathInfo(); // /create
        // 방 만들기 요청(/create) 일 때
        if ("/create".equals(path)) {
            System.out.println("[INFO]lobby-doPost-create");
            // 서비스 로직 호출 : 방장이 될 유저 ID로 새 방 생성
            Room room = roomService.createRoom(user.getUserId());
            // 생성된 방의 ID를 가지고 게임 방 화면으로 이동
            response.sendRedirect("/omok/room?roomId=" + room.getRoomId() + "&role=player");
        }

    }
}