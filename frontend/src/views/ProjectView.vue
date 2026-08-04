<template>
  <el-container class="project-page">
    <el-header class="page-header">
      <el-page-header @back="$router.push('/')">
        <template #content>
          <span class="page-title">项目经历优化</span>
        </template>
      </el-page-header>
    </el-header>

    <el-main>
      <el-row :gutter="20">
        <el-col :xs="24" :md="10">
          <el-card>
            <template #header><span>填写项目信息</span></template>
            <el-form label-position="top">
              <el-form-item label="项目名称" required>
                <el-input v-model="form.projectName" placeholder="如：校园二手交易平台" maxlength="100" />
              </el-form-item>
              <el-form-item label="担任角色">
                <el-input v-model="form.role" placeholder="如：后端开发 / 项目负责人" maxlength="50" />
              </el-form-item>
              <el-form-item label="项目描述" required>
                <el-input
                  v-model="form.originalContent"
                  type="textarea"
                  :rows="10"
                  maxlength="5000"
                  show-word-limit
                  placeholder="用你自己的话描述这个项目：背景、你做了什么、遇到什么问题、最终成果……"
                />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="optimizing" :disabled="!canOptimize" @click="handleOptimize">
                  AI 优化（STAR）
                </el-button>
              </el-form-item>
            </el-form>
          </el-card>

          <el-card class="history-card">
            <template #header><span>优化历史</span></template>
            <el-table v-loading="historyLoading" :data="history" empty-text="暂无记录" size="small">
              <el-table-column prop="projectName" label="项目" min-width="120" show-overflow-tooltip />
              <el-table-column label="时间" width="110">
                <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
              </el-table-column>
              <el-table-column label="操作" width="80">
                <template #default="{ row }">
                  <el-button type="primary" link @click="handleView(row)">查看</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>

        <el-col :xs="24" :md="14">
          <el-card v-if="result" class="result-card">
            <template #header>
              <div class="result-header">
                <span>优化结果</span>
                <el-button type="primary" plain size="small" @click="copyResult">
                  一键复制
                </el-button>
              </div>
            </template>

            <h4 class="section-title">优化后文案</h4>
            <el-alert
              type="success"
              :closable="false"
              :title="result.optimizedContent || '（生成失败）'"
            />

            <h4 class="section-title">STAR 拆解</h4>
            <div class="star-grid">
              <div v-for="item in starItems" :key="item.key" class="star-item">
                <div class="star-label">{{ item.label }}</div>
                <div class="star-value">{{ result.starContent?.[item.key] || '—' }}</div>
              </div>
            </div>

            <h4 class="section-title">补充建议</h4>
            <ul class="suggestion-list">
              <li v-for="(item, i) in result.suggestions" :key="i">{{ item }}</li>
              <li v-if="!result.suggestions || result.suggestions.length === 0">暂无建议</li>
            </ul>
          </el-card>
          <el-empty v-else description="填写项目信息后点击「AI 优化」，这里会展示 STAR 改写结果" />
        </el-col>
      </el-row>
    </el-main>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { optimizeProject, getProjects, getProject } from '@/api/project'

const form = ref({ projectName: '', role: '', originalContent: '' })
const optimizing = ref(false)
const historyLoading = ref(false)
const history = ref([])
const result = ref(null)

const starItems = [
  { key: 'situation', label: '背景 Situation' },
  { key: 'task', label: '任务 Task' },
  { key: 'action', label: '行动 Action' },
  { key: 'result', label: '结果 Result' }
]

const canOptimize = computed(() =>
  form.value.projectName.trim() && form.value.originalContent.trim()
)

function formatDate(date) {
  return date ? String(date).replace('T', ' ').slice(0, 16) : '-'
}

async function handleOptimize() {
  optimizing.value = true
  try {
    const res = await optimizeProject({
      projectName: form.value.projectName.trim(),
      role: form.value.role.trim() || undefined,
      originalContent: form.value.originalContent.trim()
    })
    result.value = res.data
    await loadHistory()
  } finally {
    optimizing.value = false
  }
}

async function loadHistory() {
  historyLoading.value = true
  try {
    const res = await getProjects()
    history.value = res.data
  } finally {
    historyLoading.value = false
  }
}

async function handleView(row) {
  const res = await getProject(row.id)
  result.value = res.data
}

async function copyResult() {
  if (!result.value?.optimizedContent) return
  try {
    await navigator.clipboard.writeText(result.value.optimizedContent)
    ElMessage.success('已复制到剪贴板')
  } catch {
    ElMessage.error('复制失败，请手动选择复制')
  }
}

onMounted(loadHistory)
</script>

<style scoped>
.project-page {
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

.history-card {
  margin-top: 20px;
}

.result-card {
  min-height: 400px;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.section-title {
  margin: 18px 0 10px;
  color: #303133;
}

.star-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.star-item {
  background: #f5f7fa;
  border-radius: 8px;
  padding: 12px;
}

.star-label {
  font-size: 13px;
  font-weight: 600;
  color: #409eff;
  margin-bottom: 6px;
}

.star-value {
  color: #606266;
  font-size: 13px;
  line-height: 1.7;
}

.suggestion-list {
  padding-left: 20px;
  color: #606266;
  line-height: 1.9;
}
</style>
