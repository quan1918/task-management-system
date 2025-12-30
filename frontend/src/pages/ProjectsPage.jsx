import { useState, useEffect } from 'react';
import { getProjects, createProject, getUsers } from '../api';
import Modal from '../components/Modal';
import '../styles/ProjectsPage.css';

function ProjectsPage() {
    const [projects, setProjects] = useState([]);
    const [users, setUsers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [isModalOpen, setIsModalOpen] = useState(false);

    // Form state
    const [formData, setFormData] = useState({
        name: '',
        description: '',
        ownerId: '',
        startDate: '',
        endDate: ''
    });

    // Load projects khi component mounts
    useEffect(() => {
        loadProjects();
        loadUsers();
    }, []);

    const loadProjects = async () => {
        setLoading(true);
        const result = await getProjects();
        if (result.success) {
            setProjects(result.data);
            setError('');
        } else {
            setError(result.error);
        }
        setLoading(false);
    };

    const loadUsers = async () => {
        const result = await getUsers();
        if (result.success) {
            setUsers(result.data);
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        // Gọi backend API để tạo project mới 
        const result = await createProject({
            ...formData,
            ownerId: parseInt(formData.ownerId)
        });

        if (result.success) {
            setIsModalOpen(false);
            setFormData({ name: '', description: '', ownerId: '', startDate: '', endDate: ''});
            loadProjects();
        } else {
            alert(result.error);
        }
    };

    const handleInputChange = (e )=> {
        setFormData({
            ...formData,
            [e.target.name]: e.target.value
        });
    };

    if (loading) return <div className ="loading">Loading projects...</div>;
    if (error) return <div className ="error">Error: {error}</div>;

    return (
        <div className="projects-page">
            <div className="page-header">
                <h1>Projects</h1>
                <button className="btn-primary" onClick={() => setIsModalOpen(true)}>
                    + Add New Project
                </button>
            </div>

            <div className="projects-list">
                {projects.length === 0 ? (
                    <p>No projects found. Create your first project!</p>
                ) : (
                    projects.map((project) => (
                        <div key={project.id} className="project-card">
                            <h3>{project.name}</h3>
                            <p>{project.description}</p>
                            <div className="project-meta">
                                <span>Owner: {project.owner?.fullName || 'N/A'}</span>
                                <span className={`status ${project.active ? 'active' : 'inactive'}`}>
                                    {project.active ? 'Active' : 'Archived'}
                                </span>
                            </div>
                        </div>
                    ))
                )}
            </div>

            {/* Modal thêm project */}
            <Modal 
                isOpen={isModalOpen}
                onClose={() => setIsModalOpen(false)}
                title="Add New Project"
            >
                <form onSubmit={handleSubmit}>
                    <div className="form-group">
                        <label>Project Name *</label>
                        <input
                            type="text"
                            name="name"
                            value={formData.name}
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
                        <label>Owner *</label>
                        <select
                            name="ownerId"
                            value={formData.ownerId}
                            onChange={handleInputChange}
                            required
                        >
                            <option value="">Select owner</option>
                            {users.map((user) => (
                                <option key={user.id} value={user.id}>
                                    {user.fullName} ({user.username})
                                </option>
                            ))}
                        </select>
                    </div>

                    <div className="form-row">
                        <div className="form-group">
                            <label>Start date</label>
                            <input
                                type="date"
                                name="startDate"
                                value={formData.startDate}
                                onChange={handleInputChange}
                            />
                        </div>

                        <div className="form-group">
                            <label>End date</label>
                            <input
                                type="date"
                                name="endDate"
                                value={formData.endDate}
                                onChange={handleInputChange}
                            />
                        </div>
                    </div>

                    <div className="form-actions">
                        <button type="submit" className="btn-primary">
                            Add Project
                        </button>
                        <button 
                            type="button"
                            className="btn-secondary"
                            onClick={() => setIsModalOpen(false)}
                        >
                            Cancel
                        </button>
                    </div>
                </form>
            </Modal>
        </div>
    );
}

export default ProjectsPage;