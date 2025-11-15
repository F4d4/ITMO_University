import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import './Layout.css';

const Layout = ({ children }) => {
  const location = useLocation();

  const isActive = (path) => {
    return location.pathname === path ? 'active' : '';
  };

  return (
    <div className="layout">
      <header className="header">
        <div className="header-content">
          <h1 className="header-title">🚗 Vehicle Management System</h1>
          <p className="header-subtitle">Система управления транспортными средствами</p>
        </div>
      </header>

      <nav className="navbar">
        <div className="nav-content">
          <Link to="/vehicles" className={`nav-link ${isActive('/vehicles')}`}>
            📋 Все транспортные средства
          </Link>
          <Link to="/vehicles/create" className={`nav-link ${isActive('/vehicles/create')}`}>
            ➕ Создать Vehicle
          </Link>
          <Link to="/special-operations" className={`nav-link ${isActive('/special-operations')}`}>
            ⚙️ Специальные операции
          </Link>
        </div>
      </nav>

      <main className="main-content">
        {children}
      </main>

      <footer className="footer">
        <p>© 2025 Vehicle Management System | ITMO University</p>
      </footer>
    </div>
  );
};

export default Layout;

