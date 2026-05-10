<template>
  <div class="portal-page">
    <section class="hero">
      <div class="hero__copy">
        <p class="hero__eyebrow">Student Center</p>
        <h1>学生宿舍端</h1>
        <p class="hero__text">{{ welcomeText }}</p>

        <div class="hero__actions">
          <button type="button" class="hero__action hero__action--primary" @click="goTo('/personal-info')">
            查看个人信息
          </button>
          <button type="button" class="hero__action hero__action--ghost" @click="goTo('/low-carbon-dashboard')">
            打开低碳看板
          </button>
        </div>
      </div>

      <div class="hero__panel">
        <div class="hero__panel-head">
          <span>当前概览</span>
          <b>宿舍服务入口</b>
        </div>

        <div class="hero__panel-grid">
          <article v-for="item in quickStats" :key="item.label" class="hero__meta-card">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <small>{{ item.description }}</small>
          </article>
        </div>
      </div>
    </section>

    <section class="service-strip">
      <article v-for="item in serviceHighlights" :key="item.title" class="service-strip__card">
        <span>{{ item.label }}</span>
        <strong>{{ item.title }}</strong>
        <p>{{ item.description }}</p>
      </article>
    </section>

    <section class="feature-grid">
      <article
        v-for="feature in featureCards"
        :key="feature.path"
        class="feature-card"
        :class="`feature-card--${feature.accent}`"
      >
        <div class="feature-card__head">
          <span class="feature-card__tag">{{ feature.tag }}</span>
          <b>{{ feature.index }}</b>
        </div>

        <div class="feature-card__body">
          <h2>{{ feature.title }}</h2>
          <p>{{ feature.description }}</p>
        </div>

        <ul class="feature-card__list">
          <li v-for="item in feature.highlights" :key="item">{{ item }}</li>
        </ul>

        <button type="button" class="feature-card__action" @click="goTo(feature.path)">
          {{ feature.action }}
        </button>
      </article>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useStudentTokenStore } from '@/stores/student-token'

const route = useRoute()
const router = useRouter()
const studentTokenStore = useStudentTokenStore()
const { studentInfo, dormLabel } = storeToRefs(studentTokenStore)

const studentName = computed(() => studentInfo.value?.name || '同学')
const dormLabelText = computed(() =>
  dormLabel.value && dormLabel.value !== '-' ? dormLabel.value : '暂未识别宿舍',
)

const welcomeText = computed(() => {
  return `${studentName.value}，这里汇集了个人资料、宿舍账务、低碳积分和奖励兑换入口，你可以从当前首页快速进入每一项高频服务。`
})

const quickStats = computed(() => [
  {
    label: '当前身份',
    value: studentName.value,
    description: '已进入学生服务视图',
  },
  {
    label: '当前宿舍',
    value: dormLabelText.value,
    description: '用于账务与低碳数据归属',
  },
  {
    label: '可用服务',
    value: '6 项',
    description: '覆盖资料、账务与低碳场景',
  },
])

const serviceHighlights = [
  {
    label: '01',
    title: '账务清晰',
    description: '围绕余额、历史与支付入口组织，减少来回跳转。',
  },
  {
    label: '02',
    title: '低碳可视',
    description: '把积分、规则与奖励兑换归并到同一服务带内。',
  },
  {
    label: '03',
    title: '入口统一',
    description: '学生端首页、资料页和看板页保持一致的生态界面语言。',
  },
] as const

