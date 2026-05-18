<template>
  <div class="page">
    <div class="shell">
      <AdminGlobalNav />

      <section class="content">
        <section class="hero">
          <div class="hero__main">
            <p class="hero__eyebrow">Admin Center</p>
            <h1>统一管理后台首页</h1>
            <p class="hero__text">{{ welcomeText }}</p>

            <div class="hero__chips">
              <span class="hero__chip">当前角色：{{ adminName }}</span>
              <span class="hero__chip hero__chip--accent">当前能力：宿舍管理 + 餐厅管理</span>
            </div>

            <div class="hero__actions">
              <button type="button" class="hero__action hero__action--primary" @click="goTo('/manager/low-carbon-overview')">
                进入宿舍总览
              </button>
              <button type="button" class="hero__action hero__action--restaurant" @click="goTo('/manager/restaurant/summary')">
                进入餐厅总览
              </button>
            </div>
          </div>

          <aside class="hero__aside">
            <div class="hero__aside-head">
              <span>今日工作聚焦</span>
              <strong>统一后台概览</strong>
            </div>

            <div class="hero__summary-grid">
              <article class="hero__summary-card">
                <small>宿舍管理</small>
                <strong>低碳、规则、奖励、水电</strong>
                <p>集中处理宿舍系统中的低碳总览、规则配置、学生档案、奖励维护和费用扣减。</p>
              </article>

              <article class="hero__summary-card hero__summary-card--accent">
                <small>餐厅管理</small>
                <strong>统计、学生、餐饮记录、趋势</strong>
                <p>集中查看学院餐饮碳排放统计，维护用餐记录，并分析日周月学期年度趋势。</p>
              </article>
            </div>
          </aside>
        </section>

        <section class="module-grid">
          <article class="module-card module-card--active">
            <div class="module-card__header">
              <span class="module-card__tag">Dormitory</span>
              <span class="module-card__state">宿舍模块</span>
            </div>

            <h2>宿舍管理</h2>
            <p>集中处理低碳规则、学生档案、奖励兑换和宿舍水电费等日常业务。</p>

            <ul class="module-card__list">
              <li>低碳总览与规则配置</li>
              <li>学生档案与奖励管理</li>
              <li>宿舍费用处理与数据维护</li>
            </ul>

            <button type="button" class="module-card__action" @click="goTo('/manager/low-carbon-overview')">
              进入宿舍模块
            </button>
          </article>

          <article class="module-card module-card--restaurant">
            <div class="module-card__header">
              <span class="module-card__tag module-card__tag--restaurant">Restaurant</span>
              <span class="module-card__state">餐厅模块</span>
            </div>

            <h2>餐厅管理</h2>
            <p>集中处理餐厅碳排放汇总、学生用餐数据、每日到年度报表以及周期趋势分析。</p>

            <ul class="module-card__list">
              <li>学院餐饮碳排放汇总</li>
              <li>餐厅学生列表与用餐录入</li>
              <li>日周月学期年度报表与趋势分析</li>
            </ul>

            <button type="button" class="module-card__action module-card__action--restaurant" @click="goTo('/manager/restaurant/summary')">
              进入餐厅模块
            </button>
          </article>
        </section>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import AdminGlobalNav from '@/components/admin-global-nav.vue'
import { useAppNavigation } from '@/composables/use-app-navigation'
import { useAdminTokenStore } from '@/stores/admin-token'

const { goTo } = useAppNavigation()
const adminStore = useAdminTokenStore()

const adminName = computed(
  () => adminStore.profile?.displayName || adminStore.profile?.username || '系统管理员',
)
const welcomeText = computed(
  () => `${adminName.value}，这里整合了宿舍管理和餐厅管理两个后台模块，你可以在同一套导航和视觉体系下直接切换业务，不再需要整页外跳。`,
)

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
  background: #dff3e6;
}

.hero__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 22px;
}

.hero__action {
  min-height: 46px;
  padding: 0 18px;
  border-radius: 14px;
  border: 1px solid transparent;
  font-size: 14px;
  font-weight: 800;
  cursor: pointer;
}

