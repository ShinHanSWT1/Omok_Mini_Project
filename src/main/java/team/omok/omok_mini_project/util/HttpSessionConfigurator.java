package team.omok.omok_mini_project.util;

import team.omok.omok_mini_project.domain.vo.UserVO;

import javax.servlet.http.HttpSession;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

public class HttpSessionConfigurator extends ServerEndpointConfig.Configurator {
    @Override
    public void modifyHandshake(ServerEndpointConfig config,
                                HandshakeRequest request,
                                HandshakeResponse response) {

        HttpSession httpSession = (HttpSession) request.getHttpSession();
        if (httpSession == null) return;

        UserVO vo = (UserVO) httpSession.getAttribute("loginUser");
        if (vo == null) return; //  여기 없으면 WS 연결 시 터질 수 있음

        config.getUserProperties().put("user_id", vo.getUserId());
    }
}
