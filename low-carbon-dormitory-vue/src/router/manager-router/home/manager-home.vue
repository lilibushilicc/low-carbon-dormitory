<template>
  <div class="page">
    <div class="shell">
      <AdminGlobalNav />

      <section class="content">
        <section class="hero">
          <div class="hero__main">
            <p class="hero__eyebrow">Admin Center</p>
            <h1>宿舍项目管理首页</h1>
            <p class="hero__text">{{ welcomeText }}</p>

            <div class="hero__chips">
              <span class="hero__chip">当前角色：{{ adminName }}</span>
              <span class="hero__chip hero__chip--accent">当前模块：宿舍管理</span>
            </div>

            <div class="hero__actions">
              <button type="button" class="hero__action hero__action--primary" @click="goTo('/manager/low-carbon-overview')">
                进入宿舍总览
              </button>
              <button type="button" class="hero__action hero__action--ghost" @click="goTo('/manager/reward-manage')">
                进入奖励管理
              </button>
            </div>
          </div>

          <aside class="hero__aside">
            <div class="hero__aside-head">
              <span>今日工作聚焦</span>
              <strong>宿舍管理总览</strong>
            </div>

            <div class="hero__summary-grid">
              <article class="hero__summary-card">
                <small>低碳总览</small>
                <strong>看板、排名、明细</strong>
                <p>集中查看宿舍低碳表现、楼栋排行和宿舍费用明细。</p>
              </article>

              <article class="hero__summary-card hero__summary-card--accent">
                <small>基础维护</small>
                <strong>规则、学生、奖励</strong>
                <p>统一维护积分规则、学生档案、奖励库存和宿舍扣费操作。</p>
              </article>
            </div>
          </aside>
        </section>

        <section class="module-grid">
          <article class="module-card module-card--active">
            <div class="module-card__header">
              <span class="module-card__tag">Dormitory</span>
              <span class="module-card__state">当前模块</span>
            </div>

            <h2>宿舍管理</h2>
            <p>集中处理低碳规则、学生档案、奖励兑换以及宿舍水电费等日常事务。</p>

            <ul class="module-card__list">
              <li>低碳总览与规则配置</li>
              <li>学生档案与奖励管理</li>
              <li>宿舍费用处理与数据维护</li>
            </ul>

            <button type="button" class="module-card__action" @click="goTo('/manager/low-carbon-overview')">
              进入当前模块
            </button>
          </article>
        </section>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AdminGlobalNav from '@/components/admin-global-nav.vue'
import { useAdminTokenStore } from '@/stores/admin-token'

const route = useRoute()
const router = useRouter()
const adminStore = useAdminTokenStore()

const adminName = computed(() => adminStore.profile?.displayName || adminStore.profile?.username || '系统管理员')
const welcomeText = computed(
  () => `${adminName.value}，这里保留了宿舍项目的核心入口，你可以从这里进入总览、规则配置、学生维护、奖励管理和宿舍扣费。`,
)

function goTo(path: string) {
  if (route.path !== path) {
    router.push(path)
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background:
    radial-gradient(circle at 0% 0%, rgba(123, 180, 148, 0.22), transparent 35%),
    radial-gradient(circle at 100% 100%, rgba(85, 136, 110, 0.16), transparent 45%),
    linear-gradient(180deg, #edf6f1 0%, #f7fbf9 100%);
  padding: clamp(18px, 2.6vw, 32px);
}

.shell {
  width: min(1460px, 100%);
  margin: 0 auto;
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 22px;
}

.content {
  min-width: 0;
  display: grid;
  gap: 20px;
}

.hero {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(320px, 0.85fr);
  gap: 18px;
  padding: clamp(22px, 3vw, 30px);
  align-items: stretch;
  border: 1px solid rgba(42, 94, 67, 0.1);
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow:
    0 20px 50px rgba(36, 69, 54, 0.1),
    0 6px 16px rgba(36, 69, 54, 0.05);
}

.hero__main,
.hero__aside {
  min-width: 0;
}

.hero__eyebrow {
  margin: 0 0 10px;
  color: #2f7d5d;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.hero h1 {
  margin: 0;
  color: #1e3e31;
  font-size: clamp(30px, 4vw, 42px);
  line-height: 1.08;
}

.hero__text {
  margin: 14px 0 0;
  color: #5b7568;
  font-size: 15px;
  line-height: 1.75;
}

.hero__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;
}

.hero__chip {
  min-height: 36px;
  display: inline-flex;
  align-items: center;
  padding: 0 14px;
  border-radius: 999px;
  background: #edf6f1;
  color: #315245;
  font-size: 13px;
  font-weight: 800;
}

.hero__chip--accent {
  background: #f9efd3;
  color: #8a5e08;
}

.hero__actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 22px;
}

