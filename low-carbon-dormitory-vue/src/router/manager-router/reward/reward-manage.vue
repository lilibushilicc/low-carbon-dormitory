<template>
  <section class="dorm-page panel" v-loading="loading">
    <section class="hero-card">
      <div class="hero-card__head">
        <div>
          <div class="hero-card__eyebrow">宿舍管理 / 运营管理</div>
          <h1>奖励与 R2 存储配置</h1>
          <p>统一维护奖励商品、库存，以及奖励图片上传使用的 Cloudflare R2 连接参数。</p>
        </div>
        <div class="actions">
          <el-button @click="goHome">返回管理首页</el-button>
          <el-button @click="loadAll">刷新数据</el-button>
        </div>
      </div>
    </section>

    <section class="content-card">
      <h2>R2 对象存储配置</h2>
      <el-form label-position="top">
        <div class="reward-grid">
          <el-form-item label="Endpoint">
            <el-input
              v-model="r2ConfigForm.endpoint"
              placeholder="https://<account-id>.r2.cloudflarestorage.com"
            />
          </el-form-item>
          <el-form-item label="Access Key ID">
            <el-input v-model="r2ConfigForm.accessKeyId" />
          </el-form-item>
          <el-form-item label="Secret Access Key">
            <el-input v-model="r2ConfigForm.secretAccessKey" show-password />
          </el-form-item>
          <el-form-item label="Bucket">
            <el-input v-model="r2ConfigForm.bucket" />
          </el-form-item>
          <el-form-item label="Public Base URL">
            <el-input v-model="r2ConfigForm.publicBaseUrl" placeholder="https://pub-xxxx.r2.dev" />
          </el-form-item>
          <el-form-item label="Region">
            <el-input v-model="r2ConfigForm.region" placeholder="auto" />
          </el-form-item>
        </div>
        <div class="storage-actions">
          <el-button :loading="testingR2Config" @click="testR2Config">测试连接</el-button>
          <el-button :disabled="!r2ConfigDirty || savingR2Config" @click="resetR2ConfigForm">恢复已保存配置</el-button>
          <el-button type="primary" :loading="savingR2Config" @click="saveR2Config">保存配置</el-button>
          <span class="storage-status">当前状态：{{ r2ConfigForm.configured ? '已配置' : '未配置' }}</span>
        </div>
      </el-form>
    </section>

    <section class="content-card">
      <h2>新增奖励</h2>
      <el-form label-position="top">
        <div class="reward-grid">
          <el-form-item label="奖励名称">
            <el-input v-model="rewardForm.rewardName" />
          </el-form-item>
          <el-form-item label="奖励描述">
            <el-input v-model="rewardForm.rewardDesc" />
          </el-form-item>
          <el-form-item label="积分消耗">
            <el-input-number v-model="rewardForm.pointsCost" :min="0" />
          </el-form-item>
          <el-form-item label="库存">
            <el-input-number v-model="rewardForm.stock" :min="0" />
          </el-form-item>
          <el-form-item label="排序">
            <el-input-number v-model="rewardForm.sortOrder" :min="0" />
          </el-form-item>
          <el-form-item label="奖励图片(可选)">
            <div class="reward-upload-field">
              <input
                ref="rewardImageInputRef"
                type="file"
                accept="image/jpeg,image/png,image/webp"
                class="reward-upload-input"
                @change="handleRewardImageChange"
              />
              <div class="reward-upload-actions">
                <el-button :loading="uploadingImage" @click="openRewardImagePicker">选择图片</el-button>
                <el-button
                  v-if="rewardForm.imageUrl"
                  :disabled="uploadingImage"
                  text
                  type="danger"
                  @click="clearRewardImage"
                >
                  清除图片
                </el-button>
              </div>
              <div class="reward-upload-tip">支持 JPG、PNG、WEBP，大小不超过 2MB。</div>
              <el-input v-model="rewardForm.imageUrl" readonly placeholder="上传后自动生成图片地址" />
              <div v-if="rewardPreviewUrl" class="reward-upload-preview">
                <img :src="rewardPreviewUrl" alt="奖励图片预览" />
              </div>
            </div>
          </el-form-item>
        </div>
        <el-button type="primary" @click="createReward">创建奖励</el-button>
      </el-form>
    </section>

    <section class="content-card">
      <h2>奖励库存修改</h2>
      <el-table :data="rewards" border>
        <el-table-column prop="rewardId" label="ID" width="80" />
        <el-table-column prop="rewardName" label="奖励名称" min-width="160" />
        <el-table-column prop="pointsCost" label="所需积分" width="110" />
        <el-table-column label="库存" width="180">
          <template #default="{ row }">
            <el-input-number v-model="row.stock" :min="0" :step="1" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" text @click="saveStock(row)">保存库存</el-button>
            <el-button type="danger" text @click="removeReward(row)">删除奖品</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { resolveErrorMessage } from '@/utils/api-response'
