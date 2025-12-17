package team.omok.omok_mini_project.util;

import javax.servlet.http.HttpSession;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

public class HttpSessionConfigurator extends ServerEndpointConfig.Configurator{
    @Override
    public void modifyHandshake(ServerEndpointConfig config,
                                HandshakeRequest request,
                                HandshakeResponse response) {

        HttpSession httpSession =
                (HttpSession) request.getHttpSession();

        if (httpSession != null) {
            String user_id =
                    (String) httpSession.getAttribute("user_id");

            config.getUserProperties().put("user_id", user_id);
        }
    }
}