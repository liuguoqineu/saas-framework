<template>
  <div class="device-replacement-container">
    <!-- 筛选区 -->
    <el-card class="filter-card">
      <el-form :inline="true" :model="filterForm" class="filter-form">
        <el-form-item label="更换单号">
          <el-input v-model="filterForm.replacementNo" placeholder="请输入更换单号" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="更换类型">
          <el-select v-model="filterForm.replacementType" placeholder="全部" clearable style="width: 130px">
            <el-option v-for="item in replacementTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="更换时间">
          <el-date-picker v-model="filterForm.replaceTimeRange" type="daterange" range-separator="至"
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
        <span class="table-title">更换档案</span>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe border style="width: 100%">
        <el-table-column prop="replacementNo" label="更换单号" min-width="160" />
        <el-table-column prop="replacementType" label="更换类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.replacementType === 1 ? 'warning' : 'danger'" size="small">
              {{ replacementTypeLabel[row.replacementType] || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="replaceTime" label="更换时间" min-width="160" />
        <el-table-column prop="replacePerson" label="更换人员" width="100" />
        <el-table-column prop="replaceReason" label="更换原因" min-width="150" show-overflow-tooltip />
        <el-table-column prop="repairNo" label="关联维修单号" min-width="140" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination v-model:current-page="pagination.page" v-model:page-size="pagination.size"
        :page-sizes="[10, 20, 50, 100]" :total="pagination.total"
        :layout="paginationLayout" @size-change="handleSearch" @current-change="fetchList"
        style="margin-top: 16px; justify-content: flex-end" />
    </el-card>

    <!-- 更换记录详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="更换记录详情" width="900px" destroy-on-close>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="更换单号">{{ detailData.replacementNo }}</el-descriptions-item>
        <el-descriptions-item label="更换类型">
          <el-tag :type="detailData.replacementType === 1 ? 'warning' : 'danger'" size="small">
            {{ replacementTypeLabel[detailData.replacementType] || '-' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="更换时间">{{ detailData.replaceTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="更换人员">{{ detailData.replacePerson || '-' }}</el-descriptions-item>
        <el-descriptions-item label="更换原因" :span="2">{{ detailData.replaceReason || '-' }}</el-descriptions-item>
        <el-descriptions-item label="关联维修单号">{{ detailData.repairNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ detailData.operator || '-' }}</el-descriptions-item>
      </el-descriptions>

      <template v-if="detailData.replacePhoto">
        <el-divider content-position="left">更换照片</el-divider>
        <div class="photo-preview">
          <el-image v-for="(url, idx) in (detailData.replacePhoto || '').split(',').filter(Boolean)" :key="idx"
            :src="url" :preview-src-list="(detailData.replacePhoto || '').split(',').filter(Boolean)"
            :initial-index="idx" fit="contain" style="max-width: 150px; max-height: 120px; margin-right: 8px" />
        </div>
      </template>

      <template v-if="detailData.items && detailData.items.length > 0">
        <el-divider content-position="left">更换明细</el-divider>
        <el-table :data="detailData.items" border size="small">
          <el-table-column type="index" label="序号" width="60" />
          <el-table-column prop="itemType" label="类型" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.itemType === 1 ? '' : 'warning'" size="small">
                {{ replacementItemTypeLabel[row.itemType] || '-' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="oldItemName" label="旧件名称" min-width="100" />
          <el-table-column prop="oldItemModel" label="旧件型号" min-width="100" />
          <el-table-column prop="oldItemStatus" label="旧件状态" width="90" align="center">
            <template #default="{ row }">
              {{ oldItemStatusLabel[row.oldItemStatus] || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="newItemName" label="新件名称" min-width="100" />
          <el-table-column prop="newItemModel" label="新件型号" min-width="100" />
          <el-table-column prop="newItemQty" label="数量" width="70" align="center" />
          <el-table-column label="关联出库单号" min-width="140">
            <template #default="{ row }">
              {{ row.stockOutOrderNo || row.newStockOutOrderNo || '-' }}
            </template>
          </el-table-column>
        </el-table>
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
import {
  deviceReplacementApi,
  replacementTypeOptions,
  replacementTypeLabel,
  replacementItemTypeLabel,
  oldItemStatusLabel
} from '@/api/deviceReplacement'

const loading = ref(false)
const tableData = ref([])

const filterForm = reactive({
  replacementNo: '',
  replacementType: null,
  replaceTimeRange: null
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

// ========== 列表查询 ==========
async function fetchList() {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      size: pagination.size,
      replacementNo: filterForm.replacementNo || undefined,
      replacementType: filterForm.replacementType || undefined,
      replaceTimeStart: filterForm.replaceTimeRange?.[0] || undefined,
      replaceTimeEnd: filterForm.replaceTimeRange?.[1] || undefined
    }
    const res = await deviceReplacementApi.page(params)
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
    replacementNo: '',
    replacementType: null,
    replaceTimeRange: null
  })
  handleSearch()
}

// ========== 详情 ==========
const detailDialogVisible = ref(false)
const detailData = ref({})

async function handleDetail(row) {
  try {
    const res = await deviceReplacementApi.detail(row.id)
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
.device-replacement-container {
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
