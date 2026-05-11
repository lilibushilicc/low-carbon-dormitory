<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useRouteTransitionState } from '@/router/route-transition-state'

type CurrentSystemBannerProps = {
  managerMode?: boolean
}

const props = withDefaults(defineProps<CurrentSystemBannerProps>(), {
  managerMode: false,
})

const route = useRoute()
const router = useRouter()
const { isRouteNavigating, pendingRoutePath } = useRouteTransitionState()

const isPortalRoute = computed(() => (props.managerMode ? route.path === '/manager/home' : route.path === '/index-student'))

const banner = computed(() => {
  if (isPortalRoute.value) {
    return {
      eyebrow: 'Unified Portal',
      title: '当前位于宿舍管理入口',
      description: '这里汇总了宿舍系统的核心入口，你可以快速进入低碳总览、规则配置、奖励管理和费用处理。',
      badge: '宿舍入口',
      accent: 'portal',
      primaryLabel: props.managerMode ? '进入宿舍总览' : '进入个人信息',
      primaryPath: props.managerMode ? '/manager/low-carbon-overview' : '/personal-info',
      secondaryLabel: props.managerMode ? '进入奖励管理' : '进入低碳看板',
      secondaryPath: props.managerMode ? '/manager/reward-manage' : '/low-carbon-dashboard',
    }
  }

  return {
    eyebrow: 'Current System',
    title: '当前位于低碳宿舍系统',
    description: '你正在使用宿舍费用、积分和奖励相关功能，当前页面只保留宿舍模块入口。',
    badge: '宿舍模块',
    accent: 'dorm',
    primaryLabel: props.managerMode ? '返回管理首页' : '返回学生首页',
    primaryPath: props.managerMode ? '/manager/home' : '/index-student',
    secondaryLabel: props.managerMode ? '查看宿舍总览' : '查看低碳看板',
    secondaryPath: props.managerMode ? '/manager/low-carbon-overview' : '/low-carbon-dashboard',
  }
})

function goTo(path: string) {
  if (route.path === path) {
    return
  }
  router.push(path)
}

function isPending(path: string) {
  return isRouteNavigating.value && pendingRoutePath.value === path
}
</script>

<template>
  <section class="system-banner" :class="`system-banner--${banner.accent}`">
    <div class="system-banner__copy">
      <p class="system-banner__eyebrow">{{ banner.eyebrow }}</p>
      <h1>{{ banner.title }}</h1>
      <p class="system-banner__desc">{{ banner.description }}</p>
    </div>

    <div class="system-banner__side">
      <span class="system-banner__badge">{{ banner.badge }}</span>

      <div class="system-banner__actions">
        <button
          type="button"
          class="system-banner__action system-banner__action--primary"
          :class="{ 'system-banner__action--pending': isPending(banner.primaryPath) }"
          @click="goTo(banner.primaryPath)"
        >
          {{ banner.primaryLabel }}
        </button>
        <button
          type="button"
          class="system-banner__action system-banner__action--ghost"
          :class="{ 'system-banner__action--pending': isPending(banner.secondaryPath) }"
          @click="goTo(banner.secondaryPath)"
        >
          {{ banner.secondaryLabel }}
        </button>
      </div>
    </div>
  </section>
</template>

<style scoped>
.system-banner {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) auto;
  gap: 22px;
  align-items: center;
  padding: 28px 30px;
  border-radius: 28px;
  border: 1px solid rgba(91, 132, 108, 0.14);
  background:
    radial-gradient(circle at top left, rgba(111, 183, 141, 0.18), transparent 32%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.98), rgba(244, 250, 246, 0.94));
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.45),
    0 24px 52px rgba(36, 69, 54, 0.08);
}

.system-banner--portal {
  border-color: rgba(91, 132, 108, 0.16);
}

.system-banner__eyebrow {
  margin: 0 0 12px;
  color: #5e7d6d;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.system-banner h1 {
  margin: 0;
  color: #224334;
  font-size: clamp(28px, 3vw, 48px);
  line-height: 1.14;
  letter-spacing: 0.01em;
}

.system-banner__desc {
  margin: 14px 0 0;
  max-width: 720px;
  color: #61796d;
  font-size: 15px;
  line-height: 1.85;
}

.system-banner__side {
  display: grid;
  gap: 14px;
  justify-items: end;
}

.system-banner__badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 38px;
  padding: 0 15px;
  border-radius: 999px;
  border: 1px solid rgba(110, 156, 128, 0.22);
  color: #2b5a43;
  background: #edf7f1;
  font-size: 12px;
  font-weight: 900;
}

.system-banner__actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: end;
}

.system-banner__action {
  min-height: 50px;
  padding: 0 18px;
  border-radius: 16px;
  font: inherit;
  font-weight: 900;
  cursor: pointer;
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease,
    border-color 0.18s ease,
    background 0.18s ease;
}

.system-banner__action:hover {
  transform: translateY(-1px);
}

.system-banner__action--pending {
  position: relative;
  overflow: hidden;
}

.system-banner__action--pending::after {
  content: '';
  position: absolute;
  inset: 0;
  background:
    linear-gradient(110deg, transparent 18%, rgba(255, 255, 255, 0.3) 50%, transparent 82%);
  background-size: 220% 100%;
  animation: bannerPendingSweep 0.9s ease infinite;
}

.system-banner__action--primary {
  border: none;
  color: #fffdf8;
  background: linear-gradient(135deg, #2f8f68 0%, #2a7b59 100%);
  box-shadow: 0 14px 28px rgba(43, 120, 84, 0.22);
}

.system-banner__action--ghost {
  border: 1px solid rgba(131, 153, 140, 0.22);
  color: #244536;
  background: rgba(255, 255, 255, 0.84);
}

@keyframes bannerPendingSweep {
  0% {
    background-position: 140% 0;
  }

  100% {
    background-position: -40% 0;
  }
}

@media (max-width: 980px) {
  .system-banner {
    grid-template-columns: 1fr;
    padding: 24px;
  }

  .system-banner__side {
    justify-items: start;
  }

  .system-banner__actions {
    justify-content: start;
  }
}
</style>
