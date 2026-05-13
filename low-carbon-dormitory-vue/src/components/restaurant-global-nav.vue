<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { useStudentTokenStore } from '@/stores/student-token'
import { useAdminTokenStore } from '@/stores/admin-token'
import { useRouteTransitionState } from '@/router/route-transition-state'

type NavItem = {
  title: string
  desc: string
  path: string
}

const router = useRouter()
const route = useRoute()
const studentTokenStore = useStudentTokenStore()
const adminStore = useAdminTokenStore()
const { isRouteNavigating, pendingRoutePath } = useRouteTransitionState()

const overviewItems: readonly NavItem[] = [
  { title: '首页', desc: '查看餐厅碳排放总览与学院汇总', path: '/manager/restaurant/summary' },
  { title: '学生信息列表', desc: '查询餐厅系统中的学生与基础信息', path: '/manager/restaurant/student/list' },
  { title: '学生用餐接口', desc: '录入学生取餐重量与剩余重量', path: '/manager/restaurant/student/add' },
]

const recordItems: readonly NavItem[] = [
  { title: '每天餐饮信息', desc: '按天查看学生餐饮记录', path: '/manager/restaurant/student/meal/daliy' },
  { title: '每周餐饮信息', desc: '按周查看学生餐饮记录', path: '/manager/restaurant/student/meal/week' },
  { title: '每月餐饮信息', desc: '按月查看学生餐饮记录', path: '/manager/restaurant/student/meal/month' },
  { title: '每学期餐饮信息', desc: '按学期汇总餐饮记录', path: '/manager/restaurant/student/meal/term' },
  { title: '每年餐饮信息', desc: '按年度汇总餐饮记录', path: '/manager/restaurant/student/meal/year' },
]

const trendItems: readonly NavItem[] = [
  { title: '每周碳排放信息', desc: '查看每周碳排放折线趋势', path: '/manager/restaurant/student/period/week' },
  { title: '每月碳排放信息', desc: '查看每月碳排放折线趋势', path: '/manager/restaurant/student/period/month' },
]

function goTo(path: string) {
  if (route.path !== path) {
    router.push(path)
  }
}

function isActive(path: string) {
  return route.path === path || route.path.startsWith(`${path}/`)
}

function isPending(path: string) {
  return isRouteNavigating.value && pendingRoutePath.value === path
}

async function logout() {
  studentTokenStore.clearStudentToken()
  adminStore.clearAdminToken()
  localStorage.removeItem('loginUser')
  await router.replace('/login')
}
</script>

<template>
  <aside class="sidebar">
    <div class="sidebar__inner">
      <button type="button" class="brand" @click="goTo('/manager/restaurant/summary')">
        <span class="brand__icon">RC</span>
        <span class="brand__text">
          <strong>餐厅管理</strong>
          <small>餐厅系统导航</small>
        </span>
      </button>

      <div class="switch-panel">
        <div class="section-chip">
          <span>当前分区</span>
          <strong>餐厅管理</strong>
        </div>
        <button type="button" class="switch-card" @click="goTo('/manager/low-carbon-overview')">
          <span class="switch-card__eyebrow">Switch Module</span>
          <strong>返回宿舍管理</strong>
          <small>切回宿舍系统自己的后台导航与页面</small>
        </button>
      </div>

      <nav class="menu">
        <section class="menu-group">
          <p class="menu-group__label">常用入口</p>
          <button
            v-for="item in overviewItems"
            :key="item.path"
            type="button"
            class="nav-card"
            :class="{ 'nav-card--active': isActive(item.path), 'nav-card--pending': isPending(item.path) }"
            @click="goTo(item.path)"
          >
            <span class="nav-card__title">{{ item.title }}</span>
            <small class="nav-card__desc">{{ item.desc }}</small>
          </button>
        </section>

        <section class="menu-group">
          <p class="menu-group__label">餐饮记录表</p>
          <button
            v-for="item in recordItems"
            :key="item.path"
            type="button"
            class="nav-card nav-card--sub"
            :class="{ 'nav-card--active': isActive(item.path), 'nav-card--pending': isPending(item.path) }"
            @click="goTo(item.path)"
          >
            <span class="nav-card__title">{{ item.title }}</span>
            <small class="nav-card__desc">{{ item.desc }}</small>
          </button>
        </section>

        <section class="menu-group">
          <p class="menu-group__label">碳排放趋势</p>
          <button
            v-for="item in trendItems"
            :key="item.path"
            type="button"
            class="nav-card nav-card--sub"
            :class="{ 'nav-card--active': isActive(item.path), 'nav-card--pending': isPending(item.path) }"
            @click="goTo(item.path)"
          >
            <span class="nav-card__title">{{ item.title }}</span>
            <small class="nav-card__desc">{{ item.desc }}</small>
          </button>
        </section>
      </nav>

      <div class="footer-card">
        <strong>餐厅系统</strong>
        <span>当前只显示餐厅系统自己的导航菜单。</span>
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
  z-index: 20;
}

