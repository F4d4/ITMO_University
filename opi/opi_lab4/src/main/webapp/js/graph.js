function drawgraph(){
    const r = parseFloat(document.querySelector('input[name="pointForm:rValue"]:checked').value);
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

    // Разметка по оси Y
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

    const table = document.getElementById("pointsTable");
    const rows = table.getElementsByTagName('tr');

    for (let i = 1; i < rows.length; i++) {
        const information = rows[i].getElementsByTagName('td');
        const result = information[0].textContent.trim();
        const x = parseFloat(information[1].textContent.trim());
        const y = parseFloat(information[2].textContent.trim());
        ctx.fillStyle = 'red';
        if (result === 'Попал') {
            ctx.fillStyle = 'green'
        }
        ctx.beginPath();
        ctx.arc(centerX + x * scale, centerY - y * scale, 3, 0, Math.PI * 2); // Точка
        ctx.fill();
    }

}

function drawResultsGraph(){
    const xValue = parseFloat(document.getElementById('pointForm:hiddenX').value);
    const yValue = parseFloat(document.getElementById('pointForm:yValue').value.trim());
    const r = parseFloat(document.querySelector('input[name="pointForm:rValue"]:checked').value);
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

    // Разметка по оси Y
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

    ctx.fillStyle = 'red'

    if(xValue >= 0 && yValue >= 0 && xValue <= r && yValue <= r ){
        ctx.fillStyle = 'green'
    }

    if(xValue <= 0 && yValue <= 0 && xValue >= -r/2 && yValue >= -r / 2 && (xValue + yValue + r * 0.5) >= 0){
        ctx.fillStyle = 'green'
    }

    if(xValue >= 0 && yValue <= 0 && yValue>-r/2 && xValue<=r/2 && Math.pow(xValue,2)+Math.pow(yValue,2)<=Math.pow(r/2,2)){
        ctx.fillStyle = 'green'
    }

    ctx.beginPath();
    ctx.arc(centerX + xValue * scale, centerY - yValue * scale, 3, 0, Math.PI * 2); // Точка
    ctx.fill();


    const table = document.getElementById("pointsTable");
    const rows = table.getElementsByTagName('tr');

    for (let i = 1; i < rows.length; i++) {
        const information = rows[i].getElementsByTagName('td');
        const result = information[0].textContent.trim();
        const x = parseFloat(information[1].textContent.trim());
        const y = parseFloat(information[2].textContent.trim());
        ctx.fillStyle = 'red';
        if (result === 'Попал') {
            ctx.fillStyle = 'green'
        }
        ctx.beginPath();
        ctx.arc(centerX + x * scale, centerY - y * scale, 3, 0, Math.PI * 2); // Точка
        ctx.fill();
    }
}

