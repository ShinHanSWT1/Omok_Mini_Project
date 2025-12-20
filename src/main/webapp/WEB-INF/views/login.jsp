<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Omok Login</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/login.css">
</head>
<body>

<%
    String error = (String) request.getAttribute("error");
    String loginIdVal = (String) request.getAttribute("loginId");
    if (loginIdVal == null) loginIdVal = "";
%>

<div class="screen">

    <!-- 에러 메시지(원하면 위치/스타일 바꿔도 됨) -->
    <% if (error != null) { %>
    <div class="error"><%= error %></div>
    <% } %>

    <!-- 로그인 폼: 시작하기 버튼 누르면 LoginServlet(/login)로 POST -->
    <form class="login-form" method="post" action="${pageContext.request.contextPath}/login">

        <!-- 아이디 -->
        <input class="input id"
               type="text"
               name="loginId"
               value="<%= loginIdVal %>"
               placeholder="아이디"
               autocomplete="username" />

        <!-- 비밀번호 -->
        <input class="input pw"
               type="password"
               name="password"
               placeholder="비밀번호"
               autocomplete="current-password" />

        <!-- 시작하기 버튼(이미지 버튼) -->
        <button class="start-btn" type="submit" aria-label="로그인">
            <img src="${pageContext.request.contextPath}/static/img/loginButton.png" alt="로그인">
        </button>

    </form>


    <a class="join-btn" href="${pageContext.request.contextPath}/register" aria-label="회원가입">
        <img src="${pageContext.request.contextPath}/static/img/registerButton.png" alt="회원가입">
    </a>

</div>

</body>
</html>
