<template>
  <div class="overdue-manage">
    <el-card>
      <template #header><div class="card-header"><span>逾期管理</span><el-button type="success" @click="exportOverdue">导出Excel</el-button></div></template>
      <el-table :data="overdues" v-loading="loading" stripe border style="width: 100%" :header-cell-style="{ textAlign: 'center' }" :cell-style="{ textAlign: 'center' }">
        <el-table-column prop="userId" label="用户编号" min-width="120"/>
        <el-table-column prop="userName" label="姓名" min-width="120"/>
        <el-table-column prop="reportType" label="类型" min-width="120">
          <template #default="{row}">{{ typeLabel(row.reportType) }}</template>
        </el-table-column>
        <el-table-column prop="reportPeriod" label="周期" min-width="160"/>
        <el-table-column prop="deadline" label="截止日期" min-width="180"/>
        <el-table-column prop="isReminded" label="是否提醒" min-width="120">
          <template #default="{row}">
            <el-tag :type="row.isReminded==1?'success':'danger'" size="small">{{ row.isReminded==1?'是':'否' }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { reportApi } from '@/api/report'

const loading = ref(false), overdues = ref([])

const typeLabel = (t) => ({ DAILY: '日报', WEEKLY: '周报', MONTHLY: '月报' }[t] || t)

const fetchData = async () => { loading.value = true; try { const res = await reportApi.getOverdueList(); overdues.value = res.data } finally { loading.value = false } }

const exportOverdue = () => { window.open(reportApi.getOverdueExportUrl(), '_blank') }

onMounted(() => fetchData())
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>