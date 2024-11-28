<%@ page contentType="text/html;charset=UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ page import="com.f4d4.server.PointResult"%>
<%@ page import="java.util.ArrayList" %>
<%@ page import="jakarta.servlet.ServletContext" %>


<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Лабораторная работа</title>
    <link rel="stylesheet" href="static/styles.css">
    <script src="https://cdn.jsdelivr.net/npm/superagent/superagent.min.js"></script>
</head>
<body>
<div class="container">
    <header>
        <h1>Вердиев Фада Ильхам оглы</h1>
        <h2>Группа: P3217</h2>
        <h2>Вариант: 51399</h2>
    </header>

    <h3>График области</h3>
    <canvas id="graphCanvas" width="400" height="400" style="border: 1px solid black;" ></canvas>


    <section class="form-section">
        <form id="pointForm" >
            <div class="form-group">
                <fieldset>
                    <legend>Изменение X</legend>
                    <c:forEach var="value" items="${['-2', '-1.5', '-1', '-0.5', '0', '0.5', '1', '1.5', '2']}" varStatus="status">
                        <label>
                            <input type="checkbox"  name="x" value="${value}" onclick="handleCheckboxSelection(this)">
                                ${value}
                        </label>
                    </c:forEach>
                </fieldset>
            </div>

            <div class="form-group">
                <label for="yValue">Изменение Y:</label>
                <input type="text" id="yValue" name="y" placeholder="(-3 ... 5)">
            </div>

            <div class="form-group">
                <label for="rValue">Изменение R:</label>
                <select id="rValue" name="r">
                    <option value="1">1</option>
                    <option value="1.5">1.5</option>
                    <option value="2">2</option>
                    <option value="2.5">2.5</option>
                    <option value="3">3</option>
                </select>
            </div>

            <button type="submit">Проверить</button>
        </form>
    </section>

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
            <c:forEach var="result" items="${applicationScope.results}">
                <tr>
                    <td>${result.res ? 'Попадание' : 'Промах'}</td>
                    <td>${result.x}</td>
                    <td>${result.y}</td>
                    <td>${result.r}</td>
                    <td>${result.dateOfRequest}</td>
                    <td>${result.executionTime}</td>
                </tr>
            </c:forEach>
            </tbody>

        </table>
    </section>
    <div id="error-message" style="display: none; background-color: #f44336; color: white; padding: 20px; position: fixed; top: 40px; right: 10px; border-radius: 50px;">
        <span id="error-text"></span>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/superagent/superagent.min.js"></script>
<script src="static/script.js"></script>
<script src="static/graph.js"></script>
<script src="static/point.js"></script>
</body>
</html>

