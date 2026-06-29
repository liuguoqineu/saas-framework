<template>
  <div class="device-container">
    <!-- 筛选区 -->
    <el-card class="filter-card">
      <el-form :inline="true" :model="filterForm" class="filter-form">
        <el-form-item label="设备编码">
          <el-input v-model="filterForm.deviceCode" placeholder="请输入设备编码" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item label="设备名称">
          <el-input v-model="filterForm.deviceName" placeholder="请输入设备名称" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filterForm.status" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="item in deviceStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="仓库">
          <el-input v-model="filterForm.warehouseName" placeholder="请输入仓库名称" clearable style="width: 130px" />
        </el-form-item>
        <el-form-item label="安装位置">
          <el-input v-model="filterForm.installLocation" placeholder="请输入安装位置" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 列表区 -->
    <el-card class="table-card">
      <div class="table-header">
        <span class="table-title">设备档案</span>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe border style="width: 100%">
        <el-table-column prop="deviceCode" label="设备编码" min-width="130" />
        <el-table-column prop="deviceName" label="设备名称" min-width="120" />
        <el-table-column prop="brand" label="品牌" width="100" />
        <el-table-column prop="model" label="型号" width="120" />
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="deviceStatusTagType[row.status]" size="small">
              {{ deviceStatusLabel[row.status] || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="warehouseName" label="所在仓库" min-width="110" />
        <el-table-column prop="installLocation" label="安装位置" min-width="120" />
        <el-table-column prop="installDate" label="安装日期" width="110" />
        <el-table-column prop="useDate" label="投用日期" width="110" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleDetail(row)">详情</el-button>
            <el-button v-if="row.status === 2" size="small" type="success" @click="handleInstall(row)">安装</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination v-model:current-page="pagination.page" v-model:page-size="pagination.size"
        :page-sizes="[10, 20, 50]" :total="pagination.total"
        :layout="paginationLayout" @size-change="handleSearch" @current-change="fetchList"
        style="margin-top: 16px; justify-content: flex-end" />
    </el-card>

    <!-- 设备详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="设备详情" width="800px" destroy-on-close>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="设备编码">{{ detailData.deviceCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="设备名称">{{ detailData.deviceName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="品牌">{{ detailData.brand || '-' }}</el-descriptions-item>
        <el-descriptions-item label="型号">{{ detailData.model || '-' }}</el-descriptions-item>
        <el-descriptions-item label="规格">{{ detailData.spec || '-' }}</el-descriptions-item>
        <el-descriptions-item label="分类">{{ detailData.category || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="deviceStatusTagType[detailData.status]" size="small">
            {{ deviceStatusLabel[detailData.status] || '-' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="所在仓库">{{ detailData.warehouseName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="安装位置">{{ detailData.installLocation || '-' }}</el-descriptions-item>
        <el-descriptions-item label="安装日期">{{ detailData.installDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="安装人员">{{ detailData.installPerson || '-' }}</el-descriptions-item>
        <el-descriptions-item label="投用日期">{{ detailData.useDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="采购单号">{{ detailData.purchaseOrderNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="入库单号">{{ detailData.stockInOrderNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="出库单号">{{ detailData.stockOutOrderNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detailData.remark || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">设备履历时间线</el-divider>
      <el-timeline v-if="timelineData.length > 0">
        <el-timeline-item v-for="item in timelineData" :key="item.id" :timestamp="item.eventTime" placement="top"
          :color="timelineEventColor[item.eventType] || '#909399'">
          <div>
            <el-tag :type="timelineEventTypeTagType[item.eventType]" size="small" style="margin-right: 8px">
              {{ timelineEventTypeLabel[item.eventType] || '-' }}
            </el-tag>
            <span>{{ item.eventDesc || '-' }}</span>
            <div style="color: #909399; font-size: 12px; margin-top: 4px; display: flex; gap: 16px; flex-wrap: wrap;">
              <span v-if="item.operator">操作人：{{ item.operator }}</span>
              <span v-if="item.relatedOrderNo">
                关联单号：
                <router-link v-if="getTimelineLink(item)" :to="getTimelineLink(item)" style="color: #409EFF; text-decoration: none;">
                  {{ item.relatedOrderNo }}
                </router-link>
                <span v-else>{{ item.relatedOrderNo }}</span>
              </span>
            </div>
          </div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无履历记录" :image-size="60" />
    </el-dialog>

    <!-- 安装信息填写对话框 -->
    <el-dialog v-model="installDialogVisible" title="安装信息填写" width="600px" destroy-on-close>
      <el-form ref="installFormRef" :model="installForm" :rules="installRules" label-width="100px">
        <el-form-item label="安装位置" prop="installLocation">
          <el-input v-model="installForm.installLocation" placeholder="请输入安装位置" />
        </el-form-item>
        <el-form-item label="安装日期" prop="installDate">
          <el-date-picker v-model="installForm.installDate" type="date" placeholder="请选择安装日期"
            value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="安装人员" prop="installPerson">
          <el-input v-model="installForm.installPerson" placeholder="请输入安装人员" />
        </el-form-item>
        <el-form-item label="投用日期" prop="useDate">
          <el-date-picker v-model="installForm.useDate" type="date" placeholder="请选择投用日期"
            value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="验收记录" prop="acceptRecord">
          <el-input v-model="installForm.acceptRecord" type="textarea" :rows="3" placeholder="请输入验收记录" />
        </el-form-item>
        <el-form-item label="安装文件" prop="installFile">
          <el-input v-model="installForm.installFile" placeholder="请输入安装文件URL" />
        </el-form-item>
        <el-form-item label="验收照片" prop="acceptPhoto">
          <el-input v-model="installForm.acceptPhoto" placeholder="请输入验收照片URL" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="installDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleInstallSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  deviceApi,
  deviceStatusOptions,
  deviceStatusLabel,
  deviceStatusTagType,
  timelineEventTypeLabel,
  timelineEventTypeTagType
} from '@/api/device'

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref([])

const filterForm = reactive({
  deviceCode: '',
  deviceName: '',
  status: null,
  warehouseName: '',
  installLocation: ''
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

const timelineEventColor = {
  1: '#409EFF',  // 采购 - 蓝色
  2: '#67C23A',  // 入库 - 绿色
  3: '#E6A23C',  // 出库 - 橙色
  4: '#409EFF',  // 安装 - 蓝色
  5: '#F56C6C',  // 报修 - 红色
  6: '#67C23A',  // 维修 - 绿色
  7: '#E6A23C',  // 配件更换 - 橙色
  8: '#F56C6C',  // 整机更换 - 红色
  9: '#909399'   // 报废 - 灰色
}

function getTimelineLink(item) {
  if (!item.eventType || !item.relatedId) return null
  switch (item.eventType) {
    case 1: return `/purchase`  // 采购单 - 跳转到采购页面
    case 2: return `/stock-in`  // 入库单 - 跳转到入库页面
    case 3: return `/stock-out` // 出库单 - 跳转到出库页面
    case 4: return null         // 安装 - 无跳转
    case 5: return `/device-repair` // 报修 - 跳转到设备维修页面
    case 6: return `/device-repair` // 维修 - 跳转到设备维修页面
    case 7: return `/device-replacement` // 配件更换 - 跳转到更换档案页面
    case 8: return `/device-replacement` // 整机更换 - 跳转到更换档案页面
    case 9: return null         // 报废 - 无跳转
    default: return null
  }
}

// ========== 详情 ==========
const detailDialogVisible = ref(false)
const detailData = ref({})
const timelineData = ref([])

async function handleDetail(row) {
  try {
    const [detailRes, timelineRes] = await Promise.all([
      deviceApi.detail(row.id),
      deviceApi.getTimeline(row.id)
    ])
    detailData.value = detailRes.data || {}
    timelineData.value = timelineRes.data || []
    detailDialogVisible.value = true
  } catch (e) {
    console.error(e)
  }
}

// ========== 安装 ==========
const installDialogVisible = ref(false)
const installFormRef = ref(null)
const installForm = reactive({
  deviceId: null,
  installLocation: '',
  installDate: '',
  installPerson: '',
  useDate: '',
  acceptRecord: '',
  installFile: '',
  acceptPhoto: ''
})

const installRules = {
  installLocation: [{ required: true, message: '请输入安装位置', trigger: 'blur' }],
  installDate: [{ required: true, message: '请选择安装日期', trigger: 'change' }],
  installPerson: [{ required: true, message: '请输入安装人员', trigger: 'blur' }]
}

function handleInstall(row) {
  Object.assign(installForm, {
    deviceId: row.id,
    installLocation: '',
    installDate: '',
    installPerson: '',
    useDate: '',
    acceptRecord: '',
    installFile: '',
    acceptPhoto: ''
  })
  installDialogVisible.value = true
}

async function handleInstallSubmit() {
  try {
    await installFormRef.value.validate()
  } catch {
    return
  }

  submitLoading.value = true
  try {
    await deviceApi.install(installForm.deviceId, {
      installLocation: installForm.installLocation,
      installDate: installForm.installDate,
      installPerson: installForm.installPerson,
      useDate: installForm.useDate,
      acceptRecord: installForm.acceptRecord,
      installFile: installForm.installFile,
      acceptPhoto: installForm.acceptPhoto
    })
    ElMessage.success('安装信息提交成功')
    installDialogVisible.value = false
    fetchList()
  } catch (e) {
    console.error(e)
  } finally {
    submitLoading.value = false
  }
}

// ========== 列表查询 ==========
async function fetchList() {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      size: pagination.size,
      deviceCode: filterForm.deviceCode || undefined,
      deviceName: filterForm.deviceName || undefined,
      status: filterForm.status !== null && filterForm.status !== undefined ? filterForm.status : undefined,
      warehouseName: filterForm.warehouseName || undefined,
      installLocation: filterForm.installLocation || undefined
    }
    const res = await deviceApi.page(params)
    tableData.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  fetchList()
}

function handleReset() {
  Object.assign(filterForm, {
    deviceCode: '',
    deviceName: '',
    status: null,
    warehouseName: '',
    installLocation: ''
  })
  handleSearch()
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.device-container {
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

@media (max-width: 768px) {
  .filter-form :deep(.el-form-item) {
    width: 100%;
  }

  .filter-form :deep(.el-form-item .el-input),
  .filter-form :deep(.el-form-item .el-select) {
    width: 100% !important;
  }
}
</style>
