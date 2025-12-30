import { useState, useEffect } from 'react';
import { getUsers, createUser, deleteUser } from '../api';
import Modal from '../components/Modal';
import '../styles/UsersPage.css';

function UsersPage() {
    const [users, setUsers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [isModalOpen, setIsModalOpen] = useState(false);

    // Form state
    const [formData, setFormData] = useState({
        username: '',
        email: '',
        password: '',
        fullName: ''
    });

    // Load users khi component mounts
    useEffect(() => {
        loadUsers();
    }, []);

    const loadUsers = async () => {
        setLoading(true);
        const result = await getUsers();
        if (result.success) {
            setUsers(result.data);
            setError('');
        } else {
            setError(result.error);
        }
        setLoading(false);
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        // Gọi backend API
        const result = await createUser(formData);

        if (result.success) {
            setIsModalOpen(false);
            setFormData({ username: '', email: '', password: '', fullName: ''});
            loadUsers();
        } else {
            alert(result.error);
        }
    };

    const handleDelete = async (userId) => {
        if (!confirm('Are you sure you want to delete this user?'))
            return;
        const result = await deleteUser(userId);
        if (result.success) {
            loadUsers();
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

    if (loading) return <div className="loading">Loading users...</div>;
    if (error) return <div className="error">Error: {error}</div>;

    return (
        <div className="users-page">
            <div className="page-header">
                <h1>Users</h1>
                <button className="btn-primary" onClick={() => setIsModalOpen(true)}>
                    +Add User
                </button>
            </div>

            <div className="users-grid">
                {users.length === 0 ? (
                    <p>No users found.</p>
                ) : (
                    users.map((user) => (
                        <div key={user.id} className="user-card">
                            <div className="user-avatar">
                                {user.fullName?.charAt(0)?.toUpperCase() || 'U' }
                            </div>
                            <div className="user-info">
                                <h3>{user.fullName}</h3>
                                <p className="user-id">{user.id}</p>
                                <p className="username">@{user.username}</p>
                                <p className="email">{user.email}</p>
                                <span className={`status ${user.active ? 'active' : 'inactive'}`}>
                                    {user.active ? 'Active' : 'Inactive'}
                                </span>
                            </div>
                            <div className="user-actions">
                                <button
                                    className="btn-danger-small"
                                    onClick={() => handleDelete(user.id)}
                                >
                                    Delete
                                </button>
                            </div>
                        </div>
                    ))
                )}
            </div>

            <Modal
                isOpen={isModalOpen}
                onClose={() => setIsModalOpen(false)}
                title="Add New User"
            >
                <form onSubmit={handleSubmit}>
                    <div className="form-group">
                        <label>Username *</label>
                        <input
                            type="text"
                            name="username"
                            value={formData.username}
                            onChange={handleInputChange}
                            required    
                            minLength="3"
                        />
                    </div>

                    <div className="form-group">
                        <label>Email *</label>
                        <input
                            type="email"
                            name="email"
                            value={formData.email}
                            onChange={handleInputChange}
                            required
                        />
                    </div>

                    <div className="form-group">
                        <label>Password *</label>
                        <input
                            type="password"
                            name="password"
                            value={formData.password}
                            onChange={handleInputChange}
                            required
                            minLength="6"
                        />
                    </div>

                    <div className="form-group">
                        <label>Full Name</label>
                        <input
                            type="text"
                            name="fullName"
                            value={formData.fullName}
                            onChange={handleInputChange}
                        />
                    </div>

                    <div className="form-actions">
                        <button type="submit" className="btn-primary">
                            Add User
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

export default UsersPage;