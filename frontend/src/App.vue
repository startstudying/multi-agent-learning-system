<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, RouterView, useRoute } from 'vue-router'
import { Activity, BookOpenCheck, GraduationCap, ServerCog, ShieldCheck } from 'lucide-vue-next'

const route = useRoute()

const activeLabel = computed(() => String(route.meta.label ?? 'Student Learning Loop'))

const shellContext = computed(() => {
  if (route.name === 'teacher') {
    return {
      eyebrow: '教师端',
      title: '审核工作台',
      items: [
        ['核心任务', '引用、安全、画像适配审核'],
        ['发布闸口', '批准或退回 AI 生成资源'],
        ['决策范围', '仅处理当前选中资源'],
      ],
    }
  }

  if (route.name === 'admin' || route.name === 'admin-model-providers') {
    return {
      eyebrow: '管理员端',
      title: route.name === 'admin-model-providers' ? '模型供应商配置' : '系统健康总览',
      items: [
        ['监控范围', route.name === 'admin-model-providers' ? 'Model Provider Registry' : 'Health 与 Analytics 当前信号'],
        ['关注重点', route.name === 'admin-model-providers' ? 'API Key 加密与默认路由' : 'Backlog、Token、Agent 活动'],
        ['接入边界', '仅展示已有后端 API'],
      ],
    }
  }

  return {
    eyebrow: '学生端',
    title: '主动学习闭环',
    items: [
      ['核心任务', '提问、看引用、练习'],
      ['证据线索', '课程来源、掌握度、traceId'],
      ['资源状态', '仅使用教师审核后资源'],
    ],
  }
})

const shellUser = computed(() => {
  if (route.name === 'teacher') {
    return {
      avatar: '师',
      name: '张老师',
      role: '教师端',
    }
  }

  if (route.name === 'admin' || route.name === 'admin-model-providers') {
    return {
      avatar: '管',
      name: '管理员',
      role: '管理后台',
    }
  }

  return {
    avatar: '张',
    name: '张三（学生）',
    role: '学生端',
  }
})
</script>

<template>
  <main class="app-shell">
    <aside class="sidebar" aria-label="Workbench navigation">
      <div class="brand-lockup">
        <div class="brand-mark">
          <GraduationCap :size="24" aria-hidden="true" />
        </div>
        <div>
          <p class="eyebrow">AI Learning OS</p>
          <h1>个性化学习智能体平台</h1>
        </div>
      </div>

      <div class="sidebar-user">
        <div class="avatar-token">{{ shellUser.avatar }}</div>
        <div>
          <strong>{{ shellUser.name }}</strong>
          <span>{{ shellUser.role }}</span>
        </div>
      </div>

      <nav class="role-switcher" aria-label="Workbench views">
        <RouterLink
          :class="['role-button', { active: route.name === 'student' }]"
          to="/"
          data-test="student-view"
        >
          <BookOpenCheck :size="16" aria-hidden="true" />
          学习工作台
        </RouterLink>
        <RouterLink
          :class="['role-button', { active: route.name === 'teacher' }]"
          to="/teacher/reviews"
          data-test="teacher-view"
        >
          <ShieldCheck :size="16" aria-hidden="true" />
          审核中心
        </RouterLink>
        <RouterLink
          :class="['role-button', { active: route.name === 'admin' || route.name === 'admin-model-providers' }]"
          to="/admin/operations"
          data-test="admin-view"
        >
          <Activity :size="16" aria-hidden="true" />
          管理后台
        </RouterLink>
        <RouterLink
          v-if="route.name === 'admin' || route.name === 'admin-model-providers'"
          :class="['role-button', { active: route.name === 'admin-model-providers' }]"
          to="/admin/model-providers"
          data-test="admin-model-providers-view"
        >
          <ServerCog :size="16" aria-hidden="true" />
          模型供应商
        </RouterLink>
      </nav>

      <section class="sidebar-panel context-rail" aria-labelledby="console-title" data-test="shell-context">
        <p id="console-title" class="eyebrow">{{ shellContext.eyebrow }}</p>
        <h2>{{ shellContext.title }}</h2>
        <dl class="state-list">
          <div v-for="[label, value] in shellContext.items" :key="label">
            <dt>{{ label }}</dt>
            <dd>{{ value }}</dd>
          </div>
          <div>
            <dt>Active page</dt>
            <dd>{{ activeLabel }}</dd>
          </div>
        </dl>
      </section>
    </aside>

    <RouterView />
  </main>
</template>
