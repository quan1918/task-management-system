import { useState } from 'react';
import DashboardPage from './pages/DashboardPage';
import './App.css';

function App() {
  return (
    <div className="App">
      <header className="app-header">
        <div className="header-content">
          <h1>Task Management System</h1>
          <p className="header-subtitle">Manage your projects, tasks, and users efficiently</p>
        </div>
      </header>

      <main className="app-main">
        <DashboardPage />
      </main>
    </div>
  );
}

export default App;