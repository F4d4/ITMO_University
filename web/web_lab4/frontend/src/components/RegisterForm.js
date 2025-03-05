import React, { useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { Link, useHistory } from 'react-router-dom';
import { register } from '../redux/actions/authAction';
import InteractiveBackground from './InteractiveBackground';
import './AuthForms.css';

const RegisterForm = () => {
    const dispatch = useDispatch();
    const history = useHistory();
    const auth = useSelector(state => state.auth);

    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [message, setMessage] = useState('');

    const handleSubmit = async (e) => {
        e.preventDefault();
        setMessage('');

        if (password !== confirmPassword) {
            setMessage('Пароли не совпадают.');
            return;
        }

        try {
            await dispatch(register(username, password));
            if (!auth.error) {
                setMessage('Регистрация прошла успешно.');
                history.push('/');
            } else {
                setMessage(auth.error);
            }
        } catch (error) {
            setMessage('Произошла ошибка при регистрации.');
        }
    };

    return (
        <div className="auth-container">
            <InteractiveBackground />
            <div className="auth-form-container">
                <div className="auth-form-content">
                    <h1>Регистрация</h1>
                    {message && (
                        <div className={`message ${message.includes('успешно') ? 'success' : 'error'}`}>
                            {message}
                        </div>
                    )}
                    <form onSubmit={handleSubmit} className="auth-form">
                        <div className="form-field">
                            <input
                                type="text"
                                value={username}
                                onChange={(e) => setUsername(e.target.value)}
                                placeholder="Логин"
                                required
                            />
                        </div>
                        <div className="form-field">
                            <input
                                type="password"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                placeholder="Пароль"
                                required
                            />
                        </div>
                        <div className="form-field">
                            <input
                                type="password"
                                value={confirmPassword}
                                onChange={(e) => setConfirmPassword(e.target.value)}
                                placeholder="Подтвердите пароль"
                                required
                            />
                        </div>
                        <button type="submit" className="submit-button">
                            Зарегистрироваться
                        </button>
                    </form>
                    <p className="auth-link">
                        Уже есть аккаунт? <Link to="/">Войти</Link>
                    </p>
                </div>
            </div>
        </div>
    );
};

export default RegisterForm;