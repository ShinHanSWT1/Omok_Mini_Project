package team.omok.omok_mini_project.domain;

import lombok.Data;

import java.util.Date;

@Data
public class UserVO {
    private int userId;
    private String loginId;
    private String user_pwd;
    private Date created_at;
    private String nickname;
    private String profile_img;
}
