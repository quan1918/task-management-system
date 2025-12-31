import axios from 'axios';

// Tạo Axios instance với cấu hình Basic Auth
const apiClient = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
    auth: {
        username: import.meta.env.VITE_API_USERNAME,
        password: import.meta.env.VITE_API_PASSWORD,
    },
    withCredentials: true,
    timeout: 10000,
});

apiClient.interceptors.response.use(
    (response) => response, // Success - pass through
    (error) => {
        // ✅ ADD: Better error logging
        if (error.response) {
            // Backend responded with error status
            console.error('API Error:', {
                status: error.response.status,
                message: error.response.data?.message || error.message,
                url: error.config?.url,
            });
        } else if (error.request) {
            // Request sent but no response (CORS/Network issue)
            console.error('Network/CORS Error:', {
                message: 'No response from server. Check CORS config or network.',
                url: error.config?.url,
                baseURL: error.config?.baseURL,
            });
        } else {
            // Request setup error
            console.error('Request Setup Error:', error.message);
        }
        return Promise.reject(error);
    }
);
// ============= PROJECT APIs =============

export const getProjects = async () => {
    try {
        const response = await apiClient.get('/api/projects');
        return { success: true, data: response.data };
    } catch (error) {
        return { success: false, error: error.response?.data?.message || 'Failed to load projects.'};
    }
};

export const createProject = async (projectData) => {
    try {
        const response = await apiClient.post('/api/projects', projectData);
        return { success: true, data: response.data};
    } catch (error) {
        return { success: false, error: error.response?.data?.message || 'Failed to create project.'};
    }
};

export const getProjectTasks = async (projectId) => {
    try {
        const response = await apiClient.get(`/api/projects/${projectId}/tasks`);
        return { success: true, data: response.data};
    } catch (error) {
        return { success: false, error: error.response?.data?.message || 'Failed to load tasks.'};
    }
};

// ============= TASK APIs =============

export const createTask = async (taskData) => {
    try {
        const response = await apiClient.post('/api/tasks', taskData);
        return { success: true, data: response.data};
    } catch (error) {
        return { success: false, error: error.response?.data?.message || 'Failed to create task.'};
    }
};

export const deleteTask = async (taskId) => {
    try {
        await apiClient.delete(`/api/tasks/${taskId}`);
        return {success: true};
    } catch (error) {
        return { success: false, error: error.response?.data?.message || 'Failed to delete task.'};
    }
};

export const updateTask = async (taskId, taskData) => {
    try {
        const response = await apiClient.put(`/api/tasks/${taskId}`, taskData);
        return { success: true, data: response.data};
    } catch (error) {
        return { success: false, error: error.response?.data?.message || 'Failed to update task.'};
    }
};

// ============= USER APIs =============

export const getUsers = async () => {
    try {
        const response = await apiClient.get('/api/users');
        return { success: true, data: response.data };
    } catch (error) {
        return { success: false, error: error.response?.data?.message || 'Failed to load users.'};
    }
};

export const createUser = async (userData) => {
    try {
        const response = await apiClient.post('/api/users', userData);
        return { success: true, data: response.data};
    } catch (error) {
        return { success: false, error: error.response?.data?.message || 'Failed to create user.'};
    }
};

export const deleteUser = async (userId) => {
    try {
        await apiClient.delete(`/api/users/${userId}`);
        return {success: true};
    } catch (error) {
        return { success: false, error: error.response?.data?.message || 'Failed to delete user.'};
    }
};