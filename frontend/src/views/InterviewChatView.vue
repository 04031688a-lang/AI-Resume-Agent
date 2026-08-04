<template>
  <el-container class="chat-page" v-loading="loading">
    <el-header class="chat-header">
      <el-page-header @back="$router.push('/interviews')">
        <template #content>
          <span class="chat-title">{{ session?.title || 'AI 模拟面试' }}</span>
        </template>
      </el-page-header>
      <div class="chat-meta">
        <el-tag size="small" :type="session?.status === 0 ? 'warning' : 'success'">
          {{ session?.status === 0 ? '进行中' : '已完成' }}
        </el-tag>
        <span class="round-text">第 {{ session?.currentRound || 0 }} / 6 轮</span>
        <el-button
          v-if="session?.status === 0"
          type="success"
          size="small"
          :loading="finishing"
          :disabled="messages.length < 2"
          @click="handleFinish"
        >
          完成面试并生成报告
        </el-button>
      </div>
    </el-header>

    <el-main class="chat-body">
      <div ref="messageListRef" class="message-list">
        <div
          v-for="(msg, index) in messages"
          :key="index"
          class="message-row"
          :class="msg.role === 'user' ? 'user' : 'assistant'"
        >
          <div class="message-bubble" :class="msg.role">
            <span class="message-content" style="white-space: pre-wrap">{{ msg.content }}</span>
            <span v-if="msg.streaming" class="streaming-dot">▋</span>
          </div>
        </div>
        <el-empty
          v-if="messages.length === 0"
          description="加载面试内容..."
          :image-size="80"
        />
      </div>

      <!-- 报告 -->
      <el-card v-if="report" class="report-card" shadow="never">
        <template #header>
          <div class="report-header">
            <span>📋 面试报告</span>
            <el-button type="primary" link @click="reportVisible = true">查看详情</el-button>
          </div>
        </template>
        <div class="report-summary">
          <el-progress
            type="dashboard"
            :percentage="report.totalScore || 0"
            :width="110"
            :color="scoreColor"
          />
          <div class="report-summary-text">
            <h4>整体总结</h4>
            <p>{{ report.summary }}</p>
          </div>
        </div>
      </el-card>

      <div v-if="session?.status === 0" class="input-area">
        <el-input
          v-model="input"
          type="textarea"
          :rows="3"
          :disabled="streaming"
          placeholder="输入你的回答，Enter 发送（Shift+Enter 换行）"
          @keydown.enter.exact.prevent="handleSend"
        />
        <div class="input-actions">
          <span v-if="streaming" class="streaming-hint">AI 正在回复...</span>
          <el-button type="primary" :loading="streaming" :disabled="!input.trim()" @click="handleSend">
            发送
          </el-button>
        </div>
      </div>
    </el-main>

    <!-- 报告详情弹窗 -->
    <el-dialog v-model="reportVisible" title="面试报告详情" width="680px" top="6vh">
      <div v-if="report" class="report-detail">
        <div class="report-score">
          <el-progress
            type="dashboard"
            :percentage="report.totalScore || 0"
            :width="150"
            :color="scoreColor"
          />
          <div class="report-score-label">综合评分</div>
        </div>

        <h4 class="section-title">维度评分</h4>
        <div v-for="(score, dim) in report.dimensionScores" :key="dim" class="dimension-item">
          <div class="dimension-label">{{ dimensionLabels[dim] || dim }}</div>
          <el-progress :percentage="score || 0" :stroke-width="10" :color="scoreColor" />
        </div>

        <h4 class="section-title">逐题点评</h4>
        <el-timeline v-if="report.questionReviews && report.questionReviews.length">
          <el-timeline-item
            v-for="(item, i) in report.questionReviews"
            :key="i"
            :timestamp="`第 ${i + 1} 题`"
            placement="top"
          >
            <p class="review-question">{{ item.question }}</p>
            <p class="review-comment">{{ item.comment }}</p>
          </el-timeline-item>
        </el-timeline>

        <h4 class="section-title">整体总结</h4>
        <p class="summary-text">{{ report.summary }}</p>

        <h4 class="section-title">改进建议</h4>
        <ul class="suggestion-list">
          <li v-for="(item, i) in report.suggestions" :key="i">{{ item }}</li>
        </ul>
      </div>
    </el-dialog>
  </el-container>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getInterview, sendAnswer, finishInterview, getInterviewReport } from '@/api/interview'

const route = useRoute()
const sessionId = route.params.id

