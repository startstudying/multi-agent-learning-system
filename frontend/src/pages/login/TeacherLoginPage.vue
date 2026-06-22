<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ShieldCheck } from 'lucide-vue-next'
import { useSession } from '../../stores/session'

const router = useRouter()
const { loginAs } = useSession()

async function enterTeacherWorkspace() {
  loginAs('teacher')
  await router.push('/')
}
</script>

<template>
  <main class="login-page" aria-label="教师登录页面" data-test="teacher-login-page">
    <section class="login-card">
      <span class="login-icon"><ShieldCheck :size="24" aria-hidden="true" /></span>
      <span class="login-eyebrow">教师登录</span>
      <h1>教师登录</h1>
      <p>进入完整工作台，查看新建对话、教师审核、管理员运维和模型供应商等全部板块。</p>
      <button class="login-primary" type="button" data-test="teacher-login-submit" @click="enterTeacherWorkspace">
        进入教师端
      </button>
      <RouterLink class="login-switch" to="/login/student" data-test="switch-to-student-login">
        我是学生，前往学生登录
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
