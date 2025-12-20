package team.omok.omok_mini_project.service;

import team.omok.omok_mini_project.domain.vo.UserVO;
import team.omok.omok_mini_project.repository.UserDAO;

public class UserService {

    private UserDAO userDAO = new UserDAO();

    public UserVO getUserById(int userId) throws Exception {

        return userDAO.findByUserId(userId);
    }

    public void register(UserVO user) throws Exception {

        if (user.getLoginId() == null || user.getLoginId().isBlank())
            throw new IllegalArgumentException("아이디 입력하세요");

        if (user.getUserPwd() == null || user.getUserPwd().isBlank())
            throw new IllegalArgumentException("비밀번호 입력하세요");

        if (user.getNickname() == null || user.getNickname().isBlank())
            throw new IllegalArgumentException("닉네임 입력하세요");

        if (userDAO.existsByLoginId(user.getLoginId()))
            throw new IllegalArgumentException("이미 존재하는 아이디");

        if (userDAO.existsByNickname(user.getNickname()))
            throw new IllegalArgumentException("이미 존재하는 닉네임");

        // 비밀번호 해싱
//        user.setUserPwd(
//                PasswordUtil.hashWithSalt(user.getUserPwd())
//        );

        userDAO.insert(user); //db저장 메서드 호출
    }
    public UserVO login(String loginId, String password) throws Exception {
        if (loginId == null || loginId.isBlank())
            throw new IllegalArgumentException("아이디 입력하세요");
        if (password == null || password.isBlank())
            throw new IllegalArgumentException("비밀번호 입력하세요");

        UserVO user = userDAO.findByLoginId(loginId); //db에서 조회
        if (user == null)
            throw new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다.");

        // 지금은 평문 비교(임시). 나중에 해시 비교로 변경.
        if (!user.getUserPwd().equals(password)) //가져온 pwd가 password랑 다른경우
            throw new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다.");

        return user;
    }
}