<template>
  <div class="contract-container">
    <el-card class="filter-card">
      <el-form :inline="true" :model="filterForm" class="filter-form">
        <el-form-item label="合同编号">
          <el-input v-model="filterForm.contractNo" placeholder="请输入合同编号" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item label="客户名称">
          <el-input v-model="filterForm.customerName" placeholder="请输入客户名称" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item label="签订日期">
          <el-date-picker v-model="filterForm.signDateRange" type="daterange" range-separator="至"
            start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD"
            style="width: 240px" />
        </el-form-item>
        <el-form-item label="到期日期">
          <el-date-picker v-model="filterForm.expireDateRange" type="daterange" range-separator="至"
            start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD"
            style="width: 240px" />
        </el-form-item>
        <el-form-item label="合同状态">
          <el-select v-model="filterForm.contractStatus" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="item in contractStatusOptions" :key="item.value" :label="item.label"
              :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <div class="table-header">
        <span class="table-title">合同列表</span>
        <div class="table-actions">
          <el-button v-permission="'contract:add'" type="primary" @click="handleAdd">新增合同</el-button>
          <el-button @click="handleExport">导出</el-button>
        </div>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe border style="width: 100%">
        <el-table-column prop="contractNo" label="合同编号" min-width="140" />
        <el-table-column prop="customerName" label="客户名称" min-width="120" />
        <el-table-column prop="signDate" label="签订日期" min-width="110" />
        <el-table-column prop="expireDate" label="到期日期" min-width="110">
          <template #default="{ row }">
            <span :style="getExpireDateStyle(row.expireDate)">{{ row.expireDate }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="contractAmount" label="合同金额(元)" min-width="120">
          <template #default="{ row }">
            {{ row.contractAmount ? Number(row.contractAmount).toLocaleString() : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="paymentMethod" label="付款方式" min-width="110" />
        <el-table-column prop="personInCharge" label="负责人" min-width="90" />
        <el-table-column prop="contractStatus" label="合同状态" min-width="90">
          <template #default="{ row }">
            <el-tag :type="contractStatusTagType[row.contractStatus]" size="small">
              {{ row.contractStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleDetail(row)">详情</el-button>
            <el-button v-permission="'contract:edit'" size="small" type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button v-permission="'contract:delete'" size="small" type="danger"
              @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination v-model:current-page="pagination.page" v-model:page-size="pagination.size"
        :page-sizes="[10, 20, 50, 100]" :total="pagination.total"
        :layout="paginationLayout" @size-change="handleSearch" @current-change="handleSearch"
        style="margin-top: 16px; justify-content: flex-end" />
    </el-card>

    <el-dialog v-model="formDialogVisible" :title="formDialogTitle" width="700px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <template v-if="!isEdit">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="客户名称" prop="customerId">
                <el-select v-model="formData.customerId" filterable placeholder="请选择客户" style="width: 100%"
                  @change="handleCustomerChange">
                  <el-option v-for="c in customerOptions" :key="c.id" :label="c.name" :value="c.id" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="合同金额" prop="contractAmount">
                <el-input-number v-model="formData.contractAmount" :min="0" :precision="2" :controls="false"
                  placeholder="请输入合同金额" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="合同状态" prop="contractStatus">
                <el-select v-model="formData.contractStatus" placeholder="请选择合同状态" style="width: 100%">
                  <el-option v-for="item in contractStatusOptions" :key="item.value" :label="item.label"
                    :value="item.value" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="签订日期" prop="signDate">
                <el-date-picker v-model="formData.signDate" type="date" placeholder="请选择签订日期"
                  value-format="YYYY-MM-DD" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="到期日期" prop="expireDate">
                <el-date-picker v-model="formData.expireDate" type="date" placeholder="请选择到期日期"
                  value-format="YYYY-MM-DD" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="付款方式" prop="paymentMethod">
                <el-select v-model="formData.paymentMethod" placeholder="请选择付款方式" style="width: 100%">
                  <el-option v-for="item in paymentMethodOptions" :key="item.value" :label="item.label"
                    :value="item.value" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="负责人" prop="personInCharge">
                <el-input v-model="formData.personInCharge" placeholder="请输入负责人" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="服务内容" prop="serviceContent">
            <el-input v-model="formData.serviceContent" type="textarea" :rows="3" placeholder="请输入服务内容，如智慧燃气系统部署、运维、售后等" />
          </el-form-item>
          <el-form-item label="备注" prop="remark">
            <el-input v-model="formData.remark" type="textarea" :rows="2" placeholder="请输入备注" />
          </el-form-item>
        </template>

        <template v-else>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="合同编号">
                <el-input v-model="formData.contractNo" disabled />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="客户名称">
                <el-input v-model="formData.customerName" disabled />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="合同状态" prop="contractStatus">
                <el-select v-model="formData.contractStatus" placeholder="请选择合同状态" style="width: 100%">
                  <el-option v-for="item in contractStatusOptions" :key="item.value" :label="item.label"
                    :value="item.value" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="到期日期" prop="expireDate">
                <el-date-picker v-model="formData.expireDate" type="date" placeholder="请选择到期日期"
                  value-format="YYYY-MM-DD" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="修改原因" prop="modifyReason">
            <el-input v-model="formData.modifyReason" type="textarea" :rows="3" placeholder="请输入修改原因" />
          </el-form-item>
        </template>

        <el-form-item label="合同扫描件">
          <el-upload :auto-upload="false" :on-change="handleFileChange" :file-list="fileList"
            :on-remove="handleFileRemove" ref="uploadRef">
            <el-button size="small" type="primary">选择文件</el-button>
            <template #tip>
              <div class="el-upload__tip">保存合同后可上传合同扫描件，单文件不超过20MB</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailDialogVisible" title="合同详情" width="800px" destroy-on-close>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="合同编号">{{ detailData.contractNo }}</el-descriptions-item>
        <el-descriptions-item label="客户名称">{{ detailData.customerName }}</el-descriptions-item>
        <el-descriptions-item label="合同状态">
          <el-tag :type="contractStatusTagType[detailData.contractStatus]" size="small">
            {{ detailData.contractStatus }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="签订日期">{{ detailData.signDate }}</el-descriptions-item>
        <el-descriptions-item label="到期日期">
          <span :style="getExpireDateStyle(detailData.expireDate)">{{ detailData.expireDate }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="合同金额">
          {{ detailData.contractAmount ? Number(detailData.contractAmount).toLocaleString() + ' 元' : '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="付款方式">{{ detailData.paymentMethod }}</el-descriptions-item>
        <el-descriptions-item label="负责人">{{ detailData.personInCharge }}</el-descriptions-item>
        <el-descriptions-item label="服务内容" :span="2">{{ detailData.serviceContent || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detailData.remark || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">合同附件</el-divider>
      <el-table :data="detailAttachments" stripe border size="small">
        <el-table-column prop="fileName" label="文件名" />
        <el-table-column prop="fileType" label="类型" width="120" />
        <el-table-column prop="fileSize" label="大小" width="100">
          <template #default="{ row }">{{ formatFileSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="上传时间" width="170" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button size="small" link type="primary" @click="downloadAttachment(row)">下载</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="detailAttachments.length === 0" description="暂无附件" />

      <el-divider content-position="left">修改记录</el-divider>
      <el-timeline>
        <el-timeline-item v-for="log in detailModifyLogs" :key="log.id" :timestamp="log.modifyTime"
          placement="top">
          <div>
            <strong>{{ log.modifyUser }}</strong> 修改了
            <el-tag size="small" type="info">{{ log.fieldName }}</el-tag>：
            <span style="color: #f56c6c">{{ log.oldValue || '空' }}</span>
            →
            <span style="color: #67c23a">{{ log.newValue || '空' }}</span>
          </div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-if="detailModifyLogs.length === 0" description="暂无修改记录" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { contractApi, contractStatusOptions, paymentMethodOptions, contractStatusTagType } from '@/api/contract'
import { customerApi } from '@/api/customer'

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref([])
const customerOptions = ref([])

const filterForm = reactive({
  contractNo: '',
  customerName: '',
  signDateRange: null,
  expireDateRange: null,
  contractStatus: ''
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const paginationLayout = computed(() => {
  return window.innerWidth < 768
    ? 'total, prev, pager, next'
    : 'total, sizes, prev, pager, next, jumper'
})

const formDialogVisible = ref(false)
const formDialogTitle = ref('新增合同')
const isEdit = ref(false)
const editId = ref(null)
const formRef = ref(null)
const uploadRef = ref(null)
const fileList = ref([])
const pendingFiles = ref([])

const formData = reactive({
  contractNo: '',
  customerId: null,
  customerName: '',
  signDate: '',
  expireDate: '',
  contractAmount: null,
  serviceContent: '',
  paymentMethod: '',
  personInChargeId: null,
  personInCharge: '',
  remark: '',
  contractStatus: '',
  modifyReason: ''
})

const formRules = computed(() => ({
  customerId: [{ required: true, message: '请选择客户', trigger: 'change' }],
  signDate: [{ required: true, message: '请选择签订日期', trigger: 'change' }],
  expireDate: [{ required: true, message: '请选择到期日期', trigger: 'change' }],
  ...(isEdit.value ? {
    contractStatus: [{ required: true, message: '请选择合同状态', trigger: 'change' }],
    modifyReason: [{ required: true, message: '请输入修改原因', trigger: 'blur' }]
  } : {})
}))

const detailDialogVisible = ref(false)
const detailData = ref({})
const detailAttachments = ref([])
const detailModifyLogs = ref([])

function getExpireDateStyle(expireDate) {
  if (!expireDate) return {}
  const expire = new Date(expireDate)
  const today = new Date()
  const oneMonthLater = new Date()
  oneMonthLater.setMonth(oneMonthLater.getMonth() + 1)
  if (expire <= oneMonthLater && expire >= today) {
    return { color: '#f56c6c', fontWeight: 'bold' }
  }
  return {}
}

async function fetchList() {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      size: pagination.size,
      contractNo: filterForm.contractNo || undefined,
      customerName: filterForm.customerName || undefined,
      signDateStart: filterForm.signDateRange?.[0] || undefined,
      signDateEnd: filterForm.signDateRange?.[1] || undefined,
      expireDateStart: filterForm.expireDateRange?.[0] || undefined,
      expireDateEnd: filterForm.expireDateRange?.[1] || undefined,
      contractStatus: filterForm.contractStatus || undefined
    }
    const res = await contractApi.page(params)
    tableData.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function fetchCustomerOptions() {
  try {
    const res = await customerApi.page({ page: 1, size: 1000 })
    customerOptions.value = res.data?.records || []
  } catch (e) {
    console.error(e)
  }
}

function handleSearch() {
  pagination.page = 1
  fetchList()
}

function handleReset() {
  Object.assign(filterForm, {
    contractNo: '',
    customerName: '',
    signDateRange: null,
    expireDateRange: null,
    contractStatus: ''
  })
  handleSearch()
}

function handleAdd() {
  isEdit.value = false
  editId.value = null
  formDialogTitle.value = '新增合同'
  Object.assign(formData, {
    contractNo: '',
    customerId: null,
    customerName: '',
    signDate: '',
    expireDate: '',
    contractAmount: null,
    serviceContent: '',
    paymentMethod: '',
    personInChargeId: null,
    personInCharge: '',
    remark: '',
    contractStatus: '',
    modifyReason: ''
  })
  fileList.value = []
  pendingFiles.value = []
  formDialogVisible.value = true
}

async function handleEdit(row) {
  isEdit.value = true
  editId.value = row.id
  formDialogTitle.value = '编辑合同'
  try {
    const res = await contractApi.detail(row.id)
    const data = res.data
    Object.assign(formData, {
      contractNo: data.contractNo || '',
      customerId: data.customerId,
      customerName: data.customerName || '',
      signDate: data.signDate || '',
      expireDate: data.expireDate || '',
      contractAmount: data.contractAmount,
      serviceContent: data.serviceContent || '',
      paymentMethod: data.paymentMethod || '',
      personInChargeId: data.personInChargeId,
      personInCharge: data.personInCharge || '',
      remark: data.remark || '',
      contractStatus: data.contractStatus || '',
      modifyReason: ''
    })

    const attRes = await contractApi.listAttachments(row.id)
    fileList.value = (attRes.data || []).map(a => ({
      name: a.fileName,
      url: a.filePath,
      id: a.id,
      status: 'success'
    }))
    pendingFiles.value = []

    formDialogVisible.value = true
  } catch (e) {
    console.error(e)
  }
}

function handleCustomerChange(customerId) {
  const customer = customerOptions.value.find(c => c.id === customerId)
  if (customer) {
    formData.customerName = customer.name
  }
}

function handleFileChange(file, fileListVal) {
  const maxSize = 20 * 1024 * 1024
  if (file.size > maxSize) {
    ElMessage.error('文件大小不能超过20MB')
    return false
  }
  if (!file.status || file.status === 'ready') {
    pendingFiles.value.push(file.raw)
  }
}

function handleFileRemove(file) {
  if (file.id) {
    contractApi.deleteAttachment(file.id).catch(() => {})
  }
  if (file.raw) {
    const idx = pendingFiles.value.indexOf(file.raw)
    if (idx > -1) pendingFiles.value.splice(idx, 1)
  }
}

async function uploadPendingFiles(contractId) {
  for (const file of pendingFiles.value) {
    try {
      const fd = new FormData()
      fd.append('file', file)
      fd.append('fileType', '合同扫描件')
      await contractApi.uploadAttachment(contractId, fd)
    } catch (e) {
      console.error('附件上传失败', e)
    }
  }
  pendingFiles.value = []
}

async function handleSubmit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitLoading.value = true
  try {
    if (isEdit.value) {
      const editData = {
        contractStatus: formData.contractStatus,
        expireDate: formData.expireDate,
        modifyReason: formData.modifyReason
      }
      await contractApi.update(editId.value, editData)
      await uploadPendingFiles(editId.value)
      ElMessage.success('合同修改成功')
    } else {
      const res = await contractApi.create(formData)
      const contractId = res.data?.id || editId.value
      if (contractId) {
        await uploadPendingFiles(contractId)
      }
      ElMessage.success('合同添加成功')
    }
    formDialogVisible.value = false
    fetchList()
  } catch (e) {
    console.error(e)
  } finally {
    submitLoading.value = false
  }
}

async function handleDetail(row) {
  try {
    const [detailRes, attRes, logRes] = await Promise.all([
      contractApi.detail(row.id),
      contractApi.listAttachments(row.id),
      contractApi.listModifyLogs(row.id)
    ])
    detailData.value = detailRes.data || {}
    detailAttachments.value = attRes.data || []
    detailModifyLogs.value = logRes.data || []
    detailDialogVisible.value = true
  } catch (e) {
    console.error(e)
  }
}

function downloadAttachment(row) {
  const token = localStorage.getItem('token')
  const url = `/api/contract/attachment/${row.id}?token=${token}`

  fetch(url, {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${token}`
    }
  })
    .then(response => {
      if (!response.ok) {
        throw new Error('下载失败')
      }
      return response.blob()
    })
    .then(blob => {
      const a = document.createElement('a')
      a.href = URL.createObjectURL(blob)
      a.download = row.fileName || '附件'
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      URL.revokeObjectURL(a.href)
    })
    .catch(() => {
      ElMessage.error('文件下载失败')
    })
}

function formatFileSize(bytes) {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定要删除合同「${row.contractNo}」吗？此操作不可恢复。`,
      '删除确认',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    await contractApi.delete(row.id)
    ElMessage.success('合同已删除')
    fetchList()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

function handleExport() {
  const params = {
    contractNo: filterForm.contractNo || undefined,
    customerName: filterForm.customerName || undefined,
    signDateStart: filterForm.signDateRange?.[0] || undefined,
    signDateEnd: filterForm.signDateRange?.[1] || undefined,
    expireDateStart: filterForm.expireDateRange?.[0] || undefined,
    expireDateEnd: filterForm.expireDateRange?.[1] || undefined,
    contractStatus: filterForm.contractStatus || undefined
  }
  const url = contractApi.getExportUrl(params)
  const token = localStorage.getItem('token')
  fetch(url, { headers: { Authorization: `Bearer ${token}` } })
    .then(res => res.blob())
    .then(blob => {
      const a = document.createElement('a')
      a.href = URL.createObjectURL(blob)
      a.download = '合同列表.xlsx'
      a.click()
      URL.revokeObjectURL(a.href)
    })
    .catch(() => ElMessage.error('导出失败'))
}

onMounted(() => {
  fetchList()
  fetchCustomerOptions()
})
</script>

<style scoped>
.contract-container {
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
