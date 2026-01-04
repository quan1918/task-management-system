import { useState, useEffect } from "react";
import { getProjects, getProjectTasks, createProject, createTask, createUser,
            deleteTask, deleteUser, updateTask,  getUsers } from '../api';
import Modal from '../components/Modal';
import '../styles/DashboardPage.css';

function DashboardPage() {

    const [projects, setProjects] = useState([]);
    const [selectedProjectId, setSelectedProjectId] = useState(null);
    const [projectTaskCounts, setProjectTaskCounts] = useState({});

    const [tasks, setTasks] = useState([]);
    const [editingTask, setEditingTask] = useState(null);

    const [users, setUsers] = useState([]);

    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    const [isProjectModalOpen, setIsProjectModalOpen] = useState(false);
    const [isTaskModalOpen, setIsTaskModalOpen] = useState(false);
    const [isUserModalOpen, setIsUserModalOpen] = useState(false);

    // search & filter
    const [searchText, setSearchText] = useState('');
    const [filterStatus, setFilterStatus] = useState('ALL');

    const [projectForm, setProjectForm] = useState({name: '', description: '', ownerId: '', startDate: '', endDate: ''});
    const [taskForm, setTaskForm] = useState({title: '', description: '', projectId: '', status: 'PENDING', priority: 'MEDIUM', dueDate: '', assigneeIds: ''});
    const [userForm, setUserForm] = useState({username: '', email: '', password: '', fullName: ''});

    useEffect(() => {
        loadAllData();
    }, []);

    useEffect(() => {
        if (projects.length > 0 && selectedProjectId === null) {
            setSelectedProjectId(projects[0].id);
        }
    }, [projects]);

    useEffect(() => { 
        if (selectedProjectId) {
            loadTasks(selectedProjectId);
        } else {
            loadAllTasks();
        }
    }, [selectedProjectId]);

    const loadAllData = async () => {
        setLoading(true);
        await Promise.all([
            loadProjects(),
            loadUsers(),
            loadAllTasks()
        ]);
        setLoading(false);
    };

    const loadProjects = async () => {
        const result = await getProjects();
        if (result.success) {
            const projectsData = result.data;
            setProjects(result.data);

            const counts = {};
            for (const project of projectsData) {
                const taskResult = await getProjectTasks(project.id);
                if (taskResult.success) {
                    counts[project.id] = taskResult.data?.length || 0;
                }
            }
            setProjectTaskCounts(counts);
        }
    };

    const loadUsers = async () => {
        const result = await getUsers();
        if (result.success) {
            setUsers(result.data);
        }
    };

    const loadAllTasks = async () => {
        setTasks([]);
    };

    const loadTasks = async (projectId) => {
        setLoading(true);
        const result = await getProjectTasks(projectId);
        if (result.success) {
            const taskData = result.data || [];
            setTasks(taskData);
            setProjectTaskCounts(prev => ({
                ...prev,
                [projectId]: taskData.length
            }));
        } else {
            setError(result.error || 'Failed to load tasks');
            setTasks([]);
        }
        setLoading(false);
    };

    const handleCreateProject = async (e) => {
        e.preventDefault();
        const result = await createProject({
            ...projectForm,
            ownerId: parseInt(projectForm.ownerId)
        });
        if (result.success) {
            setIsProjectModalOpen(false);
            setProjectForm({ name: '', description: '', ownerId: '', startDate: '', endDate: '' });
            await loadProjects();
            if (result.data?.id) {
                setSelectedProjectId(result.data.id);
            }
        } else {
            alert(result.error);
        }
    };

    const handleCreateTask = async (e) => {
        e.preventDefault();
        const assigneeIdsArray = taskForm.assigneeIds
            .split(',')
            .map(id => parseInt(id.trim()))
            .filter(id => !isNaN(id));

        const taskData = {
            title: taskForm.title,
            description: taskForm.description,
            projectId: parseInt(taskForm.projectId),
            status: taskForm.status,
            priority: taskForm.priority,
            dueDate: taskForm.dueDate,
            assigneeIds: assigneeIdsArray
        };

        let result;
        if (editingTask) {
            result = await updateTask(editingTask.id, taskData);
        } else {
            result = await createTask(taskData);
        }

        if (result.success) {
            setIsTaskModalOpen(false);
            resetTaskForm();
            if (selectedProjectId) {
                await loadTasks(selectedProjectId);
            } 
        } else {
            alert(result.error);
        }
    };

    const handleCreateUser = async (e) => {
        e.preventDefault();
        const result = await createUser(userForm);
        if (result.success) {
            setIsUserModalOpen(false);
            setUserForm({ username: '', email: '', password: '', fullName: '' });
            loadUsers();
        } else {
            alert(result.error);
        }
    };

    const handleDeleteTask = async (taskId) => {
        if (!confirm('Delete this task?')) return;
        const previousTasks = [...tasks];
        setTasks(tasks.filter(t => t.id !== taskId));

        const result = await deleteTask(taskId);
        if (!result.success) {
            alert(result.error);
            setTasks(previousTasks);
        } else {
        // Update task count
            if (selectedProjectId) {
                setProjectTaskCounts(prev => ({
                    ...prev,
                    [selectedProjectId]: (prev[selectedProjectId] || 1) - 1
                }));
            }
        }
    };

    const handleDeleteUser = async (userId) => {
        if (!confirm('Delete this user?')) return;
        const result = await deleteUser(userId);
        if (result.success) {
            loadUsers();
        } else {
            alert(result.error);
        }
    };

    const handleEditTask = (task) => {
        setEditingTask(task);

        const assigneeIds = Array.isArray(task.assignees)
            ? task.assignees.filter(a => a && a.id).map(a => a.id).join(',')
            : '';

        setTaskForm({
            title: task.title || '',
            description: task.description || '',
            projectId: task.project?.id?.toString() || '',
            status: task.status || 'PENDING',
            priority: task.priority || 'MEDIUM',
            dueDate: task.dueDate ? task.dueDate.slice(0,16) : '',
            assigneeIds: assigneeIds
        });
        setIsTaskModalOpen(true);
    };

    const resetTaskForm = () => {
        setTaskForm({ 
            title: '', 
            description: '', 
            projectId: selectedProjectId?.toString() || '', 
            status: 'PENDING', 
            priority: 'MEDIUM', 
            dueDate: '', 
            assigneeIds: '' 
        });
        setEditingTask(null);
    };

    const filteredTasks = tasks.filter(task => {
        const matchSearch = searchText === '' ||
            task.title?.toLowerCase().includes(searchText.toLowerCase()) ||
            task.description?.toLowerCase().includes(searchText.toLowerCase());

        const matchStatus = filterStatus === "ALL" || task.status === filterStatus;

        return matchSearch && matchStatus;
    });

    const getProjectTaskCount = (projectId) => {
        return tasks.filter(t => t.project?.id === projectId).length;
    };

    if (loading) return <div className="loading">Loading dashboard...</div>;

    return (
        <div className="dashboard-container">
            {error && <div className="error-message">{error}</div>}

            {!loading && (
                <div className="dashboard-content">
                    {/* NEW: Main Grid Container - Projects + Tasks in Same Row */}
                    <div className="main-grid">
                        {/* LEFT: Projects Panel (1/3 width) */}
                        <section className="content-section projects-panel">
                            <div className="section-header">
                                <h2>Projects</h2>
                            </div>
                            <div className="projects-list">
                                {/* Project list */}
                                {projects.map(project => (
                                    <div
                                        key={project.id}
                                        className={`project-item ${selectedProjectId === project.id ? 'active' : ''}`}
                                        onClick={() => setSelectedProjectId(project.id)}
                                    >
                                        <div className="project-info">
                                            <span className="project-name">{project.name}</span>
                                            <p className="project-desc">{project.description}</p>
                                        </div>
                                        <span className="project-count">
                                            {projectTaskCounts[project.id] !== undefined
                                                ? `${projectTaskCounts[project.id]} task${projectTaskCounts[project.id] !== 1 ? 's' : ''}`
                                                : 'Loading...'}
                                        </span>
                                    </div>
                                ))}
                            </div>

                            <button 
                                className="btn-add"
                                onClick={() => setIsProjectModalOpen(true)}
                            >
                                + Add New Project
                            </button>
                        </section>

                        {/* RIGHT: Tasks Panel (2/3 width) */}
                        <section className="content-section tasks-panel">
                            <div className="section-header">
                                <h2>Tasks</h2>
                                <span className="section-count">{filteredTasks.length} tasks</span>
                            </div>
                            <div className="search-bar">
                                <input
                                    type="text"
                                    placeholder="Search tasks..."
                                    value={searchText}
                                    onChange={(e) => setSearchText(e.target.value)}
                                    className="search-input"
                                />
                                <select
                                    value={filterStatus}
                                    onChange={(e) => setFilterStatus(e.target.value)}
                                    className="filter-select"
                                >
                                    <option value="ALL">All Statuses</option>
                                    <option value="PENDING">Pending</option>
                                    <option value="IN_PROGRESS">In Progress</option>
                                    <option value="COMPLETED">Completed</option>
                                    <option value="BLOCKED">Blocked</option>
                                </select>
                            </div>

                            {filteredTasks.length === 0 ? (
                                <p className="empty-state">No tasks found</p>
                            ) : (
                                <div className="tasks-list">
                                    {filteredTasks.map(task => (
                                        <div key={task.id} className="task-item">
                                            <div className="task-main">
                                                <h3 className="task-title">{task.title}</h3>
                                                <p className="task-desc">{task.description}</p>
                                                <div className="task-meta">
                                                    <span className="task-project">Project: {task.project?.name || 'N/A'}</span>
                                                    {task.assignees && task.assignees.length > 0 && (
                                                        <div className="task-assignees">
                                                            <span>Assignees: </span>
                                                            {task.assignees.map(user => (
                                                                <span key={user.id} className="assignee-avatar" title={user.fullName}>
                                                                    {user.fullName?.charAt(0)?.toUpperCase()}
                                                                </span>
                                                            ))}
                                                            <span className="assignee-names">
                                                                {task.assignees.map(u => u.fullName).join(', ')}
                                                            </span>
                                                        </div>
                                                    )}
                                                </div>
                                            </div>
                                            <div className="task-actions">
                                                <select
                                                    value={task.status}
                                                    className="task-status"
                                                    onChange={async (e) => {
                                                        const newStatus = e.target.value;
                                                        const result = await updateTask(task.id, { 
                                                            ...task, 
                                                            status: newStatus,
                                                            projectId: task.project?.id,
                                                            assigneeIds: task.assignees?.map(a => a.id) || []
                                                        });
                                                        if (result.success) {
                                                            setTasks(tasks.map(t => 
                                                                t.id === task.id ? {...t, status: newStatus} : t
                                                            ));
                                                        } else {
                                                            alert(result.error);
                                                        }
                                                    }}
                                                >
                                                    <option value="PENDING">Pending</option>
                                                    <option value="IN_PROGRESS">In Progress</option>
                                                    <option value="COMPLETED">Completed</option>
                                                    <option value="BLOCKED">Blocked</option>
                                                </select>
                                                <button onClick={() => handleEditTask(task)} className="btn-edit">
                                                    Edit
                                                </button>
                                                <button onClick={() => handleDeleteTask(task.id)} className="btn-delete">
                                                    Delete
                                                </button>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            )}

                            <button 
                                className="btn-add"
                                onClick={() => setIsTaskModalOpen(true)}
                            >
                                + Add New Task
                            </button>
                        </section>
                    </div>
                    {/* END: Main Grid */}

                    {/* Team Members Section - Stays Below (Unchanged) */}
                    <section className="content-section">
                        <div className="section-header">
                            <h2>Team Members</h2>
                            <span className="section-count">{users.length} members</span>
                        </div>

                        <div className="members-grid">
                            {users.map(user => (
                                <div key={user.id} className="member-item">
                                    <div className="member-avatar">
                                        {user.fullName?.charAt(0)?.toUpperCase() || 'U'}
                                    </div>
                                    <div className="member-info">
                                        <div className="member-id-badge">
                                            ID: {user.id}
                                        </div>
                                        <h3 className="member-name">{user.fullName}</h3>
                                        <p className="member-email">{user.email}</p>
                                        <div className="member-stats">
                                            <span className="stat">
                                                Active tasks: <strong>
                                                    {tasks.filter(t => 
                                                        t.assignees?.some(a => a.id === user.id) && 
                                                        t.status !== 'COMPLETED'
                                                        ).length
                                                    }
                                                </strong>
                                            </span>
                                            <span className="stat">
                                                Completed: <strong>
                                                    {tasks.filter(t => 
                                                        t.assignees?.some(a => a.id === user.id) && 
                                                        t.status === 'COMPLETED'
                                                        ).length
                                                    }
                                                </strong>
                                            </span>
                                        </div>
                                    </div>
                                </div>
                            ))}
                        </div>

                        <button 
                            className="btn-add"
                            onClick={() => setIsUserModalOpen(true)}
                        >
                            + Add New Team Member
                        </button>
                    </section>
                </div>
            )}
            <Modal
                isOpen={isProjectModalOpen}
                onClose={() => setIsProjectModalOpen(false)}
                title="Add New Project"
            >
                <form onSubmit={handleCreateProject}>
                    <div className="form-group">
                        <label>Project *</label>
                        <select
                            value={taskForm.projectId}
                            onChange={(e) => setTaskForm({...taskForm, projectId: e.target.value})}
                            required
                            disabled={editingTask !== null}
                        >
                            <option value="">Select Project</option>
                            {projects.map(p => (
                                <option key={p.id} value={p.id}>{p.name}</option>
                            ))}
                        </select>
                    </div>
                    <div className="form-group">
                        <label>Project Name *</label>
                        <input
                            type="text"
                            value={projectForm.name}
                            onChange={(e) => setProjectForm({...projectForm, name: e.target.value})}
                            required
                        />
                    </div>
                    <div className="form-group">
                        <label>Description</label>
                        <textarea
                            value={projectForm.description}
                            onChange={(e) => setProjectForm({...projectForm, description: e.target.value})}
                        />
                    </div>
                    <div className="form-group">
                        <label>Owner ID *</label>
                        <select
                            value={projectForm.ownerId}
                            onChange={(e) => setProjectForm({...projectForm, ownerId: e.target.value})}
                            required
                        >
                            <option value="">Select owner</option>
                            {users.map(user => (
                                <option key={user.id} value={user.id}>
                                    {user.fullName}
                                </option>
                            ))}
                        </select>
                    </div>
                    <div className="form-group">
                        <label>Start Date</label>
                        <input
                            type="date"
                            value={projectForm.startDate}
                            onChange={(e) => setProjectForm({...projectForm, startDate: e.target.value})}
                        />
                    </div>
                    <div className="form-group">
                        <label>End Date</label>
                        <input
                            type="date"
                            value={projectForm.endDate}
                            onChange={(e) => setProjectForm({...projectForm, endDate: e.target.value})}
                        />
                    </div>
                    <div className="modal-actions">
                        <button type="submit" className="btn-primary">Create Project</button>
                        <button type="button" onClick={() => setIsProjectModalOpen(false)} className="btn-secondary">
                            Cancel
                        </button>
                    </div>
                </form>
            </Modal>

            <Modal
                isOpen={isTaskModalOpen}
                onClose={() => {
                    setIsTaskModalOpen(false);
                    resetTaskForm();
                }}
                title={editingTask ? "Edit Task" : "Add New Task"}
            >
                <form onSubmit={handleCreateTask}>
                    <div className="form-group">
                        <label>Title *</label>
                        <input
                            type="text"
                            value={taskForm.title}
                            onChange={(e) => setTaskForm({...taskForm, title: e.target.value})}
                            required
                        />
                    </div>
                    <div className="form-group">
                        <label>Description</label>
                        <textarea
                            value={taskForm.description}
                            onChange={(e) => setTaskForm({...taskForm, description: e.target.value})}
                        />
                    </div>
                    <div className="form-group">
                        <label>Project *</label>
                        <select
                            value={taskForm.projectId}
                            onChange={(e) => setTaskForm({...taskForm, projectId: e.target.value})}
                            required
                        >
                            <option value="">Select project</option>
                            {projects.map(project => (
                                <option key={project.id} value={project.id}>
                                    {project.name}
                                </option>
                            ))}
                        </select>
                    </div>
                    <div className="form-group">
                        <label>Status</label>
                        <select
                            value={taskForm.status}
                            onChange={(e) => setTaskForm({...taskForm, status: e.target.value})}
                        >
                            <option value="PENDING">Pending</option>
                            <option value="IN_PROGRESS">In Progress</option>
                            <option value="COMPLETED">Completed</option>
                            <option value="BLOCKED">Blocked</option>
                        </select>
                    </div>
                    <div className="form-group">
                        <label>Priority</label>
                        <select
                            value={taskForm.priority}
                            onChange={(e) => setTaskForm({...taskForm, priority: e.target.value})}
                        >
                            <option value="LOW">Low</option>
                            <option value="MEDIUM">Medium</option>
                            <option value="HIGH">High</option>
                        </select>
                    </div>
                    <div className="form-group">
                        <label>Due Date</label>
                        <input
                            type="datetime-local"
                            value={taskForm.dueDate}
                            onChange={(e) => setTaskForm({...taskForm, dueDate: e.target.value})}
                        />
                    </div>
                    <div className="form-group">
                        <label>Assignees (IDs separated by comma)</label>
                        <input
                            type="text"
                            value={taskForm.assigneeIds}
                            onChange={(e) => setTaskForm({...taskForm, assigneeIds: e.target.value})}
                            placeholder="1,2,3"
                        />
                    </div>
                    <div className="modal-actions">
                        <button type="submit" className="btn-primary">
                            {editingTask ? 'Update Task' : 'Create Task'}
                        </button>
                        <button type="button" onClick={() => {
                            setIsTaskModalOpen(false);
                            resetTaskForm();
                        }} className="btn-secondary">
                            Cancel
                        </button>
                    </div>
                </form>
            </Modal>

            <Modal
                isOpen={isUserModalOpen}
                onClose={() => setIsUserModalOpen(false)}
                title="Add New Team Member"
            >
                <form onSubmit={handleCreateUser}>
                    <div className="form-group">
                        <label>Username *</label>
                        <input
                            type="text"
                            value={userForm.username}
                            onChange={(e) => setUserForm({...userForm, username: e.target.value})}
                            required
                        />
                    </div>
                    <div className="form-group">
                        <label>Email *</label>
                        <input
                            type="email"
                            value={userForm.email}
                            onChange={(e) => setUserForm({...userForm, email: e.target.value})}
                            required
                        />
                    </div>
                    <div className="form-group">
                        <label>Password *</label>
                        <input
                            type="password"
                            value={userForm.password}
                            onChange={(e) => setUserForm({...userForm, password: e.target.value})}
                            required
                        />
                    </div>
                    <div className="form-group">
                        <label>Full Name *</label>
                        <input
                            type="text"
                            value={userForm.fullName}
                            onChange={(e) => setUserForm({...userForm, fullName: e.target.value})}
                            required
                        />
                    </div>
                    <div className="modal-actions">
                        <button type="submit" className="btn-primary">Create User</button>
                        <button type="button" onClick={() => setIsUserModalOpen(false)} className="btn-secondary">
                            Cancel
                        </button>
                    </div>
                </form>
            </Modal>
        </div>
    );
}

export default DashboardPage;