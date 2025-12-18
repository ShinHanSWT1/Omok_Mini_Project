package team.omok.omok_mini_project.domain;

import lombok.Data;

import java.util.Date;

@Data
public class UserVO {
    private int userId;
    private String loginId;
    private String userPwd;
    private Date createdAt;
    private String nickname;
    private String profileImg;


}
