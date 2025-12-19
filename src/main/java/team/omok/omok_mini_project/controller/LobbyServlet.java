package team.omok.omok_mini_project.controller;

import team.omok.omok_mini_project.domain.RankingDTO;
import team.omok.omok_mini_project.domain.Room;
import team.omok.omok_mini_project.domain.UserVO;
import team.omok.omok_mini_project.repository.RecordDAO;
import team.omok.omok_mini_project.service.RoomService;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.List;



@WebServlet("/lobby/*")
// 방 목록 조회,방 생성, 방입장(방 관련 비즈니스 로직)
public class LobbyServlet extends HttpServlet {
    private final RoomService roomService = new RoomService();

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

        // 경로 확인 -> enter체크 -> 로그인 정보 가져와
        String path = request.getPathInfo(); // null, /enter, /quick-enter
        if ("/enter".equals(path)) {
            System.out.println("[INFO]lobby-doGet-enter");
            // 방 번호 가져오고
            String roomId = request.getParameter("roomId");
            // 해당 방에 유저 입장
            roomService.enterRoom(roomId, user);
            // 게임 방 화면으로 이동
            response.sendRedirect("/omok/room?roomId=" + roomId);
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
            // 해당 방에 유저 입장
            roomService.enterRoom(room.getRoomId(), user);
            // 게임 방 화면으로 이동
            response.sendRedirect("/omok/room?roomId=" + room.getRoomId());
            return;
        }

        // 기본: 로비 화면
        // 대기 중인 방 목록 가져옴
        List<Room> rooms = roomService.getWaitingRooms();
        // 랭킹 정보 조회
        List<RankingDTO> rankingList = RecordDAO.getTopRank();
        // JSP에서 쓸수 있도록 request에 저장
        request.setAttribute("rankingList", rankingList); // 랭킹정보
        request.setAttribute("rooms", rooms); // room 정보
        // 실제 화면인 lobby.jsp로 포워딩
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
            response.sendRedirect("/omok/room?roomId=" + room.getRoomId());
        }

    }
}