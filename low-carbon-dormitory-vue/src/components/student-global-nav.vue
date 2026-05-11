<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useAdminTokenStore } from '@/stores/admin-token'
import { useStudentTokenStore } from '@/stores/student-token'
import { useRouteTransitionState } from '@/router/route-transition-state'

type NavItem = {
  title: string
  desc: string
  path: string
  code: string
}

const router = useRouter()
const route = useRoute()
const adminTokenStore = useAdminTokenStore()
const studentTokenStore = useStudentTokenStore()
const { studentInfo, dormLabel } = storeToRefs(studentTokenStore)
const { isRouteNavigating, pendingRoutePath } = useRouteTransitionState()

const expandedGroups = ref({
  lowCarbon:
    route.path.startsWith('/low-carbon-dashboard') ||
    route.path.startsWith('/reward-exchange') ||
    route.path.startsWith('/low-carbon-rule-readonly'),
})

const topNavItems: readonly NavItem[] = [
  { title: '学生首页', desc: '返回宿舍学生端首页', path: '/index-student', code: '01' },
  { title: '个人信息', desc: '查看学生档案和宿舍信息', path: '/personal-info', code: '02' },
]

const billingItems: readonly NavItem[] = [
  { title: '宿舍水电费', desc: '查看余额、用量和账单', path: '/water-electricity', code: '03' },
]

const lowCarbonItems: readonly NavItem[] = [
  {
    title: '个人低碳看板',
    desc: '查看个人与宿舍低碳表现',
    path: '/low-carbon-dashboard',
    code: '04',
  },
  {
    title: '奖励兑换',
    desc: '浏览积分奖励并发起兑换',
    path: '/reward-exchange',
    code: '05',
  },
  {
    title: '低碳规则',
    desc: '查看积分与荣誉规则',
    path: '/low-carbon-rule-readonly',
    code: '06',
  },
] as const

const allNavItems = [...topNavItems, ...billingItems, ...lowCarbonItems]

const studentName = computed(() => studentInfo.value?.name || '同学')
const dormLabelText = computed(() =>
  dormLabel.value && dormLabel.value !== '-' ? dormLabel.value : '暂未识别宿舍',
)
const carbonScoreText = computed(() => {
  const score = studentInfo.value?.carbonScore
  return score === null || score === undefined ? '待同步' : `${score} 分`
})
const serviceCount = allNavItems.length
const currentNavTitle = computed(() => {
  const matched = allNavItems.find((item) => route.path === item.path || route.path.startsWith(`${item.path}/`))
  return matched?.title || '学生服务'
})

function goTo(path: string) {
  if (
    path.startsWith('/low-carbon-dashboard') ||
    path.startsWith('/reward-exchange') ||
    path.startsWith('/low-carbon-rule-readonly')
  ) {
    expandedGroups.value.lowCarbon = true
  }

  if (route.path === path) {
    return
  }

  router.push(path)
}

function isActive(path: string) {
  return route.path === path || route.path.startsWith(`${path}/`)
}

function isPending(path: string) {
  return isRouteNavigating.value && pendingRoutePath.value === path
}

function isGroupActive(paths: readonly string[]) {
  return paths.some((path) => route.path.startsWith(path))
}

function toggleGroup(group: 'lowCarbon') {
  expandedGroups.value[group] = !expandedGroups.value[group]
}

function openLowCarbonHome() {
  expandedGroups.value.lowCarbon = true
  goTo('/low-carbon-dashboard')
}

async function logout() {
  adminTokenStore.clearAdminToken()
  studentTokenStore.clearStudentToken()
  localStorage.removeItem('loginUser')
  await router.replace('/login')
}
</script>

