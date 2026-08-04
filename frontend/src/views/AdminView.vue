<template>
  <el-container class="admin-page">
    <el-header class="page-header">
      <el-page-header @back="$router.push('/')">
        <template #content>
          <span class="page-title">管理后台</span>
        </template>
      </el-page-header>
    </el-header>

    <el-main>
      <el-card>
        <el-tabs v-model="activeTab">
          <!-- 数据统计 -->
          <el-tab-pane label="数据统计" name="stats">
            <el-row :gutter="16">
              <el-col v-for="item in statCards" :key="item.label" :xs="12" :sm="8" :md="6">
                <el-card class="stat-card" shadow="hover">
                  <div class="stat-value">{{ item.value }}</div>
                  <div class="stat-label">{{ item.label }}</div>
                </el-card>
              </el-col>
            </el-row>
          </el-tab-pane>

          <!-- 用户管理 -->
          <el-tab-pane label="用户管理" name="users">
            <div class="toolbar">
              <el-input
                v-model="userKeyword"
                placeholder="用户名 / 邮箱 / 学校"
                clearable
                style="width: 240px"
                @keyup.enter="loadUsers"
              />
              <el-button type="primary" @click="loadUsers">查询</el-button>
            </div>
            <el-table v-loading="userLoading" :data="users" empty-text="暂无用户">
              <el-table-column prop="username" label="用户名" min-width="120" />
              <el-table-column prop="email" label="邮箱" min-width="160" show-overflow-tooltip />
              <el-table-column prop="school" label="学校" min-width="130" show-overflow-tooltip />
              <el-table-column label="角色" width="80">
                <template #default="{ row }">
                  <el-tag :type="row.role === 1 ? 'danger' : 'info'" size="small">
                    {{ row.role === 1 ? '管理员' : '学生' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="90">
                <template #default="{ row }">
                  <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                    {{ row.status === 1 ? '正常' : '禁用' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="注册时间" width="160">
                <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
              </el-table-column>
              <el-table-column label="操作" width="160" fixed="right">
                <template #default="{ row }">
                  <el-button
                    type="warning"
                    link
                    @click="toggleUser(row)"
                  >
                    {{ row.status === 1 ? '禁用' : '启用' }}
                  </el-button>
                  <el-button type="danger" link @click="removeUser(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-pagination
              class="pagination"
              background
              layout="total, prev, pager, next"
              :total="userTotal"
              :page-size="userPageSize"
              :current-page="userPage"
              @current-change="(p) => { userPage = p; loadUsers() }"
            />
          </el-tab-pane>

          <!-- 岗位管理 -->
          <el-tab-pane label="岗位管理" name="jobs">
            <div class="toolbar">
              <el-button type="primary" @click="openJobDialog()">新增岗位</el-button>
            </div>
            <el-table v-loading="jobLoading" :data="jobs" empty-text="暂无岗位">
              <el-table-column prop="title" label="岗位" min-width="150" show-overflow-tooltip />
              <el-table-column prop="company" label="公司" min-width="120" show-overflow-tooltip />
              <el-table-column prop="location" label="地点" width="100" />
              <el-table-column label="薪资" width="110">
                <template #default="{ row }">{{ formatSalary(row) }}</template>
              </el-table-column>
              <el-table-column label="状态" width="90">
                <template #default="{ row }">
                  <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                    {{ row.status === 1 ? '上架' : '下架' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="200" fixed="right">
                <template #default="{ row }">
                  <el-button type="primary" link @click="openJobDialog(row)">编辑</el-button>
                  <el-button type="warning" link @click="toggleJob(row)">
                    {{ row.status === 1 ? '下架' : '上架' }}
                  </el-button>
                  <el-button type="danger" link @click="removeJob(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <!-- AI 配置 -->
          <el-tab-pane label="AI 配置" name="ai">
            <el-form label-width="110px" class="ai-form">
              <el-form-item label="API Key">
                <el-input
                  v-model="aiForm.apiKey"
                  type="password"
                  show-password
                  :placeholder="aiConfig.apiKeyConfigured ? `已配置：${aiConfig.apiKeyMasked}（留空则不修改）` : '未配置，请输入 DeepSeek API Key'"
                  style="width: 380px"
                />
              </el-form-item>
              <el-form-item label="模型">
                <el-input v-model="aiForm.model" placeholder="deepseek-chat" style="width: 380px" />
              </el-form-item>
              <el-form-item label="接口地址">
                <el-input v-model="aiForm.baseUrl" placeholder="https://api.deepseek.com" style="width: 380px" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="aiSaving" @click="handleSaveAiConfig">保存配置</el-button>
                <span class="tip">配置保存在数据库 ai_config 表，即时生效，无需重启</span>
              </el-form-item>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </el-main>

    <!-- 岗位编辑弹窗 -->
    <el-dialog v-model="jobDialogVisible" :title="jobForm.id ? '编辑岗位' : '新增岗位'" width="600px">
      <el-form :model="jobForm" label-width="90px">
        <el-form-item label="岗位名称" required>
          <el-input v-model="jobForm.title" placeholder="如：Java 后端开发工程师" />
        </el-form-item>
        <el-form-item label="公司" required>
          <el-input v-model="jobForm.company" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="行业">
              <el-input v-model="jobForm.industry" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="地点">
              <el-input v-model="jobForm.location" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="薪资下限(K)">
              <el-input-number v-model="jobForm.salaryMin" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="薪资上限(K)">
              <el-input-number v-model="jobForm.salaryMax" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="学历要求">
              <el-input v-model="jobForm.educationRequirement" placeholder="如：本科" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="经验要求">
              <el-input v-model="jobForm.experienceRequirement" placeholder="如：应届生" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="技能要求">
          <el-input v-model="jobForm.skillsText" placeholder="多个技能用逗号分隔，如：Java, Spring Boot, MySQL" />
        </el-form-item>
        <el-form-item label="岗位描述">
          <el-input v-model="jobForm.jobDescription" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="jobDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="jobSaving" @click="saveJob">保存</el-button>
      </template>
    </el-dialog>
  </el-container>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import {
  getAdminUsers,
  updateUserStatus,
  deleteAdminUser,
  getAdminStats,
  getAiConfig,
  saveAiConfig
} from '@/api/admin'
import { getJobs, createJob, updateJob, toggleJobStatus, deleteJob } from '@/api/job'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('stats')

// 统计
const stats = ref({})
const statCards = ref([])

// 用户
const users = ref([])
const userTotal = ref(0)
const userPage = ref(1)
const userPageSize = 10
const userKeyword = ref('')
const userLoading = ref(false)

// 岗位
const jobs = ref([])
const jobLoading = ref(false)
const jobDialogVisible = ref(false)
const jobSaving = ref(false)
const jobForm = ref(emptyJobForm())

// AI 配置
const aiConfig = ref({})
const aiForm = ref({ apiKey: '', model: '', baseUrl: '' })
const aiSaving = ref(false)

function emptyJobForm() {
  return {
    id: null,
    title: '',
    company: '',
    industry: '',
    location: '',
    salaryMin: null,
    salaryMax: null,
    educationRequirement: '',
    experienceRequirement: '',
    skillsText: '',
    jobDescription: ''
  }
}

function formatDate(date) {
  return date ? String(date).replace('T', ' ').slice(0, 16) : '-'
}

function formatSalary(row) {
  const min = row.salaryMin
  const max = row.salaryMax
  if (min == null && max == null) return '面议'
  if (min == null) return `${max}K 以下`
  if (max == null) return `${min}K 以上`
  return `${min}-${max}K`
}

async function loadStats() {
  const res = await getAdminStats()
  stats.value = res.data
  statCards.value = [
    { label: '用户总数', value: stats.value.userTotal },
    { label: '活跃用户', value: stats.value.userActive },
    { label: '简历数', value: stats.value.resumeTotal },
    { label: '简历分析次数', value: stats.value.resumeAnalysisTotal },
    { label: '岗位匹配次数', value: stats.value.jobMatchTotal },
    { label: '模拟面试次数', value: stats.value.interviewTotal },
    { label: '项目优化次数', value: stats.value.projectTotal }
  ]
}

async function loadUsers() {
  userLoading.value = true
  try {
    const res = await getAdminUsers({
      keyword: userKeyword.value || undefined,
      page: userPage.value,
      size: userPageSize
    })
    users.value = res.data.records
    userTotal.value = res.data.total
  } finally {
    userLoading.value = false
  }
}

async function toggleUser(row) {
  const next = row.status === 1 ? 0 : 1
  await updateUserStatus(row.id, next)
  ElMessage.success(next === 1 ? '已启用' : '已禁用')
  loadUsers()
}

async function removeUser(row) {
  await ElMessageBox.confirm(`确定删除用户「${row.username}」吗？该操作不可恢复。`, '危险操作', {
    type: 'error',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  })
  await deleteAdminUser(row.id)
  ElMessage.success('已删除')
  loadUsers()
}

async function loadJobs() {
  jobLoading.value = true
  try {
    const res = await getJobs({ page: 1, size: 50 })
    jobs.value = res.data.records
  } finally {
    jobLoading.value = false
  }
}

function openJobDialog(row) {
  if (row) {
    jobForm.value = {
      ...emptyJobForm(),
      id: row.id,
      title: row.title,
      company: row.company,
      industry: row.industry || '',
      location: row.location || '',
      salaryMin: row.salaryMin,
      salaryMax: row.salaryMax,
      educationRequirement: row.educationRequirement || '',
      experienceRequirement: row.experienceRequirement || '',
      skillsText: (row.skills || []).join(', '),
      jobDescription: row.jobDescription || ''
    }
  } else {
    jobForm.value = emptyJobForm()
  }
  jobDialogVisible.value = true
}

async function saveJob() {
  if (!jobForm.value.title || !jobForm.value.company) {
    ElMessage.warning('请填写岗位名称和公司')
    return
  }
  jobSaving.value = true
  try {
    const payload = {
      title: jobForm.value.title,
      company: jobForm.value.company,
      industry: jobForm.value.industry || undefined,
      location: jobForm.value.location || undefined,
      salaryMin: jobForm.value.salaryMin,
      salaryMax: jobForm.value.salaryMax,
      educationRequirement: jobForm.value.educationRequirement || undefined,
      experienceRequirement: jobForm.value.experienceRequirement || undefined,
      skills: jobForm.value.skillsText
        ? jobForm.value.skillsText.split(/[,，]/).map((s) => s.trim()).filter(Boolean)
        : undefined,
      jobDescription: jobForm.value.jobDescription || undefined
    }
    if (jobForm.value.id) {
      await updateJob(jobForm.value.id, payload)
    } else {
      await createJob(payload)
    }
    ElMessage.success('保存成功')
    jobDialogVisible.value = false
    loadJobs()
  } finally {
    jobSaving.value = false
  }
}

async function toggleJob(row) {
  const next = row.status === 1 ? 0 : 1
  await toggleJobStatus(row.id, next)
  ElMessage.success(next === 1 ? '已上架' : '已下架')
  loadJobs()
}

async function removeJob(row) {
  await ElMessageBox.confirm(`确定删除岗位「${row.title}」吗？`, '提示', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  })
  await deleteJob(row.id)
  ElMessage.success('已删除')
  loadJobs()
}

async function loadAiConfig() {
  const res = await getAiConfig()
  aiConfig.value = res.data
  aiForm.value = { apiKey: '', model: res.data.model, baseUrl: res.data.baseUrl }
}

async function handleSaveAiConfig() {
  aiSaving.value = true
  try {
    await saveAiConfig({
      apiKey: aiForm.value.apiKey || undefined,
      model: aiForm.value.model || undefined,
      baseUrl: aiForm.value.baseUrl || undefined
    })
    ElMessage.success('配置已保存，即时生效')
    aiForm.value.apiKey = ''
    loadAiConfig()
  } finally {
    aiSaving.value = false
  }
}

onMounted(() => {
  if (userStore.userInfo?.role !== 1) {
    ElMessage.error('无权访问管理后台')
    router.push('/')
    return
  }
  loadStats()
  loadUsers()
  loadJobs()
  loadAiConfig()
})
</script>

<style scoped>
.admin-page {
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

.stat-card {
  text-align: center;
  margin-bottom: 16px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #409eff;
}

.stat-label {
  margin-top: 6px;
  color: #909399;
  font-size: 13px;
}

.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}

.ai-form {
  max-width: 560px;
}

.tip {
  margin-left: 12px;
  color: #909399;
  font-size: 13px;
}
</style>
