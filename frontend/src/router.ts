import { createRouter, createWebHistory, type RouterHistory } from 'vue-router'
import StudentDashboard from './pages/student/StudentDashboard.vue'
import TeacherReviewQueue from './pages/teacher/TeacherReviewQueue.vue'
import AdminOperations from './pages/admin/AdminOperations.vue'
import AdminModelProviders from './pages/admin/AdminModelProviders.vue'

export const routes = [
  {
    path: '/',
    name: 'student',
    component: StudentDashboard,
    meta: { label: 'Student Learning Loop' },
  },
  {
    path: '/teacher/reviews',
    name: 'teacher',
    component: TeacherReviewQueue,
    meta: { label: 'Teacher Review Queue' },
  },
  {
    path: '/admin/operations',
    name: 'admin',
    component: AdminOperations,
    meta: { label: 'Admin Operations' },
  },
  {
    path: '/admin/model-providers',
    name: 'admin-model-providers',
    component: AdminModelProviders,
    meta: { label: 'Model Providers' },
  },
] as const

export function createAppRouter(history: RouterHistory = createWebHistory()) {
  return createRouter({
    history,
    routes: [...routes],
  })
}

