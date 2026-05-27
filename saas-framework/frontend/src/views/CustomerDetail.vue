<template>
  <div class="page-container">
    <div class="back-bar">
      <el-button @click="goBack" :icon="ArrowLeft">返回列表</el-button>
      <span class="page-title">客户详情</span>
    </div>

    <div v-loading="loading">
      <!-- 基本信息 -->
      <el-card style="margin-bottom:16px">
        <template #header>
          <div class="card-header">
            <span>基本信息</span>
            <el-tag :type="customer.isInvalid === 1 ? 'info' : 'success'" style="margin-left:8px">
              {{ customer.isInvalid === 1 ? '无效' : '正常' }}
            </el-tag>
          </div>
        </template>
        <el-descriptions :column="3" border>
          <el-descriptions-item label="客户名称">{{ customer.name }}</el-descriptions-item>
          <el-descriptions-item label="业务一级分类">{{ customer.businessCategory || '-' }}</el-descriptions-item>
          <el-descriptions-item label="业务二级分类">{{ customer.businessType || '-' }}</el-descriptions-item>
          <el-descriptions-item label="合作一级分类">{{ customer.cooperationCategory || '-' }}</el-descriptions-item>
          <el-descriptions-item label="合作二级分类">
            <el-tag :type="getCooperationTagType(customer.cooperationCategory, customer.cooperationStatus)" size="small">
              {{ customer.cooperationStatus || '-' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="联系人">{{ customer.contactPerson || '-' }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ customer.contactPhone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="跟进人">{{ customer.followUpPerson || '-' }}</el-descriptions-item>
          <el-descriptions-item label="用气规模">{{ customer.gasScale || '-' }}</el-descriptions-item>
          <el-descriptions-item label="客户地址" :span="2">{{ customer.address || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ customer.createTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="智慧燃气系统" :span="2">{{ customer.smartGasSystem || '-' }}</el-descriptions-item>
          <el-descriptions-item label="合同信息" :span="3">{{ customer.contractInfo || '-' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 跟进记录 -->
      <el-card style="margin-bottom:16px">
        <template #header>
          <div class="card-header">
            <span>跟进记录</span>
            <el-button v-permission="'followup:add'" type="primary" size="small" @click="openFollowUpDialog">新增跟进</el-button>
          </div>
        </template>
        <el-timeline v-if="followUpRecords.length > 0">
          <el-timeline-item
            v-for="record in followUpRecords"
            :key="record.id"
            :timestamp="record.followUpTime"
            placement="top"
          >
            <el-card shadow="never" class="log-card">
              <el-descriptions :column="2" border size="small">
                <el-descriptions-item label="跟进人" :span="1">{{ record.followUpPerson || '-' }}</el-descriptions-item>
                <el-descriptions-item label="跟进方式" :span="1">
                  <el-tag size="small">{{ followUpMethodMap[record.followUpMethod] || '未知' }}</el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="跟进状态" :span="1">
                  <el-tag :type="record.followUpStatus === 1 ? 'warning' : record.followUpStatus === 2 ? 'success' : 'primary'" size="small">
                    {{ followUpStatusMap[record.followUpStatus] || '未知' }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="下一步计划" :span="2">{{ record.nextPlan || '-' }}</el-descriptions-item>
                <el-descriptions-item label="跟进内容" :span="2">
                  <div style="white-space:pre-wrap;line-height:1.6">{{ record.followUpContent }}</div>
                </el-descriptions-item>
              </el-descriptions>
              <div style="margin-top:8px;text-align:right">
                <el-button v-permission="'followup:status'" size="small" type="warning" @click="openStatusChangeDialog">变更状态</el-button>
              </div>
            </el-card>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else description="暂无跟进记录" :image-size="60" />
      </el-card>

      <!-- 附件管理 -->
      <el-card style="margin-bottom:16px">
        <template #header>
          <div class="card-header">
            <span>附件管理</span>
            <el-upload
              :show-file-list="false"
              :before-upload="beforeUpload"
              :http-request="handleUpload"
              accept=".jpg,.jpeg,.png,.pdf,.doc,.docx,.xls,.xlsx"
            >
              <el-button type="primary" size="small">📤 上传附件</el-button>
            </el-upload>
          </div>
        </template>

        <el-table :data="attachments" stripe v-if="attachments.length > 0">
          <el-table-column prop="fileName" label="文件名" min-width="200" show-overflow-tooltip />
          <el-table-column prop="fileType" label="文件类型" width="120" />
          <el-table-column prop="fileSize" label="文件大小" width="120">
            <template #default="{ row }">{{ formatFileSize(row.fileSize) }}</template>
          </el-table-column>
          <el-table-column prop="createTime" label="上传时间" width="170" />
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="success" @click="handleDownloadAttachment(row)">⬇️ 下载</el-button>
              <el-button v-permission="'customer:edit'" size="small" type="danger" @click="handleDeleteAttachment(row)">🗑️ 删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-else description="暂无附件，请点击上方按钮上传" :image-size="80" />
      </el-card>

      <!-- 修改记录 -->
      <el-card>
        <template #header>
          <span>修改记录</span>
        </template>
        <el-timeline v-if="modifyLogs.length > 0">
          <el-timeline-item
            v-for="log in modifyLogs"
            :key="log.id"
            :timestamp="log.modifyTime"
            placement="top"
          >
            <el-card shadow="never" class="log-card">
              <p class="log-user">{{ log.modifyUser || '未知' }} 修改了 <strong>{{ getFieldLabel(log.fieldName) }}</strong></p>
              <p class="log-change">
                <span class="old-value">{{ log.oldValue || '空' }}</span>
                <el-icon style="margin:0 8px"><Right /></el-icon>
                <span class="new-value">{{ log.newValue || '空' }}</span>
              </p>
            </el-card>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else description="暂无修改记录" :image-size="60" />
      </el-card>

      <!-- 状态变更记录 -->
      <el-card style="margin-bottom:16px">
        <template #header>
          <span>状态变更记录</span>
        </template>
        <el-timeline v-if="statusLogs.length > 0">
          <el-timeline-item
            v-for="log in statusLogs"
            :key="log.id"
            :timestamp="log.changeTime"
            placement="top"
          >
            <el-card shadow="never" class="log-card">
              <p class="log-user">{{ log.changePerson || '未知' }} 变更了客户状态</p>
              <p class="log-change">
                <span class="old-value">{{ log.oldCooperationCategory }}/{{ log.oldCooperationStatus }}</span>
                <el-icon style="margin:0 8px"><Right /></el-icon>
                <span class="new-value">{{ log.newCooperationCategory }}/{{ log.newCooperationStatus }}</span>
              </p>
              <p v-if="log.changeReason" style="margin:4px 0;color:#909399;font-size:12px">原因：{{ log.changeReason }}</p>
            </el-card>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else description="暂无状态变更记录" :image-size="60" />
      </el-card>
    </div>

    <!-- 新增跟进记录弹窗 -->
    <el-dialog title="新增跟进记录" v-model="followUpDialogVisible" width="600px" @close="resetFollowUpForm">
      <el-form ref="followUpFormRef" :model="followUpForm" :rules="followUpRules" label-width="100px">
        <el-form-item label="跟进时间" prop="followUpTime">
          <el-date-picker v-model="followUpForm.followUpTime" type="datetime" placeholder="选择跟进时间" style="width:100%" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
        <el-form-item label="跟进人">
          <el-input v-model="followUpForm.followUpPerson" disabled />
        </el-form-item>
        <el-form-item label="跟进方式" prop="followUpMethod">
          <el-select v-model="followUpForm.followUpMethod" placeholder="请选择" style="width:100%">
            <el-option v-for="opt in followUpMethodOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="跟进内容" prop="followUpContent">
          <el-input v-model="followUpForm.followUpContent" type="textarea" :rows="4" placeholder="请输入跟进内容" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="下一步计划">
          <el-input v-model="followUpForm.nextPlan" type="textarea" :rows="2" placeholder="请输入下一步计划" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="跟进状态" prop="followUpStatus">
          <el-select v-model="followUpForm.followUpStatus" placeholder="请选择" style="width:100%">
            <el-option v-for="opt in followUpStatusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="followUpDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="followUpSubmitting" @click="submitFollowUp">确认添加</el-button>
      </template>
    </el-dialog>

    <!-- 客户状态变更弹窗 -->
    <el-dialog title="变更客户合作状态" v-model="statusChangeDialogVisible" width="600px" @close="resetStatusChangeForm">
      <el-form ref="statusChangeFormRef" :model="statusChangeForm" :rules="statusChangeRules" label-width="120px">
        <el-form-item label="当前状态">
          <el-tag>{{ customer.cooperationCategory }} / {{ customer.cooperationStatus }}</el-tag>
        </el-form-item>
        <el-form-item label="新合作一级分类" prop="newCooperationCategory">
          <el-select v-model="statusChangeForm.newCooperationCategory" placeholder="请选择" style="width:100%" @change="onStatusChangeCategoryChange">
            <el-option v-for="cat in cooperationCategories" :key="cat" :label="cat" :value="cat" />
          </el-select>
        </el-form-item>
        <el-form-item label="新合作二级分类" prop="newCooperationStatus">
          <el-select v-model="statusChangeForm.newCooperationStatus" placeholder="请选择" style="width:100%">
            <el-option v-for="s in statusChangeCooperationStatuses" :key="typeof s === 'object' ? s.value : s" :label="typeof s === 'object' ? s.label : s" :value="typeof s === 'object' ? s.value : s" />
          </el-select>
        </el-form-item>
        <el-form-item label="变更原因" prop="changeReason">
          <el-input v-model="statusChangeForm.changeReason" type="textarea" :rows="3" placeholder="请输入变更原因" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="statusChangeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="statusChangeSubmitting" @click="submitStatusChange">确认变更</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { customerApi, cooperationCategoryMap } from '@/api/customer'
import { followUpApi, followUpMethodOptions, followUpStatusOptions, followUpMethodMap, followUpStatusMap } from '@/api/followUp'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Right } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const customerId = route.params.id

const loading = ref(false)
const customer = ref({})
const attachments = ref([])
const modifyLogs = ref([])
const followUpRecords = ref([])
const statusLogs = ref([])
const followUpDialogVisible = ref(false)
const statusChangeDialogVisible = ref(false)
const followUpSubmitting = ref(false)
const statusChangeSubmitting = ref(false)
const followUpFormRef = ref()
const statusChangeFormRef = ref()
const cooperationCategories = Object.keys(cooperationCategoryMap)

const followUpForm = reactive({
  customerId: null, followUpTime: '', followUpPerson: '', followUpPersonId: null,
  followUpMethod: null, followUpContent: '', nextPlan: '', followUpStatus: 2
})
const followUpRules = {
  followUpTime: [{ required: true, message: '请选择跟进时间', trigger: 'change' }],
  followUpMethod: [{ required: true, message: '请选择跟进方式', trigger: 'change' }],
  followUpContent: [{ required: true, message: '请输入跟进内容', trigger: 'blur' }],
  followUpStatus: [{ required: true, message: '请选择跟进状态', trigger: 'change' }]
}
const statusChangeForm = reactive({
  newCooperationCategory: '', newCooperationStatus: '', followUpRecordId: null, changeReason: ''
})
const statusChangeRules = {
  newCooperationCategory: [{ required: true, message: '请选择新合作一级分类', trigger: 'change' }],
  newCooperationStatus: [{ required: true, message: '请选择新合作二级分类', trigger: 'change' }],
  changeReason: [{ required: true, message: '请输入变更原因', trigger: 'blur' }]
}
const statusChangeCooperationStatuses = computed(() => {
  return cooperationCategoryMap[statusChangeForm.newCooperationCategory] || []
})

const fieldLabelMap = {
  name: '客户名称', address: '地址',
  contactPerson: '联系人', contactPhone: '联系电话',
  businessCategory: '业务一级分类', businessType: '业务二级分类',
  cooperationCategory: '合作一级分类', cooperationStatus: '合作二级分类',
  gasScale: '用气规模', smartGasSystem: '智慧燃气系统',
  contractInfo: '合同信息', isInvalid: '状态'
}

function getFieldLabel(fieldName) {
  return fieldLabelMap[fieldName] || fieldName
}

function getCooperationTagType(category, status) {
  if (category === '已合作') {
    if (status === '正常履约') return 'success'
    if (status === '逾期客户') return 'danger'
    if (status === '暂停合作') return 'warning'
    if (status === '终止合作') return 'info'
    return ''
  }
  if (category === '潜在') {
    if (status === '高潜力') return 'success'
    if (status === '中潜力') return 'warning'
    if (status === '低潜力') return 'info'
    return ''
  }
  if (category === '意向') return ''
  return ''
}

function formatFileSize(bytes) {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + 'B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + 'KB'
  return (bytes / (1024 * 1024)).toFixed(1) + 'MB'
}

function goBack() {
  router.push('/customer')
}

async function fetchDetail() {
  loading.value = true
  try {
    const res = await customerApi.detail(customerId)
    customer.value = res.data
  } finally {
    loading.value = false
  }
}

async function fetchAttachments() {
  try {
    const res = await customerApi.listAttachments(customerId)
    attachments.value = res.data || []
  } catch { /* ignore */ }
}

async function fetchModifyLogs() {
  try {
    const res = await customerApi.listModifyLogs(customerId)
    modifyLogs.value = res.data || []
  } catch { /* ignore */ }
}

function beforeUpload(file) {
  const allowedTypes = ['image/jpeg', 'image/png', 'application/pdf',
    'application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document']
  if (!allowedTypes.includes(file.type)) {
    ElMessage.error('仅支持 JPG/PNG/PDF/DOC/DOCX 格式')
    return false
  }
  if (file.size > 20 * 1024 * 1024) {
    ElMessage.error('文件大小不能超过 20MB')
    return false
  }
  return true
}

async function handleUpload(options) {
  const fileType = await selectFileType()
  if (!fileType) return

  const formData = new FormData()
  formData.append('file', options.file)
  formData.append('fileType', fileType)

  try {
    await customerApi.uploadAttachment(customerId, formData)
    ElMessage.success('附件上传成功')
    fetchAttachments()
  } catch { /* ignore */ }
}

function selectFileType() {
  return new Promise((resolve) => {
    ElMessageBox.prompt('请选择文件类型', '上传附件', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      inputPlaceholder: '合同扫描件/资质文件/现场照片/其他',
      inputValue: '其他',
      inputPattern: /.+/,
      inputErrorMessage: '请输入文件类型'
    }).then(({ value }) => resolve(value)).catch(() => resolve(null))
  })
}

async function handleDeleteAttachment(row) {
  await ElMessageBox.confirm(`确认删除附件「${row.fileName}」？`, '提示', { type: 'warning' })
  try {
    await customerApi.deleteAttachment(row.id)
    ElMessage.success('附件已删除')
    fetchAttachments()
  } catch { /* 取消 */ }
}

async function handleDownloadAttachment(row) {
  const token = localStorage.getItem('token')
  try {
    const response = await fetch(customerApi.getDownloadUrl(row.id), {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })

    if (!response.ok) {
      const errorData = await response.json()
      throw new Error(errorData.msg || `下载失败 (${response.status})`)
    }

    const blob = await response.blob()
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = row.fileName || 'download'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)

    ElMessage.success('✅ 文件下载成功')
  } catch (e) {
    console.error('下载失败:', e)
    ElMessage.error(`❌ 下载失败: ${e.message}`)
  }
}

function getCurrentUser() {
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
  return { id: userInfo.id, username: userInfo.realName || userInfo.username }
}

function openFollowUpDialog() {
  const user = getCurrentUser()
  followUpForm.customerId = Number(customerId)
  followUpForm.followUpPerson = user.username
  followUpForm.followUpPersonId = user.id
  followUpForm.followUpTime = ''
  followUpForm.followUpMethod = null
  followUpForm.followUpContent = ''
  followUpForm.nextPlan = ''
  followUpForm.followUpStatus = 2
  followUpDialogVisible.value = true
}

function resetFollowUpForm() {
  followUpFormRef.value?.resetFields()
}

async function submitFollowUp() {
  const valid = await followUpFormRef.value.validate().catch(() => false)
  if (!valid) return
  followUpSubmitting.value = true
  try {
    await followUpApi.createRecord(followUpForm)
    ElMessage.success('跟进记录添加成功')
    followUpDialogVisible.value = false
    fetchFollowUpRecords()
  } finally {
    followUpSubmitting.value = false
  }
}

function openStatusChangeDialog() {
  statusChangeForm.newCooperationCategory = ''
  statusChangeForm.newCooperationStatus = ''
  statusChangeForm.changeReason = ''
  statusChangeForm.followUpRecordId = null
  statusChangeDialogVisible.value = true
}

function resetStatusChangeForm() {
  statusChangeFormRef.value?.resetFields()
}

function onStatusChangeCategoryChange() {
  statusChangeForm.newCooperationStatus = ''
}

async function submitStatusChange() {
  const valid = await statusChangeFormRef.value.validate().catch(() => false)
  if (!valid) return
  statusChangeSubmitting.value = true
  try {
    await followUpApi.changeCustomerStatus(customerId, statusChangeForm)
    ElMessage.success('客户状态变更成功')
    statusChangeDialogVisible.value = false
    fetchDetail()
    fetchStatusLogs()
  } finally {
    statusChangeSubmitting.value = false
  }
}

async function fetchFollowUpRecords() {
  try {
    const res = await followUpApi.listRecordsByCustomerId(customerId)
    followUpRecords.value = res.data || []
  } catch { /* ignore */ }
}

async function fetchStatusLogs() {
  try {
    const res = await followUpApi.listStatusLogs(customerId)
    statusLogs.value = res.data || []
  } catch { /* ignore */ }
}

onMounted(() => {
  fetchDetail()
  fetchAttachments()
  fetchModifyLogs()
  fetchFollowUpRecords()
  fetchStatusLogs()
})
</script>

<style scoped>
.page-container { max-width: 1200px; }
.back-bar { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.page-title { font-size: 18px; font-weight: 600; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.log-card { padding: 8px 12px; }
.log-card p { margin: 4px 0; font-size: 13px; }
.log-user { color: #606266; }
.log-change { display: flex; align-items: center; }
.old-value { color: #F56C6C; text-decoration: line-through; }
.new-value { color: #67C23A; font-weight: 500; }
.follow-content { line-height: 1.6; }
.follow-plan { line-height: 1.4; }
</style>
