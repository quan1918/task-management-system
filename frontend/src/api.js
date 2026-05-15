import axios from 'axios';
import { getAccessToken, getRefreshToken, clearTokens, saveTokens } from './auth';
import { wsManager } from './services/WebSocketManager'; 

export const API_VERSION = '/api/v1';

// Tạo Axios instance với cấu hình Basic Auth
const apiClient = axios.create({
    baseURL: `${import.meta.env.VITE_API_BASE_URL}${API_VERSION}`,
    headers: {
        'Content-Type': 'application/json',
    },
    withCredentials: true,
    timeout: 10000,
});

// Request interceptor đính kèm Jwt token vào header Authorization
apiClient.interceptors.request.use(
    (config) => {
        const token = getAccessToken();
        if (token) {
            config.headers['Authorization'] = `Bearer ${token}`;
        }
        return config;
    },
    (error) => Promise.reject(error)
);

// Response interceptor tự động refresh token khi nhận được lỗi 401 Unauthorized
let isRefreshing = false;
let failedQueue = [];

const processQueue = (error, token = null) => {
    failedQueue.forEach(({ resolve, reject}) => {
        if (error) reject(error);
        else resolve(token);
    });
    failedQueue = [];
};

apiClient.interceptors.response.use(
    (response) => response,
    async (error) => {
        const originalRequest = error.config;

        if (error.response?.status === 401 && !originalRequest._retry) {
            if (isRefreshing) {
                return new Promise((resolve, reject) => {
                    failedQueue.push({ resolve, reject });
                }).then((token) => {
                    originalRequest.headers['Authorization'] = 'Bearer ' + token;
                    return apiClient(originalRequest);
                }).catch((err) => Promise.reject(err));
            }

            originalRequest._retry = true;
            isRefreshing = true;

            const refreshTokenValue = getRefreshToken();

            if (!refreshTokenValue) {
                clearTokens();
                window.location.href = '/login';
                return Promise.reject(error);
            }

            try {
                const response = await axios.post(
                    `${import.meta.env.VITE_API_BASE_URL}${API_VERSION}/auth/refresh`,
                    { refreshToken: refreshTokenValue }
                );
                const { accessToken, refreshToken: newRefreshToken } = response.data;
                saveTokens(accessToken, newRefreshToken);
                wsManager.updateToken(accessToken); // Cập nhật token mới cho WebSocket
                apiClient.defaults.headers['Authorization'] = 'Bearer ' + accessToken;
                processQueue(null, accessToken);
                originalRequest.headers['Authorization'] = 'Bearer ' + accessToken;
                return apiClient(originalRequest);
            } catch (refreshError) {
                processQueue(refreshError, null);
                clearTokens();
                window.location.href = '/login';
                return Promise.reject(refreshError);
            } finally {
                isRefreshing = false;
            }
        }

        if (error.response) {
            console.error('API Error:', {
                status: error.response.status,
                message: error.response.data?.message || error.message,
                url: originalRequest.url,
            });
        } else if (error.request) {
            console.error('Network/CORS Error:', {
                message: 'No response from server.',
                url: error.config?.url,
            });
        }
        return Promise.reject(error);
    }
)

// ============= PROJECT APIs =============

export const getProjects = async () => {
    try {
        const response = await apiClient.get('/projects');
        return { success: true, data: response.data };
    } catch (error) {
        return { success: false, error: error.response?.data?.message || 'Failed to load projects.'};
    }
};

export const createProject = async (projectData) => {
    try {
        const response = await apiClient.post('/projects', projectData);
        return { success: true, data: response.data};
    } catch (error) {
        return { success: false, error: error.response?.data?.message || 'Failed to create project.'};
    }
};

export const getProjectTasks = async (projectId, page = 0, size = 10) => {
    try {
        const response = await apiClient.get(`/projects/${projectId}/tasks`, {
            params: { page, size, sort: 'createdAt,desc' },
        });
        return { success: true, data: response.data};
    } catch (error) {
        return { success: false, error: error.response?.data?.message || 'Failed to load tasks.'};
    }
};

// ============= TASK APIs =============

export const createTask = async (taskData) => {
    try {
        const response = await apiClient.post('/tasks', taskData);
        return { success: true, data: response.data};
    } catch (error) {
        return { success: false, error: error.response?.data?.message || 'Failed to create task.'};
    }
};

export const deleteTask = async (taskId) => {
    try {
        await apiClient.delete(`/tasks/${taskId}`);
        return {success: true};
    } catch (error) {
        return { success: false, error: error.response?.data?.message || 'Failed to delete task.'};
    }
};

export const updateTask = async (taskId, taskData) => {
    try {
        const response = await apiClient.patch(`/tasks/${taskId}`, taskData);
        return { success: true, data: response.data};
    } catch (error) {
        return { success: false, error: error.response?.data?.message || 'Failed to update task.'};
    }
};

// ============= USER APIs =============

export const getUsers = async () => {
    try {
        const response = await apiClient.get('/users');
        return { success: true, data: response.data };
    } catch (error) {
        return { success: false, error: error.response?.data?.message || 'Failed to load users.'};
    }
};

export const createUser = async (userData) => {
    try {
        const response = await apiClient.post('/users', userData);
        return { success: true, data: response.data};
    } catch (error) {
        return { success: false, error: error.response?.data?.message || 'Failed to create user.'};
    }
};

export const deleteUser = async (userId) => {
    try {
        await apiClient.delete(`/users/${userId}`);
        return {success: true};
    } catch (error) {
        return { success: false, error: error.response?.data?.message || 'Failed to delete user.'};
    }
};

// ============= AUTH APIs =============
export const login = async (credentials) => {
    try {
        const response = await apiClient.post('/auth/login', credentials);
        return { success: true, data: response.data};
    } catch (error) {
        return {
            success: false,
            error: error.response?.data?.message || 'Invalid username or password.',
        };
    }
};

export const register = async (userData) => {
    try {
        const response = await apiClient.post('/auth/register', userData);
        return { success: true, data: response.data};
    } catch (error) {
        return {
            success: false,
            error: error.response?.data?.message || 'Failed to register user.',
        };
    }
};

export const refreshToken = async (refreshToken) => {
    try {
        const response = await apiClient.post('/auth/refresh', { refreshToken });
        return { success: true, data: response.data};
    } catch (error) {
        return {
            success: false,
            error: error.response?.data?.message || 'Failed to refresh token.',
        };
    }
};

export const logout = async () => {
    try {
        await apiClient.post('/auth/logout');
        return { success: true };
    } catch (error) {
        return {
            success: false,
            error: error.response?.data?.message || 'Failed to logout.',
        };
    }
};