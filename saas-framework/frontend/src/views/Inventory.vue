<template>
  <div class="inventory-container">
    <!-- 筛选区 -->
    <el-card class="filter-card">
      <el-form :inline="true" :model="filterForm" class="filter-form">
        <el-form-item label="物料名称">
          <el-input v-model="filterForm.itemName" placeholder="请输入物料名称" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item label="仓库">
          <el-input v-model="filterForm.warehouseName" placeholder="请输入仓库名称" clearable style="width: 130px" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="filterForm.itemType" placeholder="全部" clearable style="width: 100px">
            <el-option v-for="t in itemTypeOptions" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
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
        <span class="table-title">库存台账</span>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe border style="width: 100%">
        <el-table-column prop="itemType" label="类型" width="80">
          <template #default="{ row }">
            <el-tag :type="row.itemType === 1 ? '' : 'info'" size="small">{{ itemTypeLabel[row.itemType] || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="itemName" label="物料名称" min-width="120" />
        <el-table-column prop="brand" label="品牌" width="100" />
        <el-table-column prop="model" label="型号" width="120" />
        <el-table-column prop="spec" label="规格" width="100" />
        <el-table-column prop="warehouseName" label="仓库" min-width="110" />
        <el-table-column prop="totalQty" label="当前库存" width="90" align="center">
          <template #default="{ row }">
            <span :class="{ 'low-stock': row.minStockQty && row.totalQty <= row.minStockQty }">{{ row.totalQty }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="stockedInQty" label="累计入库" width="90" align="center" />
        <el-table-column prop="stockedOutQty" label="累计出库" width="90" align="center" />
        <el-table-column prop="unit" label="单位" width="70" align="center" />
        <el-table-column prop="minStockQty" label="预警阈值" width="90" align="center">
          <template #default="{ row }">{{ row.minStockQty ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleDetail(row)">详情</el-button>
            <el-button size="small" type="success" @click="handleStockOut(row)" :disabled="row.totalQty <= 0">出库</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination v-model:current-page="pagination.page" v-model:page-size="pagination.size"
        :page-sizes="[10, 20, 50]" :total="pagination.total"
        :layout="paginationLayout" @size-change="handleSearch" @current-change="fetchList"
        style="margin-top: 16px; justify-content: flex-end" />
    </el-card>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="库存详情" width="900px" destroy-on-close>
      <el-descriptions :column="3" border>
        <el-descriptions-item label="物料名称">{{ detailData.itemName }}</el-descriptions-item>
        <el-descriptions-item label="类型">
          <el-tag :type="detailData.itemType === 1 ? '' : 'info'" size="small">{{ itemTypeLabel[detailData.itemType] || '-' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="品牌">{{ detailData.brand || '-' }}</el-descriptions-item>
        <el-descriptions-item label="型号">{{ detailData.model || '-' }}</el-descriptions-item>
        <el-descriptions-item label="规格">{{ detailData.spec || '-' }}</el-descriptions-item>
        <el-descriptions-item label="单位">{{ detailData.unit || '-' }}</el-descriptions-item>
        <el-descriptions-item label="仓库">{{ detailData.warehouseName }}</el-descriptions-item>
        <el-descriptions-item label="当前库存">
          <span :class="{ 'low-stock': detailData.minStockQty && detailData.totalQty <= detailData.minStockQty }">{{ detailData.totalQty }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="累计入库">{{ detailData.stockedInQty }}</el-descriptions-item>
        <el-descriptions-item label="累计出库">{{ detailData.stockedOutQty }}</el-descriptions-item>
        <el-descriptions-item label="预警阈值">
          <div style="display: flex; align-items: center; gap: 8px">
            <el-input-number v-model="detailData.minStockQty" :min="0" :precision="0"
              placeholder="设置阈值" style="width: 120px" size="small" />
            <el-button type="primary" size="small" @click="handleSaveMinStockQty" :loading="minStockSaving">保存</el-button>
          </div>
          <span style="color: #999; font-size: 12px; margin-left: 4px">（库存≤此值时标红提醒）</span>
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="3">{{ detailData.remark || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">出库记录</el-divider>
      <el-table v-if="stockOutRecords.length > 0" :data="stockOutRecords" stripe border size="small">
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="orderNo" label="出库单号" min-width="170" />
        <el-table-column prop="quantity" label="出库数量" width="90" align="center" />
        <el-table-column prop="usageType" label="领用用途" width="100">
          <template #default="{ row }">{{ usageTypeLabel[row.usageType] || '-' }}</template>
        </el-table-column>
        <el-table-column prop="receiver" label="领用人" width="90" />
        <el-table-column prop="deptName" label="领用部门" min-width="100" />
        <el-table-column prop="reviewer" label="审核人" width="90" />
        <el-table-column prop="remark" label="备注" min-width="100" show-overflow-tooltip />
        <el-table-column prop="createTime" label="出库时间" min-width="170" />
      </el-table>
      <el-empty v-else description="暂无出库记录" :image-size="60" />
    </el-dialog>

    <!-- 出库对话框 -->
    <el-dialog v-model="stockOutDialogVisible" title="库存出库" width="600px" destroy-on-close>
      <el-form ref="stockOutFormRef" :model="stockOutForm" :rules="stockOutRules" label-width="100px">
        <el-form-item label="物料名称">
          <span>{{ stockOutForm.itemName }}</span>
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="仓库">
              <span>{{ stockOutForm.warehouseName }}</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="当前库存">
              <span>{{ stockOutForm.currentQty }}</span>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="出库数量" prop="quantity">
              <el-input-number v-model="stockOutForm.quantity" :min="1" :max="stockOutForm.currentQty" style="width: 100%" />
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
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { inventoryApi, itemTypeOptions, itemTypeLabel, usageTypeOptions, usageTypeLabel } from '@/api/inventory'

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref([])

const filterForm = reactive({
  itemName: '',
  warehouseName: '',
  itemType: null
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

// ========== 详情 ==========
const detailDialogVisible = ref(false)
const detailData = ref({})
const stockOutRecords = ref([])
const minStockSaving = ref(false)

async function handleDetail(row) {
  try {
    const [detailRes, stockOutRes] = await Promise.all([
      inventoryApi.detail(row.id),
      inventoryApi.listStockOutOrders(row.id)
    ])
    detailData.value = detailRes.data || {}
    stockOutRecords.value = stockOutRes.data || []
    detailDialogVisible.value = true
  } catch (e) {
    console.error(e)
  }
}

async function handleSaveMinStockQty() {
  if (detailData.value.minStockQty == null) {
    ElMessage.warning('请输入预警阈值')
    return
  }
  minStockSaving.value = true
  try {
    await inventoryApi.updateMinStockQty(detailData.value.id, detailData.value.minStockQty)
    ElMessage.success('预警阈值保存成功')
    fetchList()
  } catch (e) {
    console.error(e)
  } finally {
    minStockSaving.value = false
  }
}

// ========== 出库 ==========
const stockOutDialogVisible = ref(false)
const stockOutFormRef = ref(null)
const stockOutForm = reactive({
  inventoryId: null,
  itemName: '',
  warehouseName: '',
  currentQty: 0,
  quantity: 1,
  usageType: null,
  deptName: '',
  receiver: '',
  receiverPhone: '',
  reviewer: '',
  remark: ''
})

const stockOutRules = {
  quantity: [{ required: true, message: '请输入出库数量', trigger: 'change' }],
  usageType: [{ required: true, message: '请选择领用用途', trigger: 'change' }],
  receiver: [{ required: true, message: '请输入领用人', trigger: 'blur' }]
}

function handleStockOut(row) {
  Object.assign(stockOutForm, {
    inventoryId: row.id,
    itemName: row.itemName,
    warehouseName: row.warehouseName,
    currentQty: row.totalQty,
    quantity: 1,
    usageType: null,
    deptName: '',
    receiver: '',
    receiverPhone: '',
    reviewer: '',
    remark: ''
  })
  stockOutDialogVisible.value = true
}

async function handleStockOutSubmit() {
  try {
    await stockOutFormRef.value.validate()
  } catch {
    return
  }

  if (stockOutForm.quantity > stockOutForm.currentQty) {
    ElMessage.warning(`出库数量不能超过当前库存(${stockOutForm.currentQty})`)
    return
  }

  submitLoading.value = true
  try {
    await inventoryApi.stockOut({
      inventoryId: stockOutForm.inventoryId,
      quantity: stockOutForm.quantity,
      usageType: stockOutForm.usageType,
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

// ========== 列表查询 ==========
async function fetchList() {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      size: pagination.size,
      itemName: filterForm.itemName || undefined,
      warehouseName: filterForm.warehouseName || undefined,
      itemType: filterForm.itemType !== null && filterForm.itemType !== undefined ? filterForm.itemType : undefined
    }
    const res = await inventoryApi.page(params)
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
    itemName: '',
    warehouseName: '',
    itemType: null
  })
  handleSearch()
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.inventory-container {
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

.low-stock {
  color: #f56c6c;
  font-weight: bold;
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
