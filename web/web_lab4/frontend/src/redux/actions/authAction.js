import axios from 'axios';


axios.defaults.baseURL = 'http://localhost:8080/backend_war';
// Регистрация пользователя
export const register = (username, password) => async dispatch => {
    dispatch({ type: 'REGISTER_REQUEST' });
    try {
        const response = await axios.post('/api/users/register', {
            username,
            password
        }, {
            withCredentials: true
        });
        if (response.status === 201) {
            dispatch({ type: 'REGISTER_SUCCESS' });
        }
    } catch (error) {
        dispatch({
            type: 'REGISTER_FAILURE',
            payload: { error: error.response?.data?.message || 'Registration failed' }
        });
    }
};

// Вход пользователя
export const login = (username, password) => async dispatch => {
    dispatch({ type: 'LOGIN_REQUEST' });
    try {
        const response = await axios.post('/api/users/login', {
            username,
            password
        }, {
            withCredentials: true
        });
        if (response.status === 200) {
            const { token } = response.data;
            localStorage.setItem('token', token);
            axios.defaults.headers.common['Authorization'] = `Bearer ${token}`;
            dispatch({ type: 'LOGIN_SUCCESS', payload: { token } });
        }
    } catch (error) {
        dispatch({
            type: 'LOGIN_FAILURE',
            payload: { error: error.response?.data?.message || 'Login failed' }
        });
    }
};


