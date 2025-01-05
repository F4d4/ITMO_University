function drawgraph(){
    const r = parseFloat(document.querySelector('input[name="pointForm:rValue"]:checked').value);
    console.log(r)
    const canvas = document.getElementById('graphCanvas');
    const ctx = canvas.getContext('2d');

    ctx.clearRect(0, 0, canvas.width, canvas.height);

    const scale = 40;
    const centerX = canvas.width / 2;
    const centerY = canvas.height / 2;

    ctx.fillStyle = 'rgba(0, 0, 255, 0.5)';


    //четверть кргуа
    ctx.beginPath();
    ctx.moveTo(centerX, centerY);
    ctx.arc(centerX, centerY, r * scale / 2, 0, Math.PI / 2); // Четверть круга с радиусом r/2
    ctx.lineTo(centerX, centerY);
    ctx.fill();

    //прямоугольник
    ctx.beginPath();
    ctx.rect(centerX, centerY - r * scale, r * scale , r * scale);
    ctx.fill();



    //треугольник
    ctx.beginPath();
    ctx.moveTo(centerX, centerY);
    ctx.lineTo(centerX - r * scale / 2, centerY);
    ctx.lineTo(centerX, centerY + r * scale/2);
    ctx.closePath();
    ctx.fill();


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

}