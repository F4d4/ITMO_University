const selectElement = document.getElementById('rValue');
drawGraph(selectElement.value);
selectElement.addEventListener('change', (event) =>{
    drawGraph(selectElement.value);
})

function drawGraph(r) {
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

    for (let i = -5; i <= 5; i++) {
        const xPos = centerX + i * scale;
        ctx.beginPath();
        ctx.moveTo(xPos, centerY - 5); // Маленькая линия вверх
        ctx.lineTo(xPos, centerY + 5); // Маленькая линия вниз
        ctx.stroke();
        if (i !== 0) { // Не подписываем 0 дважды
            ctx.fillText(i, xPos - 5, centerY + 20); // Подпись значения
        }
    }


    for (let i = -5; i <= 5; i++) {
        const yPos = centerY - i * scale;
        ctx.beginPath();
        ctx.moveTo(centerX - 5, yPos); // Маленькая линия влево
        ctx.lineTo(centerX + 5, yPos); // Маленькая линия вправо
        ctx.stroke();
        if (i !== 0) { // Не подписываем 0 дважды
            ctx.fillText(i, centerX + 10, yPos + 5); // Подпись значения
        }
    }


}