.sidebar__inner {
  --sidebar-text: #5a4314;
  --sidebar-muted: #8a7750;
  --sidebar-line: rgba(160, 145, 95, 0.16);
  position: relative;
  height: calc(100vh - 32px);
  height: calc(100dvh - 32px);
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr) auto;
  gap: 16px;
  padding: 22px 18px;
  border: 1px solid rgba(151, 123, 59, 0.12);
  border-radius: 30px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(251, 248, 240, 0.96)),
    repeating-linear-gradient(
      160deg,
      rgba(255, 255, 255, 0.16) 0,
      rgba(255, 255, 255, 0.16) 14px,
      rgba(250, 246, 236, 0.14) 14px,
      rgba(250, 246, 236, 0.14) 28px
    );
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.74),
    0 20px 46px rgba(92, 70, 19, 0.08);
  backdrop-filter: blur(16px);
  overflow: hidden;
}

.sidebar__inner::before,
.sidebar__inner::after {
  content: '';
  position: absolute;
  border-radius: 999px;
  pointer-events: none;
  filter: blur(46px);
  opacity: 0.32;
}

.sidebar__inner::before {
  top: -24px;
  right: -40px;
  width: 170px;
  aspect-ratio: 1;
  background: rgba(226, 200, 121, 0.36);
}

.sidebar__inner::after {
  left: -52px;
  bottom: 100px;
  width: 140px;
  aspect-ratio: 1;
  background: rgba(205, 164, 89, 0.22);
}

.brand {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0;
  border: none;
  background: transparent;
  color: var(--sidebar-text);
  text-align: left;
  cursor: pointer;
}

.brand__icon {
  display: grid;
  place-items: center;
  width: 52px;
  height: 52px;
  border-radius: 18px;
  background:
    radial-gradient(circle at 28% 28%, rgba(255, 255, 255, 0.24), transparent 30%),
    linear-gradient(135deg, #a67b1f 0%, #7a5912 100%);
  color: #fff;
  font-size: 20px;
  font-weight: 900;
  box-shadow: 0 14px 28px rgba(122, 89, 18, 0.22);
}

.brand__text {
  display: grid;
  gap: 4px;
}

.brand__text strong {
  color: var(--sidebar-text);
  font-size: 25px;
  line-height: 1.06;
}

.brand__text small {
  color: var(--sidebar-muted);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.switch-panel {
  position: relative;
  z-index: 1;
  display: grid;
  gap: 10px;
  padding: 16px;
  border: 1px solid var(--sidebar-line);
  border-radius: 24px;
  background:
    radial-gradient(circle at top right, rgba(231, 217, 165, 0.24), transparent 36%),
    linear-gradient(150deg, rgba(251, 248, 240, 0.98), rgba(255, 253, 248, 0.96));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.62);
}

.section-chip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.section-chip span {
  color: var(--sidebar-muted);
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.section-chip strong {
  color: #7a5b18;
  font-size: 12px;
  font-weight: 800;
}

.switch-card {
  display: grid;
  gap: 6px;
  width: 100%;
  padding: 14px 16px;
  border: 1px solid rgba(160, 145, 95, 0.18);
  border-radius: 18px;
  background:
    radial-gradient(circle at right top, rgba(231, 217, 165, 0.22), transparent 34%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.98), rgba(252, 249, 239, 0.98));
  color: #5a4314;
  text-align: left;
  cursor: pointer;
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease,
    border-color 0.18s ease,
    background 0.18s ease;
}

.switch-card:hover {
  transform: translateY(-1px);
  box-shadow: 0 14px 28px rgba(122, 89, 18, 0.1);
}

.switch-card__eyebrow {
  font-size: 10px;
  font-weight: 900;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: rgba(90, 67, 20, 0.64);
}

.menu {
  position: relative;
  z-index: 1;
  min-height: 0;
  display: grid;
  align-content: start;
  gap: 18px;
  padding-right: 6px;
  padding-bottom: 10px;
  overflow-y: auto;
  overscroll-behavior: contain;
  scrollbar-gutter: stable;
}

.menu::-webkit-scrollbar {
  width: 4px;
}

.menu::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgba(166, 123, 31, 0.22);
}

