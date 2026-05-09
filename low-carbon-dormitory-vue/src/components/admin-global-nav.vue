<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useStudentTokenStore } from '@/stores/student-token'
import { useAdminTokenStore } from '@/stores/admin-token'
import { redirectToUnifiedLogin } from '@/utils/unified-login'

const router = useRouter()
const route = useRoute()
const studentTokenStore = useStudentTokenStore()
const adminStore = useAdminTokenStore()

const isHomeRoute = computed(() => route.path === '/manager/home')
const currentSection = computed(() => (isHomeRoute.value ? '管理首页' : '宿舍管理'))

const homeItems = [
  { title: '返回管理首页', desc: '回到宿舍项目管理入口', path: '/manager/home' },
  { title: '宿舍总览', desc: '查看宿舍整体低碳表现', path: '/manager/low-carbon-overview' },
] as const

const configItems = [
  { title: '规则配置', desc: '维护积分、荣誉和系数规则', path: '/manager/low-carbon-rule-config' },
  { title: '新增学生', desc: '录入学生及宿舍基础信息', path: '/manager/student-create' },
] as const

const operationItems = [
  { title: '奖励管理', desc: '维护奖励库存和兑换配置', path: '/manager/reward-manage' },
  { title: '宿舍水电扣费', desc: '处理宿舍费用扣减', path: '/manager/dorm-fee-deduct' },
] as const

function goTo(path: string) {
  if (route.path !== path) {
    router.push(path)
  }
}

function isActive(path: string) {
  return route.path === path || route.path.startsWith(`${path}/`)
}

function logout() {
  studentTokenStore.clearStudentToken()
  adminStore.clearAdminToken()
  localStorage.removeItem('loginUser')
  redirectToUnifiedLogin(true, { role: 'admin' })
}
</script>

<template>
  <aside class="sidebar">
    <div class="sidebar__inner">
      <button type="button" class="brand" @click="goTo('/manager/home')">
        <span class="brand__icon">LC</span>
        <span class="brand__text">
          <strong>低碳宿舍</strong>
          <small>管理端导航</small>
        </span>
      </button>

      <div class="switch-panel">
        <div class="section-chip">
          <span>当前分区</span>
          <strong>{{ currentSection }}</strong>
        </div>
      </div>

      <nav class="menu">
        <section class="menu-group">
          <p class="menu-group__label">管理导航</p>
          <button
            v-for="item in homeItems"
            :key="item.path"
            type="button"
            class="nav-card"
            :class="{ 'nav-card--active': isActive(item.path) }"
            @click="goTo(item.path)"
          >
            <span class="nav-card__title">{{ item.title }}</span>
            <small class="nav-card__desc">{{ item.desc }}</small>
          </button>
        </section>

        <section class="menu-group">
          <p class="menu-group__label">基础配置</p>
          <button
            v-for="item in configItems"
            :key="item.path"
            type="button"
            class="nav-card nav-card--sub"
            :class="{ 'nav-card--active': isActive(item.path) }"
            @click="goTo(item.path)"
          >
            <span class="nav-card__title">{{ item.title }}</span>
            <small class="nav-card__desc">{{ item.desc }}</small>
          </button>
        </section>

        <section class="menu-group">
          <p class="menu-group__label">运营管理</p>
          <button
            v-for="item in operationItems"
            :key="item.path"
            type="button"
            class="nav-card nav-card--sub"
            :class="{ 'nav-card--active': isActive(item.path) }"
            @click="goTo(item.path)"
          >
            <span class="nav-card__title">{{ item.title }}</span>
            <small class="nav-card__desc">{{ item.desc }}</small>
          </button>
        </section>
      </nav>

      <div class="footer-card">
        <strong>低碳宿舍管理</strong>
        <span>{{ isHomeRoute ? '从这里进入宿舍管理功能' : '当前仅保留宿舍项目菜单' }}</span>
        <button type="button" class="footer-card__btn" @click="logout">退出登录</button>
      </div>
    </div>
  </aside>
</template>

<style scoped>
.sidebar {
  position: sticky;
  top: 0;
  height: 100vh;
  height: 100dvh;
  padding: 16px 0 16px 16px;
  background: #eef2f7;
  z-index: 20;
}

.sidebar__inner {
  height: calc(100vh - 32px);
  height: calc(100dvh - 32px);
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr) auto;
  gap: 16px;
  padding: 22px 18px;
  border: 1px solid #e2e8f0;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 14px 36px rgba(15, 23, 42, 0.08);
  overflow: hidden;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0;
  border: none;
  background: transparent;
  color: #0f172a;
  text-align: left;
  cursor: pointer;
}

.brand__icon {
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  border-radius: 14px;
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
  color: #fff;
  font-size: 16px;
  font-weight: 900;
  box-shadow: 0 12px 24px rgba(37, 99, 235, 0.22);
}

.brand__text {
  display: grid;
  gap: 3px;
}

.brand__text strong {
  font-size: 16px;
  line-height: 1.2;
}

.brand__text small {
  color: #64748b;
  font-size: 12px;
}

.switch-panel {
  display: grid;
  gap: 10px;
  padding: 12px;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  background: #f8fafc;
}

.section-chip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.section-chip span {
  color: #64748b;
  font-size: 11px;
  font-weight: 700;
}

.section-chip strong {
  color: #0f172a;
  font-size: 13px;
  font-weight: 800;
}

.menu {
  min-height: 0;
  display: grid;
  gap: 14px;
  padding-right: 4px;
  padding-bottom: 8px;
  overflow-y: auto;
  overscroll-behavior: contain;
  scrollbar-gutter: stable;
}

.menu::-webkit-scrollbar {
  width: 6px;
}

.menu::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 999px;
}

.menu-group {
  display: grid;
  gap: 8px;
}

.menu-group__label {
  margin: 0;
  color: #64748b;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.nav-card {
  width: 100%;
  display: grid;
  gap: 4px;
  min-height: 56px;
  padding: 12px 14px;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  background: #fff;
  color: #0f172a;
  text-align: left;
  cursor: pointer;
}

.nav-card--sub {
  min-height: 50px;
  padding: 10px 14px;
}

.nav-card:hover {
  border-color: #bfdbfe;
  background: #f8fbff;
}

.nav-card--active {
  border-color: #93c5fd;
  background: #eff6ff;
  box-shadow: inset 3px 0 0 #2563eb;
}

.nav-card__title {
  font-size: 13px;
  font-weight: 700;
}

.nav-card__desc {
  color: #64748b;
  font-size: 12px;
  line-height: 1.4;
}

.footer-card {
  display: grid;
  gap: 6px;
  padding: 12px;
  margin-top: auto;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  background: #f8fafc;
  box-shadow: 0 -1px 0 rgba(226, 232, 240, 0.95);
}

.footer-card strong {
  font-size: 13px;
  color: #0f172a;
}

.footer-card span {
  color: #64748b;
  font-size: 11px;
}

.footer-card__btn {
  min-height: 38px;
  margin-top: 4px;
  border: 1px solid #dbe3ee;
  border-radius: 10px;
  background: #fff;
  color: #0f172a;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}

@media (max-width: 640px) {
  .sidebar {
    position: static;
    height: auto;
    padding: 0;
    background: transparent;
  }

  .sidebar__inner {
    height: auto;
  }
}
</style>
