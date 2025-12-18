package team.omok.omok_mini_project.domain;

import lombok.Data;

import java.util.Date;
@Data
public class RecordVO {
    private int userId;
    private int win_count;
    private int lose_count;
    private int rating;
    private Date updated_at;
}
