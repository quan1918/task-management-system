import { useState, useEffect, useCallback } from "react";
import { getProjects, createProject } from '../api';
import Modal from '../components/Modal';
import TeamMembers from '../components/TeamMembers';
import TasksPanel from '../components/TasksPanel';
import { wsManager } from '../services/WebSocketManager';

function DashboardPage() {

    const [projects, setProjects] = useState([]);
    const [selectedProjectId, setSelectedProjectId] = useState(null);
    const [projectTaskCounts, setProjectTaskCounts] = useState({});
    const [modalUsers, setModalUsers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [isProjectModalOpen, setIsProjectModalOpen] = useState(false);
    const [projectForm, setProjectForm] = useState({name: '', description: '', ownerId: '', startDate: '', endDate: ''});

    useEffect(() => {
        let cancelled = false;

        const load = async () => {
            const result = await getProjects();
            if (cancelled) return;                       
            if (result.success) {
                setProjects(result.data);
                const counts = {};
                result.data.forEach(project => {
                        counts[project.id] = project.taskStatistics?.total ?? 0;
                });
                setProjectTaskCounts(counts);
            }
            setLoading(false);
        };
        load();
        return () => { cancelled = true; };            
    }, []);

    useEffect(() => {
        if (projects.length > 0 && selectedProjectId === null) {
            setSelectedProjectId(projects[0].id);
        }
    }, [projects]);

    const loadProjects = async () => {
        setLoading(true);
        const result = await getProjects();
        if (result.success) {
            const projectsData = result.data;
            setProjects(projectsData);

            const counts = {};
            projectsData.forEach(project => {
                counts[project.id] = project.taskStatistics?.total ?? 0;
            });
            setProjectTaskCounts(counts);
        }
        setLoading(false);
    };

    const handleTaskCountChange = useCallback((projectId, count) => {
        setProjectTaskCounts(prev => ({ ...prev, [projectId]: count }));
    }, []);

    const handleTaskCountDelta = useCallback((projectId, delta) => {
        setProjectTaskCounts(prev => ({
            ...prev,
            [projectId]: (prev[projectId] ?? 0) + delta
        }));
    }, []);

    useEffect(() => {
        if (!projects.length || selectedProjectId === null) return;

        const entries = projects
            .filter(p => p.id !== selectedProjectId)
            .map(p => ({
                projectId: p.id,
                topic: `/topic/projects/${p.id}/tasks`
            }));

        entries.forEach(({ projectId, topic }) => {
            wsManager.subscribe(topic, (event) => {
                if (event.type === 'CREATED') handleTaskCountDelta(projectId, +1);
                else if (event.type === 'DELETED') handleTaskCountDelta(projectId, -1);
            });
        });

        return () => {
            entries.forEach(({ topic }) => wsManager.unsubscribe(topic));
        };
    }, [projects, selectedProjectId, handleTaskCountDelta]);

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

    // Shared Tailwind classes for buttons
    const inputCls = "w-full mb-2 px-3 py-2 border border-gray-300 rounded text-sm text-gray-800 bg-white transition-colors focus:outline-none focus:bor-blue-500";
    const btnPrimary = "bg-blue-500 text-white border-none px-5 py-2.5 rounded text-sm font-medium cursor-pointer transition-colors hover:bg-blue-600 active:bg-blue-700";
    const btnSecondary = "bg-white text-gray-500 border border-gray-300 px-5 py-2.5 rounded text-sm font-medium cursor-pointer transition-all hover:bg-gray-400 hover:text-gray-700";
    const btnAdd = "w-full py-2.5 bg-blue-500 text-white border-none rounded text-[0.85rem] font-medium cursor-pointer transition-colors mt-2 hover:bg-blue-600";
    const sectionCard = "bg-white border border-gray-200 rounded";
    const sectionHeader = "flex justify-between items-center pb-3 border-b border-gray-200"

    // Form group shared classes
    const FormGroup = ({ label, children }) => (
        <div className="mb-4">
            <label className="block text-sm font-medium text-gray-700 mb-2">{label}</label>
            {children}
        </div>
    );

    // Modal footer shared classes
    const ModalActions = ({ onCancel, submitLabel}) =>  (
        <div className="flex flex-col-reverse sm:flex-row sm:justify-end gap-3 px-6 py-4 border-t border-gray-200 bg-gray-50 -mx-6 -mb-6 mt-2">
            <button type="submit" className={btnPrimary}>{submitLabel}</button>
            <button type="button" onClick={onCancel} className={btnSecondary}>Cancel</button>
        </div>
    );

    if (loading) return <div className="text-center p-12 text-gray-500 text-base">Loading dashboard...</div>;

    return (
        <div className="w-full min-h-[calc(100vh-200px)] bg-gray-50 py-8">
            <div className="w-full max-w-[1400px] mx-auto px-4">
                {/* NEW: Main Grid Container - Projects + Tasks in Same Row */}
                <div className="grid grid-cols-1 lg:grid-cols-[1fr_2fr] gap-6 mb-6">
                    {/* LEFT: Projects Panel (1/3 width) */}
                    <section className={`${sectionCard} p-4`}>
                        <div className={`${sectionHeader} mb-3`}>
                            <h2 className="text-base font-semibold text-gray-800 m-0">Projects</h2>
                        </div>
                        <div className="flex flex-col gap-1.5 mb-3 h-[400px] overflow-y-auto pr-2">
                            {/* Project list */}
                            {projects.map(project => (
                                <div
                                    key={project.id}
                                    className={`flex justify-between items-center px-3 py-2.5 border rounded cursor-pointer transition-all hover:bg-gray-50
                                        ${selectedProjectId === project.id
                                            ? 'bg-blue-50 border-blue-500' : 'border-transparent'}`}
                                    onClick={() => setSelectedProjectId(project.id)}
                                >
                                    <div className="flex-1 min-w-0 mr-2">
                                        <span className="font-semibold text-gray-800 text-sm block mb-0.5">{project.name}</span>
                                        <p className="text-[0.8rem] text-gray-500 m-0 leading-snug overflow-hidden text-ellipsis whitespace-nowrap">
                                            {project.description}
                                        </p>
                                    </div>
                                    <span className="text-xs text-gray-500 font-medium whitespace-nowrap">
                                        {projectTaskCounts[project.id] !== undefined
                                            ? `${projectTaskCounts[project.id]} task${projectTaskCounts[project.id] !== 1 ? 's' : ''}`
                                            : 'Loading...'}
                                    </span>
                                </div>
                            ))}
                        </div>

                        <button 
                            className={btnAdd}
                            onClick={() => setIsProjectModalOpen(true)}
                        >
                            + Add New Project
                        </button>
                    </section>

                    {/* RIGHT: Tasks Panel (2/3 width) */}
                    <TasksPanel selectedProjectId={selectedProjectId} 
                        projects={projects}
                        onTaskCountChange={handleTaskCountChange}
                        onTaskCountDelta={handleTaskCountDelta}
                    />
                </div>
                {/* END: Main Grid */}

                {/* Team Members Section - Stays Below (Unchanged) */}
                <TeamMembers selectedProjectId={selectedProjectId} onUsersLoaded={setModalUsers} />
            </div>
            

            {/* Project Modal */}
            <Modal
                isOpen={isProjectModalOpen}
                onClose={() => setIsProjectModalOpen(false)}
                title="Add New Project"
            >
                <form onSubmit={handleCreateProject}>
                    <div className="form-group">
                        <label>Project Name *</label>
                        <input
                            type="text"
                            value={projectForm.name}
                            onChange={(e) => setProjectForm({...projectForm, name: e.target.value})}
                            required
                            className={inputCls}
                        />
                    </div>
                    <div className="form-group">
                        <label>Description</label>
                        <textarea
                            value={projectForm.description}
                            onChange={(e) => setProjectForm({...projectForm, description: e.target.value})}
                            className={`${inputCls} min-h-[80px] resize-y`}
                        />
                    </div>
                    <div className="form-group">
                        <label>Owner ID *</label>
                        <select
                            value={projectForm.ownerId}
                            onChange={(e) => setProjectForm({...projectForm, ownerId: e.target.value})}
                            required
                            className={inputCls}
                        >
                            <option value="">Select owner</option>
                            {modalUsers.map(user => (
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
                            className={inputCls}
                        />
                    </div>
                    <div className="form-group">
                        <label>End Date</label>
                        <input
                            type="date"
                            value={projectForm.endDate}
                            onChange={(e) => setProjectForm({...projectForm, endDate: e.target.value})}
                            className={inputCls}
                        />
                    </div>
                    <div className="flex gap-3">
                        <button type="submit" className={btnPrimary}>Create Project</button>
                        <button type="button" onClick={() => setIsProjectModalOpen(false)} className={btnSecondary}>
                            Cancel
                        </button>
                    </div>
                </form>
            </Modal>
        </div>
    );
}

export default DashboardPage;