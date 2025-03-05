import React from 'react';
import { BrowserRouter as Router, Route, Switch, Redirect } from 'react-router-dom';
import { useSelector } from 'react-redux';
import './App.css';
import LoginForm from './components/LoginForm';
import RegisterForm from './components/RegisterForm';
import MainPage from './components/MainPage';

function App() {
    const isAuthenticated = useSelector(state => state.auth.isAuthenticated);

    return (
        <Router>
            <div className="app-container">
                <Switch>
                    <Route exact path="/">
                        {isAuthenticated ? <Redirect to="/main" /> : <LoginForm />}
                    </Route>
                    <Route path="/register">
                        {isAuthenticated ? <Redirect to="/main" /> : <RegisterForm />}
                    </Route>
                    <Route path="/main">
                        {isAuthenticated ? <MainPage /> : <Redirect to="/" />}
                    </Route>

                </Switch>
            </div>
        </Router>
    );
}

export default App;
