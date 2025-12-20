const BOARD_SIZE = 15;
let currentTurn = null;

const boardState = Array.from(
    { length: BOARD_SIZE },
    () => Array(BOARD_SIZE).fill(null)
);

function startGame(firstTurnColor) {
    currentTurn = firstTurnColor;
    renderBoard();
}

function placeStone(x, y) {
    if (!myColor) return;                // 관전자
    if (currentTurn !== myColor) return; // 내 턴 아님
    if (boardState[y][x]) return;        // 이미 둔 곳

    sendMessage("MOVE", { x, y });
}

function applyMove(x, y, color) {
    boardState[y][x] = color;
    drawStone(x, y, color);

    currentTurn = color === "BLACK" ? "WHITE" : "BLACK";
}
