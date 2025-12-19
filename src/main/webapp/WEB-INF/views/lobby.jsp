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
      height: 100%; /* 높이도 100% 줘야 세로도 꽉 찹니다 */
      overflow: hidden; /* 스크롤바 없애기 (게임 느낌) */
      background-image: url("/image/LobbyBackground.jpg");
    }

    /* 전체 틀 (좌표의 기준점) */
    .wrap {
      width: 100%;
      height: 100%;
      border-radius: 15px;
      position: relative; /* ★중요: 자식 요소들이 이 박스를 기준으로 위치를 잡음 */
      padding: 20px; /* 끝에서 조금 띄우기 */
    }

    /* [공통 박스 스타일] */
    .panel {
      height: 90%; /* 화면 높이의 90%만 사용 (여백 고려) */
      border: 3px solid black;
      border-radius: 15px;
      background-color: #eee;
      padding: 20px; /* 패널 내부 글씨 여백 */
    }
    .left-panel {
      width: 48%;      /* 전체의 절반 */
      float: left;
      height: 85%;
      margin-top: 40px;

    }
    .right-panel {
      width: 48%;
      float: right;
      margin-top: 40px;
      height: 85%;

      /* 내부 요소 정렬을 위한 설정 */
      display: flex;
      flex-direction: column; /* 위아래로 쌓기 */
      justify-content: space-between; /* 끝과 끝으로 벌리기 */
    }
    .room-list-container {
      flex-grow: 1;      /* 높이 꽉 채우기 */
      overflow-y: auto;  /* 스크롤 생기게 */
      margin-bottom: 10px;
      border: 2px inset #ddd; /* 살짝 들어간 느낌 */
      background-color: #fff;
      border-radius: 10px;
      padding: 10px;
    }
    .room-item {
      background-color: #e3f2fd; /* 연한 파랑 */
      border: 2px solid #2196f3;
      border-radius: 8px;
      padding: 10px;
      margin-bottom: 8px;
      display: flex;
      justify-content: space-between; /* 좌우 끝으로 배치 */
      align-items: center;
      transition: 0.2s;
    }
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

    /* 2. 하단 컨트롤 영역 (버튼들) */
    .control-area {
      height: auto; /* 내용물만큼 */
      background-color: #ddd;
      border-radius: 10px;
      padding: 10px;
      border: 2px solid #999;

      display: flex;
      flex-direction: column; /* 버튼들을 위아래로 배치 (취향따라 row로 변경 가능) */
      gap: 5px; /* 버튼 사이 간격 */
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
    .btn-create { background-color: #ff9800; } /* 주황색 */
    .btn-quick { background-color: #4caf50; }  /* 초록색 */

    /* 방 번호 입력 폼 */
    .input-group {
      display: flex;
      margin-top: 5px;
    }
    .input-code {
      flex-grow: 1;
      padding: 8px;
      border: 2px solid black;
      border-radius: 5px 0 0 5px; /* 왼쪽만 둥글게 */
    }
    .btn-code {
      padding: 8px 15px;
      background-color: #607d8b;
      color: white;
      font-weight: bold;
      border: 2px solid black;
      border-left: none; /* 겹치는 테두리 제거 */
      border-radius: 0 5px 5px 0; /* 오른쪽만 둥글게 */
      cursor: pointer;
    }
    .user-profile {
      position: absolute; /* 48% 박스들과 상관없이 내 맘대로 위치 선정 */
      top: 10px;    /* 위에서 20px */
      right: 20px;  /* 오른쪽에서 20px */

      width: 120px;
      height: 40px;
      background-color: #333; /* 임시 색상 (나중에 사진 넣기) */
      border-radius: 10px;
      border: 2px solid white;
      cursor: pointer;
      z-index: 100; /* 다른 박스들보다 무조건 위에 뜨게 함 */
      text-align: center;
      line-height: 40px;
      color: white;
      font-weight: bold;
    }
    /* [5] 프로필 클릭 시 나올 메뉴 (숨김 상태) */
    .profile-menu {
      display: none; /* 평소엔 안 보임 */
      position: absolute;
      top: 90px;   /* 프로필 바로 아래 */
      right: 20px; /* 오른쪽 라인 맞춤 */

      width: 200px;
      background-color: white;
      border: 2px solid black;
      border-radius: 5px;
      padding: 5px;
      z-index: 101; /* 프로필보다 더 위에 */
      box-shadow: 0 4px 8px rgba(0,0,0,0.2);
    }
    .rank-list-container {
      width: 100%;
      height: 100%;
      overflow-y: auto; /* 내용이 많으면 스크롤 */
    }
    .rank-item {
      /* [수정] 양옆 여유 공간 확보를 위해 너비를 줄임 */
      width: 96%;
      margin: 0 auto 10px auto;

      background-color: white;
      border: 2px solid #555;
      border-radius: 10px;
      padding: 10px;

      /* 기존 margin-bottom: 10px; 은 위 margin 속성에 합쳐짐 */
      display: flex;
      align-items: center;
      box-shadow: 2px 2px 5px rgba(0,0,0,0.1);
      transition: transform 0.2s;
    }
    .rank-item:hover {
      transform: scale(1.02); /* 마우스 올리면 살짝 커짐 */
      background-color: #fff9c4; /* 연한 노란색 하이라이트 */
    }
    .rank-badge {
      width: 30px;
      height: 30px;
      border-radius: 50%;
      background-color: #ddd; /* 기본 회색 */
      color: black;
      text-align: center;
      line-height: 30px; /* 글자 수직 중앙 */
      font-weight: bold;
      margin-right: 15px;
      border: 1px solid #999;
    }

    /*1,2,3등은 금은동 배지를 추가 */
    .rank-item:nth-child(1) .rank-badge { background-color: #ffd700; border-color: #d4af37; }
    .rank-item:nth-child(2) .rank-badge { background-color: #c0c0c0; border-color: #a0a0a0; }
    .rank-item:nth-child(3) .rank-badge { background-color: #cd7f32; border-color: #8b4513; }

    /* 프로필 이미지 (원형) */
    .rank-profile-img {
      width: 40px;
      height: 40px;
      border-radius: 50%;
      border: 1px solid black;
      background-color: #ccc; /* 이미지 없을 때 회색 */
      margin-right: 15px;
      /* 실제 이미지가 있으면 cover로 채움 */
      object-fit: cover;
    }
    /* 닉네임과 점수 */
    .rank-info {
      flex-grow: 1; /* 남은 공간 차지 */
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
    function toggleMenu() {
      var menu = document.getElementById("myMenu");
      // 현재 화면에 보이면(block) -> 숨기기(none)
      // 안 보이면(none 또는 빈값) -> 보이기(block)
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

  <div id="myMenu" class="profile-menu">
    <strong>닉네임: 징징이</strong><br>
    승률: 50%<br>
    점수: 1200점<br>
    <hr>
    로그아웃
  </div>

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
          게임의 첫 승리자가 되어보세요!
        </div>
      </c:if>

    </div>
  </div>

  <div class="right-panel panel">
    <h3 style="text-align: center; margin-top: 0; border-bottom: 2px dashed #999; padding-bottom: 10px;">
      게임 방 목록
    </h3>

    <div class="room-list-container">
      <c:forEach var="room" items="${rooms}">
        <div class="room-item">
          <div>
            <div class="room-title">Room No. ${room.roomId}</div>
            <div class="room-info">상태: 대기중</div>
          </div>
          <button class="btn-join" onclick="location.href='/omok/lobby/enter?roomId=${room.roomId}'">
            입장
          </button>
        </div>
      </c:forEach>

      <c:if test="${empty rooms}">
        <div style="text-align: center; padding: 50px 0; color: gray;">
          현재 대기 중인 방이 없습니다.<br>
          새로운 방을 만들어보세요!
        </div>
      </c:if>
    </div>

    <div class="control-area">
      <div style="display: flex; gap: 5px;">
        <form action="/omok/lobby/create" method="post" style="width: 50%;">
          <button type="submit" class="btn-big btn-create">방 만들기</button>
        </form>
        <button class="btn-big btn-quick" style="width: 50%;" onclick="location.href='/omok/lobby/quick-enter'">
          빠른 참여
        </button>
      </div>

      <form action="/omok/lobby/enter" method="get" class="input-group">
        <input type="text" name="roomId" class="input-code" placeholder="방 번호 입력 ">
        <button type="submit" class="btn-code">입장</button>
      </form>
    </div>
  </div>

</div>


</body>

</html>