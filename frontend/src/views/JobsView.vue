<template>
  <el-container class="jobs-page">
    <el-header class="page-header">
      <el-page-header @back="$router.push('/')">
        <template #content>
          <span class="page-title">岗位匹配</span>
        </template>
      </el-page-header>
    </el-header>

    <el-main>
      <el-card>
        <el-tabs v-model="activeTab">
          <el-tab-pane label="岗位列表" name="jobs">
            <el-form inline class="filter-form" @submit.prevent>
              <el-form-item label="关键词">
                <el-input
                  v-model="query.keyword"
                  placeholder="岗位 / 公司"
                  clearable
                  style="width: 180px"
                  @keyup.enter="handleSearch"
                />
              </el-form-item>
              <el-form-item label="地点">
                <el-input
                  v-model="query.location"
                  placeholder="如：北京"
                  clearable
                  style="width: 130px"
                  @keyup.enter="handleSearch"
                />
              </el-form-item>
              <el-form-item label="行业">
                <el-select v-model="query.industry" clearable placeholder="全部" style="width: 130px">
                  <el-option v-for="item in industries" :key="item" :label="item" :value="item" />
                </el-select>
              </el-form-item>
              <el-form-item label="薪资">
                <el-select v-model="salaryRange" placeholder="不限" style="width: 130px">
                  <el-option
                    v-for="item in salaryOptions"
                    :key="item.label"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="handleSearch">查询</el-button>
                <el-button @click="handleReset">重置</el-button>
              </el-form-item>
            </el-form>

            <el-table v-loading="loading" :data="jobs" empty-text="暂无符合条件的岗位">
              <el-table-column prop="title" label="岗位名称" min-width="160" show-overflow-tooltip />
              <el-table-column prop="company" label="公司" min-width="120" show-overflow-tooltip />
              <el-table-column prop="location" label="地点" width="100" />
              <el-table-column label="薪资" width="110">
                <template #default="{ row }">{{ formatSalary(row) }}</template>
              </el-table-column>
              <el-table-column prop="educationRequirement" label="学历" width="80" />
              <el-table-column label="技能要求" min-width="180">
                <template #default="{ row }">
                  <el-tag
                    v-for="skill in (row.skills || [])"
                    :key="skill"
                    size="small"
                    class="skill-tag"
                  >
                    {{ skill }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="160" fixed="right">
                <template #default="{ row }">
                  <el-button type="primary" link @click="handleDetail(row)">详情</el-button>
                  <el-button
                    type="success"
                    link
                    :loading="matchingId === row.id"
                    @click="handleMatch(row)"
                  >
                    AI 匹配
                  </el-button>
                </template>
              </el-table-column>
            </el-table>

            <el-pagination
              class="pagination"
              background
              layout="total, prev, pager, next"
              :total="total"
              :page-size="query.size"
              :current-page="query.page"
              @current-change="handlePageChange"
            />
          </el-tab-pane>

          <el-tab-pane label="匹配历史" name="matches">
            <el-table v-loading="historyLoading" :data="matches" empty-text="暂无匹配记录">
              <el-table-column type="expand">
                <template #default="{ row }">
                  <div class="expand-content">
                    <h4>✅ 匹配理由</h4>
                    <ul>
                      <li v-for="(item, i) in row.matchReasons" :key="i">{{ item }}</li>
                    </ul>
                    <h4>⚠️ 技能差距</h4>
                    <ul>
                      <li v-for="(item, i) in row.skillGaps" :key="i">{{ item }}</li>
                      <li v-if="!row.skillGaps || row.skillGaps.length === 0">暂无明显差距</li>
                    </ul>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="jobTitle" label="岗位" min-width="160" show-overflow-tooltip />
              <el-table-column prop="company" label="公司" min-width="120" show-overflow-tooltip />
              <el-table-column label="匹配度" width="180">
                <template #default="{ row }">
                  <el-progress
                    :percentage="row.matchScore || 0"
                    :color="scoreColor"
                    :stroke-width="12"
                  />
                </template>
              </el-table-column>
              <el-table-column label="匹配时间" width="170">
                <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </el-main>

    <!-- 岗位详情 -->
    <el-dialog v-model="detailVisible" :title="detail?.title || '岗位详情'" width="600px">
      <template v-if="detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="公司">{{ detail.company }}</el-descriptions-item>
          <el-descriptions-item label="地点">{{ detail.location || '-' }}</el-descriptions-item>
          <el-descriptions-item label="行业">{{ detail.industry || '-' }}</el-descriptions-item>
          <el-descriptions-item label="薪资">{{ formatSalary(detail) }}</el-descriptions-item>
          <el-descriptions-item label="学历要求">{{ detail.educationRequirement || '-' }}</el-descriptions-item>
          <el-descriptions-item label="经验要求">{{ detail.experienceRequirement || '-' }}</el-descriptions-item>
        </el-descriptions>
        <h4 class="detail-section">技能要求</h4>
        <el-tag v-for="skill in (detail.skills || [])" :key="skill" class="skill-tag">
          {{ skill }}
        </el-tag>
        <h4 class="detail-section">岗位描述</h4>
        <p class="job-desc">{{ detail.jobDescription || '暂无描述' }}</p>
      </template>
    </el-dialog>

    <!-- 匹配结果 -->
    <el-dialog v-model="matchVisible" title="AI 岗位匹配报告" width="560px" top="10vh">
      <div v-if="matchResult" v-loading="matchingId">
        <div class="match-score">
          <el-progress
            type="dashboard"
            :percentage="matchResult.matchScore || 0"
            :width="150"
            :color="scoreColor"
          />
          <div class="match-score-label">匹配度</div>
        </div>
        <h4 class="report-section">✅ 匹配理由</h4>
        <ul class="report-list">
          <li v-for="(item, i) in matchResult.matchReasons" :key="i">{{ item }}</li>
        </ul>
        <h4 class="report-section">⚠️ 技能差距</h4>
        <ul class="report-list">
          <li v-for="(item, i) in matchResult.skillGaps" :key="i">{{ item }}</li>
          <li v-if="!matchResult.skillGaps || matchResult.skillGaps.length === 0">暂无明显差距</li>
        </ul>
        <el-alert
          type="info"
          :closable="false"
          title="提示：匹配结果基于你的最新简历，可在简历管理中更新后重新匹配。"
        />
      </div>
    </el-dialog>
  </el-container>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getJobs, getJob, matchJob, getMatches } from '@/api/job'

