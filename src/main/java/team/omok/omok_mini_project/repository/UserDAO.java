package team.omok.omok_mini_project.repository;

import team.omok.omok_mini_project.domain.vo.RecordVO;
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
        SELECT u.user_id, u.login_id, u.user_pwd, u.created_at, u.nickname, u.profile_img,
               r.rating, r.win_count, r.lose_count, r.updated_at
        FROM users u
        LEFT JOIN record r ON u.user_id = r.user_id
        WHERE u.user_id = ?  
    """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                // 유저 정보 매핑
                UserVO u = new UserVO();
                u.setUserId(rs.getInt("user_id"));
                u.setLoginId(rs.getString("login_id"));
                u.setUserPwd(rs.getString("user_pwd"));
                u.setCreatedAt(rs.getTimestamp("created_at"));
                u.setNickname(rs.getString("nickname"));
                u.setProfileImg(rs.getString("profile_img"));

                // record 정보 매핑
                RecordVO r = new RecordVO();
                int rating = rs.getInt("rating");
                r.setRating(rs.wasNull() ? 1000 : rating);
                r.setWin_count(rs.getInt("win_count"));
                r.setLose_count(rs.getInt("lose_count"));
                r.setUpdated_at(rs.getTimestamp("updated_at"));
                u.setRecord(r);

                // UserVO에 RecordVO 추가
                u.setRecord(r);
                return u;
            }
        }
    }

    public UserVO findByLoginId(String loginId) throws Exception {
        String sql = """
            SELECT u.user_id, u.login_id, u.user_pwd, u.created_at, u.nickname, u.profile_img,
                   r.rating, r.win_count, r.lose_count, r.updated_at
            FROM users u
            LEFT JOIN record r ON u.user_id = r.user_id
            WHERE u.login_id = ?
        """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, loginId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                // user 정보 매핑
                UserVO u = new UserVO();
                u.setUserId(rs.getInt("user_id"));
                u.setLoginId(rs.getString("login_id"));
                u.setUserPwd(rs.getString("user_pwd"));
                u.setCreatedAt(rs.getTimestamp("created_at"));
                u.setNickname(rs.getString("nickname"));
                u.setProfileImg(rs.getString("profile_img"));

                // record 정보 매핑
                RecordVO r = new RecordVO();
                int rating = rs.getInt("rating");
                r.setRating(rs.wasNull() ? 1000 : rating);
                r.setWin_count(rs.getInt("win_count"));
                r.setLose_count(rs.getInt("lose_count"));
                r.setUpdated_at(rs.getTimestamp("updated_at"));

                // UserVO에 RecordVO 추가
                u.setRecord(r);
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
