<template>
  <div class="body" :class="{ 'body--student': isStudentRoute }">
    <div class="route-progress" :class="{ 'route-progress--active': isRouteNavigating }" aria-hidden="true">
      <span class="route-progress__bar" :style="{ transform: progressScale }"></span>
    </div>

    <template v-if="isStudentRoute">
      <div class="student-layout" :class="{ 'student-layout--compact': !showStudentNav }">
        <div class="student-layout__glow student-layout__glow--top"></div>
        <div class="student-layout__glow student-layout__glow--bottom"></div>
        <StudentGlobalNav v-if="showStudentNav" class="student-layout__nav" />
        <main class="student-layout__content">
          <div class="student-layout__stage">
            <RouterView v-slot="{ Component, route: currentRoute }">
              <Transition name="page-swap" mode="out-in" appear>
                <div
                  :key="resolveAppViewKey(currentRoute.path, currentRoute.fullPath)"
                  class="route-scene"
                  :class="{ 'route-scene--navigating': isRouteNavigating }"
                >
                  <component :is="Component" />
                </div>
              </Transition>
            </RouterView>
          </div>
        </main>
      </div>
    </template>
    <RouterView v-else v-slot="{ Component, route: currentRoute }">
      <Transition name="page-swap" mode="out-in" appear>
        <div
          :key="resolveAppViewKey(currentRoute.path, currentRoute.fullPath)"
          class="route-scene route-scene--full"
          :class="{ 'route-scene--navigating': isRouteNavigating }"
        >
          <component :is="Component" />
        </div>
      </Transition>
    </RouterView>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import StudentGlobalNav from '@/components/student-global-nav.vue'
import { useRouteTransitionState } from '@/router/route-transition-state'
import { useStudentTokenStore } from '@/stores/student-token'

const MOBILE_MEDIA_QUERY = '(max-width: 900px)'

const route = useRoute()
const studentTokenStore = useStudentTokenStore()
const isMobileViewport = ref(typeof window !== 'undefined' ? window.matchMedia(MOBILE_MEDIA_QUERY).matches : false)
let viewportMediaQueryList: MediaQueryList | null = null
const { isRouteNavigating, progressScale } = useRouteTransitionState()

const isStudentRoute = computed(() => {
  return route.path !== '/' && route.path !== '/login' && !route.path.startsWith('/manager')
})

const showStudentNav = computed(() => {
  return isStudentRoute.value && studentTokenStore.isLoggedIn && !isMobileViewport.value
})

function syncViewportMode() {
  isMobileViewport.value = viewportMediaQueryList?.matches ?? false
}

function handleViewportChange() {
  syncViewportMode()
}

function resolveAppViewKey(path: string, fullPath: string) {
  if (path.startsWith('/manager')) {
    return 'manager-shell'
  }

  return fullPath
}

onMounted(() => {
  viewportMediaQueryList = window.matchMedia(MOBILE_MEDIA_QUERY)
  syncViewportMode()
  viewportMediaQueryList.addEventListener('change', handleViewportChange)
})

onBeforeUnmount(() => {
  viewportMediaQueryList?.removeEventListener('change', handleViewportChange)
  viewportMediaQueryList = null
})
</script>

<style scoped>
.body {
  min-height: 100vh;
}

.route-progress {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1600;
  height: 3px;
  pointer-events: none;
  opacity: 0;
  transition: opacity 0.18s ease;
}

.route-progress--active {
  opacity: 1;
}

