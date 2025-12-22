let socket = null;

function connectWebSocket() {
    console.log("[Websocket.js] WS_URL=" + WS_URL);
    // 예상되는 WS_URL = ws://localhost:8090/omok/ws/game/{roomId}?role=spectator
    socket = new WebSocket(WS_URL);

    socket.onopen = () => {
        console.log("WebSocket 연결 성공");
    };

    socket.onmessage = (event) => {
        const message = JSON.parse(event.data);
        handleServerMessage(message);
    };

    socket.onclose = () => {
        console.log("WebSocket 연결 종료");
    };
}

function sendMessage(type, payload) {
    socket.send(JSON.stringify({ type, payload }));
}

document.addEventListener("DOMContentLoaded", () => {
    connectWebSocket();

    document.getElementById("sendChat").onclick = () => {
        const input = document.getElementById("chatInput");
        if (!input.value.trim()) return;

        sendMessage("CHAT", input.value);
        input.value = "";
    };

    document.getElementById("leaveBtn").onclick = () => {
        socket.close();
        location.href = "/omok/lobby";
    };

    document.getElementById("chatInput").addEventListener("keydown", (e) => {
        if (e.key === "Enter") {
            document.getElementById("sendChat").click();
        }
    });
});


// document.addEventListener("DOMContentLoaded", () => {
//     document.getElementById("sendChat").onclick = () => {
//         const input = document.getElementById("chatInput");
//         if (!input.value.trim()) return;
//
//         sendMessage("CHAT", input.value);
//         input.value = "";
//     };
//
//     document.getElementById("leaveBtn").onclick = () => {
//         socket.close();
//         location.href = "/omok/lobby";
//     };
//
//     document.getElementById("chatInput").addEventListener("keydown", (e) => {
//         if (e.key === "Enter") {
//             document.getElementById("sendChat").click();
//         }
//     });
// });
//
// document.addEventListener("DOMContentLoaded", () => {
//     connectWebSocket();
//
//     document.getElementById("sendChat").onclick = () => {
//         const input = document.getElementById("chatInput");
//         if (!input.value.trim()) return;
//
//         sendMessage("CHAT", input.value);
//         input.value = "";
//     };
//
//     document.getElementById("leaveBtn").onclick = () => {
//         socket.close();
//         location.href = "/omok/lobby";
//     };
//
//     document.getElementById("chatInput").addEventListener("keydown", (e) => {
//         if (e.key === "Enter") {
//             document.getElementById("sendChat").click();
//         }
//     });
// });
