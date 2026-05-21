<template>
  <div class="dashboard">
    <h2>欢迎使用 SaaS 多租户教学框架</h2>
    <p class="desc">当前登录：{{ userStore.userInfo?.realName }}（{{ userStore.isSuperAdmin ? '超级管理员' : '租户管理员' }}）</p>

    <el-row :gutter="20" style="margin-top:30px">
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>账户体系</template>
          <p>三层账户：超级账户 → 租户 → 租户员工</p>
          <p>无公开注册，管理员创建下级账户</p>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>多租户隔离</template>
          <p>所有业务表自动追加 tenant_id 条件</p>
          <p>超级账户可查看全部数据</p>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>权限控制</template>
          <p>RBAC 角色权限模型</p>
          <p>权限继承限制，下级不能超越上级</p>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="hover" style="margin-top:20px">
      <template #header>快速开始</template>
      <el-steps :active="0" align-center style="margin-top:20px">
        <el-step title="创建租户" description="超级账户创建租户并自动生成管理员" />
        <el-step title="分配角色" description="为租户管理员分配角色和权限" />
        <el-step title="员工管理" description="租户管理员创建员工并分配角色" />
        <el-step title="业务操作" description="员工进行日常业务操作" />
      </el-steps>
    </el-card>
  </div>
</template>

<script setup>
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
</script>

<style scoped>
.dashboard h2 { margin: 0 0 8px; color: #303133; }
.desc { color: #909399; margin: 0; }
</style>
