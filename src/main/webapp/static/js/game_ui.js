const statusEl = document.getElementById("status");
const countdownEl = document.getElementById("countdown");
const boardEl = document.getElementById("board");
const playerLeftEl = document.querySelector(".player-left");
const playerRightEl = document.querySelector(".player-right");
let gridLayer = null;
let myColor = null;
let myUserId = null;
let isSpectator = false;
// console.log("game_ui.js loaded", boardEl);

const messageHandlers = {
    JOIN: handleJoin,
    LEAVE: handleLeave,
    COUNTDOWN: handleCountdown,
    GAME_START: handleGameStart,
    MOVE_OK: handleMoveOk,
    ROOM_WAIT: handleRoomWait,
    GAME_END: handleGameEnd,
    CHAT: handleChat,
    ERROR: handleError,
    ROOM_MEMBERS: handleRoomMembers,
    BOARD_SNAPSHOT: handleBoardSnapshot,
};

function handleServerMessage(msg) {
    const handler = messageHandlers[msg.type];
    if (!handler) {
        console.warn("Unhandled message type:", msg.type, msg);
        return;
    }
    handler(msg.payload);
}

function handleJoin(payload) {
    console.log("JOIN:", payload);
    isSpectator = payload.role === "SPECTATOR";

    // UI 업데이트 (기존 로직)
    updatePlayerUI(payload);

    // 채팅창에 입장 알림 출력
    // 내가 들어왔을 때 기존 멤버 리스트를 받는 경우(ROOM_MEMBERS)가 아니라면 출력
    if (payload.nickname) {
        appendSystemMessage(`[알림] ${payload.nickname}님이 입장하셨습니다.`);
    }
}

function handleLeave(payload) {
    console.log("LEAVE:", payload);

    // 채팅창에 퇴장 알림 출력
    if (payload.nickname) {
        appendSystemMessage(`[알림] ${payload.nickname}님이 퇴장하셨습니다.`);
    }

    // 플레이어가 나갔을 때 UI 초기화 (기본 이미지로 변경 등) 필요 시 추가
    if (payload.reason === "PLAYER_LEFT" || payload.reason === "PLAYER_GG") {
        resetPlayerUI(payload.userId);
    }
}

function handleCountdown(payload) {
    showCountdown(payload.sec);
}

function handleGameStart(payload) {
    if (payload.myColor) {
        myColor = payload.myColor;
    }

    if (payload.myUserId) {
        myUserId = payload.myUserId;
    }

    startGame(payload.firstTurn);
}

function handleMoveOk(payload) {
    applyMove(payload.x, payload.y, payload.color);
}

function handleRoomWait(payload) {
    statusEl.innerText = "상대방을 기다리는 중...";
    countdownEl.innerText = "";
}

function handleGameEnd(payload) {
    console.log(myUserId);
    // 타임아웃으로 인한 게임 종료 처리
    if (payload.reason === "TIMEOUT") {
        // 플레이어
        if (payload.winner) {
            if (payload.winner === myUserId) {
                alert("상대가 시간 초과로 패배했습니다!");
            } else {
                alert("시간 초과로 패배했습니다.");
            }
        }
        return;
    }

    if (payload.winner === myUserId) {
        alert("🎉 게임 종료! 승리하셨습니다!");
    } else if (payload.winner !== myUserId) {
        alert("게임에서 패배했습니다 :(")
    } else if (payload.winner) {
        alert("게임 종료: " + payload.reason);
    } else {
        alert("게임이 종료되었습니다.");
    }

    // 잠깐 딜레이 주고 이동해도 좋음
    setTimeout(() => {
        location.href = "/omok/lobby";
    }, 300);
}

function handleChat(payload) {
    const {senderRole, playerIndex, message} = payload;

    if (senderRole === "PLAYER") {
        showPlayerBubble(playerIndex, message);
    } else {
        appendSpectatorChat(message);
    }
}

function handleBoardSnapshot(payload) {
    const {board, turn, remainingTime} = payload;

    // 1. 보드 초기화 및 렌더링
    renderBoard();

    // 2. 2차원 배열을 돌며 돌 그리기
    for (let y = 0; y < board.length; y++) {
        for (let x = 0; x < board[y].length; x++) {
            const stone = board[y][x];
            if (stone === "BLACK" || stone === "WHITE") {
                drawStone(x, y, stone);
            }
        }
    }

    // 3. 현재 턴 표시 및 남은 시간 UI 연동 (필요 시)
    updateActivePlayer(turn);
    console.log(`현재 ${turn}의 턴, 남은 시간: ${remainingTime}ms`);
}


