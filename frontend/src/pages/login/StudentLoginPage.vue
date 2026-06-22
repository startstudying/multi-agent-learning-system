<script setup lang="ts">
import { useRouter } from 'vue-router'
import { GraduationCap } from 'lucide-vue-next'
import { useSession } from '../../stores/session'

const router = useRouter()
const { loginAs } = useSession()

async function enterStudentWorkspace() {
  loginAs('student')
  await router.push('/')
}
</script>

<template>
  <main class="login-page" aria-label="学生登录页面" data-test="student-login-page">
    <section class="login-card">
      <span class="login-icon"><GraduationCap :size="24" aria-hidden="true" /></span>
      <span class="login-eyebrow">学生登录</span>
      <h1>学生登录</h1>
      <p>进入新建对话学习会话，继续资料上传、RAG 问答、学习路径和测评反馈。</p>
      <button class="login-primary" type="button" data-test="student-login-submit" @click="enterStudentWorkspace">
        进入学生端
      </button>
      <RouterLink class="login-switch" to="/login/teacher" data-test="switch-to-teacher-login">
        我是教师，前往教师登录
      </RouterLink>
    </section>
  </main>
</template>

<style scoped>
.login-page {
  display: grid;
  place-items: center;
  min-height: 100vh;
  padding: 24px;
  background: #ffffff;
}

.login-card {
  display: grid;
  gap: 14px;
  width: min(100%, 430px);
  padding: 34px;
  text-align: center;
  background: #ffffff;
  border: 1px solid #ececf1;
  border-radius: 24px;
  box-shadow: 0 24px 70px rgba(15, 23, 42, 0.08);
}

.login-icon {
  display: grid;
  place-items: center;
  width: 54px;
  height: 54px;
  margin: 0 auto;
  color: #ffffff;
  background: #111827;
  border-radius: 18px;
}

.login-eyebrow {
  color: #6b7280;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.login-card h1 {
  margin: 0;
  color: #111827;
  font-size: 30px;
  line-height: 1.15;
}

.login-card p {
  margin: 0;
  color: #6b7280;
  line-height: 1.6;
}

.login-primary,
.login-switch {
  min-height: 44px;
  border-radius: 999px;
  font-weight: 800;
}

.login-primary {
  color: #ffffff;
  background: #111827;
  border: 0;
  cursor: pointer;
}

.login-switch {
  display: grid;
  place-items: center;
  color: #374151;
  text-decoration: none;
  background: #f7f7f8;
}
</style>
