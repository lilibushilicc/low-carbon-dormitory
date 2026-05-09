<template>
  <div class="body" :class="{ 'body--student': isStudentRoute }">
    <template v-if="isStudentRoute">
      <div class="student-layout">
        <StudentGlobalNav />
        <main class="student-layout__content">
          <router-view />
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
const isStudentRoute = computed(() => route.path !== '/' && !route.path.startsWith('/manager'))
</script>

<style scoped>
.body {
  min-height: 100vh;
}

.body--student {
  background:
    radial-gradient(circle at 12% 5%, rgba(123, 196, 155, 0.12), transparent 24%),
    linear-gradient(180deg, #edf6f0 0%, #f8fbf9 100%);
}

.student-layout {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  align-items: start;
}

.student-layout__content {
  min-width: 0;
  min-height: 100vh;
  padding: 24px 18px 32px 24px;
  background:
    radial-gradient(circle at 12% 5%, rgba(123, 196, 155, 0.12), transparent 24%),
    linear-gradient(180deg, #edf6f0 0%, #f8fbf9 100%);
}

@media (max-width: 900px) {
  .student-layout {
    grid-template-columns: 1fr;
  }

  .student-layout__content {
    padding: 0 12px 24px;
  }
}
</style>
