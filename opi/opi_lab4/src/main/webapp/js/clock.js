// clock.js

function updateClock() {
    const clockElement = document.getElementById('clock');
    if (!clockElement) return; // Проверка наличия элемента

    const now = new Date();

    // Форматирование даты и времени на русском языке
    const options = {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
    };
    const formattedTime = now.toLocaleDateString('ru-RU', options);

    clockElement.innerHTML = formattedTime;
}

setInterval(updateClock, 6000);

// Обновление часов сразу при загрузке страницы
window.onload = updateClock;
