import React, { useState, useEffect } from 'react';
import PointForm from './PointForm';
import Graph from './Graph';
import ResultsTable from './ResultsTable';
import InteractiveBackground from './InteractiveBackground';
import { Header } from './Header';
import './css/MainPage.css';

function MainPage() {
    const [r, setR] = useState(1);
    const [points, setPoints] = useState([]);
    const [results, setResults] = useState([]);
    const API_URL = 'http://localhost:8080/backend_war/api';

    const handleFormSubmit = async (data) => {
        try {
            const response = await fetch(`${API_URL}/points`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': 'Bearer ' + localStorage.getItem('token'),
                },
                body: JSON.stringify(data),
            });

            const text = await response.text();
            if (!response.ok) throw new Error(text);

            const result = JSON.parse(text);
            setPoints(prev => [result, ...prev]);
            setResults(prev => [result, ...prev]);
        } catch (error) {
            console.error('Error:', error);
        }
    };

    const handleGraphClick = ({ x, y }) => {
        handleFormSubmit({ x, y, r });
    };

    const handleRChange = value => {
        setR(parseFloat(value));
    };

    useEffect(() => {
        const fetchPoints = async () => {
            try {
                const response = await fetch(`${API_URL}/points`, {
                    headers: {
                        'Authorization': 'Bearer ' + localStorage.getItem('token'),
                    },
                });

                if (!response.ok) {
                    if (response.status === 401) {
                        localStorage.removeItem('token');
                        window.location.href = '/';
                    }
                    throw new Error(await response.text());
                }

                const data = await response.json();
                setPoints(data);
                setResults(data);
            } catch (error) {
                console.error('Error:', error);
            }
        };

        fetchPoints();
    }, []);

    return (
        <div className="main-page">
            <InteractiveBackground />
            <div className="main-content">
                <Header />
                <div className="content-grid">
                    <div className="form-section">
                        <PointForm onSubmit={handleFormSubmit} onRChange={handleRChange} />
                    </div>
                    <div className="graph-section">
                        <Graph r={r} points={points} onGraphClick={handleGraphClick} />
                    </div>
                    <div className="results-section">
                        <ResultsTable results={results} />
                    </div>
                </div>
            </div>
        </div>
    );
}

export default MainPage;