function showCountdown(sec) {
    statusEl.innerText = "게임 준비 중...";
    countdownEl.innerText = `시작까지 ${sec}초`;
}

function renderBoard() {
    boardEl.innerHTML = "";
    boardEl.className = "board";
    console.log("boardEl:", boardEl);

    gridLayer = document.createElement("div");
    gridLayer.className = "grid-layer";
    boardEl.appendChild(gridLayer);

    for (let y = 0; y < BOARD_SIZE; y++) {
        for (let x = 0; x < BOARD_SIZE; x++) {
            const cell = document.createElement("div");
            cell.className = "cell";
            gridLayer.appendChild(cell);
            cell.onclick = () => {
                console.log("cell clicked:", x, y);
                placeStone(x, y);
            }
        }
    }
}

function drawStone(x, y, color) {
    const idx = y * BOARD_SIZE + x;
    const cell = gridLayer.children[idx];

    cell.classList.add(color === "BLACK" ? "black" : "white");
}

function showPlayerBubble(playerIndex, message) {
    const bubble = document.getElementById(
        playerIndex === 1 ? "bubble-p1" : "bubble-p2"
    );
    if (!bubble) return;

    const textEl = bubble.querySelector(".bubble-text");
    if (textEl) textEl.textContent = message;

    bubble.classList.add("show");

    clearTimeout(bubble._hideTimer);
    bubble._hideTimer = setTimeout(() => {
        bubble.classList.remove("show");
    }, 3000);
}


function appendSpectatorChat(message) {
    const chatLog = document.getElementById("chatLog");
    if (!chatLog) return;

    const div = document.createElement("div");
    div.innerText = message;
    chatLog.appendChild(div);

    // 자동 스크롤
    chatLog.scrollTop = chatLog.scrollHeight;
}

function handleError(payload) {
    const {code, message} = payload;
    console.warn("ERROR:", code, message);

    // 지금은 간단히 알림
    alert(message);
}

function updateActivePlayer(turnColor) {
    playerLeftEl.classList.remove("active");
    playerRightEl.classList.remove("active");

    if (turnColor === "BLACK") {
        playerLeftEl.classList.add("active");
    } else if (turnColor === "WHITE") {
        playerRightEl.classList.add("active");
    }
}

function handleRoomMembers(payload) {
    console.log("현재 방 멤버 리스트:", payload);
    payload.forEach(user => updatePlayerUI(user));
}

function updatePlayerUI(payload) {
    // 관전자는 UI 상단 프로필 자리에 그리지 않음
    if (payload.role === "SPECTATOR") {
        console.log("관전자 입장: " + payload.nickname);
        appendSpectatorChat(payload.nickname + "님이 관전 중입니다.");
        return;
    }

    // 방장(OWNER_ID)이면 왼쪽, 아니면 오른쪽 배치
    const isOwner = String(payload.userId) === String(OWNER_ID);
    const targetEl = isOwner ? playerLeftEl : playerRightEl;

    if (!targetEl) return;

    // 프로필 이미지 업데이트
    const imgEl = targetEl.querySelector(".profile-img");
    if (imgEl && payload.profileImg) {
        imgEl.src = payload.profileImg;
    }

    // 닉네임 업데이트
    const nameEl = targetEl.querySelector(".player-nickname");
    if (nameEl && payload.nickname) {
        nameEl.innerText = payload.nickname;
    }
}

// 시스템 메시지 출력용 함수 추가
function appendSystemMessage(msg) {
    const chatLog = document.getElementById("chatLog");
    if (!chatLog) return;

    const div = document.createElement("div");
    div.className = "system-message"; // CSS 스타일링을 위해 클래스 추가
    div.innerText = msg;

    // 스타일 직접 지정 (TODO: CSS 파일로 빼기)
    div.style.color = "#888";
    div.style.fontSize = "0.9em";
    div.style.textAlign = "center";
    div.style.margin = "5px 0";

    chatLog.appendChild(div);
    chatLog.scrollTop = chatLog.scrollHeight;
}

// 플레이어 나갔을 때 프사 내리는 함수
function resetPlayerUI(leftUserId) {
    // 나간 사람이 방장(왼쪽)이었는지 확인
    const isOwner = String(leftUserId) === String(OWNER_ID);
    const targetEl = isOwner ? playerLeftEl : playerRightEl;

    if (targetEl) {
        const imgEl = targetEl.querySelector(".profile-img");
        if (imgEl) imgEl.src = ""; // 혹은 기본 이미지

        const nameEl = targetEl.querySelector(".player-nickname");
        if (nameEl) nameEl.innerText = "Waiting...";
    }
}