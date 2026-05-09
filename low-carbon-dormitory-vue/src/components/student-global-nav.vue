<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useAdminTokenStore } from '@/stores/admin-token'
import { useStudentTokenStore } from '@/stores/student-token'
import { redirectToUnifiedLogin } from '@/utils/unified-login'

const router = useRouter()
const route = useRoute()
const adminTokenStore = useAdminTokenStore()
const studentTokenStore = useStudentTokenStore()
const { studentInfo, dormLabel } = storeToRefs(studentTokenStore)

const expandedGroups = ref({
  lowCarbon:
    route.path.startsWith('/low-carbon-dashboard') ||
    route.path.startsWith('/reward-exchange') ||
    route.path.startsWith('/low-carbon-rule-readonly'),
})

const topNavItems = [
  { title: '学生首页', desc: '返回宿舍学生端首页', path: '/index-student' },
  { title: '个人信息', desc: '查看学生档案和宿舍信息', path: '/personal-info' },
] as const

const billingItems = [{ title: '宿舍水电费', desc: '查看余额、用量和账单', path: '/water-electricity' }] as const

const lowCarbonItems = [
  { title: '个人低碳看板', desc: '查看个人和宿舍低碳表现', path: '/low-carbon-dashboard' },
  { title: '奖励兑换', desc: '浏览积分奖励并发起兑换', path: '/reward-exchange' },
  { title: '低碳规则', desc: '查看积分和荣誉规则', path: '/low-carbon-rule-readonly' },
] as const

const studentName = computed(() => studentInfo.value?.name || '同学')
const dormLabelText = computed(() => (dormLabel.value && dormLabel.value !== '-' ? dormLabel.value : '暂未识别宿舍'))

function goTo(path: string) {
  if (
    path.startsWith('/low-carbon-dashboard') ||
    path.startsWith('/reward-exchange') ||
    path.startsWith('/low-carbon-rule-readonly')
  ) {
    expandedGroups.value.lowCarbon = true
  }
  if (route.path === path) return
  router.push(path)
}

function isActive(path: string) {
  return route.path === path || route.path.startsWith(`${path}/`)
}

function isGroupActive(paths: readonly string[]) {
  return paths.some((path) => route.path.startsWith(path))
}

function toggleGroup(group: 'lowCarbon') {
  expandedGroups.value[group] = !expandedGroups.value[group]
}

function logout() {
  adminTokenStore.clearAdminToken()
  studentTokenStore.clearStudentToken()
  localStorage.removeItem('loginUser')
  redirectToUnifiedLogin(true, { role: 'student' })
}
</script>

<template>
  <aside class="app-sidebar">
    <div class="app-sidebar__inner">
      <button type="button" class="brand" @click="goTo('/index-student')">
        <span class="brand__mark">LC</span>
        <span class="brand__copy">
          <strong>低碳校园</strong>
          <small>学生宿舍端导航</small>
        </span>
      </button>

      <nav class="nav-section">
        <button
          v-for="item in topNavItems"
          :key="item.path"
          type="button"
          class="nav-item"
          :class="{ 'nav-item--active': isActive(item.path) }"
          @click="goTo(item.path)"
        >
          <span class="nav-item__title">{{ item.title }}</span>
          <small class="nav-item__desc">{{ item.desc }}</small>
        </button>

        <button
          v-for="item in billingItems"
          :key="item.path"
          type="button"
          class="nav-item"
          :class="{ 'nav-item--active': isActive(item.path) }"
          @click="goTo(item.path)"
        >
          <span class="nav-item__title">{{ item.title }}</span>
          <small class="nav-item__desc">{{ item.desc }}</small>
        </button>

        <section class="nav-group" :class="{ 'nav-group--open': expandedGroups.lowCarbon }">
          <button
            type="button"
            class="nav-group__toggle"
            :class="{ 'nav-group__toggle--active': isGroupActive(['/low-carbon-dashboard', '/reward-exchange', '/low-carbon-rule-readonly']) }"
            @click="toggleGroup('lowCarbon')"
          >
            <span>
              <strong>低碳服务</strong>
              <small>看板、规则与兑换</small>
            </span>
            <b>{{ expandedGroups.lowCarbon ? '收起' : '展开' }}</b>
          </button>

          <div v-show="expandedGroups.lowCarbon" class="nav-group__items">
            <button
              v-for="item in lowCarbonItems"
              :key="item.path"
              type="button"
              class="nav-item nav-item--child"
              :class="{ 'nav-item--active': isActive(item.path) }"
              @click="goTo(item.path)"
            >
              <span class="nav-item__title">{{ item.title }}</span>
              <small class="nav-item__desc">{{ item.desc }}</small>
            </button>
          </div>
        </section>
      </nav>

      <div class="account-card">
        <strong>{{ studentName }}</strong>
        <span>{{ dormLabelText }}</span>
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
  background:
    radial-gradient(circle at 12% 5%, rgba(123, 196, 155, 0.12), transparent 24%),
    linear-gradient(180deg, #edf6f0 0%, #f8fbf9 100%);
  z-index: 20;
}

