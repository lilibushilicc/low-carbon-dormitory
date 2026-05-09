<template>
  <div class="portal-page">
    <section class="hero">
      <div class="hero__copy">
        <p class="hero__eyebrow">Student Center</p>
        <h1>学生宿舍端</h1>
        <p class="hero__text">{{ welcomeText }}</p>
      </div>

      <div class="hero__meta">
        <div class="hero__meta-card">
          <span>当前身份</span>
          <strong>{{ studentName }}</strong>
        </div>
        <div class="hero__meta-card">
          <span>宿舍信息</span>
          <strong>{{ dormLabelText }}</strong>
        </div>
      </div>
    </section>

    <section class="feature-grid">
      <article v-for="feature in featureCards" :key="feature.path" class="feature-card">
        <span class="feature-card__tag">{{ feature.tag }}</span>
        <h2>{{ feature.title }}</h2>
        <p>{{ feature.description }}</p>

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
const dormLabelText = computed(() => (dormLabel.value && dormLabel.value !== '-' ? dormLabel.value : '暂未识别宿舍'))

const welcomeText = computed(() => {
  return `${studentName.value}，你现在进入的是学生宿舍端，这里提供个人资料、宿舍费用、低碳积分和奖励兑换等服务。`
})

const featureCards = [
  {
    tag: 'Profile',
    title: '个人信息与宿舍档案',
    description: '查看学号、院系、班级、宿舍楼栋和房间等基础信息。',
    highlights: ['学生档案信息', '宿舍楼栋与房间', '联系方式与入住信息'],
    action: '进入个人信息',
    path: '/personal-info',
  },
  {
    tag: 'Billing',
    title: '宿舍水电费',
    description: '查询宿舍账户余额、历史费用和当前账单情况。',
    highlights: ['剩余金额与用量', '历史账单查询', '缴费与扣费结果查看'],
    action: '进入费用中心',
    path: '/water-electricity',
  },
  {
    tag: 'Low Carbon',
    title: '低碳积分服务',
    description: '查看个人低碳表现、积分规则，并参与奖励兑换。',
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
  min-height: 100vh;
  display: grid;
  gap: 24px;
  padding: clamp(20px, 3vw, 36px);
  background:
    radial-gradient(circle at 8% 0%, rgba(119, 178, 147, 0.22), transparent 34%),
    radial-gradient(circle at 100% 90%, rgba(86, 135, 111, 0.12), transparent 38%),
    linear-gradient(180deg, #ecf6f1 0%, #f7fbf9 100%);
}

.hero {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(280px, 0.9fr);
  gap: 20px;
  padding: clamp(24px, 4vw, 38px);
  border: 1px solid rgba(42, 94, 67, 0.1);
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow:
    0 20px 50px rgba(36, 69, 54, 0.1),
    0 6px 16px rgba(36, 69, 54, 0.05);
}

.hero__eyebrow {
  margin: 0 0 10px;
  color: #2f7d5d;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.hero h1 {
  margin: 0;
  color: #1e3e31;
  font-size: clamp(30px, 4vw, 42px);
  line-height: 1.1;
}

.hero__text {
  margin: 14px 0 0;
  color: #5b7568;
  font-size: 15px;
  line-height: 1.8;
}

.hero__meta {
  display: grid;
  gap: 14px;
}

.hero__meta-card {
  padding: 18px;
  border-radius: 20px;
  background: linear-gradient(160deg, rgba(235, 245, 239, 0.98), rgba(247, 251, 249, 0.98));
  border: 1px solid #d5e3db;
}

.hero__meta-card span {
  display: block;
  margin-bottom: 10px;
  color: #648073;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.hero__meta-card strong {
  color: #244536;
  font-size: 18px;
  line-height: 1.4;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.feature-card {
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

.feature-card__tag {
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

.feature-card h2 {
  margin: 0;
  color: #1f3e31;
  font-size: 24px;
}

.feature-card p {
  margin: 0;
  color: #5b7568;
  line-height: 1.7;
}

.feature-card__list {
  margin: 0;
  padding-left: 18px;
  color: #315245;
  display: grid;
  gap: 8px;
}

.feature-card__action {
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

@media (max-width: 1100px) {
  .feature-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .hero {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 680px) {
  .feature-grid {
    grid-template-columns: 1fr;
  }
}
</style>
