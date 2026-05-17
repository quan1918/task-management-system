import React, { useState, useEffect, useCallback } from 'react';
import { getUsers, deleteUser, createUser, getProjectTasks } from '../api';
import Modal from './Modal';

const inputCls = "w-full mb-2 px-3 py-2 border border-gray-300 rounded text-sm text-gray-800 bg-white transition-colors focus:outline-none focus:border-blue-500";
const btnPrimary = "bg-blue-500 text-white border-none px-5 py-2.5 rounded text-sm font-medium cursor-pointer transition-colors hover:bg-blue-600 active:bg-blue-700";
const btnSecondary = "bg-white text-gray-500 border border-gray-300 px-5 py-2.5 rounded text-sm font-medium cursor-pointer transition-all hover:bg-gray-400 hover:text-gray-700";
const btnAdd = "w-full py-2.5 bg-blue-500 text-white border-none rounded text-[0.85rem] font-medium cursor-pointer transition-colors mt-2 hover:bg-blue-600";

const TeamMembers = React.memo(function TeamMembers({ selectedProjectId, onUsersLoaded }) {

    const [users, setUsers] = useState([]);
    const [tasks, setTasks] = useState([]);
    const [isUserModalOpen, setIsUserModalOpen] = useState(false);
    const [userForm, setUserForm] = useState({ username: '', email: '', password: '', fullName: '' });

    const loadUsers = useCallback(async () => {
        const result = await getUsers();
        if (result.success) {
            setUsers(result.data);
            onUsersLoaded?.(result.data);
        }
    }, [onUsersLoaded]);

    const loadTasks = useCallback(async () => {
        if (!selectedProjectId) {setTasks([]); return;}
        const result = await getProjectTasks(selectedProjectId);
        if (result.success) setTasks(result.data?.content ?? result.data ?? []);
    }, [selectedProjectId]);

    useEffect(() => {
        loadUsers();
    }, [loadUsers]);

    useEffect(() => {
        loadTasks();
    }, [loadTasks]);

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

    const handleDeleteUser = async (userId) => {
        if (!confirm('Delete this user ?')) return;
        const result = await deleteUser(userId);
        if (result.success) loadUsers();
        else alert(result.error);
    };

    return (
        <section className="bg-white border border-gray-200 rounded p-6 mb-6">
            <div className="flex justify-between items-center pb-3 border-b border-gray-200 mb-4">
                <h2 className="text-[1.1rem] font-semibold text-gray-800 m-0">Team Members</h2>
                <span className="text-sm text-gray-500 font-medium">{users.length} members</span>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-4 h-[400px] overflow-y-auto pr-2 content-start">
                {users.map(user => (
                    <div key={user.id}
                        className="flex items-center gap-3 p-4 border border-gray-200 rounded transition-all hover:border-gray-300 hover:shadow-sm">
                        <div className="w-12 h-12 rounded-full bg-blue-500 text-white flex items-center justify-center text-xl font-semibold flex-shrink-0">
                            {user.fullName?.charAt(0)?.toUpperCase() || 'U'}
                        </div>
                        <div className="flex-1 min-w-0">
                            <div className="inline-block px-2.5 py-[3px] mb-2 bg-black text-white text-[11px] font-semibold rounded-full tracking-wide uppercase">
                                ID: {user.id}
                            </div>
                            <h3 className="text-[0.95rem] font-semibold text-gray-800 m-0 mb-1">{user.fullName}</h3>
                            <p className="text-[0.8rem] text-gray-500 m-0 mb-2 overflow-hidden text-ellipsis whitespace-nowrap">{user.email}</p>
                            <div className="flex flex-col gap-1">
                                <span className="text-xs text-gray-500">
                                    Active tasks: <strong className="text-blue-500 font-semibold">
                                        {tasks.filter(t => 
                                            t.assignees?.some(a => a.id === user.id) && t.status !== 'COMPLETED'
                                        ).length}
                                    </strong>
                                </span>
                                <span className="text-xs text-gray-500">
                                    Completed: <strong className="text-green-500 font-semibold">
                                        {tasks.filter(t =>
                                            t.assignees?.some(a => a.id === user.id) && t.status === 'COMPLETED'
                                        ).length}
                                    </strong>
                                </span>
                            </div>
                        </div>
                    </div>
                ))}
            </div>

            <button className={btnAdd} onClick={() => setIsUserModalOpen(true)}>
                + Add Team Member
            </button>

            <Modal isOpen={isUserModalOpen} onClose={() => setIsUserModalOpen(false)} title="Add New Team Member">
                <form onSubmit={handleCreateUser}>
                    <div className="form-group">
                        <label>Username *</label>
                        <input type="text" value={userForm.username}
                            onChange={(e) => setUserForm({ ...userForm, username: e.target.value })}
                            required
                            className={inputCls}
                        />
                    </div>
                    <div className="form-group">
                        <label>Email *</label>
                        <input type="email" value={userForm.email}
                            onChange={(e) => setUserForm({ ...userForm, email: e.target.value })}
                            required
                            className={inputCls}
                        />
                    </div>
                    <div className="form-group">
                        <label>Password *</label>
                        <input type="password" value={userForm.password}
                            onChange={(e) => setUserForm({ ...userForm, password: e.target.value })}
                            required
                            className={inputCls}
                        />
                    </div>
                    <div className="form-group">
                        <label>Full Name</label>
                        <input type="text" value={userForm.fullName}
                            onChange={(e) => setUserForm({ ...userForm, fullName: e.target.value })}
                            className={inputCls}
                        />
                    </div>
                    <div className="flex gap-3">
                        <button type="submit" className={btnPrimary}>Create User</button>
                        <button type="button" onClick={() => setIsUserModalOpen(false)} className={btnSecondary}>
                            Cancel
                        </button>
                    </div>
                </form>
            </Modal>
        </section>
    );
});

export default TeamMembers;