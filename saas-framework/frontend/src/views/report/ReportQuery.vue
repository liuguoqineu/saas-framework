<template>
  <div class="report-query">
    <el-card>
      <template #header><div class="card-header"><span>报表查询</span></div></template>
      <el-form :inline="true" :model="queryForm" class="filter-form">
        <el-form-item label="报表类型"><el-select v-model="queryForm.reportType" placeholder="全部" clearable style="width: 140px"><el-option label="日报" value="DAILY"/><el-option label="周报" value="WEEKLY"/><el-option label="月报" value="MONTHLY"/></el-select></el-form-item>
        <el-form-item label="报表周期">
          <el-date-picker
            v-if="queryForm.reportType === 'DAILY'"
            v-model="queryForm.reportPeriod"
            type="date"
            placeholder="选择日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            clearable
            style="width: 180px"
          />
          <el-date-picker
            v-else-if="queryForm.reportType === 'WEEKLY'"
            v-model="weeklyDateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            clearable
            style="width: 280px"
          />
          <el-date-picker
            v-else-if="queryForm.reportType === 'MONTHLY'"
            v-model="queryForm.reportPeriod"
            type="month"
            placeholder="选择月份"
            format="YYYY-MM"
            value-format="YYYY-MM"
            clearable
            style="width: 140px"
          />
          <el-input
            v-else
            v-model="queryForm.reportPeriod"
            placeholder="请先选择报表类型"
            disabled
            style="width: 180px"
          />
        </el-form-item>
        <el-form-item label="状态"><el-select v-model="queryForm.status" placeholder="全部" clearable style="width: 120px"><el-option label="草稿" value="DRAFT"/><el-option label="已提交" value="SUBMITTED"/><el-option label="已通过" value="APPROVED"/><el-option label="已驳回" value="REJECTED"/></el-select></el-form-item>
        <el-form-item><el-button type="primary" @click="fetchData">查询</el-button></el-form-item>
      </el-form>
      <el-table :data="tableData" v-loading="loading" stripe border style="width: 100%" :header-cell-style="{ textAlign: 'center' }" :cell-style="{ textAlign: 'center' }">
        <el-table-column prop="id" label="编号" min-width="100"/>
        <el-table-column prop="userId" label="用户" min-width="120"/>
        <el-table-column prop="reportType" label="类型" min-width="120">
          <template #default="{row}">{{ typeLabel(row.reportType) }}</template>
        </el-table-column>
        <el-table-column prop="reportPeriod" label="周期" min-width="160"/>
        <el-table-column prop="status" label="状态" min-width="120">
          <template #default="{row}">
            <el-tag :type="statusTag(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="submitTime" label="提交时间" min-width="180"/>
        <el-table-column label="操作" min-width="180" fixed="right">
          <template #default="{row}">
            <el-button size="small" :disabled="row.status === 'DRAFT'" @click="viewDetail(row)">查看</el-button>
            <el-button size="small" type="primary" :disabled="row.status === 'DRAFT'" @click="exportPdf(row.id)">PDF</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top:16px" v-model:current-page="queryForm.page" v-model:page-size="queryForm.size" :total="total" layout="total,prev,pager,next" @current-change="fetchData"/>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessageBox } from 'element-plus'
import { reportApi } from '@/api/report'

const loading = ref(false), tableData = ref([]), total = ref(0)
const queryForm = reactive({ page: 1, size: 20, reportType: '', reportPeriod: '', status: '' })
const weeklyDateRange = ref(null)

watch(() => queryForm.reportType, () => {
  queryForm.reportPeriod = ''
  weeklyDateRange.value = null
})

watch(weeklyDateRange, (val) => {
  if (Array.isArray(val) && val.length === 2) {
    queryForm.reportPeriod = `${val[0]} ~ ${val[1]}`
  } else {
    queryForm.reportPeriod = ''
  }
})

const typeLabel = (t) => ({ DAILY: '日报', WEEKLY: '周报', MONTHLY: '月报' }[t] || t)
const statusLabel = (s) => ({ DRAFT: '草稿', SUBMITTED: '已提交', APPROVED: '已审批', REJECTED: '已驳回' }[s] || s)
const statusTag = (s) => ({ DRAFT: 'info', SUBMITTED: 'warning', APPROVED: 'success', REJECTED: 'danger' }[s] || 'info')

const fetchData = async () => { loading.value = true; try { const res = await reportApi.page(queryForm); tableData.value = res.data.records; total.value = res.data.total } finally { loading.value = false } }

const viewDetail = (row) => { ElMessageBox.alert(row.contentText || '暂无内容', '报表 #' + row.id) }

const exportPdf = (id) => { window.open(reportApi.getExportPdfUrl(id), '_blank') }

onMounted(() => fetchData())
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.filter-form { display: flex; flex-wrap: wrap; gap: 0; }
.filter-form :deep(.el-form-item) { margin-bottom: 16px; margin-right: 20px; }
</style>