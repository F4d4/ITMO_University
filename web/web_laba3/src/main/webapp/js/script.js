document.addEventListener("DOMContentLoaded", () => {
    const button = document.getElementById('submitButton');
    if (button) {
        button.addEventListener('mouseover', checkButtonState);
    }
});


function checkButtonState() {
    const button = document.getElementById('submitButton');
    const errorMessage = document.getElementById('error-message');
    const errorText = document.getElementById('error-text');

    if (button.disabled) {
        errorText.textContent = 'Пожалуйста, введите валидные значения';
        errorMessage.style.display = 'block';

        // Скрыть сообщение через 3 секунды
        setTimeout(() => {
            errorMessage.style.display = 'none';
        }, 3000);
    }
}