<template>
  <div class="body" :class="{ 'body--student': isStudentRoute }">
    <template v-if="isStudentRoute">
      <div class="student-layout">
        <div class="student-layout__glow student-layout__glow--top"></div>
        <div class="student-layout__glow student-layout__glow--bottom"></div>
        <StudentGlobalNav class="student-layout__nav" />
        <main class="student-layout__content">
          <div class="student-layout__stage">
            <router-view />
          </div>
        </main>
      </div>
    </template>
    <router-view v-else />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import StudentGlobalNav from '@/components/student-global-nav.vue'

const route = useRoute()
const isStudentRoute = computed(() => {
  return route.path !== '/' && route.path !== '/login' && !route.path.startsWith('/manager')
})
</script>

<style scoped>
.body {
  min-height: 100vh;
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

.student-layout__stage {
  min-height: calc(100vh - 46px);
  min-height: calc(100dvh - 46px);
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