import {
  createRewardByAdmin,
  deleteRewardByAdmin,
  fetchR2StorageConfigByAdmin,
  fetchRewardsByAdmin,
  testR2StorageConfigByAdmin,
  updateR2StorageConfigByAdmin,
  updateRewardStockByAdmin,
  uploadRewardImageByAdmin,
  type AdminR2StorageConfig,
  type AdminRewardItem,
} from '@/api/modules/admin'

const router = useRouter()
const loading = ref(false)
const uploadingImage = ref(false)
const savingR2Config = ref(false)
const testingR2Config = ref(false)
const rewards = ref<AdminRewardItem[]>([])
const rewardImageInputRef = ref<HTMLInputElement | null>(null)
const rewardPreviewUrl = ref('')
const r2ConfigForm = reactive<AdminR2StorageConfig>({
  endpoint: '',
  accessKeyId: '',
  secretAccessKey: '',
  bucket: '',
  publicBaseUrl: '',
  region: 'auto',
  configured: false,
})
const savedR2Config = ref<AdminR2StorageConfig>({
  endpoint: '',
  accessKeyId: '',
  secretAccessKey: '',
  bucket: '',
  publicBaseUrl: '',
  region: 'auto',
  configured: false,
})
const rewardForm = reactive({
  rewardName: '',
  rewardDesc: '',
  pointsCost: 20,
  stock: 10,
  sortOrder: 100,
  imageUrl: '',
})

function normalizeR2Config(config: AdminR2StorageConfig) {
  return {
    endpoint: config.endpoint.trim(),
    accessKeyId: config.accessKeyId.trim(),
    secretAccessKey: config.secretAccessKey.trim(),
    bucket: config.bucket.trim(),
    publicBaseUrl: config.publicBaseUrl.trim(),
    region: config.region.trim() || 'auto',
    configured: config.configured,
  }
}

function cacheSavedR2Config(config: AdminR2StorageConfig) {
  savedR2Config.value = { ...normalizeR2Config(config) }
}

function getR2SavePayload() {
  const normalized = normalizeR2Config(r2ConfigForm)
  return {
    endpoint: normalized.endpoint,
    accessKeyId: normalized.accessKeyId,
    secretAccessKey: normalized.secretAccessKey,
    bucket: normalized.bucket,
    publicBaseUrl: normalized.publicBaseUrl,
    region: normalized.region,
  }
}

const r2ConfigDirty = computed(() => {
  const current = normalizeR2Config(r2ConfigForm)
  const saved = normalizeR2Config(savedR2Config.value)
  return current.endpoint !== saved.endpoint
    || current.accessKeyId !== saved.accessKeyId
    || current.secretAccessKey !== saved.secretAccessKey
    || current.bucket !== saved.bucket
    || current.publicBaseUrl !== saved.publicBaseUrl
    || current.region !== saved.region
})

function buildR2ConfirmMessage(payload: ReturnType<typeof getR2SavePayload>) {
  return [
    '将保存以下 R2 配置：',
    `Endpoint：${payload.endpoint}`,
    `Access Key ID：${payload.accessKeyId}`,
    `Bucket：${payload.bucket}`,
    `Public Base URL：${payload.publicBaseUrl}`,
    `Region：${payload.region}`,
    '保存后奖励图片上传会优先使用这套配置。',
  ].join('\n')
}

function goHome() {
  router.push('/manager/home')
}

function openRewardImagePicker() {
  rewardImageInputRef.value?.click()
}

function clearRewardImage() {
  rewardForm.imageUrl = ''
  rewardPreviewUrl.value = ''
  if (rewardImageInputRef.value) {
    rewardImageInputRef.value.value = ''
  }
}

function resetR2ConfigForm() {
  Object.assign(r2ConfigForm, savedR2Config.value)
  ElMessage.success('已恢复到最近一次加载或保存的配置')
}

async function handleRewardImageChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) {
    return
  }

  uploadingImage.value = true
  try {
    const { data } = await uploadRewardImageByAdmin(file)
    if (data.code !== 200 || !data.data) {
      ElMessage.error(data.msg || '图片上传失败')
      return
    }
    rewardForm.imageUrl = data.data.imageUrl
    rewardPreviewUrl.value = data.data.imageUrl
    ElMessage.success('图片上传成功')
  } catch (error) {
    console.error(error)
    ElMessage.error(resolveErrorMessage(error, '图片上传失败'))
  } finally {
    uploadingImage.value = false
    if (rewardImageInputRef.value) {
      rewardImageInputRef.value.value = ''
    }
  }
}