.route-progress__bar {
  display: block;
  width: 100%;
  height: 100%;
  transform-origin: left center;
  background: linear-gradient(90deg, #1f7a52 0%, #69bf92 50%, #d4e8ae 100%);
  box-shadow: 0 0 18px rgba(68, 156, 111, 0.36);
}

.body--student {
  position: relative;
  overflow: hidden;
  background:
    radial-gradient(circle at 10% 0%, rgba(123, 196, 155, 0.18), transparent 28%),
    radial-gradient(circle at 100% 100%, rgba(230, 216, 164, 0.14), transparent 26%),
    linear-gradient(180deg, #edf6f0 0%, #f8fbf9 48%, #f5faf7 100%);
}

.student-layout {
  position: relative;
  min-height: 100vh;
  display: grid;
  grid-template-columns: 312px minmax(0, 1fr);
  align-items: start;
  gap: 18px;
  padding-right: 18px;
}

.student-layout--compact {
  grid-template-columns: minmax(0, 1fr);
  gap: 0;
  padding-right: 0;
}

.student-layout__glow {
  position: fixed;
  z-index: 0;
  width: 34vw;
  max-width: 460px;
  min-width: 260px;
  aspect-ratio: 1;
  border-radius: 999px;
  pointer-events: none;
  filter: blur(72px);
  opacity: 0.4;
}

.student-layout__glow--top {
  top: -8vh;
  right: -10vw;
  background: rgba(136, 201, 167, 0.42);
}

.student-layout__glow--bottom {
  left: 24vw;
  bottom: -18vh;
  background: rgba(229, 215, 157, 0.28);
}

.student-layout__nav,
.student-layout__content {
  position: relative;
  z-index: 1;
}

.student-layout__content {
  min-width: 0;
  min-height: 100vh;
  padding: 18px 0 28px;
  background:
    radial-gradient(circle at 12% 5%, rgba(123, 196, 155, 0.12), transparent 24%),
    linear-gradient(180deg, rgba(248, 251, 249, 0.3), rgba(248, 251, 249, 0));
}

.student-layout--compact .student-layout__content {
  padding: 0 12px 24px;
}

.student-layout__stage {
  min-height: calc(100vh - 46px);
  min-height: calc(100dvh - 46px);
  overflow: clip;
  border-radius: 32px;
  border: 1px solid rgba(53, 96, 73, 0.08);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.4), rgba(255, 255, 255, 0.18)),
    repeating-linear-gradient(
      135deg,
      rgba(255, 255, 255, 0.18) 0,
      rgba(255, 255, 255, 0.18) 10px,
      rgba(244, 250, 246, 0.16) 10px,
      rgba(244, 250, 246, 0.16) 20px
    );
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.56),
    0 20px 46px rgba(37, 72, 54, 0.06);
}

.student-layout--compact .student-layout__stage {
  min-height: auto;
  border-radius: 24px;
}

.route-scene {
  min-height: inherit;
  transition:
    filter 0.22s ease,
    transform 0.22s ease;
}

.route-scene--full {
  min-height: 100vh;
}

.route-scene--navigating {
  filter: saturate(0.98);
}

.page-swap-enter-active,
.page-swap-leave-active {
  transition:
    opacity 0.28s ease,
    transform 0.32s cubic-bezier(0.22, 1, 0.36, 1),
    filter 0.28s ease;
  transform-origin: center top;
  will-change: opacity, transform, filter;
}

.page-swap-enter-from {
  opacity: 0;
  transform: translate3d(0, 18px, 0) scale(0.992);
  filter: blur(8px);
}

.page-swap-leave-to {
  opacity: 0;
  transform: translate3d(0, -12px, 0) scale(1.006);
  filter: blur(6px);
}

@media (prefers-reduced-motion: reduce) {
  .page-swap-enter-active,
  .page-swap-leave-active {
    transition: opacity 0.01s linear;
  }

  .page-swap-enter-from,
  .page-swap-leave-to {
    transform: none;
    filter: none;
  }
}

@media (max-width: 900px) {
  .student-layout {
    grid-template-columns: 1fr;
    gap: 0;
    padding-right: 0;
  }

  .student-layout__content {
    padding: 0 12px 24px;
  }

  .student-layout__stage {
    min-height: auto;
    border-radius: 24px;
  }
}
</style>
