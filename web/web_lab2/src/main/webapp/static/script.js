document.getElementById('pointForm').addEventListener('submit', function(event) {
    event.preventDefault(); // Останавливаем стандартную отправку формы

    const xValue = parseFloat(document.getElementById('xValue').value);
    console.log(xValue)
    const yValue = parseFloat(document.getElementById('yValue').value.trim());
    const rValue = parseFloat(document.getElementById('rValue').value);



    // Валидация
    if (!validateForm()) {
        return; // Остановить выполнение, если валидация не пройдена
    }

    drawGraph(xValue, yValue, rValue); // Вызов функции для отрисовки графика

    const url = `http://localhost:24749/fcgi-bin/app.jar?x=${xValue}&y=${yValue}&r=${rValue}`;

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

    const scale = 40;
    const centerX = canvas.width / 2;
    const centerY = canvas.height / 2;

    ctx.fillStyle = 'rgba(0, 0, 255, 0.5)'; // Синий цвет с прозрачностью

    ctx.beginPath();
    ctx.moveTo(centerX, centerY); // Начинаем из центра
    ctx.arc(centerX, centerY, r * scale, Math.PI, Math.PI * 0.5, true); // Четверть круга (левый верхний сектор)
    ctx.lineTo(centerX, centerY); // Возвращаемся в центр
    ctx.closePath(); // Замыкаем фигуру
    ctx.fill();

// Рисуем прямоугольник
    ctx.beginPath();
    ctx.rect(centerX, centerY-scale*r/2, r*scale, scale*r/2)
    ctx.closePath();
    ctx.fill();

// Рисуем треугольник
    ctx.beginPath();
    ctx.moveTo(centerX, centerY-r*scale/2); // Начинаем из центра
    ctx.lineTo(centerX, centerY ); // Вниз (по оси Y)
    ctx.lineTo(centerX - r * scale, centerY); // Влево (по оси X)
    ctx.closePath(); // Замыкаем треугольник
    ctx.fill();


    ctx.strokeStyle = 'black';
    ctx.beginPath();
    ctx.moveTo(0, centerY); // Горизонтальная ось
    ctx.lineTo(canvas.width, centerY);
    ctx.moveTo(centerX, 0); // Вертикальная ось
    ctx.lineTo(centerX, canvas.height);
    ctx.stroke();

    ctx.fillStyle = 'black';
    ctx.font = '12px Arial';

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

    ctx.fillStyle = 'red';
    ctx.beginPath();
    ctx.arc(centerX + x * scale, centerY - y * scale, 3, 0, Math.PI * 2); // Точка
    ctx.fill();
}

function updateResultsTable(data) {
    const tableBody = document.querySelector('#resultsTable tbody');

    const newRow = document.createElement('tr');
    newRow.innerHTML = `
        <td>${data.result ? 'Попадание' : 'Промах'}</td>
        <td>${data.x}</td>
        <td>${data.y}</td>
        <td>${data.r}</td>
        <td>${data.currentTime}</td>
        <td>${data.executionTime}</td>
    `;

    tableBody.appendChild(newRow);
}



function validateForm() {
    const yValue = document.getElementById('yValue').value.trim();
    const xValue = document.getElementById('xValue').value.trim();

    const numberRegex = /^-?\d+(\.\d{1,15})?$/; // Разрешает числа с не более чем 15 знаками после запятой

    if (!numberRegex.test(yValue)) {
        showError('Значение Y должно быть числом и содержать не более 15 знаков после запятой.');
        return false;
    }



    const yNum = parseFloat(yValue);
    const xNum = parseFloat(xValue);

    if (isNaN(yNum) || yNum < -5 || yNum > 3) {
        showError('Введите корректное значение для Y (в диапазоне от -5 до 3).');
        return false;
    }

    if ((yValue.startsWith('3') && yValue.length > 1) || (yValue.startsWith('-5') && yValue.length > 2)) {
        showError('Y не должно содержать цифр после запятой для значений на границе (-5 и 3).Введите либо -5 , либо 3');
        return false;
    }

    if (isNaN(xNum) ) {
        showError('Введите корректное значение для X');
        return false;
    }


    return true;
}

function showError(message) {
    const errorDiv = document.getElementById('error-message');
    const errorText = document.getElementById('error-text');

    errorText.textContent = message;

    errorDiv.style.display = 'block';

    setTimeout(() => {
        errorDiv.style.display = 'none';
    }, 5000);
}


