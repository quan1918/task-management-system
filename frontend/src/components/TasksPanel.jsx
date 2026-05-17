import React, { useState, useEffect, useCallback, use } from 'react';
import { getProjectTasks, createTask, deleteTask, updateTask } from '../api';
import Modal from './Modal';
import { useTaskWebSocket } from '../hooks/useTaskWebSocket';

const inputCls = "w-full mb-2 px-3 py-2 border border-gray-300 rounded text-sm text-gray-800 bg-white transition-colors focus:outline-none focus:border-blue-500";
const btnPrimary = "bg-blue-500 text-white border-none px-5 py-2.5 rounded text-sm font-medium cursor-pointer transition-colors hover:bg-blue-600 active:bg-blue-700";
const btnSecondary = "bg-white text-gray-500 border border-gray-300 px-5 py-2.5 rounded text-sm font-medium cursor-pointer transition-all hover:bg-gray-400 hover:text-gray-700";
const btnAdd = "w-full py-2.5 bg-blue-500 text-white border-none rounded text-[0.85rem] font-medium cursor-pointer transition-colors mt-2 hover:bg-blue-600";

const TasksPanel = React.memo(function TasksPanel({ selectedProjectId, projects, onTaskCountChange, onTaskCountDelta }) {

    const [tasks, setTasks] = useState([]);
    const [editingTask, setEditingTask] = useState(null);
    const [loading, setLoading] = useState(false);
    const [searchText, setSearchText] = useState('');
    const [filterStatus, setFilterStatus] = useState('ALL');
    const [isTaskModalOpen, setIsTaskModalOpen] = useState(false);
    const [taskForm, setTaskForm] = useState({
        title: '', description: '', projectId: '', status: 'PENDING',
        priority: 'MEDIUM', dueDate: '', assigneeIds: ''
    });
    const [currentPage, setCurrentPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [totalElements, setTotalElements] = useState(0);
    const PAGE_SIZE = 10;

    useTaskWebSocket(selectedProjectId, useCallback((event) => {
        const {type, payload} = event;

        if (type === 'CREATED') {
            setTasks(prev => [payload, ...prev]);
            onTaskCountDelta?.(selectedProjectId, +1);
        } else if (type === 'UPDATED') {
            setTasks(prev => prev.map(t => t.id === payload.id ? payload : t));
        } else if (type === 'DELETED') {
            setTasks(prev => prev.filter(t => t.id !== payload.id));
            onTaskCountDelta?.(selectedProjectId, -1);
        }
    }, [selectedProjectId, onTaskCountDelta]));

    const loadTasks = useCallback(async () => {
        if (!selectedProjectId) { setTasks([]); return; }
        setLoading(true);
        const result = await getProjectTasks(selectedProjectId, currentPage, PAGE_SIZE);
        if (result.success) {
            const paged = result.data;
            setTasks(paged.content || []);
            setTotalPages(paged.totalPages || 0);
            setTotalElements(paged.totalElements || 0);
            onTaskCountChange?.(selectedProjectId, paged.totalElements);
        } else {
            setTasks([]);
            setTotalPages(0);
        }
        setLoading(false);
    }, [selectedProjectId, onTaskCountChange, currentPage]);

    // Fetch tasks whenever selected project changes
    useEffect(() => {
        setCurrentPage(0);
    }, [selectedProjectId]);

    useEffect(() => {
    loadTasks();
    }, [loadTasks]);

    const handleCreateTask = async (e) => {
        e.preventDefault();
        const assigneeIdsArray = taskForm.assigneeIds
            .split(',').map(id => parseInt(id.trim())).filter(id => !isNaN(id));
        const taskData = {
            title: taskForm.title, description: taskForm.description,
            projectId: parseInt(taskForm.projectId), status: taskForm.status,
            priority: taskForm.priority, dueDate: taskForm.dueDate,
            assigneeIds: assigneeIdsArray
        };
        const result = editingTask
            ? await updateTask(editingTask.id, taskData)
            : await createTask(taskData);
        if (result.success) {
            setIsTaskModalOpen(false);
            resetTaskForm();
            await loadTasks();
        } else {
            alert(result.error);
        }
    };

    const handleDeleteTask = async (taskId) => {
        if (!confirm('Delete this task?')) return;
        const previousTasks = [...tasks];
        const newTasks = tasks.filter(t => t.id !== taskId);
        setTasks(newTasks);
        const result = await deleteTask(taskId);
        if (!result.success) {
            alert(result.error);
            setTasks(previousTasks);
        } else {
            onTaskCountChange?.(selectedProjectId, newTasks.length);
        }
    };

    const handleEditTask = (task) => {
        setEditingTask(task);
        const assigneeIds = Array.isArray(task.assignees)
            ? task.assignees.filter(a => a && a.id).map(a => a.id).join(',') : '';
        setTaskForm({
            title: task.title || '', description: task.description || '',
            projectId: task.project?.id?.toString() || '',
            status: task.status || 'PENDING', priority: task.priority || 'MEDIUM',
            dueDate: task.dueDate ? task.dueDate.slice(0, 16) : '', assigneeIds
        });
        setIsTaskModalOpen(true);
    };

    const resetTaskForm = () => {
        setTaskForm({
            title: '', description: '', projectId: selectedProjectId?.toString() || '',
            status: 'PENDING', priority: 'MEDIUM', dueDate: '', assigneeIds: ''
        });
        setEditingTask(null);
    };

    const filteredTasks = tasks.filter(task => {
        const matchSearch = searchText === '' ||
            task.title?.toLowerCase().includes(searchText.toLowerCase()) ||
            task.description?.toLowerCase().includes(searchText.toLowerCase());
        const matchStatus = filterStatus === 'ALL' || task.status === filterStatus;
        return matchSearch && matchStatus;
    });

    return (
        <section className="bg-white border border-gray-200 rounded p-6 flex flex-col">
            <div className="flex justify-between items-center pb-3 border-b border-gray-200 mb-4">
                <h2 className="text-[1.1rem] font-semibold text-gray-800 m-0">Tasks</h2>
                <span className="text-sm text-gray-500 font-medium">{filteredTasks.length} tasks</span>
            </div>

            {/* Search Bar */}
            <div className="flex flex-row gap-2.5 items-center p-[15px] bg-white border border-gray-200 rounded-lg mb-5">
                <input
                    type="text" placeholder="Search tasks..."
                    value={searchText} onChange={(e) => setSearchText(e.target.value)}
                    className="flex-1 px-[15px] py-[10px] border-2 border-gray-300 rounded-lg text-sm bg-white text-gray-900 min-w-[250px] transition-all placeholder:text-gray-400 focus:outline-none focus:border-blue-500"
                />
                <select value={filterStatus} onChange={(e) => setFilterStatus(e.target.value)}
                    className="w-[160px] shrink-0 px-[15px] py-[10px] border-2 border-gray-300 rounded-lg bg-white text-gray-800 cursor-pointer focus:outline-none focus:border-blue-500">
                    <option value="ALL">All Statuses</option>
                    <option value="PENDING">Pending</option>
                    <option value="IN_PROGRESS">In Progress</option>
                    <option value="COMPLETED">Completed</option>
                    <option value="BLOCKED">Blocked</option>
                </select>
            </div>

            <div className="flex-1 h-[400px] overflow-y-auto pr-2 flex flex-col">
                {loading ? (
                    <div className="flex-1 flex items-center justify-center">
                        <p className="text-gray-400 italic">Loading tasks...</p>
                    </div>
                ) : filteredTasks.length === 0 ? (
                    <div className="flex-1 flex items-center justify-center">
                        <p className="text-gray-400 italic">No tasks found</p>
                    </div>
                ) : (
                    <div className="flex flex-col gap-4 mb-4 h-[400px] overflow-y-auto pr-2">
                        {filteredTasks.map(task => (
                            <div key={task.id} className="flex flex-col md:flex-row justify-between items-start p-4 border border-gray-200 rounded transition-all hover:border-gray-300 hover:shadow-sm">
                                <div className="flex-1 md:mr-6 mb-4 md:mb-0">
                                    <h3 className="text-base font-semibold text-gray-800 m-0 mb-2">{task.title}</h3>
                                    <p className="text-sm text-gray-500 m-0 mb-3 leading-[1.4]">{task.description}</p>
                                    <div className="flex flex-col gap-2">
                                        <span className="text-[0.85rem] text-blue-500 font-medium">
                                            Project: {task.project?.name || 'N/A'}
                                        </span>

                                        {/* Due date + Priority */}
                                        <div className="flex items-center gap-3 flex-wrap">
                                            {task.dueDate && (
                                                <span className="flex items-center gap-1 text-xs text-gray-500 ">
                                                    <svg className="w-3.5 h-3.5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} 
                                                            d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
                                                    </svg>
                                                    {new Date(task.dueDate).toLocaleString('vi-VN', {
                                                        day: '2-digit', month: '2-digit', year: 'numeric'
                                                    })}
                                                </span>
                                            )}

                                            {task.priority && (
                                                <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-semibold ${
                                                    task.priority === 'HIGH'   ? 'bg-red-100 text-red-700' :
                                                    task.priority === 'MEDIUM' ? 'bg-yellow-100 text-yellow-700' :
                                                    task.priority === 'LOW'    ? 'bg-green-100 text-green-700' : ''
                                                }`}>
                                                    {task.priority.charAt(0) + task.priority.slice(1).toLowerCase()}
                                                </span>
                                            )}
                                        </div>

                                        {task.assignees && task.assignees.length > 0 && (
                                            <div className="flex items-center gap-2 text-[0.85rem] text-gray-500 flex-wrap">
                                                <span>Assignees: </span>
                                                {task.assignees.map(user => (
                                                    <span key={user.id}
                                                        className="inline-flex items-center justify-center w-7 h-7 rounded-full bg-blue-500 text-white text-xs font-semibold"
                                                        title={user.fullName}>
                                                        {user.fullName?.charAt(0)?.toUpperCase()}
                                                    </span>
                                                ))}
                                                <span className="text-[0.8rem] text-gray-600">
                                                    {task.assignees.map(u => u.fullName).join(', ')}
                                                </span>
                                            </div>
                                        )}
                                    </div>
                                </div>
                                <div className="flex flex-col gap-2 w-full md:w-[150px]">
                                    <select value={task.status}
                                        className="p-2 border border-gray-300 rounded text-[0.85rem] bg-white text-gray-800 cursor-pointer font-medium focus:outline-none focus:border-blue-500"
                                            onChange={async (e) => {
                                                const newStatus = e.target.value;
                                                const result = await updateTask(task.id, { status: newStatus });

                                            if (result.success) {
                                                setTasks(tasks.map(t => 
                                                    t.id === task.id ? { ...t, status: newStatus } : t
                                                ));
                                            } else {
                                                alert(result.error);
                                            }
                                        }}>
                                        <option value="PENDING">Pending</option>
                                        <option value="IN_PROGRESS">In Progress</option>
                                        <option value="COMPLETED">Completed</option>
                                        <option value="BLOCKED">Blocked</option>
                                    </select>
                                    <button onClick={() => handleEditTask(task)}
                                        className="px-3 py-2 rounded text-[0.85rem] font-semibold cursor-pointer bg-white text-blue-700 border border-blue-500 transition-all hover:bg-blue-50 hover:text-blue-800">
                                        Edit
                                    </button>
                                    <button onClick={() => handleDeleteTask(task.id)}
                                        className="px-3 py-2 rounded text-[0.85rem] font-semibold cursor-pointer bg-white text-red-700 border border-red-500 transition-all hover:bg-red-50 hover:text-red-800">
                                        Delete
                                    </button>
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </div>

            {totalPages > 1 && (
                <div className="flex items-center justify-between mt-4 pt-3 border-t border-gray-200">
                    <span className="text-sm text-gray-500">
                        Page {currentPage + 1} of {totalPages} &nbsp;·&nbsp; {totalElements} tasks
                    </span>
                    <div className="flex gap-2">
                        <button
                            onClick={() => setCurrentPage(p => p -1)}
                            disabled={currentPage === 0}
                            className="px-3 py-1.5 text-sm rounded border border-gray-300 bg-white text-gray-700 disabled:opacity-40 disabled:cursor-not-allowed hover:bg-gray-50"
                        >
                            ← Prev
                        </button>
                        <button
                            onClick={() => setCurrentPage(p => p + 1)}
                            disabled={currentPage === totalPages - 1}
                            className="px-3 py-1.5 text-sm rounded border border-gray-300 bg-white text-gray-700 disabled:opacity-40 disabled:cursor-not-allowed hover:bg-gray-50"
                        >
                            Next →
                        </button>
                    </div>
                </div>
            )}
            <button className={`${btnAdd} !mt-auto`} onClick={() => setIsTaskModalOpen(true)}>
                + Add New Task
            </button>

            <Modal
                isOpen={isTaskModalOpen}
                onClose={() => { setIsTaskModalOpen(false); resetTaskForm(); }}
                title={editingTask ? 'Edit Task' : 'Add New Task'}
            >
                <form onSubmit={handleCreateTask}>
                    <div className="form-group"><label>Title *</label>
                        <input type="text" value={taskForm.title} required className={inputCls}
                            onChange={(e) => setTaskForm({ ...taskForm, title: e.target.value })} />
                    </div>
                    <div className="form-group"><label>Description</label>
                        <textarea value={taskForm.description} className={`${inputCls} min-h-[80px] resize-y`}
                            onChange={(e) => setTaskForm({ ...taskForm, description: e.target.value })} />
                    </div>
                    <div className="form-group"><label>Project *</label>
                        <select value={taskForm.projectId} required className={inputCls}
                            onChange={(e) => setTaskForm({ ...taskForm, projectId: e.target.value })}>
                            <option value="">Select project</option>
                            {projects.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}
                        </select>
                    </div>
                    <div className="form-group"><label>Status</label>
                        <select value={taskForm.status} className={inputCls}
                            onChange={(e) => setTaskForm({ ...taskForm, status: e.target.value })}>
                            <option value="PENDING">Pending</option>
                            <option value="IN_PROGRESS">In Progress</option>
                            <option value="COMPLETED">Completed</option>
                            <option value="BLOCKED">Blocked</option>
                        </select>
                    </div>
                    <div className="form-group"><label>Priority</label>
                        <select value={taskForm.priority} className={inputCls}
                            onChange={(e) => setTaskForm({ ...taskForm, priority: e.target.value })}>
                            <option value="LOW">Low</option>
                            <option value="MEDIUM">Medium</option>
                            <option value="HIGH">High</option>
                        </select>
                    </div>
                    <div className="form-group"><label>Due Date</label>
                        <input type="datetime-local" value={taskForm.dueDate} className={inputCls}
                            onChange={(e) => setTaskForm({ ...taskForm, dueDate: e.target.value })} />
                    </div>
                    <div className="form-group"><label>Assignees (IDs separated by comma)</label>
                        <input type="text" value={taskForm.assigneeIds} placeholder="1,2,3" className={inputCls}
                            onChange={(e) => setTaskForm({ ...taskForm, assigneeIds: e.target.value })} />
                    </div>
                    <div className="flex gap-3">
                        <button type="submit" className={btnPrimary}>
                            {editingTask ? 'Update Task' : 'Create Task'}
                        </button>
                        <button type="button" onClick={() => { setIsTaskModalOpen(false); resetTaskForm(); }}
                            className={btnSecondary}>Cancel</button>
                    </div>
                </form>
            </Modal>
        </section>
    );
});

export default TasksPanel;