import { computed, ref } from 'vue'

export type UserRole = 'student' | 'teacher'

const SESSION_ROLE_KEY = 'ai-learning-os-role'

function isUserRole(role: string | null): role is UserRole {
  return role === 'student' || role === 'teacher'
}

function readInitialRole(): UserRole {
  if (typeof window === 'undefined') return 'student'

  const savedRole = window.localStorage.getItem(SESSION_ROLE_KEY)
  return isUserRole(savedRole) ? savedRole : 'student'
}

const currentRole = ref<UserRole>(readInitialRole())

const roleLabel = computed(() => (currentRole.value === 'teacher' ? '教师账户' : '学生账户'))
const isTeacher = computed(() => currentRole.value === 'teacher')

function loginAs(role: UserRole) {
  currentRole.value = role
  window.localStorage.setItem(SESSION_ROLE_KEY, role)
}

function logout() {
  currentRole.value = 'student'
  window.localStorage.removeItem(SESSION_ROLE_KEY)
}

export function useSession() {
  currentRole.value = readInitialRole()

  return {
    currentRole,
    roleLabel,
    isTeacher,
    loginAs,
    logout,
  }
}
