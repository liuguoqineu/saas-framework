<template>
  <div class="purchase-container">
    <!-- 筛选区 -->
    <el-card class="filter-card">
      <el-form :inline="true" :model="filterForm" class="filter-form">
        <el-form-item label="采购单号">
          <el-input v-model="filterForm.orderNo" placeholder="请输入采购单号" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="供应商">
          <el-input v-model="filterForm.supplierName" placeholder="请输入供应商名称" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item label="采购日期">
          <el-date-picker v-model="filterForm.purchaseDateRange" type="daterange" range-separator="至"
            start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD"
            style="width: 240px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filterForm.status" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="item in purchaseStatusOptions" :key="item.value" :label="item.label"
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
        <span class="table-title">采购列表</span>
        <div class="table-actions">
          <el-button type="primary" @click="handleAdd">新增采购</el-button>
        </div>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe border style="width: 100%">
        <el-table-column prop="orderNo" label="采购单号" min-width="150" />
        <el-table-column prop="supplierName" label="供应商名称" min-width="130" />
        <el-table-column prop="purchaseDate" label="采购日期" min-width="110" />
        <el-table-column prop="totalAmount" label="采购总金额" min-width="110">
          <template #default="{ row }">
            {{ row.totalAmount != null ? '¥' + Number(row.totalAmount).toFixed(2) : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" min-width="90">
          <template #default="{ row }">
            <el-tag :type="purchaseStatusTagType[row.status]" size="small">
              {{ purchaseStatusLabel[row.status] || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="purchaser" label="采购负责人" min-width="100" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleDetail(row)">查看详情</el-button>
            <el-button size="small" type="primary" @click="handleEdit(row)"
              :disabled="row.status === 1">编辑</el-button>
            <el-button size="small" type="success" @click="handleStockIn(row)"
              :disabled="row.status === 1">入库</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination v-model:current-page="pagination.page" v-model:page-size="pagination.size"
        :page-sizes="[10, 20, 50]" :total="pagination.total"
        :layout="paginationLayout" @size-change="handleSearch" @current-change="fetchList"
        style="margin-top: 16px; justify-content: flex-end" />
    </el-card>

    <!-- 新增/编辑采购单对话框 -->
    <el-dialog v-model="formDialogVisible" :title="formDialogTitle" width="960px" destroy-on-close top="5vh">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="120px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="供应商名称" prop="supplierName">
              <el-input v-model="formData.supplierName" placeholder="请输入供应商名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="采购日期" prop="purchaseDate">
              <el-date-picker v-model="formData.purchaseDate" type="date" placeholder="请选择采购日期"
                value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="供应商联系人">
              <el-input v-model="formData.supplierContact" placeholder="请输入联系人" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话">
              <el-input v-model="formData.supplierPhone" placeholder="请输入联系电话" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="供应商地址">
              <el-input v-model="formData.supplierAddress" placeholder="请输入供应商地址" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="统一信用代码">
              <el-input v-model="formData.supplierUnifiedCode" placeholder="请输入统一社会信用代码" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="采购负责人" prop="purchaser">
              <el-input v-model="formData.purchaser" placeholder="请输入采购负责人" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="负责人电话">
              <el-input v-model="formData.purchaserPhone" placeholder="请输入负责人联系方式" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="采购总金额">
              <span style="font-size: 16px; font-weight: bold; color: #f56c6c">
                ¥{{ (formData.items.reduce((sum, item) => sum + (item.totalPrice || 0), 0)).toFixed(2) }}
              </span>
              <span style="color: #999; margin-left: 8px">（根据明细自动汇总）</span>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="formData.remark" type="textarea" :rows="2" placeholder="请输入备注" />
        </el-form-item>

        <el-divider content-position="left">采购明细</el-divider>
        <div style="margin-bottom: 12px">
          <el-button type="primary" @click="addItem">+ 添加明细</el-button>
        </div>
        <div v-for="(item, index) in formData.items" :key="index" class="purchase-item-card">
          <div class="purchase-item-header">
            <span class="purchase-item-title">明细 #{{ index + 1 }}</span>
            <el-button type="danger" size="small" link @click="removeItem(index)">删除</el-button>
          </div>
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="类型">
                <el-select v-model="item.itemType" placeholder="请选择类型" style="width: 100%">
                  <el-option v-for="t in itemTypeOptions" :key="t.value" :label="t.label" :value="t.value" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="名称">
                <el-input v-model="item.itemName" placeholder="请输入名称" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="品牌">
                <el-input v-model="item.brand" placeholder="请输入品牌" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="型号">
                <el-input v-model="item.model" placeholder="请输入型号" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="规格">
                <el-input v-model="item.spec" placeholder="请输入规格" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="单位">
                <el-input v-model="item.unit" placeholder="如：台/个/套" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="数量">
                <el-input-number v-model="item.quantity" :min="1" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="单价">
                <el-input-number v-model="item.unitPrice" :precision="2" :min="0" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="总价">
                <el-input-number v-model="item.totalPrice" :precision="2" :min="0" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="出厂编号">
                <el-input v-model="item.factoryNo" placeholder="请输入出厂编号" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="合格证">
                <div class="file-upload-group">
                  <el-button v-if="item.certFile || item.certFileRaw" size="small" type="success"
                    @click="item.certFile ? downloadFile(item.certFile) : null">下载</el-button>
                  <el-upload :auto-upload="false" :on-change="(f) => handleItemFileChange(f, index, 'certFile')"
                    :show-file-list="false" accept=".jpg,.jpeg,.png,.pdf">
                    <el-button size="small" type="primary">{{ (item.certFile || item.certFileRaw) ? '重新上传' : '上传合格证' }}</el-button>
                  </el-upload>
                </div>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="检验报告">
                <div class="file-upload-group">
                  <el-button v-if="item.inspectFile || item.inspectFileRaw" size="small" type="success"
                    @click="item.inspectFile ? downloadFile(item.inspectFile) : null">下载</el-button>
                  <el-upload :auto-upload="false" :on-change="(f) => handleItemFileChange(f, index, 'inspectFile')"
                    :show-file-list="false" accept=".jpg,.jpeg,.png,.pdf">
                    <el-button size="small" type="primary">{{ (item.inspectFile || item.inspectFileRaw) ? '重新上传' : '上传检验报告' }}</el-button>
                  </el-upload>
                </div>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="发货单">
                <div class="file-upload-group">
                  <el-button v-if="item.deliveryFile || item.deliveryFileRaw" size="small" type="success"
                    @click="item.deliveryFile ? downloadFile(item.deliveryFile) : null">下载</el-button>
                  <el-upload :auto-upload="false" :on-change="(f) => handleItemFileChange(f, index, 'deliveryFile')"
                    :show-file-list="false" accept=".jpg,.jpeg,.png,.pdf">
                    <el-button size="small" type="primary">{{ (item.deliveryFile || item.deliveryFileRaw) ? '重新上传' : '上传发货单' }}</el-button>
                  </el-upload>
                </div>
              </el-form-item>
            </el-col>
          </el-row>
        </div>
        <el-empty v-if="formData.items.length === 0" description="暂无明细，请点击上方按钮添加" :image-size="60" />
      </el-form>
      <template #footer>
        <el-button @click="formDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 查看详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="采购单详情" width="900px" destroy-on-close>
      <el-descriptions :column="3" border>
        <el-descriptions-item label="采购单号">{{ detailData.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="采购日期">{{ detailData.purchaseDate }}</el-descriptions-item>
        <el-descriptions-item label="采购总金额">
          {{ detailData.totalAmount != null ? '¥' + Number(detailData.totalAmount).toFixed(2) : '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="供应商名称">{{ detailData.supplierName }}</el-descriptions-item>
        <el-descriptions-item label="供应商联系人">{{ detailData.supplierContact || '-' }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ detailData.supplierPhone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="供应商地址" :span="2">{{ detailData.supplierAddress || '-' }}</el-descriptions-item>
        <el-descriptions-item label="统一信用代码">{{ detailData.supplierUnifiedCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="采购负责人">{{ detailData.purchaser }}</el-descriptions-item>
        <el-descriptions-item label="负责人电话">{{ detailData.purchaserPhone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="purchaseStatusTagType[detailData.status]" size="small">
            {{ purchaseStatusLabel[detailData.status] || '未知' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="3">{{ detailData.remark || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detailData.createTime }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ detailData.updateTime }}</el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">采购明细</el-divider>
      <el-table :data="detailData.items" stripe border size="small">
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="itemType" label="类型" width="70">
          <template #default="{ row }">{{ itemTypeLabel[row.itemType] || '-' }}</template>
        </el-table-column>
        <el-table-column prop="itemName" label="名称" min-width="100" />
        <el-table-column prop="brand" label="品牌" width="80" />
        <el-table-column prop="model" label="型号" width="100" />
        <el-table-column prop="spec" label="规格" width="80" />
        <el-table-column prop="quantity" label="采购数量" width="80" />
        <el-table-column prop="stockedQty" label="已入库" width="70" />
        <el-table-column prop="unit" label="单位" width="60" />
        <el-table-column prop="unitPrice" label="单价" width="90">
          <template #default="{ row }">{{ row.unitPrice != null ? '¥' + Number(row.unitPrice).toFixed(2) : '-' }}</template>
        </el-table-column>
        <el-table-column prop="totalPrice" label="总价" width="90">
          <template #default="{ row }">{{ row.totalPrice != null ? '¥' + Number(row.totalPrice).toFixed(2) : '-' }}</template>
        </el-table-column>
        <el-table-column prop="factoryNo" label="出厂编号" min-width="100" />
        <el-table-column label="附件" min-width="160">
          <template #default="{ row }">
            <el-button v-if="row.certFile" size="small" link type="primary" @click="downloadFile(row.certFile)">合格证</el-button>
            <el-button v-if="row.inspectFile" size="small" link type="primary" @click="downloadFile(row.inspectFile)">检验报告</el-button>
            <el-button v-if="row.deliveryFile" size="small" link type="primary" @click="downloadFile(row.deliveryFile)">发货单</el-button>
            <span v-if="!row.certFile && !row.inspectFile && !row.deliveryFile">-</span>
          </template>
        </el-table-column>
      </el-table>

      <template v-if="stockInRecords.length > 0">
        <el-divider content-position="left">入库记录</el-divider>
        <el-table :data="stockInRecords" stripe border size="small">
          <el-table-column prop="orderNo" label="入库单号" min-width="160" />
          <el-table-column prop="itemName" label="物料名称" min-width="100" />
          <el-table-column prop="itemType" label="类型" width="70">
            <template #default="{ row }">{{ itemTypeLabel[row.itemType] || '-' }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="入库数量" width="80" />
          <el-table-column prop="warehouseName" label="仓库" min-width="100" />
          <el-table-column prop="handler" label="经办人" width="80" />
          <el-table-column prop="createTime" label="入库时间" min-width="160" />
        </el-table>
      </template>
    </el-dialog>

    <!-- 入库对话框 -->
    <el-dialog v-model="stockInDialogVisible" title="采购入库" width="800px" destroy-on-close>
      <el-form ref="stockInFormRef" :model="stockInForm" :rules="stockInRules" label-width="110px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="采购单号">
              <span>{{ stockInForm.purchaseOrderNo }}</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="供应商">
              <span>{{ stockInForm.supplierName }}</span>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="入库仓库" prop="warehouseName">
              <el-input v-model="stockInForm.warehouseName" placeholder="请输入仓库名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="入库日期">
              <el-date-picker v-model="stockInForm.stockInDate" type="date" placeholder="入库日期"
                value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="经办人">
              <el-input v-model="stockInForm.handler" placeholder="请输入经办人" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="stockInForm.remark" type="textarea" :rows="2" placeholder="入库备注（可选）" />
        </el-form-item>

        <el-divider content-position="left">入库明细</el-divider>
        <el-table :data="stockInForm.items" border size="small" style="width: 100%">
          <el-table-column prop="itemType" label="类型" width="70">
            <template #default="{ row }">{{ itemTypeLabel[row.itemType] || '-' }}</template>
          </el-table-column>
          <el-table-column prop="itemName" label="名称" min-width="100" />
          <el-table-column prop="brand" label="品牌" width="80" />
          <el-table-column prop="model" label="型号" width="100" />
          <el-table-column prop="spec" label="规格" width="80" />
          <el-table-column prop="quantity" label="采购数量" width="80" />
          <el-table-column prop="stockedQty" label="已入库" width="70" />
          <el-table-column label="本次入库" width="100">
            <template #default="{ row }">
              <el-input-number v-model="row.stockInQty" :min="0" :max="row.remainQty"
                size="small" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="备注" min-width="100">
            <template #default="{ row }">
              <el-input v-model="row.remark" placeholder="备注" size="small" />
            </template>
          </el-table-column>
        </el-table>
      </el-form>
      <template #footer>
        <el-button @click="stockInDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleStockInSubmit" :loading="submitLoading">确认入库</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { purchaseApi, purchaseStatusOptions, purchaseStatusTagType, purchaseStatusLabel, itemTypeLabel, itemTypeOptions } from '@/api/purchase'

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref([])

const filterForm = reactive({
  orderNo: '',
  supplierName: '',
  purchaseDateRange: null,
  status: null
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

// ========== 新增/编辑 ==========
const formDialogVisible = ref(false)
const formDialogTitle = ref('新增采购单')
const isEdit = ref(false)
const editId = ref(null)
const formRef = ref(null)

const formData = reactive({
  supplierName: '',
  purchaseDate: '',
  supplierContact: '',
  supplierPhone: '',
  supplierAddress: '',
  supplierUnifiedCode: '',
  totalAmount: null,
  purchaser: '',
  purchaserPhone: '',
  remark: '',
  items: []
})

const formRules = {
  supplierName: [{ required: true, message: '请输入供应商名称', trigger: 'blur' }],
  purchaseDate: [{ required: true, message: '请选择采购日期', trigger: 'change' }],
  purchaser: [{ required: true, message: '请输入采购负责人', trigger: 'blur' }]
}

function addItem() {
  formData.items.push({
    id: null,
    itemType: 1,
    itemName: '',
    brand: '',
    model: '',
    spec: '',
    quantity: 1,
    unit: '台',
    unitPrice: 0,
    totalPrice: 0,
    factoryNo: '',
    certFile: '',
    certFileName: '',
    certFileRaw: null,
    inspectFile: '',
    inspectFileName: '',
    inspectFileRaw: null,
    deliveryFile: '',
    deliveryFileName: '',
    deliveryFileRaw: null
  })
}

function removeItem(index) {
  formData.items.splice(index, 1)
}

function handleItemFileChange(file, index, field) {
  const nameMap = { certFile: 'certFileName', inspectFile: 'inspectFileName', deliveryFile: 'deliveryFileName' }
  const rawMap = { certFile: 'certFileRaw', inspectFile: 'inspectFileRaw', deliveryFile: 'deliveryFileRaw' }
  formData.items[index][nameMap[field]] = file.name
  formData.items[index][rawMap[field]] = file.raw
  formData.items[index][field] = '' // 清空旧路径，提交时上传
}

function handleAdd() {
  isEdit.value = false
  editId.value = null
  formDialogTitle.value = '新增采购单'
  Object.assign(formData, {
    supplierName: '',
    purchaseDate: new Date().toISOString().slice(0, 10),
    supplierContact: '',
    supplierPhone: '',
    supplierAddress: '',
    supplierUnifiedCode: '',
    totalAmount: null,
    purchaser: '',
    purchaserPhone: '',
    remark: '',
    items: []
  })
  addItem()
  formDialogVisible.value = true
}

async function handleEdit(row) {
  isEdit.value = true
  editId.value = row.id
  formDialogTitle.value = '编辑采购单'
  try {
    const res = await purchaseApi.detail(row.id)
    const data = res.data
    Object.assign(formData, {
      supplierName: data.supplierName || '',
      purchaseDate: data.purchaseDate || '',
      supplierContact: data.supplierContact || '',
      supplierPhone: data.supplierPhone || '',
      supplierAddress: data.supplierAddress || '',
      supplierUnifiedCode: data.supplierUnifiedCode || '',
      totalAmount: data.totalAmount,
      purchaser: data.purchaser || '',
      purchaserPhone: data.purchaserPhone || '',
      remark: data.remark || '',
      items: (data.items || []).map(item => ({
        id: item.id,
        itemType: item.itemType,
        itemName: item.itemName,
        brand: item.brand || '',
        model: item.model || '',
        spec: item.spec || '',
        quantity: item.quantity,
        unit: item.unit || '',
        unitPrice: item.unitPrice,
        totalPrice: item.totalPrice,
        factoryNo: item.factoryNo || '',
        certFile: item.certFile || '',
        certFileName: item.certFile ? '已上传' : '',
        certFileRaw: null,
        inspectFile: item.inspectFile || '',
        inspectFileName: item.inspectFile ? '已上传' : '',
        inspectFileRaw: null,
        deliveryFile: item.deliveryFile || '',
        deliveryFileName: item.deliveryFile ? '已上传' : '',
        deliveryFileRaw: null
      }))
    })
    formDialogVisible.value = true
  } catch (e) {
    console.error(e)
  }
}

async function handleSubmit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  if (!formData.items || formData.items.length === 0) {
    ElMessage.warning('请至少添加一条采购明细')
    return
  }

  for (let i = 0; i < formData.items.length; i++) {
    const item = formData.items[i]
    if (!item.itemName) {
      ElMessage.warning(`第${i + 1}条明细名称不能为空`)
      return
    }
    if (!item.quantity || item.quantity <= 0) {
      ElMessage.warning(`第${i + 1}条明细数量必须大于0`)
      return
    }
  }

  submitLoading.value = true
  try {
    // 先上传所有待上传的文件
    for (const item of formData.items) {
      if (item.certFileRaw) {
        const fd = new FormData()
        fd.append('file', item.certFileRaw)
        fd.append('fileType', '合格证')
        const res = await purchaseApi.uploadFile(fd)
        item.certFile = res.data
        item.certFileRaw = null
      }
      if (item.inspectFileRaw) {
        const fd = new FormData()
        fd.append('file', item.inspectFileRaw)
        fd.append('fileType', '检验报告')
        const res = await purchaseApi.uploadFile(fd)
        item.inspectFile = res.data
        item.inspectFileRaw = null
      }
      if (item.deliveryFileRaw) {
        const fd = new FormData()
        fd.append('file', item.deliveryFileRaw)
        fd.append('fileType', '发货单')
        const res = await purchaseApi.uploadFile(fd)
        item.deliveryFile = res.data
        item.deliveryFileRaw = null
      }
    }

    const payload = {
      supplierName: formData.supplierName,
      purchaseDate: formData.purchaseDate,
      supplierContact: formData.supplierContact,
      supplierPhone: formData.supplierPhone,
      supplierAddress: formData.supplierAddress,
      supplierUnifiedCode: formData.supplierUnifiedCode,
      totalAmount: formData.items.reduce((sum, item) => sum + (item.totalPrice || 0), 0),
      purchaser: formData.purchaser,
      purchaserPhone: formData.purchaserPhone,
      remark: formData.remark,
      items: formData.items.map(item => ({
        id: item.id,
        itemType: item.itemType,
        itemName: item.itemName,
        brand: item.brand,
        model: item.model,
        spec: item.spec,
        quantity: item.quantity,
        unit: item.unit,
        unitPrice: item.unitPrice,
        totalPrice: item.totalPrice,
        factoryNo: item.factoryNo,
        certFile: item.certFile,
        inspectFile: item.inspectFile,
        deliveryFile: item.deliveryFile
      }))
    }

    if (isEdit.value) {
      await purchaseApi.update(editId.value, payload)
      ElMessage.success('采购单修改成功')
    } else {
      await purchaseApi.create(payload)
      ElMessage.success('采购单创建成功')
    }
    formDialogVisible.value = false
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
const stockInRecords = ref([])

function downloadFile(filePath) {
  if (!filePath) return
  const url = purchaseApi.getFileDownloadUrl(filePath)
  const token = localStorage.getItem('token')
  fetch(url, { headers: { Authorization: `Bearer ${token}` } })
    .then(res => res.blob())
    .then(blob => {
      const a = document.createElement('a')
      a.href = URL.createObjectURL(blob)
      a.download = filePath
      a.click()
      URL.revokeObjectURL(a.href)
    })
    .catch(() => ElMessage.error('文件下载失败'))
}

async function handleDetail(row) {
  try {
    const [detailRes, stockInRes] = await Promise.all([
      purchaseApi.detail(row.id),
      purchaseApi.listStockInOrders(row.id)
    ])
    detailData.value = detailRes.data || {}
    stockInRecords.value = stockInRes.data || []
    detailDialogVisible.value = true
  } catch (e) {
    console.error(e)
  }
}

// ========== 入库 ==========
const stockInDialogVisible = ref(false)
const stockInFormRef = ref(null)
const stockInForm = reactive({
  purchaseOrderId: null,
  purchaseOrderNo: '',
  supplierName: '',
  warehouseName: '',
  stockInDate: new Date().toISOString().slice(0, 10),
  handler: '',
  remark: '',
  items: []
})

const stockInRules = {
  warehouseName: [{ required: true, message: '请输入入库仓库', trigger: 'blur' }]
}

async function handleStockIn(row) {
  try {
    const res = await purchaseApi.detail(row.id)
    const data = res.data

    stockInForm.purchaseOrderId = data.id
    stockInForm.purchaseOrderNo = data.orderNo
    stockInForm.supplierName = data.supplierName
    stockInForm.warehouseName = ''
    stockInForm.stockInDate = new Date().toISOString().slice(0, 10)
    stockInForm.handler = ''
    stockInForm.remark = ''
    stockInForm.items = (data.items || [])
      .filter(item => (item.quantity - item.stockedQty) > 0)
      .map(item => ({
        purchaseItemId: item.id,
        itemType: item.itemType,
        itemName: item.itemName,
        brand: item.brand,
        model: item.model,
        spec: item.spec,
        quantity: item.quantity,
        stockedQty: item.stockedQty,
        remainQty: item.quantity - item.stockedQty,
        stockInQty: item.quantity - item.stockedQty,
        remark: ''
      }))

    if (stockInForm.items.length === 0) {
      ElMessage.warning('该采购单没有待入库的明细')
      return
    }

    stockInDialogVisible.value = true
  } catch (e) {
    console.error(e)
  }
}

async function handleStockInSubmit() {
  try {
    await stockInFormRef.value.validate()
  } catch {
    return
  }

  const validItems = stockInForm.items.filter(item => item.stockInQty > 0)
  if (validItems.length === 0) {
    ElMessage.warning('请至少填写一条入库数量大于0的明细')
    return
  }

  for (const item of validItems) {
    if (item.stockInQty > item.remainQty) {
      ElMessage.warning(`${item.itemName}的入库数量不能超过剩余可入库数量(${item.remainQty})`)
      return
    }
  }

  submitLoading.value = true
  try {
    await purchaseApi.stockIn({
      purchaseOrderId: stockInForm.purchaseOrderId,
      warehouseName: stockInForm.warehouseName,
      stockInDate: stockInForm.stockInDate,
      handler: stockInForm.handler,
      remark: stockInForm.remark,
      items: validItems.map(item => ({
        purchaseItemId: item.purchaseItemId,
        quantity: item.stockInQty,
        remark: item.remark
      }))
    })
    ElMessage.success('入库操作成功')
    stockInDialogVisible.value = false
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
      orderNo: filterForm.orderNo || undefined,
      supplierName: filterForm.supplierName || undefined,
      purchaseDateStart: filterForm.purchaseDateRange?.[0] || undefined,
      purchaseDateEnd: filterForm.purchaseDateRange?.[1] || undefined,
      status: filterForm.status !== null && filterForm.status !== undefined && filterForm.status !== '' ? filterForm.status : undefined
    }
    const res = await purchaseApi.page(params)
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
    supplierName: '',
    purchaseDateRange: null,
    status: null
  })
  handleSearch()
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.purchase-container {
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

.purchase-item-card {
  background: #fafafa;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  padding: 16px 20px 4px;
  margin-bottom: 12px;
}

.purchase-item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.purchase-item-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.file-upload-group {
  display: flex;
  gap: 8px;
  align-items: center;
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
