<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>SpongeBob Lobby</title>
    <style>
        /* 기본 설정 */
        * { box-sizing: border-box; }
        body, html {
            margin: 0;
            padding: 0;
            width: 100%;
            height: 100%;
            overflow: hidden;
            background-image: url("${pageContext.request.contextPath}/static/img/LobbyBackground.jpg");
            background-size: cover;
            background-position: center;
        }

        /* 레이아웃 틀*/
        .wrap {
            width: 100%;
            height: 100%;
            position: relative;
            padding: 20px;
        }

        .panel {
            border: 3px solid black;
            border-radius: 15px;
            background-color: #eee;
            padding: 15px;
            box-shadow: 5px 5px 10px rgba(0,0,0,0.2);
            background-color: rgba(255, 255, 255, 0.5);
        }

        /* 왼쪽 패널 (랭킹) */
        .left-panel {
            width: 49%;
            height: 95%;
            float: left;
            margin-top: 20px;
            display: flex;
            flex-direction: column;
            gap: 15px;
        }

        /* 오른쪽 패널 (방목록 + 채팅) */
        .right-panel {
            width: 49%;
            height: calc(95% - 40px);
            float: right;
            margin-top: 60px;
            display: flex;
            flex-direction: column;
            gap: 10px;
        }

        /* 왼쪽: 랭킹 스타일 */
        .rank-list-container {
            flex: 1.2;
            overflow-y: auto;
            padding-right: 5px;
            min-height: 0;
        }
        .rank-item {
            width: 98%;
            margin: 0 auto 10px auto;
            background-color: white;
            border: 2px solid #555;
            border-radius: 10px;
            padding: 10px;
            display: flex; align-items: center;
            box-shadow: 2px 2px 5px rgba(0,0,0,0.1);
            transition: transform 0.2s;
        }
        .rank-item:hover { transform: scale(1.02); background-color: #fff9c4; }
        .rank-badge {
            width: 30px;
            height: 30px;
            border-radius: 50%;
            background-color: #ddd;
            color: black;
            text-align: center;
            line-height: 28px;
            font-weight: bold;
            margin-right: 15px;
            border: 1px solid #999;
            flex-shrink: 0;
        }
        .rank-item:nth-child(1) .rank-badge { background-color: #ffd700; border-color: #d4af37; }
        .rank-item:nth-child(2) .rank-badge { background-color: #c0c0c0; border-color: #a0a0a0; }
        .rank-item:nth-child(3) .rank-badge { background-color: #cd7f32; border-color: #8b4513; }
        .rank-profile-img {
            width: 40px; height: 40px; border-radius: 50%;
            border: 1px solid black; background-color: #ccc;
            margin-right: 15px; object-fit: cover;
        }
        .rank-info { flex-grow: 1; text-align: left; }
        .rank-nickname { font-size: 16px; font-weight: bold; display: block; }
        .rank-score { font-size: 13px; color: #666; }

        /* left-panel: 채팅 스타일 */
        .chat-section {
            flex: 1;
            display: flex;
            flex-direction: column;
            background-color: #fff;
            border: 2px solid #333;
            border-radius: 10px;
            padding: 10px;
        }
        .chat-messages {
            flex-grow: 1;
            overflow-y: auto;
            border: 1px solid #ddd;
            background-color: #f9f9f9;
            padding: 10px;
            margin-bottom: 10px;
            border-radius: 5px;
            font-size: 14px;
        }
        .chat-message { margin-bottom: 5px; }
        .chat-nickname { font-weight: bold; color: #d32f2f; margin-right: 5px; }

        .chat-input-area { display: flex; gap: 5px; }
        #chatInput {
            flex-grow: 1;
            padding: 8px;
            border: 2px solid #ccc;
            border-radius: 5px;
        }
        .chat-input-area button {
            background-color: transparent;
            border: none;
            padding: 0 0 0 5px;
            cursor: pointer;

            display: flex;
            align-items: center;
            justify-content: center;
        }
        .chat-input-area button img {
            height: 38px;
            width: auto;
        }

        /* 오른쪽: 방 목록 스타일 */
        .room-section {
            flex: 1;
            display: flex;
            flex-direction: column;
            border: 2px inset #ddd;
            background-color: #fff;
            border-radius: 10px;
            padding: 10px;
            overflow: hidden;
        }
        .room-list-scroll {
            flex-grow: 1;
            overflow-y: auto;
        }

        /* 방 코드 입력 영역 */
        .room-code-area {
            display: flex;
            gap: 5px;
            margin-top: auto;
            margin-bottom: 5px;
            background-color: #fff;
            padding: 5px;
            border-radius: 5px;
            border: 1px solid #ddd;
        }
        #roomCodeInput {
            flex-grow: 1;
            padding: 8px;
            border: 2px solid #ccc;
            border-radius: 5px;
        }
        .room-code-area button {
            background-color: transparent;
            border: none;
            padding: 0 0 0 5px;
            cursor: pointer;

            display: flex;
            align-items: center;
            justify-content: center;
        }
        .room-code-area button img {
            height: 38px;
            width: auto;
        }

        /* 자바스크립트로 생성될 방 카드 디자인 */
        .room-item {
            background-color: #e3f2fd;
            border: 2px solid #2196f3;
            border-radius: 8px;
            padding: 10px;
            margin-bottom: 8px;
            display: flex;
            justify-content: space-between;
            align-items: center;
            cursor: pointer;
            transition: 0.2s;
        }
        .room-item:hover { background-color: #bbdefb; transform: scale(1.01); }
        .btn-join {
            background-color: #2196f3;
            color: white;
            border: none;
            padding: 5px 15px;
            border-radius: 5px;
            cursor: pointer;
            font-weight: bold;
        }
        .btn-spectate {
            background-color: #9c27b0;
            color: white;
            border: none; padding: 5px 15px;
            border-radius: 5px;
            cursor: pointer;
            font-weight: bold;
        }


        /* 하단 버튼 영역 */
        .control-area {
            display: flex;
            gap: 10px;
            margin-top: auto;
            margin-bottom: 10px;
        }
        .btn-big {
            width: 100%;

            background-color: transparent;
            border: none;
            padding: 0;
            cursor: pointer;

            display: flex;
            justify-content: center;
            align-items: center;
            transition: transform 0.2s;
        }
        .btn-big:hover {
            transform: scale(1.03);
        }
        /* 버튼 내부 이미지 스타일 */
        .btn-big img {
            width: 100%;
            height: auto;
            display: block;
        }

        /* 유저 프로필 */
        .user-profile {
            position: absolute;
            top: 32px;
            right: 20px;

            width: 49%;
            height: 45px;

            display: flex;
            justify-content: flex-end;
            align-items: center;

            background-color: transparent;
            border: none;
            padding: 0;
            cursor: pointer;
            z-index: 100;
        }
        .user-profile img {
            height: 100%;
            width: auto;
            object-fit: contain;
        }
        .profile-menu {
            display: none;
            position: absolute;
            top: 85px;
            right: 20px;
            width: 250px;

            background-color: white;
            border: 2px solid #333;
            border-radius: 10px;
            padding: 15px;
            z-index: 200;
            box-shadow: 5px 5px 15px rgba(0,0,0,0.2);
            font-size: 14px;
            text-align: center;
        }
        /* --- 프로필 메뉴 내부 상세 디자인 추가 --- */
        .menu-header {
            font-size: 16px;
            font-weight: bold;
            margin-bottom: 10px;
            color: #333;
        }
        .menu-divider {
            border: 0;
            border-top: 2px dashed #ccc;
            margin: 10px 0;
        }
        .stat-grid {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 8px;
            margin-bottom: 10px;
        }
        .stat-item {
            background-color: #f1f1f1;
            padding: 8px;
            border-radius: 5px;
            display: flex;
            flex-direction: column;
            justify-content: center;
            align-items: center;
        }
        .stat-label { font-size: 11px; color: #666; margin-bottom: 3px; }
        .stat-value { font-size: 14px; font-weight: bold; color: #000; }

        /* 승률  */
        .stat-item.win-rate {
            grid-column: span 2;
            background-color: #e3f2fd;
            color: #1565c0;
        }

        .logout-btn {
            display: block;
            width: 100%;
            padding: 8px;
            background-color: #ff5252;
            color: white;
            text-decoration: none;
            border-radius: 5px;
            font-weight: bold;
            transition: 0.2s;
        }
        .logout-btn:hover { background-color: #d32f2f; }
    </style>
</head>

<body>

<div class="wrap">

    <div class="user-profile" onclick="toggleMenu()">
        <img src="${pageContext.request.contextPath}/static/img/UserBtn.png" alt="User">
    </div>
    <div id="myMenu" class="profile-menu">
        <c:choose>
            <c:when test="${not empty loginUser}">
                <div class="menu-header">
                    <strong>${loginUser.nickname}</strong>님의 정보
                </div>

                <div class="stat-grid">
                    <div class="stat-item">
                        <span class="stat-label">레이팅</span>
                        <span class="stat-value">${loginUser.record.rating}pt</span>
                    </div>
                    <div class="stat-item">
                        <span class="stat-label">총 경기</span>
                        <span class="stat-value">${loginUser.record.win_count + loginUser.record.lose_count}전</span>
                    </div>
                    <div class="stat-item">
                        <span class="stat-label">승리</span>
                        <span class="stat-value" style="color: #d32f2f;">${loginUser.record.win_count}승</span>
                    </div>
                    <div class="stat-item">
                        <span class="stat-label">패배</span>
                        <span class="stat-value" style="color: #1976d2;">${loginUser.record.lose_count}패</span>
                    </div>

                    <div class="stat-item win-rate">
                        <span class="stat-label">승률</span>
                        <span class="stat-value">
                            <c:set var="totalMatches" value="${loginUser.record.win_count + loginUser.record.lose_count}" />
                            <c:choose>
                                <c:when test="${totalMatches > 0}">
                                    <fmt:formatNumber value="${(loginUser.record.win_count * 100.0) / totalMatches}" pattern="0.0"/>%
                                </c:when>
                                <c:otherwise>0.0%</c:otherwise>
                            </c:choose>
                        </span>
                    </div>
                </div>
                <hr class="menu-divider">
                <a href="/omok/login" class="logout-btn">로그아웃</a>
            </c:when>

            <c:otherwise>
                <div class="menu-header">로그인 정보가 없습니다.</div>
                <hr class="menu-divider">
                <a href="/omok/login" class="logout-btn">로그인 하러가기</a>
            </c:otherwise>
        </c:choose>
    </div>

    <div class="left-panel panel">
        <h3 style="text-align: center; border-bottom: 2px dashed #999; padding-bottom: 10px; margin: 0 0 10px 0;">
            명예의 전당
        </h3>
        <div class="rank-list-container">
            <c:forEach var="ranker" items="${rankingList}">
                <div class="rank-item">
                    <div class="rank-badge">${ranker.rank}</div>
                    <img src="/omok/image/default_profile.png" class="rank-profile-img">
                    <div class="rank-info">
                        <span class="rank-nickname">${ranker.nickname}</span>
                        <span class="rank-score">Rating: ${ranker.rating}</span>
                    </div>
                </div>
            </c:forEach>
            <c:if test="${empty rankingList}">
                <div style="text-align: center; padding: 20px; color: gray;">랭킹 데이터 없음</div>
            </c:if>
        </div>

        <div class="chat-section">
            <div class="chat-messages" id="chatMessages">
            </div>
            <div class="chat-input-area">
                <input type="text" id="chatInput" placeholder="채팅 입력..." onkeypress="handleChatKeyPress(event)">
                <button onclick="sendChat()">
                    <img src="${pageContext.request.contextPath}/static/img/SendBtn.png" alt="전송">
                </button>
            </div>
        </div>
    </div>

    <div class="right-panel panel">

        <div class="room-section">
            <h3 style="margin: 0 0 10px 0;">
                대기 방
                <span id="lobbyStatus" style="font-size: 12px; color: gray; font-weight: normal;">(연결 중...)</span>
            </h3>

            <div id="roomList" class="room-list-scroll">
            </div>
        </div>

        <div class="room-code-area">
            <input type="text" id="roomCodeInput" placeholder="방 번호/코드 입력" onkeypress="handleRoomCodeKeyPress(event)">
            <button onclick="enterRoomByCode()">
                <img src="${pageContext.request.contextPath}/static/img/EnterPrivateRoomBtn.png" alt="입장">
            </button>
        </div>

        <div class="control-area">
            <button class="btn-big" style="flex: 1;" onclick="location.href='/omok/lobby/quick-enter'">
                <img src="${pageContext.request.contextPath}/static/img/QuickEntryBtn.png" alt="빠른 입장">
            </button>

            <form action="/omok/lobby/create" method="post" style="flex: 1; margin: 0;">
                <button type="submit" class="btn-big">
                    <img src="${pageContext.request.contextPath}/static/img/CreateRoomBtn.png" alt="방 만들기">
                </button>
            </form>
        </div>
    </div>

</div>

<script>
    // 1. 프로필 메뉴 토글
    function toggleMenu() {
        var menu = document.getElementById("myMenu");
        menu.style.display = (menu.style.display === "block") ? "none" : "block";
    }

    // 2. WebSocket 연결 설정
    const lobbySocket = new WebSocket("ws://" + location.host + "/omok/ws/lobby");

    lobbySocket.onopen = () => {
        console.log("[Lobby] WebSocket 연결 성공");
        document.getElementById("lobbyStatus").innerText = "✅ 실시간 연동됨";
        document.getElementById("lobbyStatus").style.color = "green";
    };

    lobbySocket.onmessage = (event) => {
        const data = JSON.parse(event.data);

        switch (data.type) {
            case "CONNECTED":
                console.log("서버 연결 메시지: " + data.message);
                break;
            case "ROOM_LIST":
                renderRoomList(data.rooms); // 방 목록 그리기 함수 호출
                break;
            case "CHAT":
                addChatMessage(data.nickname, data.message); // 채팅 추가 함수 호출
                break;
            default:
                console.log("알 수 없는 메시지:", data);
        }
    };

    lobbySocket.onclose = () => {
        document.getElementById("lobbyStatus").innerText = "❌ 연결 끊김";
        document.getElementById("lobbyStatus").style.color = "red";
    };

    // 3. 방 목록 렌더링 (디자인 입혀서 출력)
    function renderRoomList(rooms) {
        const roomListDiv = document.getElementById("roomList");
        roomListDiv.innerHTML = ""; // 기존 목록 초기화

        if (!rooms || rooms.length === 0) {
            roomListDiv.innerHTML = "<div style='text-align:center; padding:20px; color:gray;'>대기 중인 방이 없습니다.</div>";
            return;
        }

        rooms.forEach(room => {
            // HTML 요소 생성 (기존 CSS 클래스 .room-item 사용)
            const item = document.createElement("div");
            item.className = "room-item";

            // 방 정보 텍스트
            const info = document.createElement("div");
            info.innerHTML = "<strong>Room " + room.roomId + "</strong> <span style='font-size:12px; color:#666;'>(" + room.players.length + "/2명)</span>";

            // 버튼들을 담을 그룹 생성
            const btnGroup = document.createElement("div");
            btnGroup.style.display = "flex";
            btnGroup.style.gap = "5px";

            // 입장 버튼
            const joinBtn = document.createElement("button");
            joinBtn.className = "btn-join";
            joinBtn.innerText = "입장";

            // 버튼 클릭 이벤트
            joinBtn.onclick = function() {
                location.href = '/omok/lobby/enter?roomId=' + room.roomId + '&role=player';
            };

            // 관전 버튼
            const spectateBtn = document.createElement("button");
            spectateBtn.className = "btn-spectate";
            spectateBtn.innerText = "관전";
            // [수정] 사용하지 않는 주석 처리된 코드 삭제
            spectateBtn.onclick = function() {
                console.log('[lobby.jsp] spectateBtn= /omok/lobby/enter?roomId=' + room.roomId + '&role=spectator')
                location.href = '/omok/lobby/enter?roomId=' + room.roomId + '&role=spectator';
            };

            // 조립
            btnGroup.appendChild(joinBtn);
            btnGroup.appendChild(spectateBtn);

            item.appendChild(info);
            item.appendChild(btnGroup);

            roomListDiv.appendChild(item);
        });
    }

    // 4. 채팅 기능
    function addChatMessage(nickname, message) {
        const chatBox = document.getElementById("chatMessages");

        const msgDiv = document.createElement("div");
        msgDiv.className = "chat-message";
        msgDiv.innerHTML = "<span class='chat-nickname'>" + nickname + ":</span>" + message;

        chatBox.appendChild(msgDiv);
        chatBox.scrollTop = chatBox.scrollHeight; // 스크롤 맨 아래로
    }

    function sendChat() {
        const input = document.getElementById("chatInput");
        const msg = input.value.trim();
        if (msg === "") return;

        lobbySocket.send(JSON.stringify({ type: "CHAT", message: msg }));
        input.value = "";
    }

    function handleChatKeyPress(e) {
        if (e.key === "Enter") sendChat();
    }

    // [복구] 5. 방 코드로 입장하기 (기존 코드에서 누락된 부분 복구)
    function enterRoomByCode() {
        const input = document.getElementById("roomCodeInput");
        const code = input.value.trim();
        if (code === "") {
            alert("입장할 방 번호나 코드를 입력해주세요!");
            return;
        }
        location.href = '/omok/lobby/enter?roomId=' + code;
    }

    function handleRoomCodeKeyPress(e) {
        if (e.key === "Enter") enterRoomByCode();
    }
</script>

</body>
</html>