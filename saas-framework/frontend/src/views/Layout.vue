<template>
  <el-container class="layout-container">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '64px' : '220px'" class="layout-aside">
      <div class="logo">
        <span v-if="!isCollapse">SaaS 框架</span>
        <span v-else>SaaS</span>
      </div>

      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :collapse-transition="false"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
        router
      >
        <el-menu-item index="/dashboard">
          <el-icon><HomeFilled /></el-icon>
          <span>首页</span>
        </el-menu-item>

        <!-- 租户管理（仅超级管理员可见） -->
        <el-menu-item v-if="hasPermission('tenant:list')" index="/tenant">
          <el-icon><OfficeBuilding /></el-icon>
          <span>租户管理</span>
        </el-menu-item>

        <el-menu-item v-if="hasPermission('role:list')" index="/role">
          <el-icon><Management /></el-icon>
          <span>角色管理</span>
        </el-menu-item>

        <!-- 员工管理（超级管理员不可见） -->
        <el-menu-item v-if="!userStore.isSuperAdmin && hasPermission('user:list')" index="/user">
          <el-icon><User /></el-icon>
          <span>员工管理</span>
        </el-menu-item>

        <el-menu-item v-if="hasPermission('customer:list')" index="/customer">
          <el-icon><UserFilled /></el-icon>
          <span>客户管理</span>
        </el-menu-item>

        <el-menu-item v-if="hasPermission('contract:list')" index="/contract">
          <el-icon><Document /></el-icon>
          <span>合同管理</span>
        </el-menu-item>

        <el-menu-item v-if="hasPermission('repair:list')" index="/repair">
          <el-icon><SetUp /></el-icon>
          <span>报修管理</span>
        </el-menu-item>

        <el-menu-item v-if="hasPermission('statistics:customer')" index="/statistics">
          <el-icon><DataAnalysis /></el-icon>
          <span>统计分析</span>
        </el-menu-item>

        <el-menu-item v-if="hasPermission('log:list')" index="/operation-log">
          <el-icon><Tickets /></el-icon>
          <span>操作日志</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <!-- 主内容区 -->
    <el-container>
      <!-- 头部 -->
      <el-header class="layout-header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="isCollapse = !isCollapse">
            <Fold v-if="!isCollapse" />
            <Expand v-else />
          </el-icon>
          <span class="page-title">{{ currentTitle }}</span>
        </div>

        <div class="header-right">
          <el-popover placement="bottom" :width="360" trigger="click" @show="fetchReminders">
            <template #reference>
              <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99" class="reminder-badge">
                <el-icon class="reminder-icon"><Bell /></el-icon>
              </el-badge>
            </template>
            <div class="reminder-popover">
              <div class="reminder-header">
                <span>跟进提醒</span>
                <el-button v-if="unreadCount > 0" type="primary" link size="small" @click="markAllRead">全部已读</el-button>
              </div>
              <div v-if="reminders.length === 0" class="reminder-empty">暂无提醒</div>
              <div v-else class="reminder-list">
                <div
                  v-for="item in reminders"
                  :key="item.id"
                  class="reminder-item"
                  :class="{ unread: item.isRead === 0 }"
                  @click="handleReminderClick(item)"
                >
                  <div class="reminder-item-header">
                    <span class="reminder-item-time">{{ item.reminderTime }}</span>
                    <el-tag v-if="item.isRead === 0" type="danger" size="small">未读</el-tag>
                  </div>
                  <div class="reminder-item-content">{{ item.reminderContent }}</div>
                  <div class="reminder-item-person">提醒人：{{ item.reminderPerson }}</div>
                </div>
              </div>
            </div>
          </el-popover>

          <el-dropdown @command="handleCommand">
            <span class="user-info">
              {{ userStore.userInfo?.realName || userStore.userInfo?.username || '用户' }}
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>
                  租户: {{ userStore.isSuperAdmin ? '超级管理员' : userStore.userInfo?.tenantId }}
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 内容区 -->
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { followUpApi } from '@/api/followUp'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isCollapse = ref(false)
const currentTitle = computed(() => route.meta.title || '')
const activeMenu = computed(() => route.path)
const reminders = ref([])
const unreadCount = ref(0)
let reminderTimer = null

function hasPermission(permission) {
  if (!permission) return true
  return userStore.isSuperAdmin || userStore.permissions.includes(permission)
}

async function fetchReminders() {
  try {
    const res = await followUpApi.getPendingReminders()
    reminders.value = res.data || []
    unreadCount.value = reminders.value.filter(r => r.isRead === 0).length
  } catch { /* ignore */ }
}

async function markAllRead() {
  for (const item of reminders.value) {
    if (item.isRead === 0) {
      try {
        await followUpApi.markReminderRead(item.id)
        item.isRead = 1
      } catch { /* ignore */ }
    }
  }
  unreadCount.value = 0
  ElMessage.success('已全部标记为已读')
}

function handleReminderClick(item) {
  if (item.isRead === 0) {
    followUpApi.markReminderRead(item.id).then(() => {
      item.isRead = 1
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    }).catch(() => {})
  }
  if (item.customerId) {
    router.push(`/customer/${item.customerId}`)
  }
}

onMounted(() => {
  fetchReminders()
  reminderTimer = setInterval(fetchReminders, 60000)
})

onUnmounted(() => {
  if (reminderTimer) {
    clearInterval(reminderTimer)
    reminderTimer = null
  }
})

function handleCommand(cmd) {
  if (cmd === 'logout') {
    userStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.layout-aside {
  background-color: #304156;
  overflow-x: hidden;
  transition: width 0.3s;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}

.el-menu {
  border-right: none;
}

.layout-header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e6e6e6;
  padding: 0 20px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.collapse-btn {
  font-size: 20px;
  cursor: pointer;
}

.page-title {
  font-size: 16px;
  font-weight: 500;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.reminder-badge {
  cursor: pointer;
}

.reminder-icon {
  font-size: 20px;
  cursor: pointer;
}

.reminder-popover {
  max-height: 400px;
  overflow-y: auto;
}

.reminder-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 8px;
  border-bottom: 1px solid #ebeef5;
  margin-bottom: 8px;
  font-weight: 600;
}

.reminder-empty {
  text-align: center;
  color: #909399;
  padding: 20px 0;
}

.reminder-list {
  max-height: 340px;
  overflow-y: auto;
}

.reminder-item {
  padding: 8px 4px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background 0.2s;
}

.reminder-item:hover {
  background: #f5f7fa;
}

.reminder-item.unread {
  background: #ecf5ff;
}

.reminder-item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.reminder-item-time {
  font-size: 12px;
  color: #909399;
}

.reminder-item-content {
  font-size: 13px;
  color: #303133;
  margin-bottom: 2px;
}

.reminder-item-person {
  font-size: 12px;
  color: #909399;
}

.user-info {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
}

.layout-main {
  background: #f0f2f5;
  padding: 20px;
  overflow-y: auto;
}
</style>
