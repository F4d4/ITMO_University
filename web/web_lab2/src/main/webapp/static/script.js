document.getElementById('pointForm').addEventListener('submit', function(event) {
    event.preventDefault(); // Останавливаем стандартную отправку формы

    const selectedCheckbox = document.querySelector('input[name="x"]:checked'); // Найти выбранный чекбокс
    if (!selectedCheckbox) {
        showError('Выберите одно значение для X.');
        return;
    }
    const xValue = parseFloat(selectedCheckbox.value); // Получить значение выбранного чекбокса

    const yValue = parseFloat(document.getElementById('yValue').value.trim());
    const rValue = parseFloat(document.getElementById('rValue').value);

    // Валидация
    if (!validateForm()) {
        return; // Остановить выполнение, если валидация не пройдена
    }

    const data = {
        x: xValue,
        y: yValue,
        r: rValue
    };

    sendPostRequest(data)

});




function sendPostRequest(data) {
    superagent
        .post('/web_lab2/controller')
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





function handleCheckboxSelection(selectedCheckbox) {
    // Получаем все чекбоксы с name="x"
    const checkboxes = document.querySelectorAll('input[name="x"]');

    // Оставляем выбранным только текущий
    checkboxes.forEach((checkbox) => {
        if (checkbox !== selectedCheckbox) {
            checkbox.checked = false; // Снимаем выделение с других
        }
    });
}







function validateForm() {
    const checkboxes = document.querySelectorAll('input[name="x"]');
    const selectedCheckbox = Array.from(checkboxes).find((checkbox) => checkbox.checked);
    const yValue = document.getElementById('yValue').value.trim();

    if (!selectedCheckbox) {
        showError('Выберите одно значение для X.');
        return false;
    }

    const numberRegex = /^-?\d+(\.\d{1,15})?$/; // Разрешает числа с не более чем 15 знаками после запятой

    if (!numberRegex.test(yValue)) {
        showError('Значение Y должно быть числом и содержать не более 15 знаков после запятой.');
        return false;
    }

    const yNum = parseFloat(yValue);

    if (isNaN(yNum) || yNum < -3 || yNum > 5) {
        showError('Введите корректное значение для Y (в диапазоне от -3 до 5).');
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