const featureCards = [
  {
    index: '01',
    tag: 'Profile',
    accent: 'sage',
    title: '个人信息与宿舍档案',
    description: '查看学号、院系、班级、宿舍楼栋和房间等基础信息，作为后续账务与积分数据的入口。',
    highlights: ['学生档案信息', '宿舍楼栋与房间', '联系方式与入住信息'],
    action: '进入个人信息',
    path: '/personal-info',
  },
  {
    index: '02',
    tag: 'Billing',
    accent: 'gold',
    title: '宿舍水电费',
    description: '集中处理宿舍余额、用量、账单与缴费入口，方便快速定位最近一次费用变化。',
    highlights: ['剩余金额与用量', '历史账单查询', '缴费与扣费结果查看'],
    action: '进入费用中心',
    path: '/water-electricity',
  },
  {
    index: '03',
    tag: 'Low Carbon',
    accent: 'forest',
    title: '低碳积分服务',
    description: '浏览个人低碳表现、积分规则和奖励兑换，把绿色行为直接映射到可见成果。',
    highlights: ['个人低碳看板', '积分与荣誉规则', '奖励兑换记录'],
    action: '进入低碳服务',
    path: '/low-carbon-dashboard',
  },
] as const

function goTo(path: string) {
  if (route.path !== path) {
    router.push(path)
  }
}
</script>

<style scoped>
.portal-page {
  min-height: 100%;
  display: grid;
  gap: 22px;
  padding: clamp(22px, 3vw, 34px);
  background:
    radial-gradient(circle at 6% 0%, rgba(119, 178, 147, 0.22), transparent 30%),
    radial-gradient(circle at 100% 80%, rgba(231, 217, 165, 0.18), transparent 28%),
    linear-gradient(180deg, rgba(236, 246, 241, 0.82) 0%, rgba(247, 251, 249, 0.92) 100%);
  border-radius: 32px;
}

.hero {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(300px, 0.95fr);
  gap: 18px;
  align-items: stretch;
}

.hero__copy,
.hero__panel {
  position: relative;
  overflow: hidden;
  border-radius: 30px;
  border: 1px solid rgba(42, 94, 67, 0.1);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.62),
    0 20px 50px rgba(36, 69, 54, 0.08);
}

.hero__copy {
  padding: clamp(28px, 4vw, 42px);
  background:
    radial-gradient(circle at top left, rgba(166, 218, 191, 0.28), transparent 28%),
    linear-gradient(145deg, rgba(255, 255, 255, 0.98), rgba(242, 249, 245, 0.96));
}

.hero__copy::after {
  content: '';
  position: absolute;
  right: -32px;
  bottom: -40px;
  width: 180px;
  aspect-ratio: 1;
  border-radius: 999px;
  border: 1px solid rgba(85, 142, 111, 0.12);
  background: rgba(230, 216, 164, 0.16);
  filter: blur(1px);
}

.hero__eyebrow {
  margin: 0 0 12px;
  color: #2f7d5d;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.hero h1 {
  margin: 0;
  color: #1d3d2f;
  font-size: clamp(34px, 5vw, 52px);
  line-height: 1.02;
  letter-spacing: -0.04em;
  font-family: 'STZhongsong', 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.hero__text {
  max-width: 720px;
  margin: 18px 0 0;
  color: #597467;
  font-size: 15px;
  line-height: 1.85;
}

.hero__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 28px;
}

