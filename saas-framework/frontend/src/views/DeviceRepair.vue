<template>
  <div class="device-repair-container">
    <!-- 筛选区 -->
    <el-card class="filter-card">
      <el-form :inline="true" :model="filterForm" class="filter-form">
        <el-form-item label="设备编码">
          <el-input v-model="filterForm.deviceCode" placeholder="请输入设备编码" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item label="故障部位">
          <el-input v-model="filterForm.faultPart" placeholder="请输入故障部位" clearable style="width: 130px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filterForm.status" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="item in repairStatusOptions" :key="item.value" :label="item.label"
              :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="报修日期">
          <el-date-picker v-model="filterForm.repairTimeRange" type="daterange" range-separator="至"
            start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD"
            style="width: 240px" />
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
        <span class="table-title">设备维修档案</span>
        <div class="table-actions">
          <el-button type="primary" @click="handleAddRepair">设备报修</el-button>
        </div>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe border style="width: 100%">
        <el-table-column prop="repairNo" label="报修单号" min-width="140" />
        <el-table-column prop="deviceCode" label="设备编码" min-width="120" />
        <el-table-column prop="deviceName" label="设备名称" min-width="120" />
        <el-table-column prop="faultPart" label="故障部位" min-width="100" />
        <el-table-column prop="faultDescription" label="故障描述" min-width="150" show-overflow-tooltip />
        <el-table-column prop="urgency" label="紧急程度" min-width="90">
          <template #default="{ row }">
            <el-tag :type="row.urgency === '紧急' ? 'danger' : 'info'" size="small">
              {{ row.urgency }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" min-width="90">
          <template #default="{ row }">
            <el-tag :type="repairStatusTagType[row.status]" size="small">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="repairTime" label="报修时间" min-width="160" />
        <el-table-column prop="repairPerson" label="维修人员" min-width="90" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button v-if="canProcess(row.status)" size="small" type="primary" @click="handleProcess(row)">处理</el-button>
            <el-button size="small" @click="handleDetail(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination v-model:current-page="pagination.page" v-model:page-size="pagination.size"
        :page-sizes="[10, 20, 50, 100]" :total="pagination.total"
        :layout="paginationLayout" @size-change="handleSearch" @current-change="fetchList"
        style="margin-top: 16px; justify-content: flex-end" />
    </el-card>

    <!-- 设备故障报修对话框 -->
    <el-dialog v-model="repairDialogVisible" title="设备故障报修" width="700px" destroy-on-close>
      <el-form ref="repairFormRef" :model="repairForm" :rules="repairRules" label-width="100px">
        <el-form-item label="选择设备" prop="deviceId">
          <el-select v-model="repairForm.deviceId" filterable remote reserve-keyword placeholder="请输入设备编码或名称搜索"
            :remote-method="handleDeviceSearch" :loading="deviceSearchLoading" style="width: 100%"
            @change="handleDeviceSelect">
            <el-option v-for="d in deviceOptions" :key="d.id" :label="`${d.deviceCode} - ${d.deviceName}`"
              :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="设备编码">
          <span>{{ repairForm.deviceCode || '-' }}</span>
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="故障时间" prop="faultTime">
              <el-date-picker v-model="repairForm.faultTime" type="datetime" placeholder="请选择故障时间"
                value-format="YYYY-MM-DD HH:mm:ss" format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="紧急程度" prop="urgency">
              <el-select v-model="repairForm.urgency" placeholder="请选择紧急程度" style="width: 100%">
                <el-option label="普通" value="普通" />
                <el-option label="紧急" value="紧急" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="故障部位" prop="faultPart">
          <el-input v-model="repairForm.faultPart" placeholder="请输入故障部位" />
        </el-form-item>
        <el-form-item label="故障描述" prop="faultDescription">
          <el-input v-model="repairForm.faultDescription" type="textarea" :rows="3" placeholder="请输入故障描述" />
        </el-form-item>
        <el-form-item label="报修照片">
          <el-upload :auto-upload="false" :on-change="(f) => handlePhotoChange(f, 'repairPhotoBefore')" :file-list="repairPhotoBeforeList"
            :on-remove="() => handlePhotoRemove('repairPhotoBefore')" list-type="picture-card" accept="image/*">
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="repairForm.remark" type="textarea" :rows="2" placeholder="请输入备注（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="repairDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleRepairSubmit" :loading="submitLoading">提交报修</el-button>
      </template>
    </el-dialog>

    <!-- 维修处理对话框 -->
    <el-dialog v-model="processDialogVisible" title="维修处理" width="900px" destroy-on-close>
      <el-form ref="processFormRef" :model="processForm" :rules="processRules" label-width="110px">
        <el-divider content-position="left">基本信息</el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="维修开始时间" prop="repairStartTime">
              <el-date-picker v-model="processForm.repairStartTime" type="datetime" placeholder="请选择开始时间"
                value-format="YYYY-MM-DD HH:mm:ss" format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="维修结束时间" prop="repairEndTime">
              <el-date-picker v-model="processForm.repairEndTime" type="datetime" placeholder="请选择结束时间"
                value-format="YYYY-MM-DD HH:mm:ss" format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="维修时长(小时)">
              <el-input-number v-model="processForm.repairDuration" :min="0" :precision="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="维修后照片">
              <el-upload :auto-upload="false" :on-change="(f) => handlePhotoChange(f, 'repairPhotoAfter')" :file-list="repairPhotoAfterList"
                :on-remove="() => handlePhotoRemove('repairPhotoAfter')" list-type="picture-card" accept="image/*">
                <el-icon><Plus /></el-icon>
              </el-upload>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="处理方式" prop="processMethod">
          <el-input v-model="processForm.processMethod" type="textarea" :rows="2" placeholder="请输入处理方式" />
        </el-form-item>
        <el-form-item label="故障原因" prop="faultReason">
          <el-input v-model="processForm.faultReason" type="textarea" :rows="2" placeholder="请输入故障原因" />
        </el-form-item>

        <el-divider content-position="left">更换信息</el-divider>
        <el-form-item label="是否有更换" prop="hasReplacement">
          <el-radio-group v-model="processForm.hasReplacement">
            <el-radio :value="0">否</el-radio>
            <el-radio :value="1">是</el-radio>
          </el-radio-group>
        </el-form-item>

        <template v-if="processForm.hasReplacement === 1">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="更换类型">
                <el-select v-model="processForm.replacementType" placeholder="请选择更换类型" style="width: 100%">
                  <el-option v-for="t in replacementTypeOptions" :key="t.value" :label="t.label" :value="t.value" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="更换人">
                <el-input v-model="processForm.replacePerson" placeholder="请输入更换人" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="更换原因">
            <el-input v-model="processForm.replaceReason" type="textarea" :rows="2" placeholder="请输入更换原因" />
          </el-form-item>
          <el-form-item label="更换照片">
            <el-upload :auto-upload="false" :on-change="(f) => handlePhotoChange(f, 'replacePhoto')" :file-list="replacePhotoList"
              :on-remove="() => handlePhotoRemove('replacePhoto')" list-type="picture-card" accept="image/*">
              <el-icon><Plus /></el-icon>
            </el-upload>
          </el-form-item>

          <el-divider content-position="left">更换明细</el-divider>
          <div class="replacement-items-header">
            <el-button type="primary" size="small" @click="addReplacementItem">添加明细</el-button>
          </div>
          <el-table :data="processForm.replacementItems" border size="small" style="width: 100%">
            <el-table-column label="类型" width="100">
              <template #default="{ row }">
                <el-select v-model="row.itemType" placeholder="类型" size="small" style="width: 80px">
                  <el-option v-for="t in replacementItemTypeOptions" :key="t.value" :label="t.label" :value="t.value" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="旧设备" width="140">
              <template #default="{ row }">
                <el-select v-if="row.itemType === 2" v-model="row.oldDeviceId" filterable remote reserve-keyword
                  placeholder="搜索设备" size="small" :remote-method="handleOldDeviceSearch"
                  style="width: 120px">
                  <el-option v-for="d in row._oldDeviceOptions || []" :key="d.id" :label="d.deviceCode" :value="d.id" />
                </el-select>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="旧件名称" min-width="100">
              <template #default="{ row }">
                <el-input v-model="row.oldItemName" placeholder="名称" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="旧件型号" min-width="100">
              <template #default="{ row }">
                <el-input v-model="row.oldItemModel" placeholder="型号" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="旧件状态" width="100">
              <template #default="{ row }">
                <el-select v-model="row.oldItemStatus" placeholder="状态" size="small" style="width: 80px">
                  <el-option v-for="s in oldItemStatusOptions" :key="s.value" :label="s.label" :value="s.value" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="新设备" width="140">
              <template #default="{ row }">
                <el-select v-if="row.itemType === 2" v-model="row.newDeviceId" filterable remote reserve-keyword
                  placeholder="搜索设备" size="small" :remote-method="(q) => handleNewDeviceSearch(row, q)"
                  style="width: 120px">
                  <el-option v-for="d in row._newDeviceOptions || []" :key="d.id" :label="d.deviceCode" :value="d.id" />
                </el-select>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="新件名称" min-width="100">
              <template #default="{ row }">
                <el-input v-model="row.newItemName" placeholder="名称" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="新件型号" min-width="100">
              <template #default="{ row }">
                <el-input v-model="row.newItemModel" placeholder="型号" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="数量" width="90">
              <template #default="{ row }">
                <el-input-number v-model="row.newItemQty" :min="1" size="small" style="width: 70px" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="70" fixed="right">
              <template #default="{ $index }">
                <el-button size="small" type="danger" link @click="removeReplacementItem($index)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="processDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleProcessSubmit" :loading="submitLoading">提交处理</el-button>
      </template>
    </el-dialog>

    <!-- 维修详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="维修详情" width="800px" destroy-on-close>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="报修单号">{{ detailData.repairNo }}</el-descriptions-item>
        <el-descriptions-item label="设备编码">{{ detailData.deviceCode }}</el-descriptions-item>
        <el-descriptions-item label="设备名称">{{ detailData.deviceName }}</el-descriptions-item>
        <el-descriptions-item label="故障部位">{{ detailData.faultPart || '-' }}</el-descriptions-item>
        <el-descriptions-item label="紧急程度">
          <el-tag :type="detailData.urgency === '紧急' ? 'danger' : 'info'" size="small">
            {{ detailData.urgency }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="repairStatusTagType[detailData.status]" size="small">
            {{ detailData.status }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="报修时间">{{ detailData.repairTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="故障时间">{{ detailData.faultTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="维修人员">{{ detailData.repairPerson || '-' }}</el-descriptions-item>
        <el-descriptions-item label="维修时长">{{ detailData.repairDuration ? detailData.repairDuration + ' 小时' : '-' }}</el-descriptions-item>
        <el-descriptions-item label="故障描述" :span="2">{{ detailData.faultDescription || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detailData.remark || '-' }}</el-descriptions-item>
      </el-descriptions>

      <template v-if="detailData.repairPhotoBefore">
        <el-divider content-position="left">报修照片</el-divider>
        <div class="photo-preview">
          <el-image v-for="(url, idx) in (detailData.repairPhotoBefore || '').split(',').filter(Boolean)" :key="idx"
            :src="url" :preview-src-list="(detailData.repairPhotoBefore || '').split(',').filter(Boolean)"
            :initial-index="idx" fit="contain" style="max-width: 150px; max-height: 120px; margin-right: 8px" />
        </div>
      </template>

      <template v-if="detailData.processMethod || detailData.faultReason">
        <el-divider content-position="left">处理信息</el-divider>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="处理方式" :span="2">{{ detailData.processMethod || '-' }}</el-descriptions-item>
          <el-descriptions-item label="故障原因" :span="2">{{ detailData.faultReason || '-' }}</el-descriptions-item>
          <el-descriptions-item label="维修开始时间">{{ detailData.repairStartTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="维修结束时间">{{ detailData.repairEndTime || '-' }}</el-descriptions-item>
        </el-descriptions>
      </template>

      <template v-if="detailData.repairPhotoAfter">
        <el-divider content-position="left">维修后照片</el-divider>
        <div class="photo-preview">
          <el-image v-for="(url, idx) in (detailData.repairPhotoAfter || '').split(',').filter(Boolean)" :key="idx"
            :src="url" :preview-src-list="(detailData.repairPhotoAfter || '').split(',').filter(Boolean)"
            :initial-index="idx" fit="contain" style="max-width: 150px; max-height: 120px; margin-right: 8px" />
        </div>
      </template>

      <template v-if="detailData.hasReplacement === 1">
        <el-divider content-position="left">更换信息</el-divider>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="更换类型">
            {{ replacementTypeLabel[detailData.replacementType] || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="更换人">{{ detailData.replacePerson || '-' }}</el-descriptions-item>
          <el-descriptions-item label="更换原因" :span="2">{{ detailData.replaceReason || '-' }}</el-descriptions-item>
        </el-descriptions>

        <template v-if="detailData.replacementItems && detailData.replacementItems.length > 0">
          <el-divider content-position="left">更换明细</el-divider>
          <el-table :data="detailData.replacementItems" border size="small">
            <el-table-column type="index" label="序号" width="60" />
            <el-table-column prop="itemType" label="类型" width="80">
              <template #default="{ row }">
                {{ row.itemType === 1 ? '配件' : row.itemType === 2 ? '设备' : '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="oldItemName" label="旧件名称" min-width="100" />
            <el-table-column prop="oldItemModel" label="旧件型号" min-width="100" />
            <el-table-column prop="oldItemStatus" label="旧件状态" width="80">
              <template #default="{ row }">
                {{ oldItemStatusLabel[row.oldItemStatus] || '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="newItemName" label="新件名称" min-width="100" />
            <el-table-column prop="newItemModel" label="新件型号" min-width="100" />
            <el-table-column prop="newItemQty" label="数量" width="70" align="center" />
          </el-table>
        </template>

        <template v-if="detailData.replacePhoto">
          <el-divider content-position="left">更换照片</el-divider>
          <div class="photo-preview">
            <el-image v-for="(url, idx) in (detailData.replacePhoto || '').split(',').filter(Boolean)" :key="idx"
              :src="url" :preview-src-list="(detailData.replacePhoto || '').split(',').filter(Boolean)"
              :initial-index="idx" fit="contain" style="max-width: 150px; max-height: 120px; margin-right: 8px" />
          </div>
        </template>
      </template>

      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { deviceRepairApi, replacementTypeOptions, replacementTypeLabel, oldItemStatusOptions, oldItemStatusLabel, replacementItemTypeOptions } from '@/api/deviceRepair'
import { repairApi, repairStatusOptions, repairStatusTagType } from '@/api/repair'
import request from '@/utils/request'

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref([])

const filterForm = reactive({
  deviceCode: '',
  faultPart: '',
  status: '',
  repairTimeRange: null
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

// ========== 图片上传 ==========
const repairPhotoBeforeList = ref([])
const repairPhotoAfterList = ref([])
const replacePhotoList = ref([])
const pendingPhotos = reactive({
  repairPhotoBefore: [],
  repairPhotoAfter: [],
  replacePhoto: []
})

function handlePhotoChange(file, field) {
  pendingPhotos[field].push(file.raw)
}

function handlePhotoRemove(field) {
  pendingPhotos[field] = []
  if (field === 'repairPhotoBefore') repairPhotoBeforeList.value = []
  else if (field === 'repairPhotoAfter') repairPhotoAfterList.value = []
  else if (field === 'replacePhoto') replacePhotoList.value = []
}

async function uploadPhotos(files, field) {
  const urls = []
  for (const file of files) {
    try {
      const fd = new FormData()
      fd.append('file', file)
      const res = await request.post('/purchase/upload-file', fd, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
      if (res.data) urls.push(res.data)
    } catch (e) {
      console.error(`${field}照片上传失败`, e)
    }
  }
  return urls.join(',')
}

function canProcess(status) {
  return status === '待分配' || status === '已分配' || status === '处理中'
}

// ========== 列表查询 ==========
async function fetchList() {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      size: pagination.size,
      deviceCode: filterForm.deviceCode || undefined,
      faultPart: filterForm.faultPart || undefined,
      status: filterForm.status || undefined,
      repairTimeStart: filterForm.repairTimeRange?.[0] || undefined,
      repairTimeEnd: filterForm.repairTimeRange?.[1] || undefined
    }
    const res = await repairApi.page(params)
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
    faultPart: '',
    status: '',
    repairTimeRange: null
  })
  handleSearch()
}

// ========== 设备搜索 ==========
const deviceOptions = ref([])
const deviceSearchLoading = ref(false)

async function handleDeviceSearch(query) {
  if (!query) {
    deviceOptions.value = []
    return
  }
  deviceSearchLoading.value = true
  try {
    const res = await deviceRepairApi.searchDevice({ keyword: query })
    deviceOptions.value = res.data || []
  } catch (e) {
    console.error(e)
  } finally {
    deviceSearchLoading.value = false
  }
}

function handleDeviceSelect(deviceId) {
  const device = deviceOptions.value.find(d => d.id === deviceId)
  if (device) {
    repairForm.deviceCode = device.deviceCode
    repairForm.deviceName = device.deviceName
  }
}

// ========== 设备故障报修 ==========
const repairDialogVisible = ref(false)
const repairFormRef = ref(null)
const repairForm = reactive({
  deviceId: null,
  deviceCode: '',
  deviceName: '',
  faultTime: '',
  faultPart: '',
  faultDescription: '',
  repairPhotoBefore: '',
  urgency: '普通',
  remark: ''
})

const repairRules = {
  deviceId: [{ required: true, message: '请选择设备', trigger: 'change' }],
  faultTime: [{ required: true, message: '请选择故障时间', trigger: 'change' }],
  faultPart: [{ required: true, message: '请输入故障部位', trigger: 'blur' }],
  faultDescription: [{ required: true, message: '请输入故障描述', trigger: 'blur' }],
  urgency: [{ required: true, message: '请选择紧急程度', trigger: 'change' }]
}

function handleAddRepair() {
  Object.assign(repairForm, {
    deviceId: null,
    deviceCode: '',
    deviceName: '',
    faultTime: '',
    faultPart: '',
    faultDescription: '',
    repairPhotoBefore: '',
    urgency: '普通',
    remark: ''
  })
  deviceOptions.value = []
  repairPhotoBeforeList.value = []
  pendingPhotos.repairPhotoBefore = []
  repairDialogVisible.value = true
}

async function handleRepairSubmit() {
  try {
    await repairFormRef.value.validate()
  } catch {
    return
  }

  submitLoading.value = true
  try {
    // 先上传报修照片
    let photoUrl = repairForm.repairPhotoBefore
    if (pendingPhotos.repairPhotoBefore.length > 0) {
      photoUrl = await uploadPhotos(pendingPhotos.repairPhotoBefore, 'repairPhotoBefore')
    }

    await deviceRepairApi.deviceRepair({
      deviceId: repairForm.deviceId,
      deviceCode: repairForm.deviceCode,
      deviceName: repairForm.deviceName,
      faultTime: repairForm.faultTime,
      faultPart: repairForm.faultPart,
      faultDescription: repairForm.faultDescription,
      repairPhotoBefore: photoUrl,
      urgency: repairForm.urgency,
      remark: repairForm.remark
    })
    ElMessage.success('设备报修提交成功')
    repairDialogVisible.value = false
    fetchList()
  } catch (e) {
    console.error(e)
  } finally {
    submitLoading.value = false
  }
}

// ========== 维修处理 ==========
const processDialogVisible = ref(false)
const processFormRef = ref(null)
const currentProcessId = ref(null)
const processForm = reactive({
  processMethod: '',
  faultReason: '',
  repairStartTime: '',
  repairEndTime: '',
  repairDuration: null,
  repairPhotoAfter: '',
  hasReplacement: 0,
  replacementType: null,
  replacePerson: '',
  replaceReason: '',
  replacePhoto: '',
  replacementItems: []
})

const processRules = {
  processMethod: [{ required: true, message: '请输入处理方式', trigger: 'blur' }],
  faultReason: [{ required: true, message: '请输入故障原因', trigger: 'blur' }]
}

function createEmptyReplacementItem() {
  return {
    itemType: 1,
    oldDeviceId: null,
    oldItemName: '',
    oldItemModel: '',
    oldItemStatus: null,
    newDeviceId: null,
    newItemName: '',
    newItemModel: '',
    newItemQty: 1,
    _oldDeviceOptions: [],
    _newDeviceOptions: []
  }
}

function addReplacementItem() {
  processForm.replacementItems.push(createEmptyReplacementItem())
}

function removeReplacementItem(index) {
  processForm.replacementItems.splice(index, 1)
}

async function handleOldDeviceSearch(query) {
  if (!query) return
  try {
    const res = await deviceRepairApi.searchDevice({ keyword: query })
    // 更新最后一行的旧设备选项（通用方式：更新所有行的选项）
    processForm.replacementItems.forEach(item => {
      item._oldDeviceOptions = res.data || []
    })
  } catch (e) {
    console.error(e)
  }
}

async function handleNewDeviceSearch(row, query) {
  if (!query) return
  try {
    const res = await deviceRepairApi.searchDevice({ keyword: query })
    row._newDeviceOptions = res.data || []
  } catch (e) {
    console.error(e)
  }
}

function handleProcess(row) {
  currentProcessId.value = row.id
  Object.assign(processForm, {
    processMethod: '',
    faultReason: '',
    repairStartTime: '',
    repairEndTime: '',
    repairDuration: null,
    repairPhotoAfter: '',
    hasReplacement: 0,
    replacementType: null,
    replacePerson: '',
    replaceReason: '',
    replacePhoto: '',
    replacementItems: []
  })
  repairPhotoAfterList.value = []
  replacePhotoList.value = []
  pendingPhotos.repairPhotoAfter = []
  pendingPhotos.replacePhoto = []
  processDialogVisible.value = true
}

async function handleProcessSubmit() {
  try {
    await processFormRef.value.validate()
  } catch {
    return
  }

  if (processForm.hasReplacement === 1 && processForm.replacementItems.length === 0) {
    ElMessage.warning('请至少添加一条更换明细')
    return
  }

  submitLoading.value = true
  try {
    // 先上传照片
    let afterPhotoUrl = processForm.repairPhotoAfter
    if (pendingPhotos.repairPhotoAfter.length > 0) {
      afterPhotoUrl = await uploadPhotos(pendingPhotos.repairPhotoAfter, 'repairPhotoAfter')
    }

    let replacePhotoUrl = processForm.replacePhoto
    if (processForm.hasReplacement === 1 && pendingPhotos.replacePhoto.length > 0) {
      replacePhotoUrl = await uploadPhotos(pendingPhotos.replacePhoto, 'replacePhoto')
    }

    const data = {
      processMethod: processForm.processMethod,
      faultReason: processForm.faultReason,
      repairStartTime: processForm.repairStartTime || undefined,
      repairEndTime: processForm.repairEndTime || undefined,
      repairDuration: processForm.repairDuration || undefined,
      repairPhotoAfter: afterPhotoUrl || undefined,
      hasReplacement: processForm.hasReplacement
    }

    if (processForm.hasReplacement === 1) {
      data.replacementType = processForm.replacementType
      data.replacePerson = processForm.replacePerson
      data.replaceReason = processForm.replaceReason
      data.replacePhoto = replacePhotoUrl
      data.replacementItems = processForm.replacementItems.map(item => ({
        itemType: item.itemType,
        oldDeviceId: item.itemType === 2 ? item.oldDeviceId : undefined,
        oldItemName: item.oldItemName,
        oldItemModel: item.oldItemModel,
        oldItemStatus: item.oldItemStatus,
        newDeviceId: item.itemType === 2 ? item.newDeviceId : undefined,
        newItemName: item.newItemName,
        newItemModel: item.newItemModel,
        newItemQty: item.newItemQty
      }))
    }

    await deviceRepairApi.deviceProcess(currentProcessId.value, data)
    ElMessage.success('维修处理提交成功')
    processDialogVisible.value = false
    fetchList()
  } catch (e) {
    console.error(e)
  } finally {
    submitLoading.value = false
  }
}

// ========== 维修详情 ==========
const detailDialogVisible = ref(false)
const detailData = ref({})

async function handleDetail(row) {
  try {
    const res = await repairApi.detail(row.id)
    detailData.value = res.data || {}
    detailDialogVisible.value = true
  } catch (e) {
    console.error(e)
  }
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.device-repair-container {
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

.replacement-items-header {
  margin-bottom: 10px;
}

.photo-preview {
  display: flex;
  justify-content: flex-start;
  padding: 8px 0;
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
