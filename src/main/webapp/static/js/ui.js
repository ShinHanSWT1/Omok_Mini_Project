const statusEl = document.getElementById("status");
const countdownEl = document.getElementById("countdown");
const boardEl = document.getElementById("board");
console.log("ui.js loaded", boardEl);

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
};

let myColor = null;

function handleServerMessage(msg) {
    const handler = messageHandlers[msg.type];
    if (!handler) {
        console.warn("Unhandled message type:", msg.type, msg);
        return;
    }
    handler(msg.payload);
}

function handleJoin(payload) {
    // payload 예시: { userId, role }
    // 지금은 굳이 화면에 표시 안 해도 됨
    console.log("JOIN:", payload);
}

function handleLeave(payload) {
    console.log("LEAVE:", payload);
}

function handleCountdown(payload) {
    showCountdown(payload.sec);
}

function handleGameStart(payload) {
    statusEl.innerText = "게임 시작!";
    countdownEl.innerText = "";

    if (payload.myColor) {
        myColor = payload.myColor;
        console.log("내 색:", myColor);
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
    if (payload.winner) {
        alert("🎉 게임 종료! 승리하셨습니다!");
    } else if (payload.reason) {
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
    const { senderRole, playerIndex, message } = payload;

    if (senderRole === "PLAYER") {
        showPlayerBubble(playerIndex, message);
    } else {
        appendSpectatorChat(message);
    }
}


function showCountdown(sec) {
    statusEl.innerText = "게임 준비 중...";
    countdownEl.innerText = `시작까지 ${sec}초`;
}

function renderBoard() {
    boardEl.innerHTML = "";
    boardEl.className = "board";
    console.log("boardEl:", boardEl);
    for (let y = 0; y < BOARD_SIZE; y++) {
        for (let x = 0; x < BOARD_SIZE; x++) {
            const cell = document.createElement("div");
            cell.className = "cell";
            cell.onclick = () => placeStone(x, y);
            boardEl.appendChild(cell);
        }
    }
}

function drawStone(x, y, color) {
    const idx = y * BOARD_SIZE + x;
    const cell = boardEl.children[idx];
    cell.innerText = color === "BLACK" ? "●" : "○";
}

function showPlayerBubble(playerIndex, message) {
    const bubble = document.getElementById(
        playerIndex === 1 ? "bubble-p1" : "bubble-p2"
    );

    if (!bubble) return;

    bubble.innerText = message;
    bubble.style.display = "block";

    setTimeout(() => {
        bubble.style.display = "none";
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
    const { code, message } = payload;
    console.warn("ERROR:", code, message);

    // 지금은 간단히 알림
    alert(message);
}