.hero__action--primary {
  background: linear-gradient(135deg, #2f8f68 0%, #2a7b59 100%);
  color: #fff;
  box-shadow: 0 14px 30px rgba(42, 123, 89, 0.25);
}

.hero__action--restaurant {
  background: linear-gradient(135deg, #a67b1f 0%, #7a5912 100%);
  color: #fff;
  box-shadow: 0 14px 30px rgba(122, 89, 18, 0.22);
}

.hero__aside {
  display: grid;
  gap: 14px;
  padding: 18px;
  border-radius: 24px;
  background:
    radial-gradient(circle at top right, rgba(80, 154, 118, 0.2), transparent 46%),
    linear-gradient(180deg, #f4faf6 0%, #eef7f2 100%);
  border: 1px solid rgba(47, 143, 104, 0.12);
}

.hero__aside-head {
  display: grid;
  gap: 6px;
}

.hero__aside-head span {
  color: #5f776b;
  font-size: 12px;
  font-weight: 800;
}

.hero__aside-head strong {
  color: #244536;
  font-size: 20px;
}

.hero__summary-grid {
  display: grid;
  gap: 12px;
}

.hero__summary-card {
  display: grid;
  gap: 8px;
  padding: 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(39, 82, 61, 0.08);
}

.hero__summary-card small {
  color: #5f776b;
  font-size: 12px;
  font-weight: 800;
}

.hero__summary-card strong {
  color: #1e3e31;
  font-size: 16px;
}

.hero__summary-card p {
  margin: 0;
  color: #5b7568;
  font-size: 13px;
  line-height: 1.65;
}

.hero__summary-card--accent {
  background:
    radial-gradient(circle at right top, rgba(231, 217, 165, 0.24), transparent 34%),
    rgba(255, 255, 255, 0.82);
}

.module-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.module-card {
  display: grid;
  gap: 16px;
  padding: 22px;
  border-radius: 24px;
  border: 1px solid rgba(39, 82, 61, 0.08);
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 18px 40px rgba(36, 69, 54, 0.08);
}

.module-card--restaurant {
  background:
    radial-gradient(circle at right top, rgba(231, 217, 165, 0.18), transparent 32%),
    rgba(255, 255, 255, 0.94);
}

.module-card__header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.module-card__tag,
.module-card__state {
  min-height: 32px;
  display: inline-flex;
  align-items: center;
  padding: 0 12px;
  border-radius: 999px;
  background: #edf6f1;
  color: #315245;
  font-size: 12px;
  font-weight: 800;
}

.module-card__tag--restaurant {
  background: #fbf4df;
  color: #6c5520;
}

.module-card h2 {
  margin: 0;
  color: #1e3e31;
  font-size: 24px;
}

.module-card p {
  margin: 0;
  color: #5b7568;
  line-height: 1.75;
}

.module-card__list {
  display: grid;
  gap: 10px;
  margin: 0;
  padding-left: 18px;
  color: #315245;
}

.module-card__action {
  min-height: 46px;
  width: fit-content;
  padding: 0 18px;
  border: none;
  border-radius: 14px;
  background: linear-gradient(135deg, #2f8f68 0%, #2a7b59 100%);
  color: #fff;
  font-size: 14px;
  font-weight: 800;
  cursor: pointer;
  box-shadow: 0 14px 28px rgba(42, 123, 89, 0.22);
}

.module-card__action--restaurant {
  background: linear-gradient(135deg, #a67b1f 0%, #7a5912 100%);
  box-shadow: 0 14px 28px rgba(122, 89, 18, 0.22);
}

@media (max-width: 1180px) {
  .shell {
    grid-template-columns: 260px minmax(0, 1fr);
  }
}

@media (max-width: 980px) {
  .shell {
    grid-template-columns: 1fr;
  }

  .hero {
    grid-template-columns: 1fr;
  }

  .module-grid {
    grid-template-columns: 1fr;
  }
}
</style>
