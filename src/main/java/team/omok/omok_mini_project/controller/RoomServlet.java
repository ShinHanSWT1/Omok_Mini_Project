package team.omok.omok_mini_project.controller;

import team.omok.omok_mini_project.domain.Room;
import team.omok.omok_mini_project.manager.RoomManager;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/room")
public class RoomServlet extends HttpServlet {
    private final RoomManager roomManager = RoomManager.getInstance();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String roomId = request.getParameter("roomId");
        Room room = roomManager.getRoomById(roomId);

        // 관전자인지 값 저장
        boolean isSpectator = request.getParameter("role").equals("spectator");
        System.out.println("[RoomServlet] " + request.getParameter("role") + ": " + request.getParameter("roomId"));
        System.out.println("[RoomServlet] isSpectator=" + isSpectator);
        if (room == null) {
            response.sendRedirect("/omok/lobby");
            return;
        }

        request.setAttribute("room", room);
        request.setAttribute("isSpectator", isSpectator);

        request.getRequestDispatcher("/WEB-INF/views/room.jsp")
                .forward(request, response);
    }
}