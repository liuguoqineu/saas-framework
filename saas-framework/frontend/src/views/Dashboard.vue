<template>
  <div class="dashboard">
    <el-card class="welcome-card" shadow="hover">
      <div class="welcome-header">
        <img src="/logo.jpg" alt="Logo" class="welcome-logo" />
        <h2 class="welcome-title">欢迎使用客户管理系统</h2>
      </div>
      <p class="welcome-desc">高效管理 · 精准营销 · 数据驱动</p>
    </el-card>

    <div class="stat-row">
      <el-card shadow="hover" class="stat-card">
        <div class="stat-content">
          <div class="stat-icon-wrap stat-icon--blue">
            <el-icon><UserFilled /></el-icon>
          </div>
          <div class="stat-info">
            <span class="stat-value">{{ userStore.userInfo?.realName || userStore.userInfo?.username || '用户' }}</span>
            <span class="stat-label">当前登录用户</span>
          </div>
        </div>
      </el-card>
      <el-card shadow="hover" class="stat-card">
        <div class="stat-content">
          <div class="stat-icon-wrap stat-icon--green">
            <el-icon><OfficeBuilding /></el-icon>
          </div>
          <div class="stat-info">
            <span class="stat-value">{{ userStore.isSuperAdmin ? '超级管理员' : (userStore.userInfo?.tenantId || '-') }}</span>
            <span class="stat-label">所属租户</span>
          </div>
        </div>
      </el-card>
      <el-card shadow="hover" class="stat-card">
        <div class="stat-content">
          <div class="stat-icon-wrap stat-icon--orange">
            <el-icon><Management /></el-icon>
          </div>
          <div class="stat-info">
            <span class="stat-value">{{ userStore.userInfo?.roleName || (userStore.isSuperAdmin ? '超级管理员' : '普通用户') }}</span>
            <span class="stat-label">当前角色</span>
          </div>
        </div>
      </el-card>
    </div>

    <div class="feature-row">
      <el-card shadow="hover" class="feature-card">
        <template #header>系统功能</template>
        <ul class="feature-list">
          <li><el-tag type="primary" size="small">客户管理</el-tag> 全生命周期客户信息管理</li>
          <li><el-tag type="success" size="small">合同管理</el-tag> 合同创建与到期提醒</li>
          <li><el-tag type="warning" size="small">报修管理</el-tag> 报修工单全流程跟踪</li>
          <li><el-tag type="danger" size="small">跟进记录</el-tag> 客户跟进与回访记录</li>
        </ul>
      </el-card>
      <el-card shadow="hover" class="feature-card">
        <template #header>快速入口</template>
        <div class="quick-links">
          <div class="quick-row">
            <el-button type="primary" @click="$router.push('/customer')">客户列表</el-button>
            <el-button type="success" @click="$router.push('/contract')">合同管理</el-button>
          </div>
          <div class="quick-row">
            <el-button type="warning" @click="$router.push('/repair')">报修管理</el-button>
            <el-button type="info" @click="$router.push('/statistics')">统计分析</el-button>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
</script>

<style scoped>
.dashboard {
  padding: 0;
}

.welcome-card {
  margin-bottom: 20px;
  text-align: center;
}

.welcome-card :deep(.el-card__body) {
  padding: 30px 20px;
}

.welcome-header {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 12px;
}

.welcome-logo {
  width: 40px;
  height: 40px;
}

.welcome-title {
  margin: 0;
  color: #303133;
  font-size: 24px;
  font-weight: 600;
}

.welcome-desc {
  margin: 0;
  color: #909399;
  font-size: 14px;
  letter-spacing: 4px;
}

.stat-row {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
}

.stat-card {
  flex: 1;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon-wrap {
  width: 52px;
  height: 52px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-icon-wrap .el-icon {
  font-size: 26px;
  color: #fff;
}

.stat-icon--blue {
  background: linear-gradient(135deg, #409EFF, #66b1ff);
}

.stat-icon--green {
  background: linear-gradient(135deg, #67C23A, #85ce61);
}

.stat-icon--orange {
  background: linear-gradient(135deg, #E6A23C, #ebb563);
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

.feature-row {
  display: flex;
  gap: 20px;
  align-items: stretch;
}

.feature-card {
  flex: 1;
  align-self: stretch;
}

.feature-card :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.feature-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.feature-list li {
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
  font-size: 14px;
  color: #606266;
  display: flex;
  align-items: center;
  gap: 12px;
}

.feature-list li:last-child {
  border-bottom: none;
}

.quick-links {
  display: flex;
  flex-direction: column;
  gap: 16px;
  flex: 1;
}

.quick-links .quick-row {
  display: flex;
  gap: 16px;
  flex: 1;
}

.quick-links .quick-row .el-button {
  width: 100%;
  height: 40px;
}
</style>