.hero__action {
  min-height: 46px;
  padding: 0 18px;
  border-radius: 14px;
  font: inherit;
  font-weight: 800;
  cursor: pointer;
}

.hero__action--primary {
  border: none;
  color: #fff;
  background: linear-gradient(135deg, #2f8f68 0%, #2a7b59 100%);
  box-shadow: 0 10px 22px rgba(43, 120, 84, 0.18);
}

.hero__action--ghost {
  border: 1px solid #d1e0d8;
  color: #244536;
  background: rgba(255, 255, 255, 0.84);
}

.hero__aside {
  display: grid;
  gap: 14px;
  padding: 18px;
  border-radius: 24px;
  border: 1px solid #d5e3db;
  background: linear-gradient(160deg, rgba(235, 245, 239, 0.98), rgba(247, 251, 249, 0.98));
}

.hero__aside-head {
  display: grid;
  gap: 6px;
}

.hero__aside-head span {
  color: #648073;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.hero__aside-head strong {
  color: #244536;
  font-size: 24px;
  line-height: 1.2;
}

.hero__summary-grid {
  display: grid;
  gap: 10px;
}

.hero__summary-card {
  padding: 15px 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid rgba(42, 94, 67, 0.08);
  display: grid;
  gap: 8px;
}

.hero__summary-card--accent {
  background: linear-gradient(180deg, rgba(253, 249, 240, 0.9), rgba(255, 255, 255, 0.88));
}

.hero__summary-card small {
  color: #648073;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.hero__summary-card strong {
  color: #244536;
  font-size: 19px;
  line-height: 1.3;
}

.hero__summary-card p {
  margin: 0;
  color: #5f776b;
  font-size: 13px;
  line-height: 1.6;
}

.module-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 18px;
}

.module-card {
  display: grid;
  gap: 16px;
  padding: 24px;
  border: 1px solid rgba(42, 94, 67, 0.1);
  border-radius: 24px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 251, 249, 0.94));
  box-shadow:
    0 18px 38px rgba(36, 69, 54, 0.08),
    0 6px 14px rgba(36, 69, 54, 0.04);
}

.module-card__header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.module-card__tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 30px;
  width: fit-content;
  padding: 0 12px;
  border-radius: 999px;
  background: #ebf5ef;
  color: #2f7d5d;
  font-size: 12px;
  font-weight: 800;
}

.module-card__state {
  color: #648073;
  font-size: 12px;
  font-weight: 800;
}

.module-card--active .module-card__tag {
  background: #f9efd3;
  color: #8a5e08;
}

.module-card h2 {
  margin: 0;
  color: #1f3e31;
  font-size: 24px;
}

.module-card p {
  margin: 0;
  color: #5b7568;
  line-height: 1.7;
}

.module-card__list {
  margin: 0;
  padding-left: 18px;
  color: #315245;
  display: grid;
  gap: 8px;
}

.module-card__action {
  min-height: 46px;
  border: none;
  border-radius: 14px;
  color: #ffffff;
  background: linear-gradient(135deg, #2f8f68 0%, #2a7b59 100%);
  font: inherit;
  font-weight: 800;
  cursor: pointer;
  box-shadow: 0 10px 22px rgba(43, 120, 84, 0.18);
}

@media (max-width: 1500px) {
  .hero {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 1200px) {
  .content {
    width: 100%;
  }
}

@media (max-width: 760px) {
  .shell {
    grid-template-columns: 1fr;
  }
}
</style>
