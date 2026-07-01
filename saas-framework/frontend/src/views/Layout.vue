<template>
  <el-container class="layout-container">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '64px' : '220px'" class="layout-aside">
      <div class="logo">
        <img src="/logo.jpg" alt="Logo" class="logo-img" />
        <span v-if="!isCollapse" class="logo-text">客户管理系统</span>
        <span v-else class="logo-text">CRM</span>
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

        <!-- 公司管理（仅超级管理员可见） -->
        <el-menu-item v-if="hasPermission('tenant:list')" index="/tenant">
          <el-icon><OfficeBuilding /></el-icon>
          <span>公司管理</span>
        </el-menu-item>

        <el-menu-item v-if="hasPermission('role:list')" index="/role">
          <el-icon><Management /></el-icon>
          <span>角色管理</span>
        </el-menu-item>

        <!-- 员工管理 -->
        <el-menu-item v-if="hasPermission('user:list')" index="/user">
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

        <el-sub-menu v-if="hasPermission('purchase:list') || hasPermission('inventory:list') || hasPermission('stock-in:list') || hasPermission('stock-out:list') || hasPermission('device:list') || hasPermission('repair:list')" index="device">
          <template #title>
            <el-icon><Monitor /></el-icon>
            <span>设备管理</span>
          </template>
          <el-menu-item v-if="hasPermission('purchase:list')" index="/purchase">
            <el-icon><ShoppingCart /></el-icon>
            <span>采购管理</span>
          </el-menu-item>
          <el-menu-item v-if="hasPermission('stock-in:list')" index="/stock-in">
            <el-icon><Download /></el-icon>
            <span>入库管理</span>
          </el-menu-item>
          <el-menu-item v-if="hasPermission('inventory:list')" index="/inventory">
            <el-icon><Box /></el-icon>
            <span>库存台账</span>
          </el-menu-item>
          <el-menu-item v-if="hasPermission('stock-out:list')" index="/stock-out">
            <el-icon><Upload /></el-icon>
            <span>出库管理</span>
          </el-menu-item>
          <el-menu-item v-if="hasPermission('device:list')" index="/device">
            <el-icon><Cpu /></el-icon>
            <span>设备档案</span>
          </el-menu-item>
          <el-menu-item v-if="hasPermission('repair:list')" index="/device-repair">
            <el-icon><SetUp /></el-icon>
            <span>设备维修</span>
          </el-menu-item>
          <el-menu-item v-if="hasPermission('repair:list')" index="/device-replacement">
            <el-icon><RefreshRight /></el-icon>
            <span>更换档案</span>
          </el-menu-item>
        </el-sub-menu>

        <el-menu-item v-if="hasPermission('checkin:list')" index="/check-in">
          <el-icon><Clock /></el-icon>
          <span>员工打卡</span>
        </el-menu-item>

        <el-sub-menu v-if="hasPermission('report:fill') || hasPermission('report:approve') || hasPermission('report:view') || hasPermission('report:dashboard') || hasPermission('report:overdue:manage')" index="report">
          <template #title>
            <el-icon><Document /></el-icon>
            <span>报表管理</span>
          </template>
          <el-menu-item v-if="hasPermission('report:fill')" index="/my-reports">
            <el-icon><EditPen /></el-icon>
            <span>我的报表</span>
          </el-menu-item>
          <el-menu-item v-if="hasPermission('report:approve')" index="/approvals">
            <el-icon><Select /></el-icon>
            <span>审批管理</span>
          </el-menu-item>
          <el-menu-item v-if="hasPermission('report:view')" index="/report-query">
            <el-icon><Search /></el-icon>
            <span>报表查询</span>
          </el-menu-item>
          <el-menu-item v-if="hasPermission('report:dashboard')" index="/report-dashboard">
            <el-icon><DataAnalysis /></el-icon>
            <span>数据看板</span>
          </el-menu-item>
          <el-menu-item v-if="hasPermission('report:overdue:manage')" index="/overdue">
            <el-icon><Warning /></el-icon>
            <span>逾期管理</span>
          </el-menu-item>
        </el-sub-menu>

        <el-menu-item v-if="hasPermission('statistics:customer')" index="/statistics">
          <el-icon><DataAnalysis /></el-icon>
          <span>统计分析</span>
        </el-menu-item>

        <el-menu-item v-if="hasPermission('log:list')" index="/operation-log">
          <el-icon><Tickets /></el-icon>
          <span>操作日志</span>
        </el-menu-item>

        <!-- 数据备份（仅超级管理员可见） -->
        <el-menu-item v-if="userStore.isSuperAdmin" index="/backup">
          <el-icon><Coin /></el-icon>
          <span>数据备份</span>
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
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              {{ userStore.userInfo?.realName || userStore.userInfo?.username || '用户' }}
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>
                  公司: {{ userStore.isSuperAdmin ? '超级管理员' : userStore.userInfo?.tenantId }}
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

    <!-- 登录提醒弹窗 -->
    <LoginReminderDialog ref="loginReminderRef" />
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { reminderApi } from '@/api/reminder'
import LoginReminderDialog from '@/components/LoginReminderDialog.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isCollapse = ref(false)
const currentTitle = computed(() => route.meta.title || '')
const activeMenu = computed(() => route.path)
const loginReminderRef = ref(null)

function hasPermission(permission) {
  if (!permission) return true
  return userStore.isSuperAdmin || userStore.permissions.includes(permission)
}

onMounted(() => {
  showLoginReminder()
})

async function showLoginReminder() {
  if (!userStore.token) return
  try {
    const res = await reminderApi.getLoginReminders()
    if (res.data && res.data.totalCount > 0) {
      loginReminderRef.value?.open(res.data)
    }
  } catch (err) {
    if (err?.response?.status !== 401) {
      console.error('[LoginReminder] 获取提醒失败:', err)
    }
  }
}

onUnmounted(() => {
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
  gap: 8px;
  color: #fff;
  font-size: 16px;
  font-weight: bold;
  border-bottom: 1px solid rgba(255,255,255,0.1);
  padding: 0 12px;
}

.logo-img {
  width: 32px;
  height: 32px;
  flex-shrink: 0;
}

.logo-text {
  white-space: nowrap;
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
