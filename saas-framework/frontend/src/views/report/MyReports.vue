<template>
  <div class="report-container">
    <el-card class="filter-card">
      <el-form :inline="true" :model="queryForm" class="filter-form">
        <el-form-item label="报表类型">
          <el-select v-model="queryForm.reportType" placeholder="全部" clearable style="width: 120px">
            <el-option label="日报" value="DAILY"/>
            <el-option label="周报" value="WEEKLY"/>
            <el-option label="月报" value="MONTHLY"/>
          </el-select>
        </el-form-item>
        <el-form-item label="报表周期">
          <el-input v-model="queryForm.reportPeriod" placeholder="如 2026-05-28" clearable style="width: 160px"/>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="草稿" value="DRAFT"/>
            <el-option label="已提交" value="SUBMITTED"/>
            <el-option label="已审批" value="APPROVED"/>
            <el-option label="已驳回" value="REJECTED"/>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <div class="table-header">
        <span class="table-title">报表列表</span>
        <div class="table-actions">
          <el-button type="primary" @click="showCreateDialog">新建报表</el-button>
        </div>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe border style="width: 100%" :header-cell-style="{ textAlign: 'center' }" :cell-style="{ textAlign: 'center' }">
        <el-table-column prop="id" label="编号" min-width="100"/>
        <el-table-column prop="reportType" label="类型" min-width="120">
          <template #default="{row}">{{ typeLabel(row.reportType) }}</template>
        </el-table-column>
        <el-table-column prop="reportPeriod" label="周期" min-width="160">
          <template #default="{row}"><span>{{ row.reportPeriod || '-' }}</span></template>
        </el-table-column>
        <el-table-column prop="status" label="状态" min-width="120">
          <template #default="{row}">
            <el-tag :type="statusTag(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="submitTime" label="提交时间" min-width="180">
          <template #default="{row}"><span>{{ row.submitTime || '-' }}</span></template>
        </el-table-column>
        <el-table-column label="操作" min-width="320" fixed="right">
          <template #default="{row}">
            <el-button size="small" :disabled="row.status === 'DRAFT'" @click="viewDetail(row)">查看</el-button>
            <el-button v-if="row.status==='DRAFT'||row.status==='REJECTED'" size="small" type="warning" @click="editReport(row)">编辑</el-button>
            <el-button v-if="row.status==='DRAFT'||row.status==='REJECTED'" size="small" type="success" @click="submitReport(row.id)">提交</el-button>
            <el-button v-if="row.status==='DRAFT'" size="small" type="danger" @click="deleteDraft(row.id)">删除</el-button>
            <el-button v-if="row.status!=='DRAFT'" size="small" type="info" @click="viewApprovalChain(row)">审批链</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="queryForm.page"
        v-model:page-size="queryForm.size"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        :layout="paginationLayout"
        @size-change="fetchData"
        @current-change="fetchData"
        style="margin-top: 16px; justify-content: flex-end"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="700px" @close="resetForm" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="120px">
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="报表类型" required prop="reportType">
              <el-select v-model="formData.reportType" placeholder="选择类型" style="width: 100%" @change="handleReportTypeChange">
                <el-option label="日报" value="DAILY"/>
                <el-option label="周报" value="WEEKLY"/>
                <el-option label="月报" value="MONTHLY"/>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="周期" required prop="reportPeriod">
              <el-date-picker
                v-if="formData.reportType === 'DAILY'"
                v-model="reportPeriodDate"
                type="date"
                placeholder="选择日期"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
              <el-date-picker
                v-else-if="formData.reportType === 'WEEKLY'"
                v-model="reportPeriodDate"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
              <el-date-picker
                v-else-if="formData.reportType === 'MONTHLY'"
                v-model="reportPeriodDate"
                type="month"
                placeholder="选择月份"
                format="YYYY-MM"
                value-format="YYYY-MM"
                style="width: 100%"
              />
              <el-input
                v-else
                v-model="formData.reportPeriod"
                placeholder="请先选择报表类型"
                disabled
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="内容" prop="contentText">
          <el-input v-model="contentText" type="textarea" :rows="10" placeholder="请输入报表内容"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" @click="saveReport" :loading="submitLoading">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="chainVisible" title="审批流程" width="600px">
      <el-steps :active="chainActiveStep" align-center>
        <el-step v-for="(item, idx) in chainData" :key="idx"
          :title="'第' + item.approvalLevel + '级'"
          :description="item.approverName || '未知'"
          :status="stepStatus(item.status)"/>
      </el-steps>
      <el-table :data="chainData" style="margin-top: 20px" stripe border size="small" :header-cell-style="{ textAlign: 'center' }" :cell-style="{ textAlign: 'center' }">
        <el-table-column prop="approvalLevel" label="级别" min-width="80">
          <template #default="{row}">第{{ row.approvalLevel }}级</template>
        </el-table-column>
        <el-table-column prop="approverName" label="审批人" min-width="100"/>
        <el-table-column prop="status" label="状态" min-width="100">
          <template #default="{row}">
            <el-tag :type="chainStatusTag(row.status)" size="small">{{ chainStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="comment" label="意见" min-width="150">
          <template #default="{row}">{{ row.comment || '-' }}</template>
        </el-table-column>
        <el-table-column prop="approveTime" label="审批时间" min-width="170">
          <template #default="{row}">{{ row.approveTime || '-' }}</template>
        </el-table-column>
      </el-table>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { reportApi } from '@/api/report'
const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref([])
const total = ref(0)
const templates = ref([])
const dialogVisible = ref(false)
const contentText = ref('')
const editingId = ref(null)
const formRef = ref(null)
const chainVisible = ref(false)
const chainData = ref([])

const queryForm = reactive({ page: 1, size: 10, reportType: '', reportPeriod: '', status: '' })
const formData = reactive({ reportType: '', reportPeriod: '' })
const reportPeriodDate = ref(null)

const formRules = {
  reportType: [{ required: true, message: '请选择报表类型', trigger: 'change' }],
  reportPeriod: [{ required: true, message: '请选择周期', trigger: 'change' }]
}

const dialogTitle = computed(() => editingId.value ? '编辑报表' : '新建报表')

const paginationLayout = computed(() => {
  return window.innerWidth < 768
    ? 'total, prev, pager, next'
    : 'total, sizes, prev, pager, next, jumper'
})

const typeLabel = (t) => ({ DAILY: '日报', WEEKLY: '周报', MONTHLY: '月报' }[t] || t)
const statusLabel = (s) => ({ DRAFT: '草稿', SUBMITTED: '已提交', APPROVED: '已审批', REJECTED: '已驳回' }[s] || s)
const statusTag = (s) => ({ DRAFT: 'info', SUBMITTED: 'warning', APPROVED: 'success', REJECTED: 'danger' }[s] || 'info')

const handleReportTypeChange = () => {
  reportPeriodDate.value = null
  formData.reportPeriod = ''
}

watch(reportPeriodDate, (val) => {
  if (formData.reportType === 'DAILY' || formData.reportType === 'MONTHLY') {
    formData.reportPeriod = val || ''
  } else if (formData.reportType === 'WEEKLY') {
    formData.reportPeriod = Array.isArray(val) && val.length === 2 ? `${val[0]} ~ ${val[1]}` : ''
  }
})

const formatWeekPeriod = (dateRange) => {
  if (!dateRange || !Array.isArray(dateRange) || dateRange.length !== 2) return ''
  return `${dateRange[0]} ~ ${dateRange[1]}`
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await reportApi.page(queryForm)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}

const fetchTemplates = async () => {
  try { const res = await reportApi.getTemplates(); templates.value = res.data } catch (e) { /* ignore */ }
}

const handleReset = () => {
  Object.assign(queryForm, { page: 1, size: 10, reportType: '', reportPeriod: '', status: '' })
  fetchData()
}

const showCreateDialog = () => { editingId.value = null; contentText.value = ''; reportPeriodDate.value = null; dialogVisible.value = true }

const viewDetail = (row) => {
  ElMessageBox.alert(row.contentText || '暂无内容', `报表 #${row.id}`)
}

const editReport = (row) => {
  editingId.value = row.id
  formData.reportType = row.reportType
  formData.reportPeriod = row.reportPeriod
  contentText.value = row.contentText || ''

  if (row.reportType === 'DAILY' && row.reportPeriod) {
    reportPeriodDate.value = row.reportPeriod
  } else if (row.reportType === 'MONTHLY' && row.reportPeriod) {
    reportPeriodDate.value = row.reportPeriod
  } else if (row.reportType === 'WEEKLY' && row.reportPeriod) {
    const dates = row.reportPeriod.split(' ~ ')
    if (dates.length === 2) {
      reportPeriodDate.value = [dates[0], dates[1]]
    } else {
      reportPeriodDate.value = null
    }
  } else {
    reportPeriodDate.value = null
  }

  dialogVisible.value = true
}

const saveReport = async () => {
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  let periodValue = ''
  if (formData.reportType === 'DAILY') {
    periodValue = reportPeriodDate.value || ''
  } else if (formData.reportType === 'WEEKLY') {
    periodValue = formatWeekPeriod(reportPeriodDate.value)
  } else if (formData.reportType === 'MONTHLY') {
    periodValue = reportPeriodDate.value || ''
  }

  if (!periodValue) {
    ElMessage.warning('请选择周期')
    return
  }

  submitLoading.value = true
  try {
    const data = { reportType: formData.reportType, reportPeriod: periodValue, contentText: contentText.value }
    if (editingId.value) {
      await reportApi.update(editingId.value, data)
      ElMessage.success('更新成功')
    } else {
      await reportApi.create(data)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (e) { /* handled by interceptor */ } finally { submitLoading.value = false }
}

const submitReport = async (id) => {
  try { await ElMessageBox.confirm('确认提交该报表？', '提示'); await reportApi.submit(id); ElMessage.success('提交成功'); fetchData() } catch (e) { /* cancel */ }
}

const deleteDraft = async (id) => {
  try { await ElMessageBox.confirm('确认删除该草稿？', '提示', { type: 'warning' }); await reportApi.deleteDraft(id); ElMessage.success('删除成功'); fetchData() } catch (e) { /* cancel */ }
}

const resetForm = () => { formData.reportType = ''; formData.reportPeriod = ''; reportPeriodDate.value = null; contentText.value = '' }

const chainStatusLabel = (s) => ({ PENDING: '待审批', APPROVED: '已通过', REJECTED: '已驳回', SUPERSEDED: '已作废' }[s] || s)
const chainStatusTag = (s) => ({ PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', SUPERSEDED: 'info' }[s] || 'info')
const stepStatus = (s) => ({ PENDING: 'process', APPROVED: 'finish', REJECTED: 'error', SUPERSEDED: 'wait' }[s] || 'wait')

const chainActiveStep = computed(() => {
  if (!chainData.value.length) return 0
  const hasRejected = chainData.value.some(i => i.status === 'REJECTED')
  if (hasRejected) return chainData.value.findIndex(i => i.status === 'REJECTED')
  return chainData.value.filter(i => i.status === 'APPROVED').length
})

const viewApprovalChain = async (row) => {
  try {
    const res = await reportApi.getApprovalChain(row.id)
    chainData.value = res.data || []
    chainVisible.value = true
  } catch (e) { /* handled */ }
}

onMounted(() => { fetchTemplates(); fetchData() })
</script>

<style scoped>
.report-container {
  padding: 0;
}

.filter-card {
  margin-bottom: 16px;
}

.filter-form {
  display: flex;
  flex-wrap: wrap;
  gap: 0;
}

.table-card {
  margin-bottom: 16px;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.table-title {
  font-size: 16px;
  font-weight: 600;
}

.table-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

@media (max-width: 768px) {
  .filter-form :deep(.el-form-item) {
    width: 100%;
  }

  .filter-form :deep(.el-form-item .el-input),
  .filter-form :deep(.el-form-item .el-select),
  .filter-form :deep(.el-form-item .el-date-editor) {
    width: 100% !important;
  }
}
</style>
