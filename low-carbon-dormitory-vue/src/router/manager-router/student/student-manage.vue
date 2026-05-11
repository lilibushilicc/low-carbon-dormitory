<template>
  <section class="dorm-page panel">
    <section class="hero-card">
      <div class="hero-card__head">
        <div>
          <div class="hero-card__eyebrow">宿舍管理 / 学生管理</div>
          <h1>学生管理</h1>
          <p>查看当前学生档案，并在无历史业务数据关联时执行删除。</p>
        </div>

        <div class="actions">
          <el-button :loading="loading" @click="loadStudents">刷新列表</el-button>
          <el-button type="primary" @click="goCreate">新增学生</el-button>
        </div>
      </div>
    </section>

    <section class="content-card">
      <div class="toolbar">
        <el-input
          v-model="keyword"
          clearable
          placeholder="按学号、姓名、宿舍或学院筛选"
        />
        <div class="summary">
          <strong>{{ filteredStudents.length }}</strong>
          <span>名学生</span>
        </div>
      </div>

      <el-table
        :data="filteredStudents"
        v-loading="loading"
        row-key="studentId"
        empty-text="暂无学生数据"
      >
        <el-table-column prop="stuNum" label="学号" min-width="140" />
        <el-table-column prop="name" label="姓名" min-width="120" />
        <el-table-column label="宿舍" min-width="140">
          <template #default="{ row }">
            {{ formatDorm(row) }}
          </template>
        </el-table-column>
        <el-table-column prop="college" label="学院" min-width="160" show-overflow-tooltip />
        <el-table-column prop="major" label="专业" min-width="160" show-overflow-tooltip />
        <el-table-column prop="carbonScore" label="低碳积分" min-width="100" />
        <el-table-column label="创建时间" min-width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button
              type="danger"
              plain
              :loading="deletingId === row.studentId"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import {
  deleteStudentByAdmin,
  fetchStudentDeleteCheckByAdmin,
  fetchStudentsByAdmin,
  type AdminStudentListItem,
} from '@/api/modules/admin'

const router = useRouter()
const loading = ref(false)
const deletingId = ref<number | null>(null)
const keyword = ref('')
const students = ref<AdminStudentListItem[]>([])

const filteredStudents = computed(() => {
  const query = keyword.value.trim().toLowerCase()
  if (!query) {
    return students.value
  }

  return students.value.filter((item) => buildStudentSearchText(item).includes(query))
})

function formatDorm(student: AdminStudentListItem) {
  const building = student.dormBuilding?.trim() ?? ''
  const room = student.dormRoom?.trim() ?? ''
  if (building && room) {
    return `${building}-${room}`
  }
  return building || room || '-'
}

function formatDateTime(value?: string) {
  if (!value) {
    return '-'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return date.toLocaleString('zh-CN', { hour12: false })
}

function buildStudentSearchText(student: AdminStudentListItem) {
  return [
    student.stuNum,
    student.name,
    student.college,
    student.major,
    student.className,
    student.grade,
    student.dormBuilding,
    student.dormRoom,
    formatDorm(student),
  ]
    .filter(Boolean)
    .join(' ')
    .toLowerCase()
}

function goCreate() {
  router.push('/manager/student-create')
}

async function loadStudents() {
  loading.value = true
  try {
    const { data } = await fetchStudentsByAdmin()
    if (data.code !== 200) {
      ElMessage.error(data.msg || '加载学生列表失败')
      return
    }
    students.value = data.data ?? []
  } catch (error) {
    console.error(error)
    ElMessage.error('加载学生列表失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

async function handleDelete(student: AdminStudentListItem) {
  deletingId.value = student.studentId
  try {
    const { data: checkData } = await fetchStudentDeleteCheckByAdmin(student.studentId)
    if (checkData.code !== 200 || !checkData.data) {
      ElMessage.error(checkData.msg || '删除校验失败')
      return
    }

    if (!checkData.data.deletable) {
      ElMessage.warning(checkData.data.reason || '当前学生暂不允许删除')
      return
    }

    await ElMessageBox.confirm(
      `确认删除学生“${student.name}（${student.stuNum}）”吗？该操作会同步清理扩展档案并重算宿舍空床数。`,
      '删除学生',
      {
        type: 'warning',
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
      },
    )

    const { data } = await deleteStudentByAdmin(student.studentId)
    if (data.code !== 200) {
      ElMessage.error(data.msg || '删除学生失败')
      return
    }

    ElMessage.success('学生已删除')
    await loadStudents()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      return
    }
    console.error(error)
    ElMessage.error('删除学生失败，请稍后重试')
  } finally {
    deletingId.value = null
  }
}

onMounted(() => {
  void loadStudents()
})
</script>

<style scoped>
.panel {
  display: grid;
  gap: 18px;
}

.hero-card__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.hero-card h1 {
  margin: 12px 0 8px;
  color: #244536;
}

.hero-card p {
  margin: 0;
  color: #5f776b;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.toolbar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 14px;
  align-items: center;
  margin-bottom: 16px;
}

.summary {
  display: inline-flex;
  align-items: baseline;
  gap: 6px;
  color: #4f6a5b;
  white-space: nowrap;
}

.summary strong {
  font-size: 22px;
  color: #204232;
}

@media (max-width: 900px) {
  .hero-card__head,
  .toolbar {
    grid-template-columns: 1fr;
    display: grid;
  }

  .actions {
    justify-content: flex-start;
  }
}
</style>
