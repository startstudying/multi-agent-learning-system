import { createRouter, createWebHistory, type RouterHistory } from 'vue-router'
import NewChatPage from './pages/chat/NewChatPage.vue'
import StudentLoginPage from './pages/login/StudentLoginPage.vue'
import TeacherLoginPage from './pages/login/TeacherLoginPage.vue'
import TeacherReviewQueue from './pages/teacher/TeacherReviewQueue.vue'
import AdminOperations from './pages/admin/AdminOperations.vue'
import AdminModelProviders from './pages/admin/AdminModelProviders.vue'

export const routes = [
  {
    path: '/',
    name: 'new-chat',
    component: NewChatPage,
    meta: { label: '新建对话' },
  },
  {
    path: '/chat/new',
    name: 'new-chat-alias',
    component: NewChatPage,
    meta: { label: '新建对话' },
  },
  {
    path: '/student',
    redirect: '/',
  },
  {
    path: '/login/student',
    name: 'student-login',
    component: StudentLoginPage,
    meta: { label: '学生登录' },
  },
  {
    path: '/login/teacher',
    name: 'teacher-login',
    component: TeacherLoginPage,
    meta: { label: '教师登录' },
  },
  {
    path: '/teacher/reviews',
    name: 'teacher',
    component: TeacherReviewQueue,
    meta: { label: '教师审核队列' },
  },
  {
    path: '/admin/operations',
    name: 'admin',
    component: AdminOperations,
    meta: { label: '管理员运维' },
  },
  {
    path: '/admin/model-providers',
    name: 'admin-model-providers',
    component: AdminModelProviders,
    meta: { label: 'Model Providers 配置' },
  },
] as const

export function createAppRouter(history: RouterHistory = createWebHistory()) {
  return createRouter({
    history,
    routes: [...routes],
  })
}