const activeTab = ref('jobs')
const jobs = ref([])
const total = ref(0)
const loading = ref(false)
const historyLoading = ref(false)
const matches = ref([])
const matchingId = ref(null)

const industries = ['互联网', '智能硬件', '金融', '教育', '制造业', '其他']

const salaryOptions = [
  { label: '不限', value: null },
  { label: '5K 以下', value: { salaryMax: 5 } },
  { label: '5-10K', value: { salaryMin: 5, salaryMax: 10 } },
  { label: '10-15K', value: { salaryMin: 10, salaryMax: 15 } },
  { label: '15K 以上', value: { salaryMin: 15 } }
]

const query = ref({
  keyword: '',
  location: '',
  industry: '',
  salaryMin: null,
  salaryMax: null,
  page: 1,
  size: 10
})
const salaryRange = ref(null)

const detailVisible = ref(false)
const detail = ref(null)
const matchVisible = ref(false)
const matchResult = ref(null)

function scoreColor(percentage) {
  if (percentage >= 80) return '#67c23a'
  if (percentage >= 60) return '#e6a23c'
  return '#f56c6c'
}

function formatSalary(row) {
  const min = row.salaryMin
  const max = row.salaryMax
  if (min == null && max == null) return '面议'
  if (min == null) return `${max}K 以下`
  if (max == null) return `${min}K 以上`
  return `${min}-${max}K`
}

function formatDate(date) {
  return date ? String(date).replace('T', ' ').slice(0, 16) : '-'
}

function buildParams() {
  const params = { page: query.value.page, size: query.value.size }
  if (query.value.keyword) params.keyword = query.value.keyword
  if (query.value.location) params.location = query.value.location
  if (query.value.industry) params.industry = query.value.industry
  if (salaryRange.value) {
    if (salaryRange.value.salaryMin != null) params.salaryMin = salaryRange.value.salaryMin
    if (salaryRange.value.salaryMax != null) params.salaryMax = salaryRange.value.salaryMax
  }
  return params
}

async function loadJobs() {
  loading.value = true
  try {
    const res = await getJobs(buildParams())
    jobs.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

async function loadMatches() {
  historyLoading.value = true
  try {
    const res = await getMatches()
    matches.value = res.data
  } finally {
    historyLoading.value = false
  }
}

function handleSearch() {
  query.value.page = 1
  loadJobs()
}

function handleReset() {
  query.value = { keyword: '', location: '', industry: '', page: 1, size: 10 }
  salaryRange.value = null
  loadJobs()
}

function handlePageChange(page) {
  query.value.page = page
  loadJobs()
}

async function handleDetail(row) {
  const res = await getJob(row.id)
  detail.value = res.data
  detailVisible.value = true
}

async function handleMatch(row) {
  matchingId.value = row.id
  try {
    const res = await matchJob(row.id)
    matchResult.value = res.data
    matchVisible.value = true
  } finally {
    matchingId.value = null
  }
}

onMounted(() => {
  loadJobs()
  loadMatches()
})
</script>

<style scoped>
.jobs-page {
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

.filter-form {
  margin-bottom: 4px;
}

.skill-tag {
  margin-right: 6px;
  margin-bottom: 4px;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}

.expand-content {
  padding: 8px 48px;
  color: #606266;
  line-height: 1.8;
}

.expand-content h4 {
  margin: 8px 0 4px;
  color: #303133;
}

.detail-section {
  margin: 16px 0 8px;
  color: #303133;
}

.job-desc {
  color: #606266;
  line-height: 1.8;
  white-space: pre-wrap;
}

.match-score {
  text-align: center;
  margin-bottom: 16px;
}

.match-score-label {
  margin-top: 4px;
  color: #909399;
  font-size: 14px;
}

.report-section {
  margin: 14px 0 6px;
  color: #303133;
}

.report-list {
  padding-left: 20px;
  color: #606266;
  line-height: 1.8;
}
</style>
