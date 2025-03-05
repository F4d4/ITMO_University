const initialState = {
    isAuthenticated: false,
    token: null,
    loading: false,
    error: null,
};

const authReducer = (state = initialState, action) => {
    switch(action.type) {
        case 'LOGIN_REQUEST':
        case 'REGISTER_REQUEST':
            return {
                ...state,
                loading: true,
                error: null,
            };
        case 'LOGIN_SUCCESS':
            return {
                ...state,
                isAuthenticated: true,
                token: action.payload.token,
                loading: false,
                error: null,
            };
        case 'REGISTER_SUCCESS':
            return {
                ...state,
                loading: false,
                error: null,
            };
        case 'LOGIN_FAILURE':
        case 'REGISTER_FAILURE':
            return {
                ...state,
                loading: false,
                error: action.payload.error,
            };
        case 'LOGOUT':
            return {
                ...state,
                isAuthenticated: false,
                token: null,
                loading: false,
                error: null,
            };
        default:
            return state;
    }
};

export default authReducer;
