<%--
  Created by IntelliJ IDEA.
  User: leeyj
  Date: 25. 12. 14.
  Time: 오후 2:09
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="en">

<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>SpongeBob Lobby</title>
  <style>
    * {
      box-sizing: border-box;
    }
    body, html {
      margin: 0;
      padding: 0;
      width: 100%;
      height: 100%;
      overflow: hidden;
      background-image: url("/image/LobbyBackground.jpg");
    }

    /* 전체 틀 */
    .wrap {
      width: 100%;
      height: 100%;
      border-radius: 15px;
      position: relative; /* 자식 요소들이 이 박스를 기준으로 위치를 잡음 */
      padding: 20px;
    }

    /* 공통 박스 스타일 */
    .panel {
      height: 90%;
      border: 3px solid black;
      border-radius: 15px;
      background-color: #eee;
      padding: 20px; /* 패널 내부 글씨 여백 */
    }
    /* 랭킹 패널 */
    .left-panel {
      width: 48%;
      float: left;
      height: 85%;
      margin-top: 40px;

    }
    /* 방 리스트, 채팅 */
    .right-panel {
      width: 48%;
      float: right;
      margin-top: 40px;
      height: 85%;

      /* 내부 요소 정렬을 위한 설정 */
      display: flex;
      flex-direction: column;
      justify-content: space-between;
    }
    /* 방 리스트 */
    .room-list-container {
      flex-grow: 1;
      overflow-y: auto;  /* 스크롤 생기게 */
      margin-bottom: 10px;
      border: 2px inset #ddd;
      background-color: #fff;
      border-radius: 10px;
      padding: 10px;
    }
    .room-item {
      background-color: #e3f2fd;
      border: 2px solid #2196f3;
      border-radius: 8px;
      padding: 10px;
      margin-bottom: 8px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      transition: 0.2s;
    }
    /* 마우스 effect*/
    .room-item:hover {
      transform: scale(1.02);
      background-color: #bbdefb;
    }
    .room-title { font-weight: bold; font-size: 15px; }
    .room-info { font-size: 12px; color: #555; }
    /* 입장 버튼 (작은 것) */
    .btn-join {
      background-color: #2196f3;
      color: white;
      border: none;
      padding: 5px 10px;
      border-radius: 5px;
      cursor: pointer;
      font-weight: bold;
    }

    /* 2단 컨트롤 영역 (btn) */
    .control-area {
      height: auto;
      background-color: #ddd;
      border-radius: 10px;
      padding: 10px;
      border: 2px solid #999;

      display: flex;
      flex-direction: column;
      gap: 5px;
    }

    /* 큰 버튼 공통 스타일 */
    .btn-big {
      width: 100%;
      padding: 10px;
      font-size: 16px;
      font-weight: bold;
      color: white;
      border: 2px solid black;
      border-radius: 8px;
      cursor: pointer;
    }
    .btn-create { background-color: #ff9800; }
    .btn-quick { background-color: #4caf50; }

    /* 방 번호 입력 폼 */
    .input-group {
      display: flex;
      margin-top: 5px;
    }
    /* 방 코드 입력 부분 */
    .input-code {
      flex-grow: 1;
      padding: 8px;
      border: 2px solid black;
      border-radius: 5px 0 0 5px;
    }
    /* 코드 입력 후 전송 버튼*/
    .btn-code {
      padding: 8px 15px;
      background-color: #607d8b;
      color: white;
      font-weight: bold;
      border: 2px solid black;
      border-left: none;
      border-radius: 0 5px 5px 0;
      cursor: pointer;
    }
    .user-profile {
      position: absolute;
      top: 10px;
      right: 20px;

      width: 120px;
      height: 40px;
      background-color: #333; /* 임시 색상 (나중에 사진 넣기) */
      border-radius: 10px;
      border: 2px solid white;
      cursor: pointer;
      z-index: 100;
      text-align: center;
      line-height: 40px;
      color: white;
      font-weight: bold;
    }
    /* 프로필 클릭 시 나올 메뉴 (숨김 상태) */
    .profile-menu {
      display: none;
      position: absolute;
      top: 90px;
      right: 20px;

      width: 200px;
      background-color: white;
      border: 2px solid black;
      border-radius: 5px;
      padding: 5px;
      z-index: 101;
      box-shadow: 0 4px 8px rgba(0,0,0,0.2);
    }
    .rank-list-container {
      width: 100%;
      height: 100%;
      overflow-y: auto;
    }
    .rank-item {

      width: 96%;
      margin: 0 auto 10px auto;

      background-color: white;
      border: 2px solid #555;
      border-radius: 10px;
      padding: 10px;

      display: flex;
      align-items: center;
      box-shadow: 2px 2px 5px rgba(0,0,0,0.1);
      transition: transform 0.2s;
    }
    .rank-item:hover {
      transform: scale(1.02); /* 마우스 올리면 살짝 커짐 */
      background-color: #fff9c4;
    }
    /* 순위 표시 아이콘*/
    .rank-badge {
      width: 30px;
      height: 30px;
      border-radius: 50%;
      background-color: #ddd;
      color: black;
      text-align: center;
      line-height: 30px;
      font-weight: bold;
      margin-right: 15px;
      border: 1px solid #999;
    }

    /* 1,2,3등은 금은동 배지를 추가 */
    .rank-item:nth-child(1) .rank-badge { background-color: #ffd700; border-color: #d4af37; }
    .rank-item:nth-child(2) .rank-badge { background-color: #c0c0c0; border-color: #a0a0a0; }
    .rank-item:nth-child(3) .rank-badge { background-color: #cd7f32; border-color: #8b4513; }

    /* 프로필 이미지  */
    .rank-profile-img {
      width: 40px;
      height: 40px;
      border-radius: 50%;
      border: 1px solid black;
      background-color: #ccc; /* 색 수정 필요 */
      margin-right: 15px;

      object-fit: cover;
    }
    /* 닉네임과 점수 */
    .rank-info {
      flex-grow: 1;
      text-align: left;
    }
    .rank-nickname {
      font-size: 16px;
      font-weight: bold;
      color: #333;
      display: block;
    }
    .rank-score {
      font-size: 14px;
      color: #666;
    }

  </style>
  <script>
      /* 유저 프로필 토글 */
    function toggleMenu() {
      var menu = document.getElementById("myMenu");

      if (menu.style.display === "block") {
        menu.style.display = "none";
      } else {
        menu.style.display = "block";
      }
    }
  </script>


</head>
<body>
<div class="wrap">
  <div class="user-profile" onclick="toggleMenu()">
    User
  </div>

    <%-- 유저 프로필 조회 더미 데이터   --%>
  <div id="myMenu" class="profile-menu">
    <strong>닉네임: 징징이</strong><br>
    승률: 50%<br>
    점수: 1200점<br>
    <hr>
    로그아웃
  </div>

    <%-- 랭크    --%>
  <div class="left-panel panel">
    <h3 style="text-align: center; margin-top: 0; border-bottom: 2px dashed #999; padding-bottom: 10px;">
      여기서 제일 잘하는 사람
    </h3>

    <div class="rank-list-container">

      <c:forEach var="ranker" items="${rankingList}">
        <div class="rank-item">
          <div class="rank-badge">${ranker.rank}</div>

          <img src="/omok/image/default_profile.png" alt="P" class="rank-profile-img">

          <div class="rank-info">
            <span class="rank-nickname">${ranker.nickname}</span>
            <span class="rank-score">Rating: ${ranker.rating}</span>
          </div>
        </div>
      </c:forEach>

      <c:if test="${empty rankingList}">
        <div style="text-align: center; padding: 20px; color: gray;">
          아직 랭킹 정보가 없습니다.<br>
        </div>
      </c:if>
    </div>
  </div>

  <h2>로비</h2>

  <ul>
    <c:forEach var="room" items="${rooms}">
      <li>
        방 ${room.roomId}
        <button onclick="location.href='/omok/lobby/enter?roomId=${room.roomId}'">입장</button>
      </li>
    </c:forEach>
  </ul>

    <!-- 오른쪽: 방 목록 + 채팅 -->
    <div class="right-section">
        <!-- 방 목록 (위) -->
        <div class="room-section">
            <h2>대기 방</h2>
            <p class="status" id="lobbyStatus">로비 연결 중...</p>

            <!-- 방 목록  -->
            <ul class="room-list" id="roomList">
                <!-- JavaScript로 동적 생성 -->
            </ul>

            <!-- btn -->
            <div class="action-buttons">
                <button onclick="location.href='/omok/lobby/quick-enter'">⚡ 빠른 입장</button>
                <form method="post" action="/omok/lobby/create" style="flex: 1; margin: 0;">
                    <button type="submit" style="width: 100%;">➕ 방 생성</button>
                </form>
            </div>
        </div>

        <!-- 채팅 (아래) -->
        <div class="chat-section">
            <h3>💬 로비 채팅</h3>
            <div class="chat-messages" id="chatMessages">
                <!-- 채팅 메시지가 여기에 표시됨 -->
            </div>
            <div class="chat-input-area">
                <input type="text" id="chatInput" placeholder="메시지를 입력하세요..."
                       onkeypress="handleChatKeyPress(event)">
                <button onclick="sendChat()">전송</button>
            </div>
        </div>
    </div>
</div>

<script>
    // WebSocket 연결
    const lobbySocket = new WebSocket(
        "ws://" + location.host + "/omok/ws/lobby"
    );

    // WebSocket 연결 성공
    lobbySocket.onopen = () => {
        console.log("[Lobby] WebSocket 연결 성공");
        document.getElementById("lobbyStatus").innerText = "✅ 로비 접속 완료";
    };

    // WebSocket 메시지 수신
    lobbySocket.onmessage = (event) => {
        const data = JSON.parse(event.data);
        console.log("[Lobby] 메시지 수신:", data);

        switch (data.type) {
            case "CONNECTED":
                // 연결 확인 메시지
                console.log(data.message);
                break;

            case "ROOM_LIST":
                // 방 목록 업데이트
                renderRoomList(data.rooms);
                break;

            case "CHAT":
                // 채팅 메시지 표시
                addChatMessage(data.nickname, data.message);
                break;

            default:
                console.log("[Lobby] 알 수 없는 메시지:", data);
        }
    };

    // WebSocket 연결 종료
    lobbySocket.onclose = () => {
        console.log("[Lobby] WebSocket 연결 종료");
        document.getElementById("lobbyStatus").innerText = "❌ 로비 연결 끊김";
    };

    // WebSocket 에러
    lobbySocket.onerror = (error) => {
        console.error("[Lobby] WebSocket 에러:", error);
        document.getElementById("lobbyStatus").innerText = "⚠️ 로비 연결 오류";
    };

    /**
     * 방 목록을 동적으로 렌더링
     */
    function renderRoomList(rooms) {
        const roomList = document.getElementById("roomList");
        roomList.innerHTML = ""; // 기존 목록 초기화

        // 디버깅: 받은 방 목록 확인
        console.log("[Lobby] 방 목록 렌더링:", rooms);

        if (rooms.length === 0) {
            roomList.innerHTML = "<li style='text-align: center; color: #999; padding: 30px;'>대기 중인 방이 없습니다</li>";
            return;
        }

        rooms.forEach(room => {
            // 디버깅: 각 방 정보 확인
            console.log("[Lobby] Room 정보:", room);
            console.log("[Lobby] roomId:", room.roomId);

            const li = document.createElement("li");
            li.className = "room-item";

            const roomInfo = document.createElement("span");
            roomInfo.innerText = `방 (${room.roomId.substring(0, 8)})... (${room.players.length}/2)`;

            const enterBtn = document.createElement("button");
            enterBtn.type = "button";
            enterBtn.innerText = "입장";

            // ⭐ data attribute에 roomId 저장 (더 확실한 방법)
            enterBtn.setAttribute('data-room-id', room.roomId);
            console.log("[DEBUG] 버튼 생성 - data-room-id 설정:", room.roomId);

            enterBtn.addEventListener('click', function(e) {
                e.preventDefault();

                const roomId = this.getAttribute('data-room-id');

                console.log("=== 입장 버튼 클릭 ===");
                console.log("data-room-id:", roomId);
                console.log("roomId 타입:", typeof roomId);
                console.log("roomId 길이:", roomId ? roomId.length : 0);
                console.log("=====================");

                if (!roomId || roomId === '' || roomId === 'null' || roomId === 'undefined') {
                    alert(`에러!\nroomId: ${roomId}\n새로고침 후 다시 시도해주세요.`);
                    return;
                }
                console.log(roomId);
                const url = '/omok/lobby/enter?roomId='+roomId;
                console.log("이동할 URL:", url);
                location.href = url;
            });

            li.appendChild(roomInfo);
            li.appendChild(enterBtn);
            roomList.appendChild(li);
        });
    }

    /**
     * 채팅 메시지 표시
     */
    function addChatMessage(nickname, message) {
        const chatMessages = document.getElementById("chatMessages");

        const messageDiv = document.createElement("div");
        messageDiv.className = "chat-message";

        const nicknameSpan = document.createElement("span");
        nicknameSpan.className = "chat-nickname";
        nicknameSpan.innerText = nickname + ":";

        const messageText = document.createTextNode(" " + message);

        messageDiv.appendChild(nicknameSpan);
        messageDiv.appendChild(messageText);
        chatMessages.appendChild(messageDiv);

        // 스크롤을 맨 아래로
        chatMessages.scrollTop = chatMessages.scrollHeight;
    }

    /**
     * 채팅 메시지 전송
     */
    function sendChat() {
        const chatInput = document.getElementById("chatInput");
        const message = chatInput.value.trim();

        if (message === "") {
            return;
        }

        // 서버로 채팅 메시지 전송
        const chatData = {
            type: "CHAT",
            message: message
        };

        lobbySocket.send(JSON.stringify(chatData));

        // 입력창 초기화
        chatInput.value = "";
    }

    /**
     * 엔터키로 채팅 전송
     */
    function handleChatKeyPress(event) {
        if (event.key === "Enter") {
            sendChat();
        }
    }
</script>

</body>
</html>
