<script setup lang="ts">
import { computed } from 'vue'
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

const isHomeRoute = computed(() => route.path === '/manager/home')
const isRestaurantRoute = computed(() => route.path.startsWith('/manager/restaurant'))
const currentSection = computed(() => {
  if (isHomeRoute.value) {
    return '管理员首页'
  }

  return isRestaurantRoute.value ? '餐厅管理' : '宿舍管理'
})

const portalItems: readonly NavItem[] = [
  { title: '返回管理员首页', desc: '查看统一后台入口与模块概览', path: '/manager/home' },
  { title: '宿舍总览', desc: '查看宿舍模块的整体低碳表现', path: '/manager/low-carbon-overview' },
]

const dormConfigItems: readonly NavItem[] = [
  { title: '规则配置', desc: '维护积分、荣誉和系数规则', path: '/manager/low-carbon-rule-config' },
  { title: '学生管理', desc: '查看宿舍系统学生档案并执行维护', path: '/manager/student-manage' },
  { title: '新增学生', desc: '录入宿舍系统学生及宿舍信息', path: '/manager/student-create' },
]

const dormOperationItems: readonly NavItem[] = [
  { title: '奖励管理', desc: '维护宿舍奖励库存和兑换配置', path: '/manager/reward-manage' },
  { title: '宿舍水电扣费', desc: '处理宿舍费用扣减与账单操作', path: '/manager/dorm-fee-deduct' },
]

const restaurantItems: readonly NavItem[] = [
  { title: '餐厅总览', desc: '查看学院餐饮碳排放统计汇总', path: '/manager/restaurant/summary' },
  { title: '餐厅学生列表', desc: '查询餐厅系统中的学生与用餐档案', path: '/manager/restaurant/student/list' },
  { title: '新增用餐记录', desc: '录入学生取餐重量和剩余重量', path: '/manager/restaurant/student/add' },
]

const restaurantReportItems: readonly NavItem[] = [
  { title: '每日餐饮记录', desc: '按天查看学生餐饮碳排放数据', path: '/manager/restaurant/student/meal/daliy' },
  { title: '每周餐饮记录', desc: '按周查看学生餐饮碳排放数据', path: '/manager/restaurant/student/meal/week' },
  { title: '每月餐饮记录', desc: '按月查看学生餐饮碳排放数据', path: '/manager/restaurant/student/meal/month' },
  { title: '学期餐饮记录', desc: '按学期汇总学生餐饮碳排放数据', path: '/manager/restaurant/student/meal/term' },
  { title: '年度餐饮记录', desc: '按年度汇总学生餐饮碳排放数据', path: '/manager/restaurant/student/meal/year' },
  { title: '每周碳排趋势', desc: '查看每周餐饮碳排放折线趋势', path: '/manager/restaurant/student/period/week' },
  { title: '每月碳排趋势', desc: '查看每月餐饮碳排放折线趋势', path: '/manager/restaurant/student/period/month' },
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
      <button type="button" class="brand" @click="goTo('/manager/home')">
        <span class="brand__icon">LC</span>
        <span class="brand__text">
          <strong>低碳校园</strong>
          <small>统一管理后台</small>
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
          <p class="menu-group__label">统一入口</p>
          <button
            v-for="item in portalItems"
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
          <p class="menu-group__label">宿舍配置</p>
          <button
            v-for="item in dormConfigItems"
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
          <p class="menu-group__label">宿舍运营</p>
          <button
            v-for="item in dormOperationItems"
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
          <p class="menu-group__label">餐厅管理</p>
          <button
            v-for="item in restaurantItems"
            :key="item.path"
            type="button"
            class="nav-card nav-card--restaurant"
            :class="{ 'nav-card--active': isActive(item.path), 'nav-card--pending': isPending(item.path) }"
            @click="goTo(item.path)"
          >
            <span class="nav-card__title">{{ item.title }}</span>
            <small class="nav-card__desc">{{ item.desc }}</small>
          </button>
        </section>

        <section class="menu-group">
          <p class="menu-group__label">餐厅报表</p>
          <button
            v-for="item in restaurantReportItems"
            :key="item.path"
            type="button"
            class="nav-card nav-card--sub nav-card--restaurant-sub"
            :class="{ 'nav-card--active': isActive(item.path), 'nav-card--pending': isPending(item.path) }"
            @click="goTo(item.path)"
          >
            <span class="nav-card__title">{{ item.title }}</span>
            <small class="nav-card__desc">{{ item.desc }}</small>
          </button>
        </section>
      </nav>

      <div class="footer-card">
        <strong>统一管理后台</strong>
        <span>{{ isHomeRoute ? '从这里进入宿舍管理与餐厅管理两个业务模块。' : `当前正在使用${currentSection}模块。` }}</span>
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
  --sidebar-text: #204232;
  --sidebar-muted: #698275;
  --sidebar-line: rgba(93, 131, 109, 0.12);
  position: relative;
  height: calc(100vh - 32px);
  height: calc(100dvh - 32px);
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr) auto;
  gap: 16px;
  padding: 22px 18px;
  border: 1px solid rgba(39, 82, 61, 0.08);
  border-radius: 30px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(244, 250, 246, 0.96)),
    repeating-linear-gradient(
      160deg,
      rgba(255, 255, 255, 0.16) 0,
      rgba(255, 255, 255, 0.16) 14px,
      rgba(242, 248, 244, 0.14) 14px,
      rgba(242, 248, 244, 0.14) 28px
    );
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.74),
    0 20px 46px rgba(31, 63, 47, 0.08);
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
  opacity: 0.34;
}