const loading = ref(false)
const streaming = ref(false)
const finishing = ref(false)
const session = ref(null)
const messages = ref([])
const input = ref('')
const report = ref(null)
const reportVisible = ref(false)
const messageListRef = ref()

const dimensionLabels = {
  expression: '表达清晰度',
  content: '内容质量',
  logic: '逻辑性',
  profession: '专业度'
}

function scoreColor(percentage) {
  if (percentage >= 80) return '#67c23a'
  if (percentage >= 60) return '#e6a23c'
  return '#f56c6c'
}

function scrollToBottom() {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
}

async function loadDetail() {
  loading.value = true
  try {
    const res = await getInterview(sessionId)
    session.value = res.data.session
    messages.value = res.data.messages.map((m) => ({ ...m, streaming: false }))
    if (session.value.status === 1) {
      await loadReport()
    }
    scrollToBottom()
  } finally {
    loading.value = false
  }
}

async function loadReport() {
  try {
    const res = await getInterviewReport(sessionId)
    report.value = res.data
  } catch {
    report.value = null
  }
}

async function handleSend() {
  const content = input.value.trim()
  if (!content || streaming.value) return
  input.value = ''

  messages.value.push({ role: 'user', content, streaming: false })
  const assistant = { role: 'assistant', content: '', streaming: true }
  messages.value.push(assistant)
  streaming.value = true
  scrollToBottom()

  await sendAnswer(sessionId, content, {
    onDelta: (obj) => {
      assistant.content += obj.content || ''
      scrollToBottom()
    },
    onEvent: (name, obj) => {
      if (name === 'error') {
        ElMessage.error(obj.message || 'AI 服务异常')
        assistant.streaming = false
      }
    },
    onError: (msg) => {
      ElMessage.error(msg)
      assistant.streaming = false
    },
    onDone: async () => {
      assistant.streaming = false
      streaming.value = false
      await loadDetail()
    }
  })
}

async function handleFinish() {
  await ElMessageBox.confirm('确定结束面试并生成报告吗？', '提示', {
    type: 'info',
    confirmButtonText: '生成报告',
    cancelButtonText: '再答一会儿'
  })
  finishing.value = true
  try {
    const res = await finishInterview(sessionId)
    report.value = res.data
    reportVisible.value = true
    await loadDetail()
  } finally {
    finishing.value = false
  }
}

onMounted(loadDetail)
</script>

<style scoped>
.chat-page {
  height: 100vh;
}

.chat-header {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.chat-title {
  font-size: 16px;
  font-weight: 600;
}

.chat-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.round-text {
  color: #909399;
  font-size: 13px;
}

.chat-body {
  display: flex;
  flex-direction: column;
  max-width: 860px;
  width: 100%;
  margin: 0 auto;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0 16px;
  min-height: 300px;
}

.message-row {
  display: flex;
  margin-bottom: 16px;
}

.message-row.user {
  justify-content: flex-end;
}

.message-bubble {
  max-width: 72%;
  padding: 10px 14px;
  border-radius: 10px;
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.message-bubble.user {
  background: #409eff;
  color: #fff;
  border-top-right-radius: 2px;
}

.message-bubble.assistant {
  background: #fff;
  border: 1px solid #e4e7ed;
  color: #303133;
  border-top-left-radius: 2px;
}

.streaming-dot {
  animation: blink 1s infinite;
  color: #67c23a;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.2; }
}

.report-card {
  margin-bottom: 16px;
  border: 1px solid #e4e7ed;
}

.report-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.report-summary {
  display: flex;
  gap: 20px;
  align-items: center;
}

.report-summary-text h4 {
  margin-bottom: 6px;
  color: #303133;
}

.report-summary-text p {
  color: #606266;
  font-size: 13px;
  line-height: 1.7;
}

.input-area {
  padding: 12px 0 4px;
  border-top: 1px solid #e4e7ed;
}

.input-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
  margin-top: 8px;
}

.streaming-hint {
  color: #909399;
  font-size: 13px;
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

.section-title {
  margin: 18px 0 10px;
  color: #303133;
}

.dimension-item {
  margin-bottom: 12px;
}

.dimension-label {
  margin-bottom: 4px;
  font-size: 13px;
  color: #606266;
}

.review-question {
  color: #303133;
  font-weight: 500;
  margin-bottom: 4px;
}

.review-comment {
  color: #606266;
  font-size: 13px;
  line-height: 1.7;
}

.summary-text {
  color: #606266;
  line-height: 1.8;
}

.suggestion-list {
  padding-left: 20px;
  color: #606266;
  line-height: 1.9;
}
</style>
