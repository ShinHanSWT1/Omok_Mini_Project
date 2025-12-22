package team.omok.omok_mini_project.controller;

import team.omok.omok_mini_project.domain.vo.UserVO;
import team.omok.omok_mini_project.service.UserService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private UserService userService;

    @Override
    public void init() {
//        DataSource ds = (DataSource) getServletContext().getAttribute("dataSource");
//        userService = new UserService(new UserDAO(ds));
        this.userService = new UserService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        String guestLogin = request.getParameter("guestLogin");
        if ("true".equalsIgnoreCase(guestLogin)) {
            UserVO guest = new UserVO();

            int guestUserId = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);

            guest.setUserId(guestUserId);
            guest.setLoginId("guest_" + guestUserId);
            guest.setNickname("게스트#" + String.format("%04d", guestUserId % 10000));
            guest.setProfileImg("/assets/profile/default.png"); // 너희 기본 경로로 맞추기
            guest.setGuest(true);

            HttpSession session = request.getSession();
            session.setAttribute("loginUser", guest);

            response.sendRedirect(request.getContextPath() + "/lobby");
            return;
        }

        // ★ login.jsp의 name과 일치해야 함
        String loginId = request.getParameter("loginId");
        String password = request.getParameter("password");

        try {
            UserVO user = userService.login(loginId, password);

            HttpSession session = request.getSession();
            session.setAttribute("loginUser", user);

            // 컨텍스트 포함 redirect
            response.sendRedirect(request.getContextPath() + "/lobby");

        } catch (IllegalArgumentException e) {
            // 로그인 실패(아이디/비번 틀림 등)
            request.setAttribute("error", e.getMessage());
            request.setAttribute("loginId", loginId);
            request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);

        } catch (Exception e) {
            e.printStackTrace();

            String msg = e.getClass().getSimpleName() + ": " + e.getMessage();
            request.setAttribute("error", msg);

            request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
        }

    }
}
