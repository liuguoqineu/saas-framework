<template>
  <div class="stock-in-container">
    <!-- 筛选区 -->
    <el-card class="filter-card">
      <el-form :inline="true" :model="filterForm" class="filter-form">
        <el-form-item label="入库单号">
          <el-input v-model="filterForm.orderNo" placeholder="请输入入库单号" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="物料名称">
          <el-input v-model="filterForm.itemName" placeholder="请输入物料名称" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item label="仓库">
          <el-input v-model="filterForm.warehouseName" placeholder="请输入仓库名称" clearable style="width: 130px" />
        </el-form-item>
        <el-form-item label="入库日期">
          <el-date-picker v-model="filterForm.stockInDateRange" type="daterange" range-separator="至"
            start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD"
            style="width: 240px" />
        </el-form-item>
        <el-form-item label="验收状态">
          <el-select v-model="filterForm.checkStatus" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="item in checkStatusOptions" :key="item.value" :label="item.label"
              :value="item.value" />
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
        <span class="table-title">入库单列表</span>
        <div class="table-actions">
          <el-button type="primary" @click="handleIndependentStockIn">独立入库</el-button>
        </div>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe border style="width: 100%">
        <el-table-column prop="orderNo" label="入库单号" min-width="150" />
        <el-table-column prop="itemType" label="类型" width="80">
          <template #default="{ row }">
            <el-tag :type="row.itemType === 1 ? '' : 'info'" size="small">{{ itemTypeLabel[row.itemType] || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="itemName" label="物料名称" min-width="120" />
        <el-table-column prop="brand" label="品牌" width="100" />
        <el-table-column prop="model" label="型号" width="120" />
        <el-table-column prop="quantity" label="数量" width="80" align="center" />
        <el-table-column prop="warehouseName" label="仓库" min-width="110" />
        <el-table-column prop="handler" label="经办人" width="90" />
        <el-table-column prop="checkStatus" label="验收状态" width="110">
          <template #default="{ row }">
            <el-tag :type="checkStatusTagType[row.checkStatus]" size="small">
              {{ checkStatusLabel[row.checkStatus] || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="入库时间" min-width="160" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleDetail(row)">查看</el-button>
            <el-button size="small" type="success" @click="handleCheck(row)"
              :disabled="row.checkStatus !== 0">验收</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination v-model:current-page="pagination.page" v-model:page-size="pagination.size"
        :page-sizes="[10, 20, 50]" :total="pagination.total"
        :layout="paginationLayout" @size-change="handleSearch" @current-change="fetchList"
        style="margin-top: 16px; justify-content: flex-end" />
    </el-card>

    <!-- 独立入库对话框 -->
    <el-dialog v-model="independentDialogVisible" title="独立入库" width="700px" destroy-on-close>
      <el-form ref="independentFormRef" :model="independentForm" :rules="independentRules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="类型" prop="itemType">
              <el-select v-model="independentForm.itemType" placeholder="请选择类型" style="width: 100%">
                <el-option v-for="t in itemTypeOptions" :key="t.value" :label="t.label" :value="t.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="物料名称" prop="itemName">
              <el-input v-model="independentForm.itemName" placeholder="请输入物料名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="品牌">
              <el-input v-model="independentForm.brand" placeholder="请输入品牌" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="型号">
              <el-input v-model="independentForm.model" placeholder="请输入型号" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="规格">
              <el-input v-model="independentForm.spec" placeholder="请输入规格" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="数量" prop="quantity">
              <el-input-number v-model="independentForm.quantity" :min="1" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="单位">
              <el-input v-model="independentForm.unit" placeholder="如：台/个/套" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="仓库" prop="warehouseName">
              <el-input v-model="independentForm.warehouseName" placeholder="请输入仓库名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="库位">
              <el-input v-model="independentForm.location" placeholder="请输入库位" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="经办人">
              <el-input v-model="independentForm.handler" placeholder="请输入经办人" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="independentForm.remark" type="textarea" :rows="2" placeholder="入库备注（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="independentDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleIndependentSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 入库验收对话框 -->
    <el-dialog v-model="checkDialogVisible" title="入库验收" width="500px" destroy-on-close>
      <el-form ref="checkFormRef" :model="checkForm" :rules="checkRules" label-width="100px">
        <el-form-item label="入库单号">
          <span>{{ checkForm.orderNo }}</span>
        </el-form-item>
        <el-form-item label="物料名称">
          <span>{{ checkForm.itemName }}</span>
        </el-form-item>
        <el-form-item label="验收结果" prop="checkStatus">
          <el-select v-model="checkForm.checkStatus" placeholder="请选择验收结果" style="width: 100%">
            <el-option v-for="item in checkStatusOptions" :key="item.value" :label="item.label"
              :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="验收照片">
          <el-input v-model="checkForm.checkPhoto" placeholder="请输入验收照片URL" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="checkDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCheckSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 入库详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="入库单详情" width="900px" destroy-on-close>
      <el-descriptions :column="3" border>
        <el-descriptions-item label="入库单号">{{ detailData.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="类型">
          <el-tag :type="detailData.itemType === 1 ? '' : 'info'" size="small">{{ itemTypeLabel[detailData.itemType] || '-' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="物料名称">{{ detailData.itemName }}</el-descriptions-item>
        <el-descriptions-item label="品牌">{{ detailData.brand || '-' }}</el-descriptions-item>
        <el-descriptions-item label="型号">{{ detailData.model || '-' }}</el-descriptions-item>
        <el-descriptions-item label="规格">{{ detailData.spec || '-' }}</el-descriptions-item>
        <el-descriptions-item label="数量">{{ detailData.quantity }}</el-descriptions-item>
        <el-descriptions-item label="单位">{{ detailData.unit || '-' }}</el-descriptions-item>
        <el-descriptions-item label="仓库">{{ detailData.warehouseName }}</el-descriptions-item>
        <el-descriptions-item label="库位">{{ detailData.location || '-' }}</el-descriptions-item>
        <el-descriptions-item label="经办人">{{ detailData.handler || '-' }}</el-descriptions-item>
        <el-descriptions-item label="验收状态">
          <el-tag :type="checkStatusTagType[detailData.checkStatus]" size="small">
            {{ checkStatusLabel[detailData.checkStatus] || '未知' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="验收照片" :span="3">
          <span v-if="detailData.checkPhoto">{{ detailData.checkPhoto }}</span>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="来源单号">{{ detailData.purchaseOrderNo || '独立入库' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detailData.remark || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detailData.createTime }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ detailData.updateTime }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { stockInApi, checkStatusOptions, checkStatusLabel, checkStatusTagType, itemTypeOptions, itemTypeLabel } from '@/api/stockIn'

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref([])

const filterForm = reactive({
  orderNo: '',
  itemName: '',
  warehouseName: '',
  stockInDateRange: null,
  checkStatus: null
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

// ========== 独立入库 ==========
const independentDialogVisible = ref(false)
const independentFormRef = ref(null)
const independentForm = reactive({
  itemType: 1,
  itemName: '',
  brand: '',
  model: '',
  spec: '',
  quantity: 1,
  unit: '',
  warehouseName: '',
  location: '',
  handler: '',
  remark: ''
})

const independentRules = {
  itemType: [{ required: true, message: '请选择类型', trigger: 'change' }],
  itemName: [{ required: true, message: '请输入物料名称', trigger: 'blur' }],
  quantity: [{ required: true, message: '请输入数量', trigger: 'change' }],
  warehouseName: [{ required: true, message: '请输入仓库名称', trigger: 'blur' }]
}

function handleIndependentStockIn() {
  Object.assign(independentForm, {
    itemType: 1,
    itemName: '',
    brand: '',
    model: '',
    spec: '',
    quantity: 1,
    unit: '',
    warehouseName: '',
    location: '',
    handler: '',
    remark: ''
  })
  independentDialogVisible.value = true
}

async function handleIndependentSubmit() {
  try {
    await independentFormRef.value.validate()
  } catch {
    return
  }

  submitLoading.value = true
  try {
    await stockInApi.independentStockIn({
      itemType: independentForm.itemType,
      itemName: independentForm.itemName,
      brand: independentForm.brand,
      model: independentForm.model,
      spec: independentForm.spec,
      quantity: independentForm.quantity,
      unit: independentForm.unit,
      warehouseName: independentForm.warehouseName,
      location: independentForm.location,
      handler: independentForm.handler,
      remark: independentForm.remark
    })
    ElMessage.success('独立入库操作成功')
    independentDialogVisible.value = false
    fetchList()
  } catch (e) {
    console.error(e)
  } finally {
    submitLoading.value = false
  }
}

// ========== 入库验收 ==========
const checkDialogVisible = ref(false)
const checkFormRef = ref(null)
const checkForm = reactive({
  id: null,
  orderNo: '',
  itemName: '',
  checkStatus: null,
  checkPhoto: ''
})

const checkRules = {
  checkStatus: [{ required: true, message: '请选择验收结果', trigger: 'change' }]
}

function handleCheck(row) {
  Object.assign(checkForm, {
    id: row.id,
    orderNo: row.orderNo,
    itemName: row.itemName,
    checkStatus: null,
    checkPhoto: ''
  })
  checkDialogVisible.value = true
}

async function handleCheckSubmit() {
  try {
    await checkFormRef.value.validate()
  } catch {
    return
  }

  submitLoading.value = true
  try {
    await stockInApi.check(checkForm.id, {
      checkStatus: checkForm.checkStatus,
      checkPhoto: checkForm.checkPhoto
    })
    ElMessage.success('验收操作成功')
    checkDialogVisible.value = false
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
    const res = await stockInApi.detail(row.id)
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
      warehouseName: filterForm.warehouseName || undefined,
      stockInDateStart: filterForm.stockInDateRange?.[0] || undefined,
      stockInDateEnd: filterForm.stockInDateRange?.[1] || undefined,
      checkStatus: filterForm.checkStatus !== null && filterForm.checkStatus !== undefined && filterForm.checkStatus !== '' ? filterForm.checkStatus : undefined
    }
    const res = await stockInApi.page(params)
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
    warehouseName: '',
    stockInDateRange: null,
    checkStatus: null
  })
  handleSearch()
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.stock-in-container {
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
