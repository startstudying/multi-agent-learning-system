<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import {
  Bot,
  ChevronDown,
  Compass,
  GraduationCap,
  HelpCircle,
  LogOut,
  MessageSquareText,
  PanelLeft,
  Plus,
  Search,
  Settings,
  ShieldCheck,
  SlidersHorizontal,
  UserRound,
  Wrench,
} from 'lucide-vue-next'
import { useSession, type UserRole } from '../../stores/session'

type SidebarNavItem = {
  label: string
  detail: string
  to?: string
  routeNames: string[]
  icon: unknown
  roles?: UserRole[]
  disabled?: boolean
  test?: string
}

type SidebarQuickAction = {
  label: string
  detail: string
  icon: unknown
  to?: string
  test?: string
  legacyTest?: string
}

type ConversationItem = {
  title: string
  meta: string
  tone: 'blue' | 'green' | 'amber'
  roles?: UserRole[]
}

type ConversationGroup = {
  label: string
  items: ConversationItem[]
}

type AccountMenuItem = {
  label: string
  detail: string
  icon: unknown
  danger?: boolean
}

const route = useRoute()
const router = useRouter()
const { currentRole, roleLabel, logout } = useSession()
const isAccountMenuOpen = ref(false)

const quickActions: SidebarQuickAction[] = [
  {
    label: '新建对话',
    detail: '开启一轮学习智能体会话',
    icon: Plus,
    to: '/',
    test: 'new-chat',
    legacyTest: 'new-learning-task',
  },
  {
    label: '搜索',
    detail: '查找历史、资源与 Trace',
    icon: Search,
  },
  {
    label: '探索',
    detail: '发现课程助手与工作流',
    icon: Compass,
  },
]

const allNavigation: SidebarNavItem[] = [
  {
    label: '教师审核',
    detail: '资源发布闸口',
    to: '/teacher/reviews',
    routeNames: ['teacher'],
    icon: ShieldCheck,
    roles: ['teacher'],
    test: 'teacher-view',
  },
  {
    label: '管理员运维',
    detail: '健康与指标',
    to: '/admin/operations',
    routeNames: ['admin'],
    icon: Wrench,
    test: 'admin-view',
  },
  {
    label: '模型供应商',
    detail: '路由与密钥配置',
    to: '/admin/model-providers',
    routeNames: ['admin-model-providers'],
    icon: SlidersHorizontal,
    test: 'admin-model-providers-view',
  },
]

const navigation = computed(() =>
  allNavigation.filter((item) => !item.roles || item.roles.includes(currentRole.value)),
)

const conversationGroups: ConversationGroup[] = [
  {
    label: '今天',
    items: [
      { title: 'Java 后端课程资料问答', meta: 'RAG · 引用校验', tone: 'blue' },
      { title: '学习路径节点拆解', meta: '画像 · 掌握度', tone: 'green' },
    ],
  },
  {
    label: '更早',
    items: [
      { title: '待教师审核资源清单', meta: '治理 · 队列', tone: 'amber', roles: ['teacher'] },
    ],
  },
]

const visibleConversationGroups = computed(() =>
  conversationGroups
    .map((group) => ({
      ...group,
      items: group.items.filter((item) => !item.roles || item.roles.includes(currentRole.value)),
    }))
    .filter((group) => group.items.length > 0),
)

const accountMenuItems: AccountMenuItem[] = [
  {
    label: '个人账户',
    detail: '资料、用量与偏好',
    icon: UserRound,
  },
  {
    label: '设置',
    detail: '主题、模型与通知',
    icon: Settings,
  },
  {
    label: '帮助与反馈',
    detail: '文档、支持与问题上报',
    icon: HelpCircle,
  },
  {
    label: '切换账号',
    detail: '退出当前工作区会话',
    icon: LogOut,
    danger: true,
  },
]

const routeContext = computed(() => {
  if (route.name === 'teacher') {
    return '审核工作台'
  }

  if (route.name === 'admin' || route.name === 'admin-model-providers') {
    return '系统健康总览'
  }

  return '新建对话学习会话'
})

function isActive(item: SidebarNavItem) {
  return item.routeNames.includes(String(route.name))
}

function toggleAccountMenu() {
  isAccountMenuOpen.value = !isAccountMenuOpen.value
}

function closeAccountMenu() {
  isAccountMenuOpen.value = false
}

async function handleAccountMenuItem(item: AccountMenuItem) {
  closeAccountMenu()

  if (item.danger) {
    logout()
    await router.push('/login/student')
  }
}
</script>

