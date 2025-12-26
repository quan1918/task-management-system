# Task Management System - Frontend (Demo UI)

> ⚠️ **LƯU Ý QUAN TRỌNG:** Đây là project tập trung vào **BACKEND (Spring Boot)**. 
> Frontend chỉ là giao diện demo đơn giản để visualize backend APIs, phù hợp cho fresher level.

---

## 📋 Mục lục

1. [Giới thiệu](#giới-thiệu)
2. [Tech Stack](#tech-stack)
3. [Cấu trúc Project](#cấu-trúc-project)
4. [Cài đặt và Chạy](#cài-đặt-và-chạy)
5. [Backend APIs](#backend-apis)
6. [Tính năng](#tính-năng)

---

## Giới thiệu

### Backend (Spring Boot) - Trọng tâm của project
- ✅ Clean Architecture với 4 layers (API, Service, Repository, Entity)
- ✅ 18 RESTful APIs với validation đầy đủ
- ✅ PostgreSQL database với relationships
- ✅ Spring Security với Basic Authentication
- ✅ Exception handling toàn cục
- ✅ Deploy trên Render (production-ready)

### Frontend (React) - UI demo đơn giản
- ✅ 3 pages: Projects, Tasks, Users
- ✅ Gọi backend APIs qua Axios
- ✅ CRUD operations cơ bản
- ✅ Form đơn giản với validation
- ✅ **Giữ code đơn giản để dễ giải thích trong phỏng vấn**

**Tại sao giữ frontend đơn giản?**
- Frontend chỉ để demo backend APIs hoạt động
- Tránh over-engineering (không cần React Query, Redux, TypeScript)
- Dễ maintain và dễ giải thích source code
- Tập trung showcase backend skills trong interview

---

## Tech Stack

### Frontend (Minimal - Demo Purpose Only)
```json
{
  "dependencies": {
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "axios": "^1.6.0"
  }
}
```

- **React 18** - UI library
- **Vite** - Build tool
- **Axios** - HTTP client để gọi backend APIs
- **CSS thuần** - Styling đơn giản (không dùng frameworks)

### Backend (Main Focus - Production Ready)
- **Spring Boot 3.2** - Java framework
- **PostgreSQL** - Relational database
- **Spring Data JPA** - ORM
- **Spring Security** - Authentication
- **Maven** - Build tool

### ❌ KHÔNG sử dụng (để giữ frontend đơn giản)
- React Query / TanStack Query
- Redux / Zustand / Context API phức tạp
- React Hook Form / Yup
- TypeScript
- Material-UI / Ant Design / Chakra UI
- Tailwind CSS
- React Router (dùng conditional rendering)
- date-fns / moment.js

→ **Mục tiêu:** Code dễ đọc, dễ hiểu, dễ giải thích trong 5 phút

---

## Cấu trúc Project (Đơn giản)

```
frontend/
├── src/
│   ├── api.js                  # Tất cả API calls (Axios + Basic Auth)
│   ├── App.jsx                 # Main component với navigation
│   ├── main.jsx                # Entry point
│   │
│   ├── pages/
│   │   ├── ProjectsPage.jsx    # List projects + Create project
│   │   ├── TasksPage.jsx       # List tasks + Create/Delete task
│   │   └── UsersPage.jsx       # List users + Create user
│   │
│   ├── components/
│   │   ├── Modal.jsx           # Reusable modal dialog
│   │   └── Navbar.jsx          # Simple navigation bar
│   │
│   └── styles/
│       └── global.css          # All CSS trong 1 file
│
├── .env.development            # Local backend URL + credentials
├── .env.production             # Production backend URL
├── package.json
├── vite.config.js
└── README.md
```

**Giải thích cấu trúc:**

### `src/api.js` - Centralized API Layer
Tất cả backend API calls trong 1 file duy nhất:
```javascript
// Axios instance với Basic Auth
export const api = axios.create({ ... });

// API functions
export const getProjects = () => api.get('/api/projects');
export const createProject = (data) => api.post('/api/projects', data);
export const getProjectTasks = (projectId) => api.get(`/api/projects/${projectId}/tasks`);
export const createTask = (data) => api.post('/api/tasks', data);
export const deleteTask = (id) => api.delete(`/api/tasks/${id}`);
export const getUsers = () => api.get('/api/users');
export const createUser = (data) => api.post('/api/users', data);
```
→ Dễ track tất cả API calls, không cần nhiều files

### 3 Pages
- **ProjectsPage:** Hiển thị list + form tạo project mới
- **TasksPage:** Chọn project → load tasks → create/delete
- **UsersPage:** Hiển thị list + form tạo user mới

### Components
- **Modal:** Reusable dialog (dùng cho tất cả forms)
- **Navbar:** 3 links (Projects | Tasks | Users)

### Không có:
- ❌ Nhiều layers (hooks/, utils/, api/)
- ❌ Component phân cấp phức tạp
- ❌ Custom hooks
- ❌ Utility functions

---

## Cài đặt và Chạy

### Yêu cầu
- Node.js 18+
- Backend đang chạy (local hoặc Render)

### Bước 1: Khởi tạo project

```bash
cd frontend
npm create vite@latest . -- --template react
npm install
npm install axios
```

### Bước 2: Cấu hình môi trường

Tạo file `.env.development`:
```env
VITE_API_BASE_URL=http://localhost:8080
VITE_API_USERNAME=admin
VITE_API_PASSWORD=admin
```

Tạo file `.env.production`:
```env
VITE_API_BASE_URL=https://task-management-system-0c0p.onrender.com
VITE_API_USERNAME=admin
VITE_API_PASSWORD=admin
```

### Bước 3: Chạy app

```bash
npm run dev
```

Mở browser: `http://localhost:5173`

### Bước 4: Build production (optional)

```bash
npm run build
npm run preview
```

---

## Backend APIs

Backend Spring Boot cung cấp **18 REST APIs**. Frontend chỉ dùng **8 APIs** quan trọng nhất:

### 🎯 APIs Frontend Sử Dụng

#### Projects
```javascript
GET    /api/projects           // Lấy danh sách projects
POST   /api/projects           // Tạo project mới
GET    /api/projects/{id}/tasks // Lấy tasks của 1 project
```

#### Tasks
```javascript
POST   /api/tasks              // Tạo task mới
DELETE /api/tasks/{id}         // Xóa task
PUT    /api/tasks/{id}         // Update task
```

#### Users
```javascript
GET    /api/users              // Lấy danh sách users
POST   /api/users              // Tạo user mới
```

### Request/Response Examples

**Tạo Project:**
```javascript
POST /api/projects
{
  "name": "Website Redesign",
  "description": "Redesign company website",
  "ownerId": 1,
  "startDate": "2025-12-20",
  "endDate": "2026-03-31"
}
```

**Tạo Task:**
```javascript
POST /api/tasks
{
  "title": "Fix login bug",
  "description": "Users cannot login with special characters",
  "priority": "HIGH",
  "dueDate": "2025-12-31T17:00:00",
  "estimatedHours": 8,
  "assigneeIds": [1, 2],
  "projectId": 1
}
```

**Tạo User:**
```javascript
POST /api/users
{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "SecurePass123!",
  "fullName": "John Doe"
}
```

---

## Tính năng

### 1. Projects Page

**Hiển thị:**
- Table với danh sách projects (name, owner, dates)
- Button "Add Project"

**Tạo project:**
- Click "Add Project" → Modal mở
- Form: name, description, ownerId, startDate, endDate
- Submit → Gọi `POST /api/projects`
- Success → Đóng modal, reload list

### 2. Tasks Page

**Hiển thị:**
- Dropdown chọn project
- Table với tasks của project đã chọn
- Button "Add Task"
- Mỗi task có button "Delete"

**Tạo task:**
- Click "Add Task" → Modal mở
- Form: title, description, priority, dueDate, assigneeIds (comma-separated), projectId
- Submit → Gọi `POST /api/tasks`
- Success → Đóng modal, reload list

**Xóa task:**
- Click "Delete" → Confirm dialog
- Yes → Gọi `DELETE /api/tasks/{id}`
- Success → Reload list

### 3. Users Page

**Hiển thị:**
- Table với danh sách users (username, email, fullName)
- Button "Add User"

**Tạo user:**
- Click "Add User" → Modal mở
- Form: username, email, password, fullName
- Submit → Gọi `POST /api/users`
- Success → Đóng modal, reload list

---

## Code Examples

### `src/api.js` - API Layer

```javascript
import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;
const USERNAME = import.meta.env.VITE_API_USERNAME;
const PASSWORD = import.meta.env.VITE_API_PASSWORD;

// Create axios instance with Basic Auth
const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Authorization': 'Basic ' + btoa(`${USERNAME}:${PASSWORD}`),
    'Content-Type': 'application/json'
  }
});

// API functions
export const getProjects = () => api.get('/api/projects');
export const createProject = (data) => api.post('/api/projects', data);
export const getProjectTasks = (projectId) => api.get(`/api/projects/${projectId}/tasks`);
export const createTask = (data) => api.post('/api/tasks', data);
export const deleteTask = (id) => api.delete(`/api/tasks/${id}`);
export const getUsers = () => api.get('/api/users');
export const createUser = (data) => api.post('/api/users', data);
```

### `src/pages/ProjectsPage.jsx` - Example Page

```javascript
import { useState, useEffect } from 'react';
import { getProjects, createProject } from '../api';
import Modal from '../components/Modal';

export default function ProjectsPage() {
  const [projects, setProjects] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [formData, setFormData] = useState({});

  // Load projects khi component mount
  useEffect(() => {
    loadProjects();
  }, []);

  const loadProjects = async () => {
    try {
      const response = await getProjects();
      setProjects(response.data);
    } catch (error) {
      alert('Error loading projects: ' + error.message);
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      await createProject(formData);
      setShowModal(false);
      loadProjects(); // Reload list
    } catch (error) {
      alert('Error creating project: ' + error.message);
    }
  };

  if (loading) return <div>Loading...</div>;

  return (
    <div>
      <h1>Projects</h1>
      <button onClick={() => setShowModal(true)}>Add Project</button>
      
      <table>
        <thead>
          <tr>
            <th>Name</th>
            <th>Owner</th>
            <th>Start Date</th>
            <th>End Date</th>
          </tr>
        </thead>
        <tbody>
          {projects.map(p => (
            <tr key={p.id}>
              <td>{p.name}</td>
              <td>{p.owner.fullName}</td>
              <td>{p.startDate}</td>
              <td>{p.endDate}</td>
            </tr>
          ))}
        </tbody>
      </table>

      {showModal && (
        <Modal title="Create Project" onClose={() => setShowModal(false)}>
          <form onSubmit={handleSubmit}>
            <input 
              placeholder="Name" 
              onChange={e => setFormData({...formData, name: e.target.value})}
              required 
            />
            {/* ... other fields ... */}
            <button type="submit">Create</button>
          </form>
        </Modal>
      )}
    </div>
  );
}
```

### `src/components/Modal.jsx` - Reusable Modal

```javascript
export default function Modal({ title, children, onClose }) {
  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-content" onClick={e => e.stopPropagation()}>
        <div className="modal-header">
          <h2>{title}</h2>
          <button onClick={onClose}>✕</button>
        </div>
        <div className="modal-body">
          {children}
        </div>
      </div>
    </div>
  );
}
```

---

## Giải thích trong Interview

### Câu hỏi: "Tại sao frontend đơn giản như vậy?"

**Trả lời:**
> "Project này tập trung vào backend với Spring Boot. Frontend chỉ là UI demo để visualize backend APIs hoạt động.
> 
> Backend có:
> - Clean Architecture với 4 layers rõ ràng
> - 18 RESTful APIs với validation đầy đủ  
> - PostgreSQL với relationships phức tạp
> - Exception handling toàn cục
> - Security với Basic Auth
> 
> Frontend tôi giữ đơn giản vì:
> - Không muốn over-engineering
> - Dễ maintain và dễ giải thích code
> - Focus showcase backend skills
> - Thực tế fresher level không cần React Query, Redux"

### Câu hỏi: "Có thể scale frontend không?"

**Trả lời:**
> "Có thể! Nếu cần scale, tôi sẽ:
> 1. Thêm React Router cho routing
> 2. Thêm React Query cho caching
> 3. Thêm form validation library
> 4. Refactor thành nhiều components nhỏ hơn
> 
> Nhưng hiện tại giữ đơn giản để focus vào backend architecture."

---

## Lưu ý Development

### Authentication
- Username/password hardcode trong `.env`
- Axios tự động thêm Basic Auth header
- Không có login page

### Error Handling
- `try-catch` trong mỗi API call
- `alert()` để hiển thị lỗi (đơn giản)
- Backend trả về error message rõ ràng

### Form Validation
- HTML5 validation (`required`, `minlength`, `type="email"`)
- Không dùng Yup hay React Hook Form
- Backend có validation, frontend chỉ cần basic

### State Management
- `useState` cho local state
- `useEffect` để load data
- Không dùng Context API, Redux
- Re-fetch sau mỗi create/delete

### Styling
- CSS thuần trong `global.css`
- Flexbox/Grid cho layout
- Không dùng CSS-in-JS, Tailwind
- Đơn giản, dễ đọc

---

## Scripts

```bash
npm run dev      # Start dev server (port 5173)
npm run build    # Build for production
npm run preview  # Preview production build
```

---

## Deployment

### Netlify
1. Connect GitHub repo
2. Build command: `npm run build`
3. Publish directory: `dist`
4. Add environment variables

### Vercel
```bash
npm install -g vercel
vercel
```

---

## Links

- **Backend Repository:** [GitHub](../)
- **Backend API (Production):** https://task-management-system-0c0p.onrender.com
- **Backend README:** [../README.md](../README.md)

---

**Last Updated:** December 25, 2025  
**Version:** 1.0.0 (Fresher-Level Demo)  
**Focus:** Backend (Spring Boot) > Frontend (React)
