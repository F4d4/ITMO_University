import React, { useEffect, useRef } from 'react';
import './css/InteractiveBackground.css';

const InteractiveBackground = () => {
    const canvasRef = useRef(null);
    const stars = useRef([]);
    const animationFrameId = useRef(null);

    useEffect(() => {
        const canvas = canvasRef.current;
        const ctx = canvas.getContext('2d');
        let width = window.innerWidth;
        let height = window.innerHeight;

        const resizeCanvas = () => {
            width = window.innerWidth;
            height = window.innerHeight;
            canvas.width = width;
            canvas.height = height;
            initStars();
        };

        const initStars = () => {
            // Создаем массив звезд для эффекта звездного неба
            stars.current = Array.from({ length: 100 }, () => ({
                x: Math.random() * width,
                y: Math.random() * height,
                radius: Math.random() * 1.5 + 0.5,
                twinkleSpeed: Math.random() * 0.02 + 0.01,
                phase: Math.random() * Math.PI * 2,
                speedX: (Math.random() - 0.5) * 0.2,
                speedY: (Math.random() - 0.5) * 0.2,
            }));
        };

        const animate = () => {
            ctx.clearRect(0, 0, width, height);
            stars.current.forEach(star => {
                // Обновляем фазу мерцания
                star.phase += star.twinkleSpeed;
                // Смещаем звезду
                star.x += star.speedX;
                star.y += star.speedY;

                // Зацикливание звезд по краям экрана
                if (star.x < 0) star.x = width;
                if (star.x > width) star.x = 0;
                if (star.y < 0) star.y = height;
                if (star.y > height) star.y = 0;

                // Вычисляем прозрачность по синусоиде для эффекта мерцания
                const alpha = 0.5 + 0.5 * Math.sin(star.phase);
                ctx.beginPath();
                ctx.arc(star.x, star.y, star.radius, 0, Math.PI * 2);
                ctx.fillStyle = `rgba(255, 255, 255, ${alpha.toFixed(2)})`;
                ctx.fill();
            });
            animationFrameId.current = requestAnimationFrame(animate);
        };

        resizeCanvas();
        animate();

        window.addEventListener('resize', resizeCanvas);
        return () => {
            window.removeEventListener('resize', resizeCanvas);
            if (animationFrameId.current) cancelAnimationFrame(animationFrameId.current);
        };
    }, []);

    return <canvas ref={canvasRef} className="interactive-background" />;
};

export default InteractiveBackground;
