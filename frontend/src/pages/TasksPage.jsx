import { useState, useEffect } from 'react';
import { getProjects, getProjectTasks, createTask, deleteTask, updateTask, getUsers} from '../api';
import Modal from '../components/Modal';
import '../styles/TasksPage.css';

function TasksPage() {
    const [projects, setProjects] = useState([]);
    const [users, setUsers] = useState([]);
    const [selectedProjectId, setSelectedProjectId] = useState('');
    const [tasks, setTasks] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [editingTask, setEditingTask] = useState(null);

    // Form state
    const [formData, setFormData] = useState({
        title: '',
        description: '',
        projectId: '',
        status: 'PENDING',
        priority: 'MEDIUM',
        dueDate: '',
        assigneeIds: ''
    });

    useEffect(() => {
        loadProjects();
        loadUsers();
    }, []);

    useEffect(() => {
        if (selectedProjectId) {
            loadTasks(selectedProjectId);
        }
    }, [selectedProjectId]);

    const loadProjects = async () => {
        const result = await getProjects();
        if (result.success) {
            setProjects(result.data);
        }
    };

    const loadUsers = async () => {
        const result = await getUsers();
        if (result.success) {
            setUsers(result.data);
        }
    };

    const loadTasks = async (projectId) => {
        setLoading(true);
        const result = await getProjectTasks(projectId);
        if (result.success) {
            setTasks(result.data);
            setError('');
        } else {
            setError(result.error);
            setTasks([]);
        }
        setLoading(false);
    };

    // Xử lý cả Create và Edit
    const handleSubmit = async (e) => {
        e.preventDefault();

        const assigneeIdsArray = formData.assigneeIds
            .split(',')
            .map(id => parseInt(id.trim()))
            .filter(id => !isNaN(id));

        const taskData = {
            title: formData.title,
            description: formData.description,
            projectId: parseInt(formData.projectId),
            status: formData.status,
            priority: formData.priority,
            dueDate: formData.dueDate,
            assigneeIds: assigneeIdsArray
        };

        let result;
        if (editingTask) {
            // gọi updateTask
            result = await updateTask(editingTask.id, taskData);
        } else {
            // gọi CreateTask
            result = await createTask(taskData);
        }

        if (result.success) {
            setIsModalOpen(false);
            resetForm();
            if (selectedProjectId) {
                loadTasks(selectedProjectId);
            }
        } else {
            alert(result.error);
        }
    };

    // reset form và edit state
    const resetForm = () => {
        setFormData({
            title: '',
            description: '',
            projectId: '',
            status: 'PENDING',
            priority: 'MEDIUM',
            dueDate: '',
            assigneedIds: ''
        });
        setEditingTask(null);
    };

    // Mở modal edit với data của task
    const handleEdit = (task) => {
        setEditingTask(task);
        setFormData({
            title: task.title,
            description: task.description,
            projectId: task.project.id.toString(),
            status: task.status,
            priority: task.priority,
            dueDate: task.dueDate ? task.dueDate.slice(0, 16) : '',
            assigneeIds: task.assignees?.map(a => a.id).join(',') || ''
        });
        setIsModalOpen(true);
    };
    
    const handleDelete = async (taskId) => {
        if (!confirm ('Are you sure you want to delete this task?')) return;

        const result = await deleteTask(taskId);
        if (result.success) {
            loadTasks(selectedProjectId);
        } else {
            alert(result.error);
        } 
    };

    const handleInputChange = (e) => {
        setFormData({
            ...formData,
            [e.target.name]: e.target.value
        });
    };

    // đóng modal và reset form
    const handleCloseModal = () => {
        setIsModalOpen(false);
        resetForm();
    };

    const getStatusBadgeClass = (status) => {
        const statusMap = {
            UNASSIGNED: 'status-unassigned',
            PENDING: 'status-pending',
            IN_PROGRESS: 'status-in-progress',
            COMPLETED: 'status-completed',
            BLOCKED: 'status-blocked',
            CANCELED: 'status-canceled'
        };
        return statusMap[status] || 'status-pending';
    };

    const getProprityBadgeClass = (priority) => {
        const priorityMao = {
            LOW: 'priority-low',
            MEDIUM: 'priority-medium',
            HIGH: 'priority-high',
            CRITICAL: 'priority-critical'
        };
        return priorityMao[priority] || 'priority-medium';
    };

    return (
        <div className='tasks-page'>
            <div className='page-header'>
                <h1>Tasks</h1>
                <button className='btn-primary' onClick={() => setIsModalOpen(true)}>
                    + Add New Task
                </button>
            </div>

            <div className="project-selector">
                <label>Select Project:</label>
                <select 
                    value={selectedProjectId}
                    onChange={(e) => setSelectedProjectId(e.target.value)}
                >
                    <option value="">-- Choose a project --</option>
                    {projects.map((project) => (
                        <option key={project.id} value={project.id}>
                            {project.name}
                        </option>
                    ))}
                </select>
            </div>
            
            {loading && <div className>Loading tasks...</div>}
            {error && <div className>Error: {error}</div>}

            {!loading && !error && selectedProjectId && (
                <div className="tasks-list">
                    {tasks.length === 0 ? (
                        <p>No tasks found for this project.</p>
                    ) : (
                        tasks.map((task) => (
                            <div key={task.id} className="task-card">
                                <div className="task-header">    
                                    <h3>{task.title}</h3>
                                    <div className="task-badges">
                                        <span className={`badge ${getStatusBadgeClass(task.status)}`}>
                                            {task.status}
                                        </span>
                                        <span className={`badge ${getProprityBadgeClass(task.priority)}`}>
                                            {task.priority}
                                        </span>
                                    </div>
                                </div>
                                <p>{task.description}</p>
                                <div className="task-meta">
                                    <span>Project: {task.project?.name}</span>
                                    {task.assignees?.length > 0 && (
                                        <span>
                                            Assignees: {task.assignees.map(a => a.fullName).join(', ')}
                                        </span>
                                    )}
                                </div>
                                <div className="task-actions">
                                    <button
                                        className="btn-edit"
                                        onClick={() => handleEdit(task)}
                                    >
                                        Edit
                                    </button>
                                    <button
                                        className="btn-danger"
                                        onClick={() => handleDelete(task.id)}
                                    >
                                        Delete
                                    </button>
                                </div>
                            </div>
                        ))
                    )}
                </div>
            )}

            {/* Modal for Create/Edit Task */}
            <Modal
                isOpen={isModalOpen}
                onCLose={handleCloseModal}
                title={editingTask ? 'Edit Task' : 'Add New Task'}
            >
                <form onSubmit={handleSubmit}>
                    <div className="form-group">
                        <label>Task Title *</label>
                        <input
                            type="text"
                            name="title"
                            value={formData.title}
                            onChange={handleInputChange}
                            required
                        />
                    </div>

                    <div className="form-group">
                        <label>Description</label>
                        <textarea
                            name="description"
                            value={formData.description}
                            onChange={handleInputChange}
                            rows="3"
                        />
                    </div>

                    <div className="form-group">
                        <label>Project *</label>
                        <select
                            name="projectId"
                            value={formData.projectId}
                            onChange={handleInputChange}
                        >
                            <option value="">Select a project</option>
                            {projects.map((project) => (
                                <option key={project.id} value={project.id}>
                                    {project.name}
                                </option>
                            ))}
                        </select>
                    </div>

                    <div className="form-row">
                        <div className='form-group'>
                            <label>Status</label>
                            <select
                                name="status"
                                value={formData.status}
                                onChange={handleInputChange}
                            >
                                <option value="UNASSIGNED">Unassigned</option>
                                <option value="PENDING">Pending</option>
                                <option value="IN_PROGRESS">In Progress</option>
                                <option value="COMPLETED">Completed</option>
                                <option value="BLOCKED">Blocked</option>
                                <option value="CANCELLED">Cancelled</option>
                            </select>
                        </div>

                        <div className="form-group">
                            <label>Priority *</label>
                            <select
                                name="priority"
                                value={formData.priority}
                                onChange={handleInputChange}
                            >
                                <option value="LOW">Low</option>
                                <option value="MEDIUM">Medium</option>
                                <option value="HIGH">High</option>
                                <option value="CRITICAL">Critical</option>
                            </select>
                        </div>
                    </div>

                    <div className="form-group">
                        <label>Due Date</label>
                        <input
                            type="datetime-local"
                            name="dueDate"
                            value={formData.dueDate}
                            onChange={handleInputChange}
                        />
                    </div>

                    <div className="form-group">
                        <label>Assignees (comma-separated user IDs)</label>
                        <input
                            type="text"
                            name="assigneeIds"
                            value={formData.assigneeIds}
                            onChange={handleInputChange}
                            placeholder="e.g., 1,2,3"
                        />
                        <small>Available users: {users.map(u => `${u.id} (${u.name})`).join(', ')}</small>
                    </div>

                    <div className="form-actions">
                        <button type="submit" className="btn-primary">
                            {editingTask ? 'Update Task' : 'Create Task'}
                        </button>
                        <button
                            type="button"
                            className="btn-secondary"
                            onClick={handleCloseModal}
                        >
                            Cancel
                        </button>
                    </div>
                </form>
            </Modal>
        </div>
    );
}

export default TasksPage;


