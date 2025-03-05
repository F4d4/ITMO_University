import React, { useState } from 'react';
import './css/ResultTable.css';


function ResultsTable({ results }) {
    const [currentPage, setCurrentPage] = useState(1);
    const [rowsPerPage, setRowsPerPage] = useState(10);

    const totalPages = Math.ceil(results.length / rowsPerPage);
    const indexOfLastRow = currentPage * rowsPerPage;
    const indexOfFirstRow = indexOfLastRow - rowsPerPage;
    const currentRows = results.slice(indexOfFirstRow, indexOfLastRow);

    const formatNumber = (num) => num.toFixed(2);

    return (
        <div className="results-container">
            <div className="table-wrapper">
                <table className="results-table">
                    <thead>
                    <tr>
                        <th>X</th>
                        <th>Y</th>
                        <th>R</th>
                        <th>Результат</th>
                        <th>Время</th>
                    </tr>
                    </thead>
                    <tbody>
                    {currentRows.map((result, index) => (
                        <tr key={index} className="result-row">
                            <td className="mono">{formatNumber(result.x)}</td>
                            <td className="mono">{formatNumber(result.y)}</td>
                            <td className="mono">{result.r}</td>
                            <td>
                                    <span className={`status-badge ${result.hit ? 'hit' : 'miss'}`}>
                                        {result.hit ? 'Попадание' : 'Мимо'}
                                    </span>
                            </td>
                            <td className="time">{new Date(result.time).toLocaleString()}</td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>

            <div className="pagination">
                <div className="rows-select">
                    <span>Строк на странице</span>
                    <select
                        value={rowsPerPage}
                        onChange={(e) => {
                            setRowsPerPage(Number(e.target.value));
                            setCurrentPage(1);
                        }}
                    >
                        <option value={5}>5</option>
                        <option value={10}>10</option>
                        <option value={25}>25</option>
                        <option value={50}>50</option>
                    </select>
                </div>

                <div className="page-controls">
                    <span className="page-info">
                        Страница {currentPage} из {totalPages}
                    </span>
                    <div className="buttons">
                        <button
                            className="page-button"
                            onClick={() => setCurrentPage(p => Math.max(1, p - 1))}
                            disabled={currentPage === 1}
                        >
                            ←
                        </button>
                        <button
                            className="page-button"
                            onClick={() => setCurrentPage(p => Math.min(totalPages, p + 1))}
                            disabled={currentPage === totalPages}
                        >
                            →
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
}

export default ResultsTable;