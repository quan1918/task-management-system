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
});

// ============= PROJECT APIs =============

export const getProjects = async () => {
    try {
        const response = await apiClient.get('/api/projects/');
        return { success: true, data: response.data };
    } catch (error) {
        return { success: false, error: error.response?.data?.message || 'Failed to load projects.'};
    }
};

export const createProject = async (projectData) => {
    try {
        const response = await apiClient.post('/api/projects/', projectData);
        return { success: true, data: response.data};
    } catch (error) {
        return { success: false, error: error.response?.data?.message || 'Failed to create project.'};
    }
};

export const getProjectTasks = async (projectId) => {
    try {
        const response = await apiClient.get(`/api/projects/${projectId}/tasks/`);
        return { success: true, data: response.data};
    } catch (error) {
        return { success: false, error: error.response?.data?.message || 'Failed to load tasks.'};
    }
};

// ============= TASK APIs =============

export const createTask = async (taskData) => {
    try {
        const response = await apiClient.post('/api/tasks/', taskData);
        return { success: true, data: response.data};
    } catch (error) {
        return { success: false, error: error.response?.data?.message || 'Failed to create task.'};
    }
};

export const deleteTask = async (taskId) => {
    try {
        await apiClient.delete(`/api/tasks/${taskId}/`);
        return {success: true};
    } catch (error) {
        return { success: false, error: error.response?.data?.message || 'Failed to delete task.'};
    }
};

export const updateTask = async (taskId, taskData) => {
    try {
        const response = await apiClient.put(`/api/tasks/${taskId}/`, taskData);
        return { success: true, data: response.data};
    } catch (error) {
        return { success: false, error: error.response?.data?.message || 'Failed to update task.'};
    }
};

// ============= USER APIs =============

export const getUsers = async () => {
    try {
        const response = await apiClient.get('/api/users/');
        return { success: true, data: response.data };
    } catch (error) {
        return { success: false, error: error.response?.data?.message || 'Failed to load users.'};
    }
};

export const createUser = async (userData) => {
    try {
        const response = await apiClient.post('/api/users/', userData);
        return { success: true, data: response.data};
    } catch (error) {
        return { success: false, error: error.response?.data?.message || 'Failed to create user.'};
    }
};