<template>
  <aside class="sidebar left-sidebar" aria-label="AI Learning OS 导航">
    <div class="sidebar-top">
      <div class="workspace-switcher" aria-label="当前工作区">
        <div class="workspace-mark">
          <GraduationCap :size="20" aria-hidden="true" />
        </div>
        <div class="workspace-copy">
          <strong>AI Learning OS</strong>
          <span>个性化学习智能体平台</span>
        </div>
        <ChevronDown :size="16" aria-hidden="true" />
      </div>
      <p class="visually-hidden" data-test="shell-context">{{ routeContext }}</p>

      <div class="quick-actions" aria-label="快捷入口">
        <RouterLink class="visually-hidden" to="/" data-test="new-learning-task">新建对话</RouterLink>
        <component
          :is="action.to ? RouterLink : 'button'"
          v-for="action in quickActions"
          :key="action.label"
          class="quick-action"
          :type="action.to ? undefined : 'button'"
          :to="action.to"
          :data-test="action.test"
          :data-legacy-test="action.legacyTest"
        >
          <span class="action-icon"><component :is="action.icon" :size="17" aria-hidden="true" /></span>
          <span>
            <strong>{{ action.label }}</strong>
            <small>{{ action.detail }}</small>
          </span>
        </component>
      </div>
    </div>

    <div class="sidebar-scroll-area">
      <nav class="sidebar-section" aria-label="现有模块">
        <div class="section-heading">
          <span>模块</span>
          <PanelLeft :size="14" aria-hidden="true" />
        </div>
        <div class="sidebar-nav-list">
          <RouterLink
            v-for="item in navigation"
            :key="item.label"
            :class="['sidebar-nav-item', { active: isActive(item) }]"
            :to="item.to ?? '/'"
            :data-test="item.test"
          >
            <component :is="item.icon" :size="17" aria-hidden="true" />
            <span>
              <strong>{{ item.label }}</strong>
              <small>{{ item.detail }}</small>
            </span>
          </RouterLink>
        </div>
      </nav>

      <section class="conversation-section" aria-label="会话历史">
        <div class="section-heading">
          <span>最近</span>
          <MessageSquareText :size="14" aria-hidden="true" />
        </div>

        <div class="conversation-groups">
          <section v-for="group in visibleConversationGroups" :key="group.label" class="conversation-group">
            <h2>{{ group.label }}</h2>
            <button
              v-for="item in group.items"
              :key="`${group.label}-${item.title}`"
              class="conversation-item"
              type="button"
            >
              <span :class="['conversation-dot', item.tone]" aria-hidden="true" />
              <span>
                <strong>{{ item.title }}</strong>
                <small>{{ item.meta }}</small>
              </span>
            </button>
          </section>
        </div>
      </section>
    </div>

    <div class="sidebar-footer">
      <div v-if="isAccountMenuOpen" class="account-menu" data-test="account-menu" role="menu">
        <button
          v-for="item in accountMenuItems"
          :key="item.label"
          :class="['account-menu-item', { danger: item.danger }]"
          type="button"
          role="menuitem"
          @click="handleAccountMenuItem(item)"
        >
          <component :is="item.icon" :size="16" aria-hidden="true" />
          <span>
            <strong>{{ item.label }}</strong>
            <small>{{ item.detail }}</small>
          </span>
        </button>
      </div>

      <button
        class="account-trigger"
        type="button"
        data-test="account-menu-trigger"
        :aria-expanded="isAccountMenuOpen"
        aria-haspopup="menu"
        @click="toggleAccountMenu"
      >
        <span class="avatar-token">
          <UserRound :size="18" aria-hidden="true" />
        </span>
        <span class="account-copy">
          <strong>当前用户</strong>
          <small>{{ roleLabel }}</small>
        </span>
        <Bot :size="16" aria-hidden="true" />
      </button>
    </div>
  </aside>
</template>

<style scoped>
.left-sidebar {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: var(--shell-sidebar-width);
  min-height: 100vh;
  padding: 10px;
  color: #202123;
  background: #f7f7f8;
  border-right: 1px solid #ececf1;
}

.sidebar-top,
.sidebar-scroll-area,
.sidebar-footer {
  min-width: 0;
}

.workspace-switcher,
.quick-action,
.sidebar-nav-item,
.conversation-item,
.account-trigger,
.account-menu-item {
  display: flex;
  align-items: center;
  width: 100%;
  min-width: 0;
  border: 0;
  border-radius: 10px;
  cursor: pointer;
}

.workspace-switcher {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr) auto;
  gap: 9px;
  min-height: 44px;
  padding: 6px 8px;
  background: transparent;
}

.workspace-switcher:hover,
.quick-action:hover,
.sidebar-nav-item:hover,
.conversation-item:hover,
.account-trigger:hover,
.account-menu-item:hover {
  background: #ececf1;
}

.workspace-mark,
.action-icon,
.avatar-token {
  display: grid;
  place-items: center;
  flex: 0 0 auto;
}

.workspace-mark {
  width: 34px;
  height: 34px;
  color: #ffffff;
  background: #111827;
  border-radius: 9px;
  box-shadow: inset 0 -10px 18px rgba(255, 255, 255, 0.12);
}

