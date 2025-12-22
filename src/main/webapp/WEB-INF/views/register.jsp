<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!doctype html>
<html lang="ko">
<head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <title>회원가입</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/register.css">
</head>
<body>

<%
    String error = (String) request.getAttribute("error");

    String loginId = (String) request.getAttribute("loginId");
    String nickname = (String) request.getAttribute("nickname");
    String profileImg = (String) request.getAttribute("profileImg");

    if (loginId == null) loginId = "";
    if (nickname == null) nickname = "";


    if (profileImg == null || profileImg.isBlank()) profileImg = "/static/img/profiles/p1.png";
%>

<div class="screen">
    <div class="box">

        <h2 class="title">회원가입</h2>

        <% if (error != null) { %>
        <div class="error"><%= error %></div>
        <% } %>

        <form method="post" action="${pageContext.request.contextPath}/register">

            <div class="row">
                <label for="loginId">아이디</label>
                <input id="loginId" name="loginId" value="<%= loginId %>" placeholder="예) test1" autocomplete="username"/>
            </div>

            <div class="row">
                <label for="password">비밀번호</label>
                <input id="password" type="password" name="password" placeholder="비밀번호 입력" autocomplete="new-password"/>
            </div>

            <div class="row">
                <label for="nickname">닉네임</label>
                <input id="nickname" name="nickname" value="<%= nickname %>" placeholder="예) minseok" autocomplete="nickname"/>
            </div>

            <div class="row">
                <label>프로필 선택</label>

                <div class="profiles" id="profiles">
                    <img class="profile-item"
                         src="${pageContext.request.contextPath}/static/img/profiles/p1.png"
                         data-value="/static/img/profiles/p1.png" alt="p1">
                    <img class="profile-item"
                         src="${pageContext.request.contextPath}/static/img/profiles/p2.png"
                         data-value="/static/img/profiles/p2.png" alt="p2">
                    <img class="profile-item"
                         src="${pageContext.request.contextPath}/static/img/profiles/p3.png"
                         data-value="/static/img/profiles/p3.png" alt="p3">
                    <img class="profile-item"
                         src="${pageContext.request.contextPath}/static/img/profiles/p4.png"
                         data-value="/static/img/profiles/p4.png" alt="p4">
                    <img class="profile-item"
                         src="${pageContext.request.contextPath}/static/img/profiles/p5.png"
                         data-value="/static/img/profiles/p5.png" alt="p5">
                </div>


                <input type="hidden" name="profileImg" id="profileImg" value="<%= profileImg %>">

                <div class="hint">원하는 프로필 이미지를 클릭하세요.</div>
            </div>

            <div class="actions">
                <button class="btn-primary" type="submit">가입하기</button>
                <a class="btn-link" href="${pageContext.request.contextPath}/login">로그인으로</a>
            </div>

        </form>
    </div>
</div>

<script>
    const items = document.querySelectorAll(".profile-item");
    const hidden = document.getElementById("profileImg");

    const initial = hidden.value;

    items.forEach(img => {
        if (img.dataset.value === initial) img.classList.add("selected");

        img.addEventListener("click", () => {
            items.forEach(i => i.classList.remove("selected"));
            img.classList.add("selected");
            hidden.value = img.dataset.value;
        });
    });

    // 아무것도 선택 안 되어 있으면 첫 번째 선택
    if (![...items].some(i => i.classList.contains("selected")) && items.length > 0) {
        items[0].classList.add("selected");
        hidden.value = items[0].dataset.value;
    }
</script>

</body>
</html>