.menu-group {
  display: grid;
  gap: 10px;
}

.menu-group__label {
  margin: 0;
  color: #8a7750;
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.nav-card {
  position: relative;
  width: 100%;
  display: grid;
  gap: 5px;
  min-height: 68px;
  padding: 14px 16px;
  border: 1px solid rgba(228, 220, 198, 0.96);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.82);
  color: var(--sidebar-text);
  text-align: left;
  cursor: pointer;
  transition:
    transform 0.18s ease,
    border-color 0.18s ease,
    box-shadow 0.18s ease,
    background 0.18s ease;
}

.nav-card::after {
  content: '';
  position: absolute;
  left: 0;
  top: 14px;
  bottom: 14px;
  width: 3px;
  border-radius: 999px;
  background: transparent;
  transition: background 0.18s ease;
}

.nav-card--sub {
  min-height: 62px;
  padding: 12px 16px;
}

.nav-card:hover {
  transform: translateY(-1px);
  border-color: rgba(185, 158, 92, 0.42);
  background:
    radial-gradient(circle at right top, rgba(231, 217, 165, 0.18), transparent 34%),
    rgba(255, 255, 255, 0.9);
}

.nav-card--active {
  border-color: rgba(185, 158, 92, 0.42);
  background:
    radial-gradient(circle at right top, rgba(231, 217, 165, 0.3), transparent 32%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.99), rgba(252, 249, 239, 0.96));
  box-shadow:
    inset 0 0 0 1px rgba(247, 239, 219, 0.9),
    0 12px 26px rgba(122, 89, 18, 0.1);
}

.nav-card--active::after {
  background: linear-gradient(180deg, #a67b1f, #d3af59);
}

.nav-card--pending {
  border-color: rgba(185, 158, 92, 0.42);
  box-shadow:
    inset 0 0 0 1px rgba(247, 239, 219, 0.92),
    0 14px 28px rgba(122, 89, 18, 0.1);
}

.nav-card--pending::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  background:
    linear-gradient(110deg, transparent 18%, rgba(255, 255, 255, 0.72) 50%, transparent 82%);
  background-size: 220% 100%;
  animation: navPendingSweep 0.9s ease infinite;
  pointer-events: none;
}

.nav-card__title {
  color: var(--sidebar-text);
  font-size: 15px;
  font-weight: 900;
  line-height: 1.2;
}

.nav-card__desc {
  color: var(--sidebar-muted);
  font-size: 12px;
  line-height: 1.5;
}

.footer-card {
  position: relative;
  z-index: 1;
  display: grid;
  gap: 12px;
  padding: 14px 16px 16px;
  margin-top: auto;
  border: 1px solid rgba(228, 220, 198, 0.96);
  border-radius: 22px;
  background:
    radial-gradient(circle at right top, rgba(231, 217, 165, 0.18), transparent 36%),
    rgba(255, 255, 255, 0.88);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.62);
}

.footer-card strong {
  font-size: 16px;
  line-height: 1.2;
  color: var(--sidebar-text);
}

.footer-card span {
  color: var(--sidebar-muted);
  font-size: 13px;
  line-height: 1.4;
}

.footer-card__btn {
  min-height: 42px;
  margin-top: 2px;
  border: 1px solid rgba(228, 220, 198, 0.96);
  border-radius: 14px;
  background: linear-gradient(180deg, #fffdf9, #f8f2e4);
  color: var(--sidebar-text);
  font-size: 14px;
  font-weight: 800;
  cursor: pointer;
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease,
    border-color 0.18s ease,
    background 0.18s ease;
}

.footer-card__btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 12px 24px rgba(122, 89, 18, 0.08);
}

@keyframes navPendingSweep {
  0% {
    background-position: 140% 0;
  }

  100% {
    background-position: -40% 0;
  }
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
