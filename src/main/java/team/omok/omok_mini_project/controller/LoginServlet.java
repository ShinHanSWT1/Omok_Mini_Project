package team.omok.omok_mini_project.controller;

import team.omok.omok_mini_project.domain.vo.UserVO;
import team.omok.omok_mini_project.service.UserService;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
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
