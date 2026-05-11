<template>
  <div class="page dorm-shell">
    <div class="shell">
      <AdminGlobalNav />
      <section class="content">
        <CurrentSystemBanner manager-mode />
        <RouterView v-slot="{ Component, route }">
          <Transition name="manager-page-swap" mode="out-in" appear>
            <div :key="route.fullPath" class="content-scene">
              <component :is="Component" />
            </div>
          </Transition>
        </RouterView>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import AdminGlobalNav from '@/components/admin-global-nav.vue'
import CurrentSystemBanner from '@/components/current-system-banner.vue'
</script>

<style scoped>
.page {
  min-height: 100vh;
  background:
    radial-gradient(circle at 10% 0%, rgba(123, 196, 155, 0.18), transparent 28%),
    radial-gradient(circle at 100% 100%, rgba(230, 216, 164, 0.14), transparent 26%),
    linear-gradient(180deg, #edf6f0 0%, #f8fbf9 48%, #f5faf7 100%);
  padding: clamp(14px, 2vw, 24px);
}

.shell {
  width: min(1540px, 100%);
  margin: 0 auto;
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.content {
  min-width: 0;
  display: grid;
  gap: 16px;
}

.content-scene {
  min-width: 0;
}

.manager-page-swap-enter-active,
.manager-page-swap-leave-active {
  transition:
    opacity 0.24s ease,
    transform 0.28s cubic-bezier(0.22, 1, 0.36, 1);
  will-change: opacity, transform;
}

.manager-page-swap-enter-from {
  opacity: 0;
  transform: translate3d(0, 16px, 0);
}

.manager-page-swap-leave-to {
  opacity: 0;
  transform: translate3d(0, -10px, 0);
}

@media (prefers-reduced-motion: reduce) {
  .manager-page-swap-enter-active,
  .manager-page-swap-leave-active {
    transition: opacity 0.01s linear;
  }

  .manager-page-swap-enter-from,
  .manager-page-swap-leave-to {
    transform: none;
  }
}

.dorm-shell :deep(.board-page) {
  min-height: auto;
  padding: 0;
}

.dorm-shell :deep(.board-shell) {
  max-width: 100%;
  margin: 0;
}

.dorm-shell :deep(.board-content),
.dorm-shell :deep(.dorm-page) {
  min-width: 0;
  display: grid;
  gap: 24px;
}

.dorm-shell :deep(.hero-card),
.dorm-shell :deep(.content-card),
.dorm-shell :deep(.feedback-card),
.dorm-shell :deep(.hero),
.dorm-shell :deep(.toolbar),
.dorm-shell :deep(.panel),
.dorm-shell :deep(.stat),
.dorm-shell :deep(.dorm-card),
.dorm-shell :deep(.card) {
  border-radius: 24px;
  border: 1px solid rgba(53, 96, 73, 0.08);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.95), rgba(248, 251, 249, 0.9)),
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

.dorm-shell :deep(.hero-card),
.dorm-shell :deep(.hero) {
  padding: 28px 28px;
  background:
    radial-gradient(circle at top right, rgba(178, 222, 197, 0.18), transparent 34%),
    rgba(255, 255, 255, 0.94);
}

.dorm-shell :deep(.hero-card h1),
.dorm-shell :deep(.hero h1) {
  color: #204232;
}

.dorm-shell :deep(.hero-card p),
.dorm-shell :deep(.hero p),
.dorm-shell :deep(.subtitle),
.dorm-shell :deep(.hero__text),
.dorm-shell :deep(.hero__meta) {
  color: #698275;
}

.dorm-shell :deep(.hero-card__eyebrow),
.dorm-shell :deep(.hero__eyebrow),
.dorm-shell :deep(.panel__eyebrow),
.dorm-shell :deep(.tag),
.dorm-shell :deep(.eyebrow) {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  padding: 8px 14px;
  border-radius: 999px;
  border: 1px solid rgba(93, 131, 109, 0.12);
  background: rgba(247, 251, 249, 0.92);
  color: #4f6a5b;
  font-size: 12px;
  font-weight: 800;
}

.dorm-shell :deep(.card-grid),
.dorm-shell :deep(.stats),
.dorm-shell :deep(.grid),
.dorm-shell :deep(.detail-grid) {
  gap: 16px;
}

.dorm-shell :deep(.toolbar),
.dorm-shell :deep(.panel),
.dorm-shell :deep(.content-card) {
  padding: 20px 22px;
}

.dorm-shell :deep(.el-form-item__label) {
  color: #4f6a5b;
  font-weight: 700;
}

.dorm-shell :deep(.el-input__wrapper),
.dorm-shell :deep(.el-select__wrapper),
.dorm-shell :deep(.el-textarea__inner) {
  background: rgba(255, 255, 255, 0.92);
  box-shadow: inset 0 0 0 1px rgba(93, 131, 109, 0.12) !important;
}

.dorm-shell :deep(.el-button--primary),
.dorm-shell :deep(.el-button--success),
.dorm-shell :deep(.module-card__action) {
  border: none;
  background: linear-gradient(135deg, #1d744d 0%, #133d2b 100%);
  box-shadow: 0 12px 24px rgba(29, 90, 60, 0.18);
}

.dorm-shell :deep(.el-button:not(.el-button--primary):not(.el-button--success)) {
  border-color: rgba(93, 131, 109, 0.12);
  background: rgba(255, 255, 255, 0.9);
  color: #204232;
}

.dorm-shell :deep(.el-table) {
  --el-table-bg-color: #ffffff;
  --el-table-tr-bg-color: #ffffff;
  --el-table-row-hover-bg-color: #f5faf7;
  --el-table-header-bg-color: #f5faf7;
  --el-table-border-color: rgba(93, 131, 109, 0.12);
  --el-table-text-color: #204232;
  --el-table-header-text-color: #4f6a5b;
}

.dorm-shell :deep(.el-table th.el-table__cell),
.dorm-shell :deep(.el-table td.el-table__cell) {
  background: transparent;
}

.dorm-shell :deep(.el-pagination) {
  margin-top: 22px;
  justify-content: flex-end;
  color: #698275;
}

@media (max-width: 1180px) {
  .shell {
    grid-template-columns: 256px minmax(0, 1fr);
  }
}

@media (max-width: 980px) {
  .shell {
    grid-template-columns: 1fr;
  }

  .page {
    padding: 16px;
  }

  .dorm-shell :deep(.hero-card),
  .dorm-shell :deep(.hero) {
    padding: 26px 24px;
  }
}

@media (max-width: 760px) {
  .dorm-shell :deep(.stats),
  .dorm-shell :deep(.grid),
  .dorm-shell :deep(.detail-grid),
  .dorm-shell :deep(.card-grid),
  .dorm-shell :deep(.toolbar) {
    grid-template-columns: 1fr;
  }
}
</style>
