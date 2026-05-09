<template>
  <div class="page dorm-shell">
    <div class="shell">
      <AdminGlobalNav />
      <section class="content">
        <CurrentSystemBanner manager-mode />
        <router-view />
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
  background: #eef2f7;
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
  border: 1px solid #d9e2ec;
  background: rgba(255, 255, 255, 0.94);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.52),
    0 10px 30px rgba(15, 23, 42, 0.06);
}

.dorm-shell :deep(.hero-card),
.dorm-shell :deep(.hero) {
  padding: 28px 28px;
  background: #fff;
}

.dorm-shell :deep(.hero-card h1),
.dorm-shell :deep(.hero h1) {
  color: #0f172a;
}

.dorm-shell :deep(.hero-card p),
.dorm-shell :deep(.hero p),
.dorm-shell :deep(.subtitle),
.dorm-shell :deep(.hero__text),
.dorm-shell :deep(.hero__meta) {
  color: #64748b;
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
  border: 1px solid #dbe3ee;
  background: #f8fafc;
  color: #475569;
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
  color: #475569;
  font-weight: 700;
}

.dorm-shell :deep(.el-input__wrapper),
.dorm-shell :deep(.el-select__wrapper),
.dorm-shell :deep(.el-textarea__inner) {
  background: #fff;
  box-shadow: inset 0 0 0 1px #dbe3ee !important;
}

.dorm-shell :deep(.el-button--primary),
.dorm-shell :deep(.el-button--success),
.dorm-shell :deep(.module-card__action) {
  border: none;
  background: #2563eb;
  box-shadow: 0 10px 20px rgba(37, 99, 235, 0.16);
}

.dorm-shell :deep(.el-button:not(.el-button--primary):not(.el-button--success)) {
  border-color: #dbe3ee;
  background: #fff;
  color: #0f172a;
}

.dorm-shell :deep(.el-table) {
  --el-table-bg-color: #ffffff;
  --el-table-tr-bg-color: #ffffff;
  --el-table-row-hover-bg-color: #f8fafc;
  --el-table-header-bg-color: #f8fafc;
  --el-table-border-color: #e2e8f0;
  --el-table-text-color: #0f172a;
  --el-table-header-text-color: #475569;
}

.dorm-shell :deep(.el-table th.el-table__cell),
.dorm-shell :deep(.el-table td.el-table__cell) {
  background: transparent;
}

.dorm-shell :deep(.el-pagination) {
  margin-top: 22px;
  justify-content: flex-end;
  color: #64748b;
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
