const canvas = document.getElementById('graphCanvas');



const scale = 40;
const centerX = canvas.width / 2;
const centerY = canvas.height / 2;


canvas.addEventListener('click', (event) => {
    try {
        const r = document.querySelector('input[name="pointForm:rValue"]:checked').value;
        const rect = canvas.getBoundingClientRect();
        const pixelX = event.clientX - rect.left;
        const pixelY = event.clientY - rect.top;

        // Преобразуем пиксельные координаты в координаты графика
        let graphX = (pixelX - centerX) / scale;
        let graphY = (centerY - pixelY) / scale;

        graphX = Math.round(graphX * 100) / 100;
        graphY = Math.round(graphY * 100) / 100;

        document.getElementById('pointForm:hiddenX').value = graphX;
        document.getElementById('pointForm:yValue').value = graphY;
        document.getElementById('pointForm:hiddenY').value = graphY;
        document.getElementById('pointForm:hiddenSubmitButton').click();
    }catch (error){
        rIsNan('Пожалуйста, выберите R.')
    }

});

function rIsNan(massage) {
    const errorDiv = document.getElementById('error-message');
    const errorText = document.getElementById('error-text');

    errorText.textContent = massage ;

    errorDiv.style.display = 'block';

    setTimeout(() => {
        errorDiv.style.display = 'none';
    }, 3000);
}
