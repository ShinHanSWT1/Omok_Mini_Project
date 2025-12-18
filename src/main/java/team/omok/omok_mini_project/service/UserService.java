package team.omok.omok_mini_project.service;

import team.omok.omok_mini_project.domain.UserVO;
import team.omok.omok_mini_project.repository.UserDAO;

public class UserService {
    private UserDAO userDAO = new UserDAO();

    public UserVO login(String login_id, String password) {

        if (login_id == null || password == null) {
            throw new IllegalArgumentException("입력값이 없습니다.");
        }

        UserVO user = userDAO.findById(login_id);

        if (user == null) {
            throw new IllegalArgumentException("존재하지 않는 아이디입니다.");
        }

        // 추후 해시 비교
        if (!user.getUser_pwd().equals(password)) {
            throw new IllegalArgumentException("비밀번호가 틀렸습니다.");
        }

        return user;
    }
}