async function saveR2Config() {
  const payload = getR2SavePayload()
  if (!r2ConfigDirty.value) {
    ElMessage.info('R2 配置未变更')
    return
  }

  try {
    await ElMessageBox.confirm(buildR2ConfirmMessage(payload), '确认保存 R2 配置', {
      type: 'warning',
      confirmButtonText: '确认保存',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }

  savingR2Config.value = true
  try {
    const { data } = await updateR2StorageConfigByAdmin(payload)
    if (data.code !== 200 || !data.data) {
      ElMessage.error(data.msg || 'R2 配置保存失败')
      return
    }
    Object.assign(r2ConfigForm, data.data)
    cacheSavedR2Config(data.data)
    ElMessage.success('R2 配置已保存')
  } catch (error) {
    console.error(error)
    ElMessage.error(resolveErrorMessage(error, 'R2 配置保存失败'))
  } finally {
    savingR2Config.value = false
  }
}

async function testR2Config() {
  testingR2Config.value = true
  try {
    const { data } = await testR2StorageConfigByAdmin(getR2SavePayload())
    if (data.code !== 200 || data.data !== true) {
      ElMessage.error(data.msg || 'R2 连接测试失败')
      return
    }
    ElMessage.success('R2 连接测试成功')
  } catch (error) {
    console.error(error)
    ElMessage.error(resolveErrorMessage(error, 'R2 连接测试失败'))
  } finally {
    testingR2Config.value = false
  }
}

async function loadAll() {
  loading.value = true
  try {
    const [rewardsRes, r2Res] = await Promise.all([
      fetchRewardsByAdmin(),
      fetchR2StorageConfigByAdmin(),
    ])
    if (rewardsRes.data.code === 200 && rewardsRes.data.data) {
      rewards.value = rewardsRes.data.data.map((item) => ({ ...item }))
    }
    if (r2Res.data.code === 200 && r2Res.data.data) {
      Object.assign(r2ConfigForm, r2Res.data.data)
      cacheSavedR2Config(r2Res.data.data)
    }
  } catch (error) {
    console.error(error)
    ElMessage.error('加载管理数据失败')
  } finally {
    loading.value = false
  }
}

async function createReward() {
  if (!rewardForm.rewardName.trim() || !rewardForm.rewardDesc.trim()) {
    ElMessage.error('请填写奖励名称和描述')
    return
  }

  try {
    const { data } = await createRewardByAdmin({
      rewardName: rewardForm.rewardName.trim(),
      rewardDesc: rewardForm.rewardDesc.trim(),
      pointsCost: rewardForm.pointsCost,
      stock: rewardForm.stock,
      sortOrder: rewardForm.sortOrder,
      imageUrl: rewardForm.imageUrl.trim() || undefined,
      status: 1,
    })

    if (data.code !== 200) {
      ElMessage.error(data.msg || '新增奖励失败')
      return
    }

    ElMessage.success('奖励新增成功')
    rewardForm.rewardName = ''
    rewardForm.rewardDesc = ''
    clearRewardImage()
    await loadAll()
  } catch (error) {
    console.error(error)
    ElMessage.error('新增奖励失败')
  }
}

async function saveStock(item: AdminRewardItem) {
  try {
    const { data } = await updateRewardStockByAdmin(item.rewardId, item.stock)
    if (data.code !== 200) {
      ElMessage.error(data.msg || '库存更新失败')
      return
    }
    ElMessage.success('库存更新成功')
  } catch (error) {
    console.error(error)
    ElMessage.error('库存更新失败')
  }
}

async function removeReward(item: AdminRewardItem) {
  try {
    await ElMessageBox.confirm(`确认删除奖品“${item.rewardName}”吗？删除后将不可恢复。`, '删除奖品', {
      type: 'warning',
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }

  try {
    const { data } = await deleteRewardByAdmin(item.rewardId)
    if (data.code !== 200) {
      ElMessage.error(data.msg || '删除奖品失败')
      return
    }
    ElMessage.success('奖品已删除')
    await loadAll()
  } catch (error) {
    console.error(error)
    ElMessage.error('删除奖品失败')
  }
}

loadAll()
</script>

<style scoped>
.panel {
  display: grid;
  gap: 18px;
}

.hero-card,
.content-card {
  padding: 24px;
}

.hero-card__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.hero-card h1 {
  margin: 12px 0 8px;
  color: #244536;
}

.hero-card p,
h2 {
  color: #244536;
}

.hero-card p {
  margin: 0;
  color: #5f776b;
}

h2 {
  margin: 0 0 12px;
}

.reward-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.reward-upload-field {
  display: grid;
  gap: 10px;
}

.reward-upload-input {
  display: none;
}

.reward-upload-actions,
.storage-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.reward-upload-tip,
.storage-status {
  font-size: 12px;
  color: #5f776b;
}

.reward-upload-preview {
  width: min(240px, 100%);
  overflow: hidden;
  border: 1px solid #dce7e1;
  border-radius: 12px;
  background: #f5faf7;
}

.reward-upload-preview img {
  display: block;
  width: 100%;
  aspect-ratio: 4 / 3;
  object-fit: cover;
}

@media (max-width: 1000px) {
  .reward-grid {
    grid-template-columns: 1fr;
  }

  .hero-card__head {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
