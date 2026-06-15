import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/views/Layout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'tenant',
        name: 'Tenant',
        component: () => import('@/views/Tenant.vue'),
        meta: { title: '租户管理', permission: 'tenant:list' }
      },
      {
        path: 'role',
        name: 'Role',
        component: () => import('@/views/Role.vue'),
        meta: { title: '角色管理', permission: 'role:list' }
      },
      {
        path: 'user',
        name: 'User',
        component: () => import('@/views/User.vue'),
        meta: { title: '员工管理', permission: 'user:list' }
      },
      {
        path: 'customer',
        name: 'Customer',
        component: () => import('@/views/Customer.vue'),
        meta: { title: '客户管理', permission: 'customer:list' }
      },
      {
        path: 'customer/:id',
        name: 'CustomerDetail',
        component: () => import('@/views/CustomerDetail.vue'),
        meta: { title: '客户详情', permission: 'customer:list' }
      },
      {
        path: 'contract',
        name: 'Contract',
        component: () => import('@/views/Contract.vue'),
        meta: { title: '合同管理', permission: 'contract:list' }
      },
      {
        path: 'repair',
        name: 'Repair',
        component: () => import('@/views/Repair.vue'),
        meta: { title: '报修管理', permission: 'repair:list' }
      },
      {
        path: 'check-in',
        name: 'CheckIn',
        component: () => import('@/views/CheckIn.vue'),
        meta: { title: '员工打卡', permission: 'checkin:list' }
      },
      {
        path: 'statistics',
        name: 'Statistics',
        component: () => import('@/views/Statistics.vue'),
        meta: { title: '统计分析', permission: 'statistics:customer' }
      },
      {
        path: 'operation-log',
        name: 'OperationLog',
        component: () => import('@/views/OperationLog.vue'),
        meta: { title: '操作日志', permission: 'log:list' }
      },
      {
        path: 'backup',
        name: 'Backup',
        component: () => import('@/views/Backup.vue'),
        meta: { title: '数据备份' }
      },
      {
        path: 'my-reports',
        name: 'MyReports',
        component: () => import('@/views/report/MyReports.vue'),
        meta: { title: '我的报表' }
      },
      {
        path: 'approvals',
        name: 'Approvals',
        component: () => import('@/views/report/Approvals.vue'),
        meta: { title: '审批管理', permission: 'report:approve' }
      },
      {
        path: 'report-query',
        name: 'ReportQuery',
        component: () => import('@/views/report/ReportQuery.vue'),
        meta: { title: '报表查询' }
      },
      {
        path: 'report-dashboard',
        name: 'ReportDashboard',
        component: () => import('@/views/report/Dashboard.vue'),
        meta: { title: '数据看板' }
      },
      {
        path: 'overdue',
        name: 'Overdue',
        component: () => import('@/views/report/Overdue.vue'),
        meta: { title: '逾期管理', permission: 'report:overdue:manage' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：检查登录状态和权限
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')

  if (to.path === '/login') {
    // 已登录用户访问登录页，直接跳转首页
    if (token) {
      next('/dashboard')
    } else {
      next()
    }
    return
  }

  // 未登录用户访问其他页面，跳转登录页
  if (!token) {
    next('/login')
    return
  }

  // 检查权限（如果有配置 permission 元数据）
  const requiredPermission = to.meta.permission
  if (requiredPermission) {
    const userInfoStr = localStorage.getItem('userInfo')
    if (userInfoStr) {
      const userInfo = JSON.parse(userInfoStr)
      // 超级管理员拥有所有权限
      if (userInfo.tenantId !== 0) {
        const permissions = userInfo.permissions || []
        if (!permissions.includes(requiredPermission)) {
          next('/dashboard')
          return
        }
      }
    }
  }

  next()
})

export default router
