<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
    <title>로비</title>
    <style>
        body {
            background-image: url('${pageContext.request.contextPath}/img/LobbyBackground.jpg');
            background-size: cover;
            background-position: center;
            background-repeat: no-repeat;
            background-attachment: fixed;
            margin: 0;
            padding: 20px;
            font-family: Arial, sans-serif;
        }

        .container {
            display: flex;
            gap: 20px;
            max-width: 1400px;
            margin: 0 auto;
            height: 90vh;
        }

        /* 왼쪽: 랭킹 */
        .ranking-section {
            flex: 0 0 300px;
            background: rgba(255, 255, 255, 0.95);
            padding: 20px;
            border-radius: 10px;
            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
        }

        .ranking-section h2 {
            margin-top: 0;
            color: #333;
            border-bottom: 2px solid #4CAF50;
            padding-bottom: 10px;
        }

        .ranking-list {
            list-style: none;
            padding: 0;
            margin: 0;
        }

        .ranking-item {
            background: #f9f9f9;
            padding: 12px;
            margin: 8px 0;
            border-radius: 5px;
            border-left: 4px solid #4CAF50;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }

        .ranking-item.rank-1 { border-left-color: gold; }
        .ranking-item.rank-2 { border-left-color: silver; }
        .ranking-item.rank-3 { border-left-color: #CD7F32; }

        .rank-number {
            font-weight: bold;
            font-size: 18px;
            color: #666;
            margin-right: 10px;
        }

        .nickname {
            flex: 1;
            font-weight: 500;
        }

        .rating {
            color: #4CAF50;
            font-weight: bold;
        }

        /* 오른쪽: 방 목록 + 채팅 */
        .right-section {
            flex: 1;
            display: flex;
            flex-direction: column;
            gap: 20px;
        }

        /* 방 목록 (오른쪽 위) */
        .room-section {
            flex: 1;
            background: rgba(255, 255, 255, 0.95);
            padding: 20px;
            border-radius: 10px;
            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
            display: flex;
            flex-direction: column;
        }

        .room-section h2 {
            margin-top: 0;
            color: #333;
            border-bottom: 2px solid #2196F3;
            padding-bottom: 10px;
        }

        .status {
            color: #666;
            font-size: 14px;
            margin-bottom: 10px;
        }

        .room-list {
            list-style: none;
            padding: 0;
            margin: 10px 0;
            flex: 1;
            overflow-y: auto;
        }

        .room-item {
            background: #f5f5f5;
            padding: 15px;
            margin: 8px 0;
            border-radius: 5px;
            display: flex;
            justify-content: space-between;
            align-items: center;
            transition: background 0.2s;
        }

        .room-item:hover {
            background: #e8e8e8;
        }

        .room-item button {
            background: #4CAF50;
            color: white;
            border: none;
            padding: 8px 20px;
            border-radius: 5px;
            cursor: pointer;
            transition: background 0.2s;
        }

        .room-item button:hover {
            background: #45a049;
        }

        .action-buttons {
            margin-top: 15px;
            display: flex;
            gap: 10px;
        }

        .action-buttons button {
            flex: 1;
            background: #2196F3;
            color: white;
            border: none;
            padding: 12px 20px;
            border-radius: 5px;
            cursor: pointer;
            font-size: 14px;
            transition: background 0.2s;
        }

        .action-buttons button:hover {
            background: #0b7dda;
        }

        /* 채팅 (오른쪽 아래) */
        .chat-section {
            flex: 0 0 300px;
            background: rgba(255, 255, 255, 0.95);
            padding: 20px;
            border-radius: 10px;
            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
            display: flex;
            flex-direction: column;
        }

        .chat-section h3 {
            margin-top: 0;
            color: #333;
            border-bottom: 2px solid #FF9800;
            padding-bottom: 10px;
        }

        .chat-messages {
            flex: 1;
            overflow-y: auto;
            border: 1px solid #ddd;
            padding: 10px;
            margin-bottom: 10px;
            background: white;
            border-radius: 5px;
        }

        .chat-message {
            margin: 8px 0;
            padding: 5px;
            word-wrap: break-word;
        }

        .chat-message .chat-nickname {
            font-weight: bold;
            color: #2196F3;
            margin-right: 5px;
        }

        .chat-input-area {
            display: flex;
            gap: 10px;
        }

        .chat-input-area input {
            flex: 1;
            padding: 10px;
            border: 1px solid #ddd;
            border-radius: 5px;
            font-size: 14px;
        }

        .chat-input-area button {
            background: #FF9800;
            color: white;
            border: none;
            padding: 10px 20px;
            border-radius: 5px;
            cursor: pointer;
            transition: background 0.2s;
        }

        .chat-input-area button:hover {
            background: #F57C00;
        }
    </style>
</head>
<body>

<div class="container">
    <!-- 왼쪽: 랭킹 -->
    <div class="ranking-section">
        <h2>🏆 랭킹 TOP 5</h2>
        <ul class="ranking-list">
            <c:forEach var="ranking" items="${rankings}">
                <li class="ranking-item rank-${ranking.rank}">
                    <span class="rank-number">${ranking.rank}</span>
                    <span class="nickname">${ranking.nickname}</span>
                    <span class="rating">${ranking.rating}</span>
                </li>
            </c:forEach>
            <c:if test="${empty rankings}">
                <li style="text-align: center; color: #999; padding: 20px;">
                    랭킹 데이터가 없습니다
                </li>
            </c:if>
        </ul>
    </div>

    <!-- 오른쪽: 방 목록 + 채팅 -->
    <div class="right-section">
        <!-- 방 목록 (위) -->
        <div class="room-section">
            <h2>🎮 대기 방</h2>
            <p class="status" id="lobbyStatus">로비 연결 중...</p>

            <!-- 방 목록 (동적으로 렌더링됨) -->
            <ul class="room-list" id="roomList">
                <!-- JavaScript로 동적 생성 -->
            </ul>

            <!-- 버튼들 -->
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
