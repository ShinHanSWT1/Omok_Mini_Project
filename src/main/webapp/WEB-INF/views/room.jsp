<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="team.omok.omok_mini_project.domain.Room" %>
<%@ page import="team.omok.omok_mini_project.domain.vo.UserVO" %>
<%
    Room room = (Room) request.getAttribute("room");
    UserVO loginUser = (UserVO) session.getAttribute("loginUser");
%>


<!DOCTYPE html>
<html>
<head>
    <title>오목 게임방</title>

    <!-- CSS -->
    <link rel="stylesheet" href="<%=request.getContextPath()%>/static/css/room.css">

    <!-- 서버에서 내려주는 초기 데이터 -->
    <script>
        const ROOM_ID = "<%= room.getRoomId() %>";
        const OWNER_ID = "<%= room.getOwnerId() %>";

        // 관전 여부 (URL 파라미터 기준) true or false
        const IS_SPECTATOR = <%= Boolean.TRUE.equals(request.getAttribute("isSpectator")) %>;

        let WS_URL = "ws://" + location.host
            + "<%=request.getContextPath()%>/ws/game/"
            + ROOM_ID;

        if (IS_SPECTATOR) {
            WS_URL += "?role=spectator";
        } else {
            WS_URL += "?role=player"
        }
        console.log("[room.jsp]" + "IS_SPECTATOR=" + IS_SPECTATOR + " : WS_URL=" + WS_URL);
    </script>


    <!-- JS -->
    <script defer src="<%=request.getContextPath()%>/static/js/game.js"></script>
    <script defer src="<%=request.getContextPath()%>/static/js/game_ui.js"></script>
    <script defer src="<%=request.getContextPath()%>/static/js/websocket.js"></script>
    </script>
</head>

<body>

<div id="status"></div>
<div id="countdown"></div>
<div class="room-container">
    <!-- 왼쪽 : 게임 -->
    <section class="game-section">
        <!-- 게임 보드 영역 -->
        <div class="board-area">
            <div class="player player-left">
                <div class="profile-frame">
                    <img class="profile-img" src="">
                </div>
                <div class="player-nickname"></div>

                <div class="speech-bubble" id="bubble-p1">
                    <span class="bubble-text"></span>
                </div>
            </div>

            <div class="board-stack">
                <!-- 타이틀 -->
                <div class="game-title floating-title">
                    <img src="<%=request.getContextPath()%>/static/img/game/gametitle.png" alt="SpongeBob Omok">
                </div>
                <div class="board-wrapper">
                    <div id="board"></div>
                </div>

                <!-- 타이머 바   -->
                <div class="timerbar"></div>
            </div>
            <div class="player player-right">
                <div class="profile-frame">
                    <img class="profile-img" src="">
                </div>
                <div class="player-nickname"></div>

                <div class="speech-bubble" id="bubble-p2">
                    <span class="bubble-text"></span>
                </div>
            </div>
        </div>
    </section>

    <div class="vertical-divider"></div>

    <!-- 오른쪽 : 채팅 -->
    <section class="chat-section">

        <button id="leaveBtn" class="leave-btn">LEAVE</button>

        <div id="chatLog" class="chat-log"></div>

        <div class="chat-input">
            <input id="chatInput" placeholder="메시지 입력">
            <button id="sendChat">SEND</button>
        </div>

    </section>
</div>


</body>


</html>
