<template>
  <el-container class="home">
    <el-header class="home-header">
      <div class="brand">AI Resume Agent</div>
      <el-dropdown @command="handleCommand">
        <span class="user-name">
          {{ userStore.userInfo?.username || '未登录' }}
          <el-icon><arrow-down /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">个人信息</el-dropdown-item>
            <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </el-header>

    <el-main>
      <el-alert
        class="welcome"
        :title="`欢迎回来，${userStore.userInfo?.username || '同学'}！`"
        type="success"
        :closable="false"
        show-icon
        description="以下是即将上线的求职助手功能，敬请期待。"
      />

      <el-row :gutter="20">
        <el-col v-for="feature in features" :key="feature.title" :xs="24" :sm="12" :md="6">
          <el-card
            class="feature-card"
            shadow="hover"
            :class="{ clickable: feature.to }"
            @click="feature.to && $router.push(feature.to)"
          >
            <div class="feature-icon">{{ feature.icon }}</div>
            <h3>{{ feature.title }}</h3>
            <p>{{ feature.description }}</p>
            <el-tag size="small" :type="feature.to ? 'success' : 'info'">
              {{ feature.to ? '立即使用' : '开发中' }}
            </el-tag>
          </el-card>
        </el-col>
      </el-row>
    </el-main>
  </el-container>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const features = [
  { icon: '📄', title: '简历分析', description: '上传简历，AI 生成评分与改进建议', to: '/resumes' },
  { icon: '🎯', title: '岗位匹配', description: '查看岗位匹配度与技能差距', to: '/jobs' },
  { icon: '🎤', title: 'AI 模拟面试', description: '多轮问答训练，生成面试报告' },
  { icon: '✨', title: '项目经历优化', description: '按 STAR 法则改写项目描述' }
]

onMounted(() => {
  userStore.fetchProfile().catch(() => {})
})

async function handleCommand(command) {
  if (command === 'logout') {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
    userStore.logout()
    router.push('/login')
  } else if (command === 'profile') {
    router.push('/')
  }
}
</script>

<style scoped>
.home {
  min-height: 100%;
}

.home-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
}

.brand {
  font-size: 18px;
  font-weight: 600;
  color: #409eff;
}

.user-name {
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  color: #303133;
}

.welcome {
  margin-bottom: 20px;
}

.feature-card {
  margin-bottom: 20px;
  text-align: center;
}

.feature-icon {
  font-size: 40px;
  margin-bottom: 12px;
}

.feature-card h3 {
  margin-bottom: 8px;
  color: #303133;
}

.feature-card p {
  color: #909399;
  font-size: 13px;
  margin-bottom: 12px;
}

.feature-card.clickable {
  cursor: pointer;
}
</style>
