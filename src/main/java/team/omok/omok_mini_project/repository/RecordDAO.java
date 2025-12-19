package team.omok.omok_mini_project.repository;

import team.omok.omok_mini_project.domain.RankingDTO;
import team.omok.omok_mini_project.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class LobbyDAO {
    public static List<RankingDTO> getTopRank() {
        List<RankingDTO> list = new ArrayList<RankingDTO>();

        String sql = "SELECT u.nickname, r.rating " +
                "FROM record r JOIN users u " +
                "ON r.user_id = u.user_id " +
                "ORDER BY r.rating DESC LIMIT 5";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()){
            int currentRank = 1;
            while(rs.next()) {
                RankingDTO dto = new RankingDTO();
                dto.setRank(currentRank++);
                dto.setNickname(rs.getString("nickname"));
                dto.setRating(rs.getInt("rating"));
                list.add(dto);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public void updataRating(int userId, boolean isWin) {
        String sql;

        if(isWin) {
            sql = "UPDATE record SET rating = rating +15, win_count = win_count+1, updated_at = timestamp WHERE user_id = ?";
        } else {
            sql = "UPDATE record SET rating = GREATEST(0, rating - 10), lose_count = lose_count + 1, updated_at = timestamp WHERE user_id = ?";
        }

        try(Connection conn = DBConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
