import React, { useRef, useEffect } from 'react';
import './css/graph.css';

function Graph({ r, points, onGraphClick }) {
    const canvasRef = useRef(null);

    const drawGraph = (ctx) => {
        const width = 300;
        const height = 300;
        const dpr = window.devicePixelRatio || 1;

        if (canvasRef.current) {
            canvasRef.current.width = width * dpr;
            canvasRef.current.height = height * dpr;
            canvasRef.current.style.width = `${width}px`;
            canvasRef.current.style.height = `${height}px`;
            ctx.scale(dpr, dpr);
        }

        ctx.clearRect(0, 0, width, height);

        const minX = -3;
        const maxX = 3;
        const minY = -3;
        const maxY = 3;
        const scaleX = width / (maxX - minX);
        const scaleY = height / (maxY - minY);

        const xToCanvas = (x) => (x - minX) * scaleX;
        const yToCanvas = (y) => (maxY - y) * scaleY;

        // Background gradient
        const gradient = ctx.createLinearGradient(0, 0, width, height);
        gradient.addColorStop(0, 'rgba(13, 17, 23, 0.95)');
        gradient.addColorStop(1, 'rgba(23, 27, 33, 0.95)');
        ctx.fillStyle = gradient;
        ctx.fillRect(0, 0, width, height);

        // Draw shapes with cosmic colors
        ctx.fillStyle = 'rgba(88, 101, 242, 0.2)';
        ctx.strokeStyle = 'rgba(88, 101, 242, 0.3)';

        ctx.beginPath();
        ctx.moveTo(xToCanvas(0), yToCanvas(0));
        ctx.arc(xToCanvas(0), yToCanvas(0), (r / 2) * scaleX, Math.PI, 0.5 * Math.PI, true);
        ctx.closePath();
        ctx.fill();
        ctx.stroke();

        // Triangle
        ctx.beginPath();
        ctx.moveTo(xToCanvas(0), yToCanvas(0));
        ctx.lineTo(xToCanvas(r / 2), yToCanvas(0));
        ctx.lineTo(xToCanvas(0), yToCanvas(r / 2));
        ctx.closePath();
        ctx.fill();
        ctx.stroke();

        // Rectangle
        ctx.fillRect(
            xToCanvas(0),
            yToCanvas(0),
            r/2 * scaleX,
            r * scaleY
        );
        ctx.strokeRect(
            xToCanvas(0),
            yToCanvas(0),
            r/2 * scaleX,
            r * scaleY
        );

        // Grid
        ctx.strokeStyle = 'rgba(88, 101, 242, 0.1)';
        ctx.lineWidth = 0.5;

        for (let i = minX; i <= maxX; i += 0.5) {
            ctx.beginPath();
            ctx.moveTo(xToCanvas(i), 0);
            ctx.lineTo(xToCanvas(i), height);
            ctx.stroke();
        }

        for (let i = minY; i <= maxY; i += 0.5) {
            ctx.beginPath();
            ctx.moveTo(0, yToCanvas(i));
            ctx.lineTo(width, yToCanvas(i));
            ctx.stroke();
        }

        // Axes
        ctx.strokeStyle = 'rgba(88, 101, 242, 0.5)';
        ctx.lineWidth = 1.5;
        ctx.beginPath();
        ctx.moveTo(xToCanvas(minX), yToCanvas(0));
        ctx.lineTo(xToCanvas(maxX), yToCanvas(0));
        ctx.moveTo(xToCanvas(0), yToCanvas(minY));
        ctx.lineTo(xToCanvas(0), yToCanvas(maxY));
        ctx.stroke();

        // Axis labels
        ctx.fillStyle = 'rgba(255, 255, 255, 0.8)';
        ctx.font = '10px "Inter", sans-serif';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'top';

        for (let i = minX; i <= maxX; i++) {
            if (i !== 0) {
                ctx.fillText(i.toString(), xToCanvas(i), yToCanvas(0) + 5);
            }
        }

        ctx.textAlign = 'right';
        ctx.textBaseline = 'middle';
        for (let i = minY; i <= maxY; i++) {
            if (i !== 0) {
                ctx.fillText(i.toString(), xToCanvas(0) - 5, yToCanvas(i));
            }
        }

        // Draw points with cosmic glow effect
        points.forEach(point => {
            const x = xToCanvas(point.x);
            const y = yToCanvas(point.y);

            // Outer glow
            ctx.shadowColor = point.hit ? 'rgba(88, 242, 88, 0.6)' : 'rgba(242, 88, 88, 0.6)';
            ctx.shadowBlur = 15;

            ctx.fillStyle = point.hit
                ? 'rgba(88, 242, 88, 0.8)' // Cosmic green
                : 'rgba(242, 88, 88, 0.8)'; // Cosmic red

            ctx.beginPath();
            ctx.arc(x, y, 4, 0, 2 * Math.PI);
            ctx.fill();

            // Inner bright core
            ctx.shadowBlur = 0;
            ctx.fillStyle = point.hit ? '#ffffff' : '#ffffff';
            ctx.beginPath();
            ctx.arc(x, y, 2, 0, 2 * Math.PI);
            ctx.fill();
        });
    };

    useEffect(() => {
        const canvas = canvasRef.current;
        const ctx = canvas.getContext('2d');
        drawGraph(ctx);
    }, [r, points]);

    const handleClick = (e) => {
        const canvas = canvasRef.current;
        const rect = canvas.getBoundingClientRect();
        const width = 300;
        const height = 300;
        const minX = -3;
        const maxX = 3;
        const minY = -3;
        const maxY = 3;

        const scaleX = width / (maxX - minX);
        const scaleY = height / (maxY - minY);

        const x = (e.clientX - rect.left) / scaleX + minX;
        const y = maxY - (e.clientY - rect.top) / scaleY;

        onGraphClick({ x, y });
    };

    return (
        <div className="graph-container">
            <canvas
                ref={canvasRef}
                onClick={handleClick}
                className="graph-canvas"
                width={300}
                height={300}
            />
        </div>
    );
}

export default Graph;