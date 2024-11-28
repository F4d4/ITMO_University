<%@ page contentType="text/html;charset=UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ page import="com.f4d4.server.PointResult"%>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Результат</title>
    <link rel="stylesheet" href="static/styles.css">
    <script src="https://cdn.jsdelivr.net/npm/superagent/superagent.min.js"></script>
</head>
<body>
<div class="container">
    <header>
        <h1>Вердиев Фада Ильхам оглы</h1>
        <h2>Группа: P3217</h2>
        <h2>Вариант: 51399</h2>
        <h2>Результаты вашего последнего запроса</h2>
    </header>
    <div class=container>
        <form action="${pageContext.request.contextPath}">
            <button type="submit" class="submit-btn">Вернуться к проверке</button>
        </form>
    </div>
    <section id="result-section">
        <h3>Результаты</h3>
        <table id="resultsTable">
            <thead>
            <tr>
                <th>Результат</th>
                <th>X</th>
                <th>Y</th>
                <th>R</th>
                <th>Время запроса </th>
                <th>Время выполнения micsec</th>
            </tr>
            </thead>
            <tbody>
            <%Object param = request.getAttribute("new_result");%>
            <%if(param!=null){%>
                <%PointResult pointResult = (PointResult) param;%>
            <tr>
                <td><%=pointResult.getRes() ? "Попадание" : "Промах"%></td>
                <td><%=pointResult.getX()%></td>
                <td><%=pointResult.getY()%></td>
                <td><%=pointResult.getR()%></td>
                <td><%=pointResult.getDateOfRequest()%></td>
                <td><%=pointResult.getExecutionTime()%></td>
            </tr>
            <%}%>
            </tbody>
        </table>
    </section>
</div>
</body>
</html>