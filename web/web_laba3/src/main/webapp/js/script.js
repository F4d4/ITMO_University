function validateForm(){
    const xValue = document.getElementById('pointForm:hiddenX').value;
    const yValue = document.getElementById('pointForm:yValue').value.trim();
    const rValue = document.getElementById('pointForm:hiddenR').value;

    const numberRegex = /^-?\d+(\.\d{1,15})?$/;
    if (!numberRegex.test(yValue)) {
        showError('Значение Y должно быть числом и содержать не более 15 знаков после запятой.');
        return false;
    }

    if (!numberRegex.test(xValue)) {
        showError('Пожалуйста , выберете x');
        return false;
    }

    if (!numberRegex.test(rValue)) {
        showError('Пожалуйста , выберете r');
        return false;
    }

    const yNum = parseFloat(yValue);

    if(isNaN(yNum) || yNum<-3 || yNum > 3){
        showError('Введите корректное значение для Y (в диапазоне от -3 до 3).')
        return false;
    }
    return true;

}


function showError(massage) {
    const errorDiv = document.getElementById('error-message');
    const errorText = document.getElementById('error-text');

    // Устанавливаем текст ошибки
    errorText.textContent = massage ;

    // Показываем сообщение об ошибке
    errorDiv.style.display = 'block';

    // Скрываем сообщение через 3 секунды
    setTimeout(() => {
        errorDiv.style.display = 'none';
    }, 5000); // Через 3 секунды скрываем сообщение
}