.workspace-copy,
.quick-action span:last-child,
.sidebar-nav-item span,
.conversation-item span:last-child,
.account-copy,
.account-menu-item span {
  display: grid;
  gap: 1px;
  min-width: 0;
  text-align: left;
}

.workspace-copy strong,
.quick-action strong,
.sidebar-nav-item strong,
.conversation-item strong,
.account-copy strong,
.account-menu-item strong {
  overflow: hidden;
  color: #202123;
  font-size: 14px;
  font-weight: 650;
  line-height: 1.25;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.workspace-copy span,
.quick-action small,
.sidebar-nav-item small,
.conversation-item small,
.account-copy small,
.account-menu-item small {
  overflow: hidden;
  color: #6b7280;
  font-size: 12px;
  line-height: 1.25;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.quick-actions {
  display: grid;
  gap: 2px;
  margin-top: 8px;
}

.quick-action {
  gap: 10px;
  min-height: 42px;
  padding: 7px 9px;
  color: #202123;
  text-align: left;
  background: transparent;
  transition:
    background-color 150ms ease,
    transform 150ms ease;
}

.quick-action:first-child {
  background: #ffffff;
  box-shadow: 0 1px 0 rgba(17, 24, 39, 0.06);
}

.quick-action:first-child:hover {
  background: #ffffff;
  transform: translateY(-1px);
}

.action-icon {
  width: 28px;
  height: 28px;
  color: #343541;
  border-radius: 8px;
}

.quick-action:first-child .action-icon {
  color: #ffffff;
  background: #10a37f;
}

.sidebar-scroll-area {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  gap: 16px;
  min-height: 0;
  padding: 8px 0;
  overflow-y: auto;
}

.sidebar-section,
.conversation-section,
.conversation-groups,
.conversation-group,
.sidebar-nav-list {
  display: grid;
  gap: 3px;
}

.section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 5px 9px 4px;
  color: #6b7280;
  font-size: 12px;
  font-weight: 650;
}

.sidebar-nav-item {
  gap: 10px;
  min-height: 40px;
  padding: 7px 9px;
  color: #343541;
  text-decoration: none;
  background: transparent;
  transition:
    background-color 150ms ease,
    color 150ms ease;
}

.sidebar-nav-item.active {
  color: #0f766e;
  background: #e6f4f1;
}

.sidebar-nav-item.active strong,
.sidebar-nav-item.active svg {
  color: #0f766e;
}

.sidebar-nav-item svg,
.account-trigger > svg,
.account-menu-item svg {
  flex: 0 0 auto;
  color: #6b7280;
}

.conversation-group h2 {
  padding: 8px 9px 3px;
  color: #8a8f98;
  font-size: 12px;
  font-weight: 650;
}

.conversation-item {
  gap: 9px;
  min-height: 38px;
  padding: 7px 9px;
  color: #202123;
  text-align: left;
  background: transparent;
}

.conversation-dot {
  width: 7px;
  height: 7px;
  border-radius: 999px;
}

.conversation-dot.blue {
  background: #2563eb;
}

.conversation-dot.green {
  background: #10a37f;
}

.conversation-dot.amber {
  background: #f59e0b;
}

.sidebar-footer {
  position: relative;
  margin-top: auto;
}

.account-trigger {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr) auto;
  gap: 9px;
  min-height: 48px;
  padding: 7px 8px;
  color: #202123;
  text-align: left;
  background: transparent;
}

.avatar-token {
  width: 34px;
  height: 34px;
  color: #ffffff;
  background:
    radial-gradient(circle at 35% 25%, rgba(255, 255, 255, 0.38), transparent 32%),
    #10a37f;
  border-radius: 999px;
}

.account-menu {
  position: absolute;
  right: 0;
  bottom: calc(100% + 8px);
  left: 0;
  z-index: 10;
  display: grid;
  gap: 2px;
  padding: 6px;
  background: #ffffff;
  border: 1px solid #ececf1;
  border-radius: 14px;
  box-shadow: 0 18px 44px rgba(17, 24, 39, 0.16);
  transform-origin: bottom center;
  animation: menu-rise 160ms ease both;
}

.account-menu-item {
  gap: 9px;
  min-height: 44px;
  padding: 8px;
  color: #202123;
  text-align: left;
  background: transparent;
}

.account-menu-item.danger strong,
.account-menu-item.danger svg {
  color: #b42318;
}

.visually-hidden {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0 0 0 0);
  white-space: nowrap;
  border: 0;
}

@keyframes menu-rise {
  from {
    opacity: 0;
    transform: translateY(6px) scale(0.98);
  }

  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@media (max-width: 1220px) {
  .left-sidebar {
    width: 100%;
    min-height: auto;
  }

  .sidebar-scroll-area {
    max-height: 320px;
  }
}

@media (max-width: 760px) {
  .left-sidebar {
    max-width: 100vw;
    padding: 10px;
  }

  .workspace-copy span,
  .quick-action small,
  .sidebar-nav-item small,
  .conversation-item small {
    display: none;
  }
}
</style>