.app-sidebar__inner {
  height: calc(100vh - 36px);
  height: calc(100dvh - 36px);
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  gap: 18px;
  padding: 26px 22px;
  border: 1px solid rgba(39, 82, 61, 0.1);
  border-radius: 24px;
  background:
    radial-gradient(circle at top right, rgba(123, 196, 155, 0.18), transparent 30%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(247, 251, 249, 0.92));
  box-shadow: 0 20px 48px rgba(31, 63, 47, 0.08);
  backdrop-filter: blur(16px);
  overflow: hidden;
}

.brand {
  display: grid;
  justify-items: start;
  gap: 14px;
  border: none;
  background: transparent;
  color: #244536;
  text-align: left;
  cursor: pointer;
  padding: 0;
}

.brand__mark {
  width: 52px;
  height: 52px;
  border-radius: 16px;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #1a6f49, #133c2a);
  color: #ffffff;
  font-size: 20px;
  font-weight: 900;
}

.brand__copy {
  display: grid;
  gap: 8px;
}

.brand__copy strong {
  font-size: 22px;
  line-height: 1.2;
}

.brand__copy small {
  color: #5f776b;
  font-size: 14px;
  font-weight: 700;
}

.nav-section {
  min-height: 0;
  display: grid;
  align-content: start;
  gap: 14px;
  padding-right: 8px;
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

.nav-group {
  display: grid;
  gap: 10px;
}

.nav-group__toggle,
.nav-item {
  width: 100%;
  text-align: left;
  border: 1px solid #d6e5dc;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.72);
  color: #244536;
  cursor: pointer;
}

.nav-item {
  min-height: 78px;
  display: grid;
  align-content: center;
  gap: 8px;
  padding: 14px 16px;
}

.nav-group__toggle {
  min-height: 86px;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  padding: 16px;
}

.nav-group__toggle span {
  display: grid;
  gap: 8px;
}

.nav-group__toggle strong,
.nav-item__title {
  font-size: 16px;
  font-weight: 900;
}

.nav-group__toggle small,
.nav-item__desc {
  color: #5f776b;
  font-size: 13px;
  font-weight: 700;
}

.nav-group__toggle b {
  color: #6b8378;
  font-size: 12px;
}

.nav-item--active,
.nav-group__toggle--active {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 251, 249, 0.96));
  box-shadow:
    inset 0 0 0 1px rgba(235, 244, 239, 0.9),
    0 10px 24px rgba(13, 36, 25, 0.14);
}

.nav-group__items {
  display: grid;
  gap: 10px;
  padding-left: 8px;
}

.nav-item--child {
  min-height: 72px;
}

.account-card {
  display: grid;
  gap: 8px;
  padding: 16px;
  margin-top: auto;
  border-radius: 18px;
  border: 1px solid #d6e5dc;
  background: rgba(255, 255, 255, 0.76);
  color: #244536;
  box-shadow: 0 -1px 0 rgba(214, 229, 220, 0.95);
}

.account-card span {
  color: #5f776b;
  font-size: 13px;
  font-weight: 700;
}

.account-card__logout {
  margin-top: 6px;
  min-height: 40px;
  border: 1px solid #d6e5dc;
  border-radius: 12px;
  background: #f9fcfa;
  color: #244536;
  font-size: 14px;
  font-weight: 800;
  cursor: pointer;
}
</style>
