<template>
  <el-container class="resume-page">
    <el-header class="page-header">
      <div>
        <el-page-header @back="$router.push('/')">
          <template #content>
            <span class="page-title">简历管理</span>
          </template>
        </el-page-header>
      </div>
    </el-header>

    <el-main>
      <el-card class="upload-card">
        <el-upload
          drag
          action="#"
          accept=".pdf,.doc,.docx,.txt"
          :auto-upload="false"
          :limit="1"
          :on-change="handleFileChange"
          :file-list="fileList"
        >
          <el-icon class="el-icon--upload"><upload-filled /></el-icon>
          <div class="el-upload__text">拖拽文件到此处，或 <em>点击选择</em></div>
          <template #tip>
            <div class="el-upload__tip">支持 PDF / Word / TXT，单个文件不超过 10MB</div>
          </template>
        </el-upload>
        <el-button
          type="primary"
          class="upload-btn"
          :loading="uploading"
          :disabled="!selectedFile"
          @click="handleUpload"
        >
          上传简历
        </el-button>
      </el-card>

      <el-card class="list-card">
        <template #header>
          <span>我的简历</span>
        </template>

        <el-table v-loading="loading" :data="resumes" empty-text="还没有简历，先上传一份吧">
          <el-table-column prop="fileName" label="文件名" min-width="200" show-overflow-tooltip />
          <el-table-column label="大小" width="110">
            <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
          </el-table-column>
          <el-table-column label="解析状态" width="110">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="上传时间" width="170">
            <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <el-button
                type="primary"
                link
                :loading="analyzingId === row.id"
                :disabled="row.status !== 2"
                @click="handleAnalyze(row)"
              >
                {{ analyzingId === row.id ? '分析中' : 'AI 分析' }}
              </el-button>
              <el-button type="success" link :disabled="!hasReport(row.id)" @click="handleViewReport(row)">
                查看报告
              </el-button>
              <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </el-main>

    <el-dialog v-model="reportVisible" title="简历分析报告" width="640px" top="8vh">
      <div v-if="report" v-loading="reportLoading">
        <div class="report-score">
          <el-progress
            type="dashboard"
            :percentage="report.totalScore || 0"
            :width="150"
            :color="scoreColor"
          />
          <div class="report-score-label">综合评分</div>
        </div>

        <div class="dimensions">
          <div v-for="(score, dim) in report.dimensionScores" :key="dim" class="dimension-item">
            <div class="dimension-label">{{ dimensionLabels[dim] || dim }}</div>
            <el-progress :percentage="score || 0" :stroke-width="10" :color="scoreColor" />
          </div>
        </div>

        <template v-if="report.status === 1">
          <h4 class="report-section">✅ 优点</h4>
          <ul class="report-list">
            <li v-for="(item, i) in report.strengths" :key="i">{{ item }}</li>
          </ul>

          <h4 class="report-section">⚠️ 不足</h4>
          <ul class="report-list">
            <li v-for="(item, i) in report.weaknesses" :key="i">{{ item }}</li>
          </ul>

          <h4 class="report-section">💡 改进建议</h4>
          <ul class="report-list">
            <li v-for="(item, i) in report.suggestions" :key="i">{{ item }}</li>
          </ul>
        </template>
        <el-alert v-else type="error" title="本次分析失败，请重试" :closable="false" />
      </div>
    </el-dialog>
  </el-container>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import {
  uploadResume,
  getResumes,
  deleteResume,
  analyzeResume,
  getResumeAnalysis
} from '@/api/resume'

const resumes = ref([])
const loading = ref(false)
const uploading = ref(false)
const selectedFile = ref(null)
const fileList = ref([])
const analyzingId = ref(null)
const reportVisible = ref(false)
const reportLoading = ref(false)
const report = ref(null)
const reportIds = ref(new Set())

const dimensionLabels = {
  content: '内容质量',
  structure: '结构清晰度',
  keywords: '关键词覆盖',
  quantification: '量化程度'
}

const statusMap = {
  0: { text: '待解析', type: 'info' },
  1: { text: '解析中', type: 'warning' },
  2: { text: '已解析', type: 'success' },
  3: { text: '解析失败', type: 'danger' }
}

function statusText(status) {
  return statusMap[status]?.text || '未知'
}

function statusType(status) {
  return statusMap[status]?.type || 'info'
}

function formatSize(size) {
  if (!size) return '-'
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

function formatDate(date) {
  return date ? String(date).replace('T', ' ').slice(0, 16) : '-'
}

function scoreColor(percentage) {
  if (percentage >= 80) return '#67c23a'
  if (percentage >= 60) return '#e6a23c'
  return '#f56c6c'
}

function handleFileChange(file) {
  selectedFile.value = file.raw
}

async function handleUpload() {
  if (!selectedFile.value) return
  uploading.value = true
  try {
    await uploadResume(selectedFile.value)
    ElMessage.success('上传成功')
    fileList.value = []
    selectedFile.value = null
    await loadResumes()
  } finally {
    uploading.value = false
  }
}

async function loadResumes() {
  loading.value = true
  try {
    const res = await getResumes()
    resumes.value = res.data
    await refreshReportStatus()
  } finally {
    loading.value = false
  }
}

async function refreshReportStatus() {
  const ids = []
  for (const item of resumes.value) {
    if (item.status === 2) ids.push(item.id)
  }
  const result = new Set()
  await Promise.all(
    ids.map(async (id) => {
      try {
        await getResumeAnalysis(id)
        result.add(id)
      } catch {
        // 尚无报告，忽略
      }
    })
  )
  reportIds.value = result
}

function hasReport(id) {
  return reportIds.value.has(id)
}

async function handleAnalyze(row) {
  analyzingId.value = row.id
  try {
    const res = await analyzeResume(row.id)
    report.value = res.data
    reportIds.value.add(row.id)
    reportVisible.value = true
  } finally {
    analyzingId.value = null
  }
}

async function handleViewReport(row) {
  reportLoading.value = true
  reportVisible.value = true
  try {
    const res = await getResumeAnalysis(row.id)
    report.value = res.data
  } finally {
    reportLoading.value = false
  }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除简历「${row.fileName}」吗？`, '提示', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  })
  await deleteResume(row.id)
  ElMessage.success('已删除')
  await loadResumes()
}

onMounted(loadResumes)
</script>

<style scoped>
.resume-page {
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

.upload-card {
  margin-bottom: 20px;
}

.upload-btn {
  margin-top: 16px;
}

.report-score {
  text-align: center;
  margin-bottom: 20px;
}

.report-score-label {
  margin-top: 4px;
  color: #909399;
  font-size: 14px;
}

.dimensions {
  margin-bottom: 20px;
}

.dimension-item {
  margin-bottom: 12px;
}

.dimension-label {
  margin-bottom: 4px;
  font-size: 13px;
  color: #606266;
}

.report-section {
  margin: 16px 0 8px;
  color: #303133;
}

.report-list {
  padding-left: 20px;
  color: #606266;
  line-height: 1.8;
}
</style>
