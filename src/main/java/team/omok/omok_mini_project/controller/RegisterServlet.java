package team.omok.omok_mini_project.controller;

import team.omok.omok_mini_project.domain.vo.UserVO;
import team.omok.omok_mini_project.service.UserServices;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    private UserServices userService;

    @Override
    public void init() {
        this.userService = new UserServices();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");

        String loginId = req.getParameter("loginId");
        String password = req.getParameter("password");
        String nickname = req.getParameter("nickname");
        String profileImg = req.getParameter("profileImg"); // JSP hidden input 값

        // 이미지 입력안할시 1번 이미지로
        if (profileImg == null || profileImg.isBlank()) {
            profileImg = "/static/img/profiles/p1.png";
        }

        //  허용된 경로만 저장 (보안/검증)
        // - /omok 같은 contextPath는 DB에 저장하지 않는 게 좋음
        // - 항상 "/static/..."로 통일
        if (!profileImg.startsWith("/static/img/profiles/")) {
            profileImg = "/static/img/profiles/p1.png";
        }

        UserVO user = new UserVO();
        user.setLoginId(loginId);
        user.setUserPwd(password);   // (지금은 평문, 나중에 해시)
        user.setNickname(nickname);
        user.setProfileImg(profileImg);

        try {
            userService.register(user);
            resp.sendRedirect(req.getContextPath() + "/login");
        } catch (Exception e) {
            req.setAttribute("error", e.getMessage());
            req.setAttribute("loginId", loginId);
            req.setAttribute("nickname", nickname);
            req.setAttribute("profileImg", profileImg);

            req.getRequestDispatcher("/WEB-INF/views/register.jsp")
                    .forward(req, resp);
        }
    }
}