<template>
  <aside class="app-sidebar">
    <div class="app-sidebar__inner">
      <div class="app-sidebar__veil app-sidebar__veil--top"></div>
      <div class="app-sidebar__veil app-sidebar__veil--bottom"></div>

      <button type="button" class="brand" @click="goTo('/index-student')">
        <span class="brand__mark">
          <span class="brand__mark-core">LC</span>
        </span>
        <span class="brand__copy">
          <small>Low Carbon Dormitory</small>
          <strong>低碳校园</strong>
        </span>
      </button>

      <section class="overview-card">
        <div class="overview-card__head">
          <span class="overview-card__eyebrow">当前入住</span>
          <b>{{ currentNavTitle }}</b>
        </div>

        <strong class="overview-card__dorm">{{ dormLabelText }}</strong>

        <div class="overview-card__grid">
          <article class="overview-stat">
            <span>身份</span>
            <strong>{{ studentName }}</strong>
          </article>
          <article class="overview-stat">
            <span>个人积分</span>
            <strong>{{ carbonScoreText }}</strong>
          </article>
        </div>

        <div class="overview-card__foot">
          <span>{{ serviceCount }} 项服务可用</span>
          <i></i>
        </div>
      </section>

      <nav class="nav-section">
        <section class="nav-block">
          <div class="nav-block__header">
            <p class="nav-block__label">门户入口</p>
            <span class="nav-block__hint">{{ topNavItems.length }} 项</span>
          </div>

          <button
            v-for="item in topNavItems"
            :key="item.path"
            type="button"
            class="nav-item"
            :class="{ 'nav-item--active': isActive(item.path), 'nav-item--pending': isPending(item.path) }"
            @click="goTo(item.path)"
          >
            <span class="nav-item__code">{{ item.code }}</span>
            <span class="nav-item__main">
              <span class="nav-item__title">{{ item.title }}</span>
              <small class="nav-item__desc">{{ item.desc }}</small>
            </span>
          </button>
        </section>

        <section class="nav-block">
          <div class="nav-block__header">
            <p class="nav-block__label">账务服务</p>
            <span class="nav-block__hint">{{ billingItems.length }} 项</span>
          </div>

          <button
            v-for="item in billingItems"
            :key="item.path"
            type="button"
            class="nav-item"
            :class="{ 'nav-item--active': isActive(item.path) }"
            @click="goTo(item.path)"
          >
            <span class="nav-item__code">{{ item.code }}</span>
            <span class="nav-item__main">
              <span class="nav-item__title">{{ item.title }}</span>
              <small class="nav-item__desc">{{ item.desc }}</small>
            </span>
          </button>
        </section>

        <section class="nav-block nav-group">
          <div class="nav-block__header">
            <p class="nav-block__label">低碳服务</p>
            <span class="nav-block__hint">{{ lowCarbonItems.length }} 项</span>
          </div>

          <div class="nav-group__row">
            <button
              type="button"
              class="nav-group__entry"
              :class="{
                'nav-group__entry--active': isGroupActive([
                  '/low-carbon-dashboard',
                  '/reward-exchange',
                  '/low-carbon-rule-readonly',
                ]),
              }"
              @click="openLowCarbonHome"
            >
              <span class="nav-group__toggle-main">
                <strong>低碳服务</strong>
                <small>点击进入低碳看板，再展开查看全部入口</small>
              </span>
            </button>

            <button type="button" class="nav-group__switch" @click="toggleGroup('lowCarbon')">
              {{ expandedGroups.lowCarbon ? '收起' : '展开' }}
            </button>
          </div>

          <div v-show="expandedGroups.lowCarbon" class="nav-group__items">
            <button
              v-for="item in lowCarbonItems"
              :key="item.path"
              type="button"
              class="nav-item nav-item--child"
              :class="{ 'nav-item--active': isActive(item.path), 'nav-item--pending': isPending(item.path) }"
              @click="goTo(item.path)"
            >
              <span class="nav-item__code">{{ item.code }}</span>
              <span class="nav-item__main">
                <span class="nav-item__title">{{ item.title }}</span>
                <small class="nav-item__desc">{{ item.desc }}</small>
              </span>
            </button>
          </div>
        </section>
      </nav>

      <div class="account-card">
        <div class="account-card__meta">
          <strong>{{ studentName }}</strong>
          <span>{{ dormLabelText }}</span>
        </div>
        <button type="button" class="account-card__logout" @click="logout">退出登录</button>
      </div>
    </div>
  </aside>
</template>

<style scoped>
.app-sidebar {
  position: sticky;
  top: 0;
  height: 100vh;
  height: 100dvh;
  padding: 18px 0 18px 18px;
  z-index: 20;
}

