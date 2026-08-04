<template>
  <el-container class="interview-page">
    <el-header class="page-header">
      <el-page-header @back="$router.push('/')">
        <template #content>
          <span class="page-title">AI 模拟面试</span>
        </template>
      </el-page-header>
    </el-header>

    <el-main>
      <el-card>
        <el-tabs v-model="activeTab">
          <el-tab-pane label="开始面试" name="create">
            <el-form label-width="90px" class="create-form">
              <el-form-item label="面试类型">
                <el-radio-group v-model="form.interviewType">
                  <el-radio-button value="general">通用面试</el-radio-button>
                  <el-radio-button value="technical">技术面试</el-radio-button>
                  <el-radio-button value="behavioral">行为面试</el-radio-button>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="关联岗位">
                <el-select
                  v-model="form.jobId"
                  placeholder="选填，按岗位定制面试题"
                  clearable
                  filterable
                  style="width: 320px"
                >
                  <el-option
                    v-for="job in jobs"
                    :key="job.id"
                    :label="`${job.title} @ ${job.company}`"
                    :value="job.id"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="目标企业">
                <el-input
                  v-model="form.targetCompany"
                  placeholder="选填，如：字节跳动（不选岗位时按企业真题风格出题）"
                  clearable
                  style="width: 320px"
                />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="creating" @click="handleCreate">
                  开始面试
                </el-button>
                <span class="tip">共 6 轮问答，结束后生成面试报告</span>
              </el-form-item>
            </el-form>
          </el-tab-pane>

          <el-tab-pane label="面试记录" name="history">
            <el-table v-loading="loading" :data="sessions" empty-text="还没有面试记录">
              <el-table-column prop="title" label="面试主题" min-width="200" show-overflow-tooltip />
              <el-table-column label="类型" width="110">
                <template #default="{ row }">
                  {{ typeLabel(row.interviewType) }}
                </template>
              </el-table-column>
              <el-table-column label="状态" width="100">
                <template #default="{ row }">
                  <el-tag :type="row.status === 0 ? 'warning' : 'success'">
                    {{ row.status === 0 ? '进行中' : '已完成' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="轮次" width="90">
                <template #default="{ row }">
                  {{ row.currentRound }}/6
                </template>
              </el-table-column>
              <el-table-column label="开始时间" width="170">
                <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
              </el-table-column>
              <el-table-column label="操作" width="120" fixed="right">
                <template #default="{ row }">
                  <el-button type="primary" link @click="$router.push(`/interviews/${row.id}`)">
                    {{ row.status === 0 ? '继续面试' : '查看' }}
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </el-main>
  </el-container>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createInterview, getInterviews } from '@/api/interview'
import { getJobs } from '@/api/job'

const router = useRouter()
const activeTab = ref('create')
const loading = ref(false)
const creating = ref(false)
const sessions = ref([])
const jobs = ref([])

const form = ref({
  interviewType: 'general',
  jobId: null,
  targetCompany: ''
})

const typeLabels = {
  general: '通用面试',
  technical: '技术面试',
  behavioral: '行为面试'
}

function typeLabel(type) {
  return typeLabels[type] || type
}

function formatDate(date) {
  return date ? String(date).replace('T', ' ').slice(0, 16) : '-'
}

async function loadSessions() {
  loading.value = true
  try {
    const res = await getInterviews()
    sessions.value = res.data
  } finally {
    loading.value = false
  }
}

async function loadJobs() {
  const res = await getJobs({ page: 1, size: 50 })
  jobs.value = res.data.records
}

async function handleCreate() {
  creating.value = true
  try {
    const res = await createInterview({
      interviewType: form.value.interviewType,
      jobId: form.value.jobId || undefined,
      targetCompany: form.value.targetCompany.trim() || undefined
    })
    ElMessage.success('面试已创建，第一个问题即将开始')
    router.push(`/interviews/${res.data.id}`)
  } finally {
    creating.value = false
  }
}

onMounted(() => {
  loadSessions()
  loadJobs()
})
</script>

<style scoped>
.interview-page {
  min-height: 100%;
}

.page-header {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
}

.create-form {
  max-width: 560px;
}

.tip {
  margin-left: 12px;
  color: #909399;
  font-size: 13px;
}
</style>
