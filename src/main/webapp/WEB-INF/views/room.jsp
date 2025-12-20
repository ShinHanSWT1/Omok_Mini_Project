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
    const WS_URL = "ws://" + location.host + "<%=request.getContextPath()%>/ws/game/" + ROOM_ID;
  </script>

  <!-- JS -->
  <script defer src="<%=request.getContextPath()%>/static/js/game.js"></script>
  <script defer src="<%=request.getContextPath()%>/static/js/ui.js"></script>
  <script defer src="<%=request.getContextPath()%>/static/js/websocket.js"></script>
</head>

<body>

<h2>오목 게임방</h2>
<p>방 ID: <%= room.getRoomId() %></p>
<p>방장: <%= room.getOwnerId() %></p>

<p id="status">상대방을 기다리는 중...</p>
<p id="countdown"></p>


<button id="leaveBtn">방 나가기</button>

<div class="container">
  <!-- 게임 영역 -->
  <div class="game-area">
    <div class="board-wrapper">
      <div id="board"></div>

      <!-- 플레이어 프로필 -->
      <div class="player player-1">
        <img src="<%= request.getContextPath() %>/static/img/profile/<%= loginUser.getProfileImg() %>">
        <div class="bubble" id="bubble-p1"></div>
      </div>

      <div class="player player-2">
        <img src="<%= request.getContextPath() %>/static/img/profile/<%= loginUser.getProfileImg() %>">
        <div class="bubble" id="bubble-p2"></div>
      </div>
    </div>
  </div>

  <!-- 채팅 영역 -->
  <div class="chat-area">
    <h3>CHATTING</h3>
    <div id="chatLog"></div>
    <div class="chat-input">
      <input type="text" id="chatInput" placeholder="메시지 입력">
      <button id="sendChat">전송</button>
    </div>
  </div>
</div>

</body>


</html>
