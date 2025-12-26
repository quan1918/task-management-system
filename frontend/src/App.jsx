import { useState } from 'react';
import ProjectsPage from './pages/ProjectsPage';
import TasksPage from './pages/TasksPage';
import './App.css';

function App() {
  const [currentPage, setCurrentPage] = useState('projects');

  return (
    <div className="app">
      <nav className='navbar'>
        <div className='navbar-brand'>
          <h1>Task Management System</h1>
        </div>
        <div className="navbar-links">
          <button
            className={currentPage === 'projects' ? 'active' : ''}
            onClick={() => setCurrentPage('projects')}  
          >
            Projects
          </button>
          <button
            className={currentPage === 'tasks' ? 'active' : ''}
            onClick={() => setCurrentPage('tasks')}  
          >
            Tasks
          </button>
        </div>
      </nav>
      <main className="main-content">
        {currentPage === 'projects' && <ProjectsPage />}
        {currentPage === 'tasks' && <TasksPage />}
      </main>
    </div>
  );
}

export default App;