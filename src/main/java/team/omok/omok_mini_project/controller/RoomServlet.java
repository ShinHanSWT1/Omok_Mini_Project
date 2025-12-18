package team.omok.omok_mini_project.controller;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import team.omok.omok_mini_project.domain.Room;
import team.omok.omok_mini_project.manager.RoomManager;

@WebServlet("/room")
public class RoomServlet extends HttpServlet {
    private final RoomManager roomManager = RoomManager.getInstance();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String roomId = request.getParameter("roomId");
        Room room = roomManager.getRoom(roomId);

        if (room == null) {
            response.sendRedirect("/omok/lobby");
            return;
        }

        request.setAttribute("room", room);
        request.getRequestDispatcher("/WEB-INF/views/room.jsp")
                .forward(request, response);
    }
}