.hero__action {
  min-height: 50px;
  padding: 0 20px;
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

.hero__action:hover,
.feature-card__action:hover {
  transform: translateY(-1px);
}

.hero__action--primary {
  border: none;
  color: #fffdf8;
  background: linear-gradient(135deg, #2f8f68 0%, #2a7b59 100%);
  box-shadow: 0 14px 30px rgba(43, 120, 84, 0.22);
}

.hero__action--ghost {
  border: 1px solid rgba(129, 172, 147, 0.3);
  color: #244536;
  background: rgba(255, 255, 255, 0.84);
}

.hero__panel {
  padding: 22px;
  background:
    radial-gradient(circle at top right, rgba(166, 218, 191, 0.22), transparent 34%),
    linear-gradient(180deg, rgba(249, 252, 250, 0.98), rgba(241, 248, 244, 0.96));
}

.hero__panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.hero__panel-head span {
  color: #607b6e;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.hero__panel-head b {
  color: #2b5a43;
  font-size: 13px;
}

.hero__panel-grid {
  display: grid;
  gap: 12px;
}

.hero__meta-card {
  display: grid;
  gap: 8px;
  padding: 16px;
  border-radius: 22px;
  border: 1px solid rgba(213, 227, 219, 0.92);
  background: rgba(255, 255, 255, 0.72);
}

.hero__meta-card span {
  color: #6a8478;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.hero__meta-card strong {
  color: #224334;
  font-size: 20px;
  line-height: 1.35;
}

.hero__meta-card small {
  color: #688174;
  line-height: 1.6;
}

.service-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.service-strip__card {
  display: grid;
  gap: 8px;
  padding: 18px 20px;
  border-radius: 24px;
  border: 1px solid rgba(42, 94, 67, 0.08);
  background: rgba(255, 255, 255, 0.72);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.56);
}

.service-strip__card span {
  color: #658074;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.14em;
}

.service-strip__card strong {
  color: #214334;
  font-size: 20px;
}

.service-strip__card p {
  margin: 0;
  color: #60796d;
  line-height: 1.7;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.feature-card {
  position: relative;
  display: grid;
  gap: 18px;
  padding: 24px;
  border-radius: 28px;
  border: 1px solid rgba(42, 94, 67, 0.08);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 251, 249, 0.94)),
    repeating-linear-gradient(
      180deg,
      rgba(255, 255, 255, 0.08) 0,
      rgba(255, 255, 255, 0.08) 14px,
      rgba(245, 250, 246, 0.06) 14px,
      rgba(245, 250, 246, 0.06) 28px
    );
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.7),
    0 18px 38px rgba(36, 69, 54, 0.08);
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    border-color 0.2s ease;
}

.feature-card::after {
  content: '';
  position: absolute;
  inset: auto -28px -28px auto;
  width: 120px;
  aspect-ratio: 1;
  border-radius: 999px;
  opacity: 0.6;
  filter: blur(24px);
}

.feature-card:hover {
  transform: translateY(-3px);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.72),
    0 24px 42px rgba(36, 69, 54, 0.12);
}

.feature-card--sage::after {
  background: rgba(170, 219, 190, 0.32);
}

.feature-card--gold::after {
  background: rgba(231, 217, 165, 0.38);
}

.feature-card--forest::after {
  background: rgba(112, 173, 142, 0.28);
}

.feature-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.feature-card__tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 32px;
  width: fit-content;
  padding: 0 12px;
  border-radius: 999px;
  background: #ebf5ef;
  color: #2f7d5d;
  font-size: 12px;
  font-weight: 900;
}

.feature-card__head b {
  color: #6d8378;
  font-size: 13px;
  letter-spacing: 0.16em;
}

.feature-card__body {
  display: grid;
  gap: 12px;
}

.feature-card h2 {
  margin: 0;
  color: #1f3e31;
  font-size: 26px;
  line-height: 1.18;
}

.feature-card p {
  margin: 0;
  color: #5b7568;
  line-height: 1.78;
}

.feature-card__list {
  margin: 0;
  padding-left: 18px;
  color: #315245;
  display: grid;
  gap: 8px;
}

.feature-card__action {
  min-height: 48px;
  border: none;
  border-radius: 16px;
  color: #ffffff;
  background: linear-gradient(135deg, #2f8f68 0%, #2a7b59 100%);
  font: inherit;
  font-weight: 900;
  cursor: pointer;
  box-shadow: 0 12px 24px rgba(43, 120, 84, 0.18);
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease;
}

@media (max-width: 1100px) {
  .hero,
  .feature-grid,
  .service-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .hero__copy {
    grid-column: 1 / -1;
  }
}

@media (max-width: 820px) {
  .portal-page {
    padding: 20px 16px;
    border-radius: 24px;
  }

  .hero,
  .feature-grid,
  .service-strip {
    grid-template-columns: 1fr;
  }

  .hero__copy,
  .hero__panel,
  .feature-card {
    border-radius: 24px;
  }

  .hero__actions {
    flex-direction: column;
  }

  .hero__action {
    width: 100%;
  }
}
</style>
