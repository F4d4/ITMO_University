document.getElementById('pointForm').addEventListener('submit', function(event) {
    event.preventDefault(); // Останавливаем стандартную отправку формы

    // Получаем значения из формы
    const xValue = parseFloat(document.getElementById('xValue').value);
    const yValue = parseFloat(document.getElementById('yValue').value.trim());
    const rValue = parseFloat(document.getElementById('rValue').value.trim());



    // Валидация
    if (!validateForm()) {
        return; // Остановить выполнение, если валидация не пройдена
    }

    // Рисуем график сразу после успешной валидации
    drawGraph(xValue, yValue, rValue); // Вызов функции для отрисовки графика

    // Формируем URL для GET-запроса
    const url = `http://localhost:24749/fcgi-bin/app.jar?x=${xValue}&y=${yValue}&r=${rValue}`;

    // Отправляем GET-запрос через AJAX с использованием fetch API
    fetch(url)
        .then(response => {
            if (!response.ok) {
                throw new Error(`Ошибка HTTP: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            // Обрабатываем полученные данные и выводим на страницу
            updateResultsTable(data); // Добавляем данные в таблицу
        })
        .catch(error => {
            // Обрабатываем ошибки
            console.error('Ошибка при отправке GET-запроса:', error);
            showError('Произошла ошибка при отправке данных на сервер.');
        });
});


function drawGraph(x, y, r) {
    const canvas = document.getElementById('graphCanvas');
    const ctx = canvas.getContext('2d');


    ctx.clearRect(0, 0, canvas.width, canvas.height);

    //  масштабирование (40 пикселей на единицу)
    const scale = 40;
    const centerX = canvas.width / 2;
    const centerY = canvas.height / 2;

    // Рисуем область (сектор четвертой четверти и треугольник во второй)
    ctx.fillStyle = 'rgba(0, 0, 255, 0.5)'; // Синий цвет с прозрачностью

    // 1. Рисуем четверть окружности
    ctx.beginPath();
    ctx.moveTo(centerX, centerY); // Начинаем из центра
    ctx.arc(centerX, centerY, r * scale / 2, 0, Math.PI / 2); // Четверть круга с радиусом r/2
    ctx.lineTo(centerX, centerY); // Возвращаемся в центр
    ctx.fill();

    // 2. Рисуем прямоугольник
    ctx.beginPath();
    ctx.rect(centerX, centerY - r * scale, r * scale / 2, r * scale); // Прямоугольник вправо и вверх
    ctx.fill();

    // 3. Рисуем треугольник
    ctx.beginPath();
    ctx.moveTo(centerX, centerY); // Начало из центра
    ctx.lineTo(centerX - r * scale / 2, centerY); // Лево (по оси X)
    ctx.lineTo(centerX, centerY - r * scale); // Вверх (по оси Y)
    ctx.closePath(); // Замыкаем треугольник
    ctx.fill();

    // Рисуем оси координат
    ctx.strokeStyle = 'black';
    ctx.beginPath();
    ctx.moveTo(0, centerY); // Горизонтальная ось
    ctx.lineTo(canvas.width, centerY);
    ctx.moveTo(centerX, 0); // Вертикальная ось
    ctx.lineTo(centerX, canvas.height);
    ctx.stroke();

    // Добавляем разметку и подписи для осей
    ctx.fillStyle = 'black';
    ctx.font = '12px Arial';

    // Разметка по оси X
    for (let i = -r; i <= r; i++) {
        const xPos = centerX + i * scale;
        ctx.beginPath();
        ctx.moveTo(xPos, centerY - 5); // Маленькая линия вверх
        ctx.lineTo(xPos, centerY + 5); // Маленькая линия вниз
        ctx.stroke();
        if (i !== 0) { // Не подписываем 0 дважды
            ctx.fillText(i, xPos - 5, centerY + 20); // Подпись значения
        }
    }

    // Разметка по оси Y
    for (let i = -r; i <= r; i++) {
        const yPos = centerY - i * scale;
        ctx.beginPath();
        ctx.moveTo(centerX - 5, yPos); // Маленькая линия влево
        ctx.lineTo(centerX + 5, yPos); // Маленькая линия вправо
        ctx.stroke();
        if (i !== 0) { // Не подписываем 0 дважды
            ctx.fillText(i, centerX + 10, yPos + 5); // Подпись значения
        }
    }

    // Рисуем точку
    ctx.fillStyle = 'red'; // Красный цвет для точки
    ctx.beginPath();
    ctx.arc(centerX + x * scale, centerY - y * scale, 3, 0, Math.PI * 2); // Точка
    ctx.fill();
}

function updateResultsTable(data) {
    const tableBody = document.querySelector('#resultsTable tbody');

    // Создаем новую строку с результатами
    const newRow = document.createElement('tr');
    newRow.innerHTML = `
        <td>${data.result ? 'Попадание' : 'Промах'}</td>
        <td>${data.x}</td>
        <td>${data.y}</td>
        <td>${data.r}</td>
        <td>${data.currentTime}</td>
        <td>${data.executionTime}</td>
    `;

    // Добавляем строку в таблицу
    tableBody.appendChild(newRow);
}


// Функция для валидации значений формы
function validateForm() {
    const yValue = document.getElementById('yValue').value.trim();
    const rValue = document.getElementById('rValue').value.trim();

    // Регулярное выражение для проверки чисел с не более чем 15 знаками после запятой
    const numberRegex = /^-?\d+(\.\d{1,15})?$/; // Разрешает числа с не более чем 15 знаками после запятой

    if (!numberRegex.test(yValue)) {
        showError('Значение Y должно быть числом и содержать не более 15 знаков после запятой.');
        return false;
    }

    if (!numberRegex.test(rValue)) {
        showError('Значение R должно быть числом и содержать не более 15 знаков после запятой.');
        return false;
    }

    const yNum = parseFloat(yValue);
    const rNum = parseFloat(rValue);

    if (isNaN(yNum) || yNum < -5 || yNum > 3) {
        showError('Введите корректное значение для Y (в диапазоне от -5 до 3).');
        return false;
    }

    if ((yValue.startsWith('3') && yValue.length > 1) || (yValue.startsWith('-5') && yValue.length > 2)) {
        showError('Y не должно содержать цифр после запятой для значений на границе (-5 и 3).Введите либо -5 , либо 3');
        return false;
    }

    if (isNaN(rNum) || rNum < 2 || rNum > 5) {
        showError('Введите корректное значение для R (в диапазоне от 2 до 5).');
        return false;
    }

    if ((rValue.startsWith('5') && rValue.length > 1) || (rValue.startsWith('2') && rValue.length > 1)) {
        showError('R не должно содержать цифр после запятой для значений на границе (2 и 5).Введите либо 2 , либо 5');
        return false;
    }

    return true;
}

function showError(message) {
    const errorDiv = document.getElementById('error-message');
    const errorText = document.getElementById('error-text');

    // Устанавливаем текст ошибки
    errorText.textContent = message;

    // Показываем сообщение об ошибке
    errorDiv.style.display = 'block';

    // Скрываем сообщение через 3 секунды
    setTimeout(() => {
        errorDiv.style.display = 'none';
    }, 5000); // Через 3 секунды скрываем сообщение
}