.app-sidebar__inner {
  --sidebar-text: #204232;
  --sidebar-muted: #698275;
  --sidebar-line: rgba(93, 131, 109, 0.12);
  --sidebar-line-strong: rgba(93, 131, 109, 0.22);
  position: relative;
  height: calc(100vh - 36px);
  height: calc(100dvh - 36px);
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr) auto;
  gap: 14px;
  padding: 20px 18px 18px;
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

.app-sidebar__veil {
  position: absolute;
  border-radius: 999px;
  pointer-events: none;
  filter: blur(46px);
  opacity: 0.34;
}

.app-sidebar__veil--top {
  top: -24px;
  right: -40px;
  width: 170px;
  aspect-ratio: 1;
  background: rgba(132, 199, 164, 0.38);
}

.app-sidebar__veil--bottom {
  left: -52px;
  bottom: 100px;
  width: 140px;
  aspect-ratio: 1;
  background: rgba(231, 217, 165, 0.28);
}

.brand,
.overview-card,
.nav-section,
.account-card {
  position: relative;
  z-index: 1;
}

.brand {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  gap: 12px;
  padding: 0;
  border: none;
  background: transparent;
  color: var(--sidebar-text);
  text-align: left;
  cursor: pointer;
}

.brand__mark {
  width: 52px;
  height: 52px;
  border-radius: 18px;
  display: grid;
  place-items: center;
  background:
    radial-gradient(circle at 28% 28%, rgba(255, 255, 255, 0.24), transparent 30%),
    linear-gradient(135deg, #1d744d 0%, #133d2b 100%);
  box-shadow: 0 14px 28px rgba(29, 90, 60, 0.22);
}

.brand__mark-core {
  color: #fff;
  font-size: 20px;
  font-weight: 900;
  letter-spacing: 0.02em;
}

.brand__copy {
  display: grid;
  gap: 4px;
}

.brand__copy small {
  color: var(--sidebar-muted);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.brand__copy strong {
  color: var(--sidebar-text);
  font-size: 25px;
  line-height: 1.06;
}

.overview-card {
  display: grid;
  gap: 14px;
  padding: 16px;
  border-radius: 24px;
  border: 1px solid var(--sidebar-line);
  background:
    radial-gradient(circle at top right, rgba(171, 220, 193, 0.24), transparent 36%),
    linear-gradient(150deg, rgba(239, 247, 242, 0.98), rgba(252, 253, 252, 0.96));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.62);
}

.overview-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.overview-card__eyebrow {
  color: var(--sidebar-muted);
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.overview-card__head b {
  color: #2f5f47;
  font-size: 12px;
}

.overview-card__dorm {
  color: var(--sidebar-text);
  font-size: 28px;
  line-height: 1;
  letter-spacing: -0.04em;
}

.overview-card__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.overview-stat {
  display: grid;
  gap: 6px;
  padding: 10px 12px;
  border-radius: 18px;
  border: 1px solid rgba(214, 229, 220, 0.96);
  background: rgba(255, 255, 255, 0.76);
}

.overview-stat span {
  color: var(--sidebar-muted);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.overview-stat strong {
  color: var(--sidebar-text);
  font-size: 15px;
  line-height: 1.2;
}

.overview-card__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: #5f786b;
  font-size: 12px;
  font-weight: 700;
}

.overview-card__foot i {
  width: 34px;
  height: 8px;
  border-radius: 999px;
  background: linear-gradient(90deg, #2f8f68, #d9e8b2);
}

.nav-section {
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

.nav-section::-webkit-scrollbar {
  width: 4px;
}

.nav-section::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgba(47, 143, 104, 0.22);
}

.nav-block {
  display: grid;
  gap: 10px;
}

.nav-block__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.nav-block__label {
  margin: 0;
  color: #5f786b;
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.nav-block__hint {
  color: #7c9287;
  font-size: 11px;
  font-weight: 800;
}

.nav-item,
.nav-group__entry,
.nav-group__switch {
  width: 100%;
  border: 1px solid rgba(214, 229, 220, 0.96);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.78);
  color: var(--sidebar-text);
  cursor: pointer;
  transition:
    transform 0.18s ease,
    border-color 0.18s ease,
    box-shadow 0.18s ease,
    background 0.18s ease;
}

.nav-item {
  position: relative;
  display: grid;
  grid-template-columns: 38px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
  min-height: 68px;
  padding: 12px 14px;
  text-align: left;
}

.nav-item::after {
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

.nav-item__code {
  width: 38px;
  height: 38px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  border: 1px solid rgba(214, 229, 220, 0.96);
  background: #f5faf7;
  color: #5b7668;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.08em;
}

.nav-item__main {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.nav-item__title {
  color: var(--sidebar-text);
  font-size: 15px;
  font-weight: 900;
  line-height: 1.2;
}

.nav-item__desc {
  color: var(--sidebar-muted);
  font-size: 12px;
  line-height: 1.5;
}

.nav-item--child {
  min-height: 62px;
}

.nav-item:hover,
.nav-group__entry:hover,
.nav-group__switch:hover,
.account-card__logout:hover {
  transform: translateY(-1px);
}

.nav-item--active,
.nav-group__entry--active,
.nav-group__switch:hover {
  border-color: rgba(127, 171, 145, 0.42);
  background:
    radial-gradient(circle at right top, rgba(178, 222, 197, 0.28), transparent 32%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 251, 249, 0.96));
  box-shadow:
    inset 0 0 0 1px rgba(235, 244, 239, 0.9),
    0 12px 26px rgba(13, 36, 25, 0.1);
}

.nav-item--active::after {
  background: linear-gradient(180deg, #2f8f68, #7cc39f);
}

.nav-item--active .nav-item__code {
  border-color: rgba(95, 153, 120, 0.24);
  background: linear-gradient(180deg, #edf7f1, #f8fbf9);
  color: #27543d;
}

.nav-item--pending {
  border-color: rgba(83, 153, 114, 0.42);
  box-shadow:
    inset 0 0 0 1px rgba(235, 244, 239, 0.92),
    0 14px 28px rgba(29, 75, 52, 0.1);
}

.nav-item--pending::before {
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

.nav-group {
  display: grid;
  gap: 10px;
}

.nav-group__row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
}

.nav-group__entry {
  min-height: 70px;
  display: grid;
  align-items: center;
  padding: 14px 16px;
  text-align: left;
}

.nav-group__toggle-main {
  display: grid;
  gap: 5px;
}

.nav-group__toggle-main strong {
  color: var(--sidebar-text);
  font-size: 15px;
  font-weight: 900;
  line-height: 1.2;
}

.nav-group__toggle-main small {
  color: var(--sidebar-muted);
  font-size: 12px;
  line-height: 1.45;
}

.nav-group__switch {
  width: 74px;
  min-height: 70px;
  padding: 0 10px;
  color: #678073;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.04em;
}

.nav-group__entry--active {
  position: relative;
}

.nav-group__entry--active::after {
  content: '';
  position: absolute;
  left: 0;
  top: 14px;
  bottom: 14px;
  width: 3px;
  border-radius: 999px;
  background: linear-gradient(180deg, #2f8f68, #7cc39f);
}

.nav-group__items {
  display: grid;
  gap: 8px;
  padding-left: 12px;
}

@keyframes navPendingSweep {
  0% {
    background-position: 140% 0;
  }

  100% {
    background-position: -40% 0;
  }
}

.account-card {
  display: grid;
  gap: 12px;
  padding: 14px 16px 16px;
  border-radius: 22px;
  border: 1px solid rgba(214, 229, 220, 0.96);
  background:
    radial-gradient(circle at right top, rgba(167, 220, 192, 0.18), transparent 36%),
    rgba(255, 255, 255, 0.84);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.6);
}

.account-card__meta {
  display: grid;
  gap: 6px;
}

.account-card__meta strong {
  color: var(--sidebar-text);
  font-size: 16px;
  line-height: 1.2;
}

.account-card__meta span {
  color: var(--sidebar-muted);
  font-size: 13px;
  line-height: 1.4;
}

.account-card__logout {
  min-height: 42px;
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
    border-color 0.18s ease;
}

.account-card__logout:hover {
  box-shadow: 0 12px 24px rgba(29, 75, 52, 0.08);
}

@media (max-width: 900px) {
  .app-sidebar {
    height: auto;
    padding: 12px 12px 0;
  }

  .app-sidebar__inner {
    height: auto;
    border-radius: 24px;
  }

  .overview-card__dorm {
    font-size: 24px;
  }
}
</style>
