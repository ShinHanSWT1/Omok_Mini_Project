<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>Omok - Start</title>

    <!-- start 전용 CSS -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/start.css">
</head>
<body>

<main class="start-screen">
    <!-- 로그인 페이지로 이동 -->
    <form action="${pageContext.request.contextPath}/login" method="get">
        <button type="submit" class="start-button" aria-label="시작하기">
            <img src="${pageContext.request.contextPath}/static/img/startButton.png" alt="시작하기">
        </button>
    </form>
</main>

</body>
</html>
