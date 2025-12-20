package team.omok.omok_mini_project.repository;

import team.omok.omok_mini_project.domain.vo.UserVO;
import team.omok.omok_mini_project.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UserDAO {
    //    private DataSource ds;  //일단 Connection으로 하기로함

    //    public UserDAO(DataSource ds) {
//        this.ds = ds;//db주입받음


    public UserVO findByUserId(int userId) throws Exception {
        String sql = """
            SELECT user_id, login_id, user_pwd, created_at, nickname, profile_img
            FROM users
            WHERE user_id=?
        """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                UserVO u = new UserVO();
                u.setUserId(rs.getInt("user_id"));
                u.setLoginId(rs.getString("login_id"));
                u.setUserPwd(rs.getString("user_pwd"));
                u.setCreatedAt(rs.getTimestamp("created_at"));
                u.setNickname(rs.getString("nickname"));
                u.setProfileImg(rs.getString("profile_img"));
                return u;
            }
        }
    }

    public UserVO findByLoginId(String loginId) throws Exception {
        String sql = """
            SELECT user_id, login_id, user_pwd, created_at, nickname, profile_img
            FROM users
            WHERE login_id=?
        """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, loginId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                UserVO u = new UserVO();
                u.setUserId(rs.getInt("user_id"));
                u.setLoginId(rs.getString("login_id"));
                u.setUserPwd(rs.getString("user_pwd"));
                u.setCreatedAt(rs.getTimestamp("created_at"));
                u.setNickname(rs.getString("nickname"));
                u.setProfileImg(rs.getString("profile_img"));
                return u;
            }
        }
    }

   public boolean existsByLoginId(String loginId) throws Exception {
       String sql = "SELECT 1 FROM users WHERE login_id=?";
       try (Connection con = DBConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(sql)) {

           ps.setString(1, loginId);
           return ps.executeQuery().next();
       }
   }

    public boolean existsByNickname(String nickname) throws Exception {
        String sql = "SELECT 1 FROM users WHERE nickname=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, nickname);
            return ps.executeQuery().next();
        }
    }

    public void insert(UserVO user) throws Exception {
        String sql = """
            INSERT INTO users (login_id, user_pwd, nickname, profile_img)
            VALUES (?, ?, ?, ?)
        """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, user.getLoginId());
            ps.setString(2, user.getUserPwd());
            ps.setString(3, user.getNickname());
            ps.setString(4, user.getProfileImg());

            ps.executeUpdate();
        }
    }
}