.sidebar__inner::before {
  top: -24px;
  right: -40px;
  width: 170px;
  aspect-ratio: 1;
  background: rgba(132, 199, 164, 0.38);
}

.sidebar__inner::after {
  left: -52px;
  bottom: 100px;
  width: 140px;
  aspect-ratio: 1;
  background: rgba(231, 217, 165, 0.28);
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
    linear-gradient(135deg, #1d744d 0%, #133d2b 100%);
  color: #fff;
  font-size: 20px;
  font-weight: 900;
  box-shadow: 0 14px 28px rgba(29, 90, 60, 0.22);
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
    radial-gradient(circle at top right, rgba(171, 220, 193, 0.24), transparent 36%),
    linear-gradient(150deg, rgba(239, 247, 242, 0.98), rgba(252, 253, 252, 0.96));
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
  color: #2f5f47;
  font-size: 12px;
  font-weight: 800;
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
  background: rgba(47, 143, 104, 0.22);
}

.menu-group {
  display: grid;
  gap: 10px;
}

.menu-group__label {
  margin: 0;
  color: #5f786b;
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
  border: 1px solid rgba(214, 229, 220, 0.96);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.78);
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

.nav-card--restaurant {
  border-color: rgba(160, 145, 95, 0.16);
  background:
    radial-gradient(circle at right top, rgba(231, 217, 165, 0.18), transparent 34%),
    rgba(255, 255, 255, 0.82);
}

.nav-card--restaurant-sub {
  border-color: rgba(160, 145, 95, 0.14);
}

.nav-card:hover {
  transform: translateY(-1px);
  border-color: rgba(127, 171, 145, 0.42);
  background:
    radial-gradient(circle at right top, rgba(178, 222, 197, 0.2), transparent 34%),
    rgba(255, 255, 255, 0.88);
}

.nav-card--active {
  border-color: rgba(127, 171, 145, 0.42);
  background:
    radial-gradient(circle at right top, rgba(178, 222, 197, 0.28), transparent 32%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 251, 249, 0.96));
  box-shadow:
    inset 0 0 0 1px rgba(235, 244, 239, 0.9),
    0 12px 26px rgba(13, 36, 25, 0.1);
}

.nav-card--active::after {
  background: linear-gradient(180deg, #2f8f68, #7cc39f);
}

.nav-card--pending {
  border-color: rgba(83, 153, 114, 0.42);
  box-shadow:
    inset 0 0 0 1px rgba(235, 244, 239, 0.92),
    0 14px 28px rgba(29, 75, 52, 0.1);
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
  border: 1px solid rgba(214, 229, 220, 0.96);
  border-radius: 22px;
  background:
    radial-gradient(circle at right top, rgba(167, 220, 192, 0.18), transparent 36%),
    rgba(255, 255, 255, 0.84);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.6);
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
  border: 1px solid rgba(214, 229, 220, 0.96);
  border-radius: 14px;
  background: linear-gradient(180deg, #fbfdfb, #f2f7f4);
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
  box-shadow: 0 12px 24px rgba(29, 75, 52, 0.08);
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
