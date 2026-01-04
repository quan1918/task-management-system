# Task Management System - Frontend

> Modern React 19 frontend application for task management system with project tracking and team collaboration.

---

## 📋 Table of Contents

1. [Overview](#overview)
2. [Tech Stack](#tech-stack)
3. [Project Structure](#project-structure)
4. [Installation & Setup](#installation--setup)
5. [Features](#features)
6. [Backend Integration](#backend-integration)
7. [Development](#development)

---

## Overview

A single-page React application that provides a comprehensive dashboard for managing projects, tasks, and team members. Built with React 19 and Vite for optimal performance.

### Key Features
- ✅ **Unified Dashboard** - Single page with all management features
- ✅ **Project Management** - Create and track multiple projects
- ✅ **Task Management** - Full CRUD operations with status tracking
- ✅ **User Management** - Team member administration
- ✅ **Real-time Search** - Filter tasks by title and description
- ✅ **Status Filtering** - Filter tasks by status (PENDING, IN_PROGRESS, COMPLETED, BLOCKED)
- ✅ **Responsive Design** - Works on desktop and mobile devices

---

## Tech Stack

### Core Technologies
```json
{
  "dependencies": {
    "react": "^19.2.0",
    "react-dom": "^19.2.0",
    "axios": "^1.13.2"
  }
}
```

- **React 19.2** - Latest React with improved performance
- **Vite 7.2** - Lightning-fast build tool and dev server
- **Axios 1.13** - HTTP client for API communication
- **CSS3** - Modern styling with Flexbox/Grid

### Design Decisions

**✅ What We Use:**
- Component-based architecture
- React Hooks (useState, useEffect)
- Axios interceptors for authentication
- CSS modules for styling
- Environment variables for configuration

**❌ What We Don't Use (Kept Simple):**
- No React Router (single page application)
- No Redux/Zustand (local state management)
- No React Query (direct API calls)
- No TypeScript (vanilla JavaScript)
- No UI frameworks (custom CSS)

---

## Project Structure

```
frontend/
├── src/
│   ├── api.js                    # Centralized API layer with Axios
│   ├── App.jsx                   # Root component with header
│   ├── App.css                   # App-level styling
│   ├── main.jsx                  # Entry point
│   ├── index.css                 # Global styles
│   │
│   ├── pages/
│   │   └── DashboardPage.jsx     # Main dashboard (635 lines)
│   │                              # - Project panel
│   │                              # - Task panel with filters
│   │                              # - User panel
│   │
│   ├── components/
│   │   └── Modal.jsx             # Reusable modal dialog
│   │
│   └── styles/
│       ├── DashboardPage.css     # Dashboard-specific styles
│       └── Modal.css             # Modal styling
│
├── public/                       # Static assets
├── .env.development              # Local environment config
├── .env.production               # Production environment config
├── package.json                  # Dependencies
├── vite.config.js               # Vite configuration
└── README.md                    # This file
```

### Key Files

#### `src/api.js` - Centralized API Layer
All backend API calls in one place with error handling:
```javascript
// Axios instance with Basic Auth
const apiClient = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL,
    auth: {
        username: import.meta.env.VITE_API_USERNAME,
        password: import.meta.env.VITE_API_PASSWORD,
    }
});

// API functions
export const getProjects = async () => { /* ... */ };
export const createProject = async (data) => { /* ... */ };
export const getProjectTasks = async (projectId) => { /* ... */ };
export const createTask = async (data) => { /* ... */ };
export const updateTask = async (id, data) => { /* ... */ };
export const deleteTask = async (id) => { /* ... */ };
export const getUsers = async () => { /* ... */ };
export const createUser = async (data) => { /* ... */ };
export const deleteUser = async (id) => { /* ... */ };
```

#### `src/pages/DashboardPage.jsx` - Main Dashboard
Single page application with three main sections:
- **Projects Panel**: List of projects with task counts
- **Tasks Panel**: Task list with search/filter and CRUD operations
- **Users Panel**: Team member list with creation/deletion

#### `src/components/Modal.jsx` - Reusable Modal
Generic modal component used for all forms (create project, create task, create user).

---

## Installation & Setup

### Prerequisites
- Node.js 18+ 
- npm or yarn
- Backend API running (local or production)

### Step 1: Install Dependencies

```bash
cd frontend
npm install
```

### Step 2: Configure Environment

Create `.env.development` for local development:
```env
```

Create `.env.production` for production:
```env
```

### Step 3: Run Development Server

```bash
npm run dev
```

Access at: `http://localhost:5173`

### Step 4: Build for Production

```bash
npm run build
npm run preview
```

Output in `dist/` folder.

---

## Features

### 1. Project Management

**Display:**
- Grid layout showing all projects
- Each card shows project name, description, and task count
- Click to select and view tasks

**Create Project:**
- Modal form with fields: name, description, owner (user dropdown), start date, end date
- Form validation
- Success: Refreshes project list

**API Used:**
- `GET /api/projects` - Load all projects
- `POST /api/projects` - Create new project

### 2. Task Management

**Display:**
- Task list filtered by selected project
- Shows: title, description, status, priority, due date, assignees
- Real-time search by title/description
- Status filter dropdown (ALL, PENDING, IN_PROGRESS, COMPLETED, BLOCKED)

**Create Task:**
- Modal form with fields: title, description, project, status, priority, due date, assignees
- Assignees: Multi-select from users list
- Success: Refreshes task list

**Update Task:**
- Click task status to change (PENDING → IN_PROGRESS → COMPLETED → BLOCKED)
- Inline status update without page reload

**Delete Task:**
- Delete button on each task
- Confirmation dialog
- Success: Removes from list

**API Used:**
- `GET /api/projects/{id}/tasks` - Load project tasks
- `POST /api/tasks` - Create new task
- `PUT /api/tasks/{id}` - Update task (status change)
- `DELETE /api/tasks/{id}` - Delete task

### 3. User Management

**Display:**
- Table showing all users (username, email, full name)
- Delete button for each user

**Create User:**
- Modal form with fields: username, email, password, full name
- Email validation
- Success: Refreshes user list

**Delete User:**
- Click delete button
- Confirmation dialog
- Success: Removes from list

**API Used:**
- `GET /api/users` - Load all users
- `POST /api/users` - Create new user
- `DELETE /api/users/{id}` - Delete user

### 4. Search & Filter

**Search:**
- Text input in task panel
- Filters tasks by title and description (case-insensitive)
- Updates instantly on typing

**Status Filter:**
- Dropdown with options: ALL, PENDING, IN_PROGRESS, COMPLETED, BLOCKED
- Combines with search filter
- Shows task count for current filter

---

## Backend Integration

### Authentication
- **Method**: Basic Authentication
- **Credentials**: From environment variables
- **Implementation**: Axios auth config

### API Base URL
- **Local**: `http://localhost:8080`
- **Production**: `https://task-management-system-latest-97wu.onrender.com`

### Request/Response Examples

**Create Project:**
```javascript
POST /api/projects
{
  "name": "Website Redesign",
  "description": "Modernize company website",
  "ownerId": 1,
  "startDate": "2026-01-01",
  "endDate": "2026-06-30"
}
```

**Create Task:**
```javascript
POST /api/tasks
{
  "title": "Design homepage mockup",
  "description": "Create wireframes and mockups",
  "projectId": 1,
  "status": "PENDING",
  "priority": "HIGH",
  "dueDate": "2026-02-01T17:00:00",
  "assigneeIds": [1, 2]
}
```

**Update Task Status:**
```javascript
PUT /api/tasks/{id}
{
  "status": "IN_PROGRESS"
}
```

**Create User:**
```javascript
POST /api/users
{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "SecurePass123",
  "fullName": "John Doe"
}
```

### Error Handling
- Axios interceptor catches all API errors
- Console logging for debugging
- User-friendly error messages via alert
- Graceful fallback on network failures

---

## Development

### Available Scripts

```bash
npm run dev      # Start development server (port 5173)
npm run build    # Build for production
npm run preview  # Preview production build
npm run lint     # Run ESLint
```

### State Management
- **Local State**: `useState` for component state
- **Side Effects**: `useEffect` for data loading
- **No Global State**: All state managed in DashboardPage component

### Styling Approach
- **CSS Modules**: Separate CSS files per component
- **Responsive**: Flexbox and Grid for layouts
- **Modern**: CSS variables for theming
- **No Framework**: Custom CSS for full control

### Code Organization
- **Single Page**: All features in DashboardPage
- **Reusable Components**: Modal for all forms
- **Centralized API**: All API calls in api.js
- **Clear Separation**: Components, styles, API layer

---

## Deployment

### Build Command
```bash
npm run build
```

### Output Directory
```
dist/
```

### Deployment Platforms

**Netlify:**
1. Connect GitHub repository
2. Build command: `npm run build`
3. Publish directory: `dist`
4. Add environment variables in Settings

**Vercel:**
```bash
npm install -g vercel
vercel --prod
```

**Render:**
1. Create new Static Site
2. Build command: `npm run build`
3. Publish directory: `dist`

---

## Environment Variables

Required environment variables:

| Variable | Description | Example |
|----------|-------------|---------|
| `VITE_API_BASE_URL` | Backend API URL | `http://localhost:8080` |
| `VITE_API_USERNAME` | Basic Auth username | `admin` |
| `VITE_API_PASSWORD` | Basic Auth password | `password` |

---

## Links

- **Backend Repository**: [GitHub](../)
- **Production Frontend**: [https://task-management-frontend-8brf.onrender.com/](https://task-management-frontend-8brf.onrender.com/)
- **Production API**: [https://task-management-system-latest-97wu.onrender.com/api](https://task-management-system-latest-97wu.onrender.com/api)
- **Backend README**: [../README.md](../README.md)

---

**Last Updated:** January 4, 2026  
**Version:** 0.7.0  
**Tech Stack:** React 19.2 + Vite 7.2 + Axios 1.13
