const canvas = document.getElementById('graphCanvas');


// Константы для преобразования координат
const scale = 40; // Масштаб: 1 единица на графике = 40 пикселей
const centerX = canvas.width / 2; // Центр графика по X
const centerY = canvas.height / 2; // Центр графика по Y

// Функция для обработки клика
canvas.addEventListener('click', (event) => {
    // Получаем координаты клика относительно canvas
    const rect = canvas.getBoundingClientRect();
    const pixelX = event.clientX - rect.left;
    const pixelY = event.clientY - rect.top;

    // Преобразуем пиксельные координаты в координаты графика
    const graphX = (pixelX - centerX) / scale;
    const graphY = (centerY - pixelY) / scale;

    // Сохраняем или отправляем координаты


    const rValue = parseFloat(document.getElementById('rValue').value);

    const data = {
        x: graphX.toFixed(3),
        y: graphY.toFixed(3),
        r: rValue
    }

    sendPointPostRequest(data);

});

function sendPointPostRequest(data) {
    superagent
        .post(`${window.location.origin}${pageContextPath}/controller`)
        .type('form')
        .send({
            x: data.x,
            y: data.y,
            r: data.r
        })
        .then(response => {
            if (!response.ok) {
                throw new Error('Ошибка HTTP: ' + response.status);
            }
            // Открываем новую вкладку и пишем туда содержимое
            document.open()
            document.write(response.text);
            document.close();
        })
        .catch(error => {
            console.error('Ошибка:', error);
            showError('Не удалось обновить результаты. Попробуйте снова.');
        });
}