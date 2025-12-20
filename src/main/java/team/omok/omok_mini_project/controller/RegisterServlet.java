package team.omok.omok_mini_project.controller;

import team.omok.omok_mini_project.domain.vo.UserVO;
import team.omok.omok_mini_project.service.UserService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    private UserService userService;

    @Override
    public void init() {
        this.userService = new UserService();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) //get요청 받으면 회원가입화면표시)
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
        String profileImg = req.getParameter("profileImg"); //이미지 일단 문자열로 함 추후 수정예정
        //기본값
        if (profileImg == null || profileImg.isBlank()) {
            profileImg = "/assets/profiles/p1.png";
        }
        // 허용된 경로만 저장
        if (!profileImg.startsWith("/assets/profiles/")) {
            profileImg = "/assets/profiles/p1.png";
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
