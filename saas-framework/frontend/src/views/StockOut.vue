<template>
  <div class="stock-out-container">
    <!-- 筛选区 -->
    <el-card class="filter-card">
      <el-form :inline="true" :model="filterForm" class="filter-form">
        <el-form-item label="出库单号">
          <el-input v-model="filterForm.orderNo" placeholder="请输入出库单号" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="物料名称">
          <el-input v-model="filterForm.itemName" placeholder="请输入物料名称" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item label="领用用途">
          <el-select v-model="filterForm.usageType" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="t in usageTypeOptions" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="领用人">
          <el-input v-model="filterForm.receiver" placeholder="请输入领用人" clearable style="width: 120px" />
        </el-form-item>
        <el-form-item label="出库日期">
          <el-date-picker v-model="filterForm.dateRange" type="daterange" range-separator="至"
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
        <span class="table-title">出库单列表</span>
        <div class="table-actions">
          <el-button type="primary" @click="handleStockOutOpen">出库操作</el-button>
        </div>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe border style="width: 100%">
        <el-table-column prop="orderNo" label="出库单号" min-width="160" />
        <el-table-column prop="itemType" label="类型" width="80">
          <template #default="{ row }">
            <el-tag :type="row.itemType === 1 ? '' : 'info'" size="small">{{ itemTypeLabel[row.itemType] || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="itemName" label="物料名称" min-width="120" />
        <el-table-column prop="brand" label="品牌" width="100" />
        <el-table-column prop="model" label="型号" width="120" />
        <el-table-column prop="quantity" label="数量" width="80" align="center" />
        <el-table-column prop="usageType" label="用途" width="100">
          <template #default="{ row }">
            <el-tag :type="usageTypeTagType[row.usageType]" size="small">{{ usageTypeLabel[row.usageType] || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="receiver" label="领用人" width="90" />
        <el-table-column prop="deptName" label="领用部门" min-width="100" />
        <el-table-column prop="createTime" label="出库时间" min-width="170" />
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button size="small" link type="primary" @click="handleDetail(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination v-model:current-page="pagination.page" v-model:page-size="pagination.size"
        :page-sizes="[10, 20, 50]" :total="pagination.total"
        :layout="paginationLayout" @size-change="handleSearch" @current-change="fetchList"
        style="margin-top: 16px; justify-content: flex-end" />
    </el-card>

    <!-- 出库操作对话框 -->
    <el-dialog v-model="stockOutDialogVisible" title="出库操作" width="640px" destroy-on-close>
      <el-form ref="stockOutFormRef" :model="stockOutForm" :rules="stockOutRules" label-width="100px">
        <el-form-item label="库存物料" prop="inventoryId">
          <el-select v-model="stockOutForm.inventoryId" placeholder="请搜索选择库存物料"
            filterable remote :remote-method="searchInventory" :loading="inventoryLoading"
            style="width: 100%" @change="handleInventoryChange">
            <el-option v-for="item in inventoryOptions" :key="item.id" :label="`${item.itemName} | ${item.brand || '-'} | ${item.model || '-'} | ${item.warehouseName || '-'} | 库存:${item.totalQty}`"
              :value="item.id" />
          </el-select>
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="类型">
              <span>{{ itemTypeLabel[stockOutForm.itemType] || '-' }}</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="物料名称">
              <span>{{ stockOutForm.itemName || '-' }}</span>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="品牌">
              <span>{{ stockOutForm.brand || '-' }}</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="型号">
              <span>{{ stockOutForm.model || '-' }}</span>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="规格">
              <span>{{ stockOutForm.spec || '-' }}</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="单位">
              <span>{{ stockOutForm.unit || '-' }}</span>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="出库数量" prop="quantity">
              <el-input-number v-model="stockOutForm.quantity" :min="1" :max="stockOutForm.maxQty" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="领用用途" prop="usageType">
              <el-select v-model="stockOutForm.usageType" placeholder="请选择领用用途" style="width: 100%">
                <el-option v-for="t in usageTypeOptions" :key="t.value" :label="t.label" :value="t.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row v-if="stockOutForm.usageType === 2" :gutter="20">
          <el-col :span="12">
            <el-form-item label="维修单号">
              <el-input v-model="stockOutForm.repairOrderId" placeholder="请输入维修单号" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="领用人" prop="receiver">
              <el-input v-model="stockOutForm.receiver" placeholder="请输入领用人" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话">
              <el-input v-model="stockOutForm.receiverPhone" placeholder="请输入联系电话" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="领用部门">
              <el-input v-model="stockOutForm.deptName" placeholder="请输入领用部门" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="审核人">
              <el-input v-model="stockOutForm.reviewer" placeholder="请输入审核人" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="stockOutForm.remark" type="textarea" :rows="2" placeholder="出库备注（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stockOutDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleStockOutSubmit" :loading="submitLoading">确认出库</el-button>
      </template>
    </el-dialog>

    <!-- 出库详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="出库单详情" width="700px" destroy-on-close>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="出库单号">{{ detailData.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="类型">
          <el-tag :type="detailData.itemType === 1 ? '' : 'info'" size="small">{{ itemTypeLabel[detailData.itemType] || '-' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="物料名称">{{ detailData.itemName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="品牌">{{ detailData.brand || '-' }}</el-descriptions-item>
        <el-descriptions-item label="型号">{{ detailData.model || '-' }}</el-descriptions-item>
        <el-descriptions-item label="规格">{{ detailData.spec || '-' }}</el-descriptions-item>
        <el-descriptions-item label="单位">{{ detailData.unit || '-' }}</el-descriptions-item>
        <el-descriptions-item label="出库数量">{{ detailData.quantity }}</el-descriptions-item>
        <el-descriptions-item label="领用用途">
          <el-tag :type="usageTypeTagType[detailData.usageType]" size="small">{{ usageTypeLabel[detailData.usageType] || '-' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item v-if="detailData.usageType === 2" label="维修单号">{{ detailData.repairOrderId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="领用人">{{ detailData.receiver || '-' }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ detailData.receiverPhone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="领用部门">{{ detailData.deptName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="审核人">{{ detailData.reviewer || '-' }}</el-descriptions-item>
        <el-descriptions-item label="出库时间">{{ detailData.createTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detailData.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { stockOutApi, usageTypeOptions, usageTypeLabel, usageTypeTagType, itemTypeLabel } from '@/api/stockOut'
import { inventoryApi } from '@/api/inventory'

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref([])

const filterForm = reactive({
  orderNo: '',
  itemName: '',
  usageType: null,
  receiver: '',
  dateRange: null
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

// ========== 出库操作 ==========
const stockOutDialogVisible = ref(false)
const stockOutFormRef = ref(null)
const inventoryLoading = ref(false)
const inventoryOptions = ref([])

const stockOutForm = reactive({
  inventoryId: null,
  itemType: null,
  itemName: '',
  brand: '',
  model: '',
  spec: '',
  unit: '',
  maxQty: 0,
  quantity: 1,
  usageType: null,
  repairOrderId: '',
  deptName: '',
  receiver: '',
  receiverPhone: '',
  reviewer: '',
  remark: ''
})

const stockOutRules = {
  inventoryId: [{ required: true, message: '请选择库存物料', trigger: 'change' }],
  quantity: [{ required: true, message: '请输入出库数量', trigger: 'change' }],
  usageType: [{ required: true, message: '请选择领用用途', trigger: 'change' }],
  receiver: [{ required: true, message: '请输入领用人', trigger: 'blur' }]
}

async function searchInventory(query) {
  if (!query) {
    inventoryOptions.value = []
    return
  }
  inventoryLoading.value = true
  try {
    const res = await inventoryApi.page({ page: 1, size: 20, itemName: query })
    inventoryOptions.value = res.data?.records || []
  } catch (e) {
    console.error(e)
  } finally {
    inventoryLoading.value = false
  }
}

function handleInventoryChange(id) {
  const item = inventoryOptions.value.find(i => i.id === id)
  if (item) {
    stockOutForm.itemType = item.itemType
    stockOutForm.itemName = item.itemName
    stockOutForm.brand = item.brand
    stockOutForm.model = item.model
    stockOutForm.spec = item.spec
    stockOutForm.unit = item.unit
    stockOutForm.maxQty = item.totalQty
    stockOutForm.quantity = 1
  }
}

function handleStockOutOpen() {
  Object.assign(stockOutForm, {
    inventoryId: null,
    itemType: null,
    itemName: '',
    brand: '',
    model: '',
    spec: '',
    unit: '',
    maxQty: 0,
    quantity: 1,
    usageType: null,
    repairOrderId: '',
    deptName: '',
    receiver: '',
    receiverPhone: '',
    reviewer: '',
    remark: ''
  })
  inventoryOptions.value = []
  stockOutDialogVisible.value = true
}

async function handleStockOutSubmit() {
  try {
    await stockOutFormRef.value.validate()
  } catch {
    return
  }

  if (stockOutForm.quantity > stockOutForm.maxQty) {
    ElMessage.warning(`出库数量不能超过当前库存(${stockOutForm.maxQty})`)
    return
  }

  submitLoading.value = true
  try {
    await stockOutApi.stockOut({
      inventoryId: stockOutForm.inventoryId,
      quantity: stockOutForm.quantity,
      usageType: stockOutForm.usageType,
      repairOrderId: stockOutForm.repairOrderId || undefined,
      deptName: stockOutForm.deptName,
      receiver: stockOutForm.receiver,
      receiverPhone: stockOutForm.receiverPhone,
      reviewer: stockOutForm.reviewer,
      remark: stockOutForm.remark
    })
    ElMessage.success('出库操作成功')
    stockOutDialogVisible.value = false
    fetchList()
  } catch (e) {
    console.error(e)
  } finally {
    submitLoading.value = false
  }
}

// ========== 查看详情 ==========
const detailDialogVisible = ref(false)
const detailData = ref({})

async function handleDetail(row) {
  try {
    const res = await stockOutApi.detail(row.id)
    detailData.value = res.data || {}
    detailDialogVisible.value = true
  } catch (e) {
    console.error(e)
  }
}

// ========== 列表查询 ==========
async function fetchList() {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      size: pagination.size,
      orderNo: filterForm.orderNo || undefined,
      itemName: filterForm.itemName || undefined,
      usageType: filterForm.usageType !== null && filterForm.usageType !== undefined ? filterForm.usageType : undefined,
      receiver: filterForm.receiver || undefined,
      startDate: filterForm.dateRange?.[0] || undefined,
      endDate: filterForm.dateRange?.[1] || undefined
    }
    const res = await stockOutApi.page(params)
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
    orderNo: '',
    itemName: '',
    usageType: null,
    receiver: '',
    dateRange: null
  })
  handleSearch()
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.stock-out-container {
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
