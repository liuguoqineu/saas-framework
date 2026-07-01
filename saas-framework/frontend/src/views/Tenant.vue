<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>公司管理</span>
          <div class="header-actions">
            <el-button v-permission="'tenant:add'" type="primary" @click="openCreateDialog">创建公司</el-button>
          </div>
        </div>
      </template>

      <!-- 搜索栏 -->
      <el-form inline>
        <el-form-item label="公司名称">
          <el-input v-model="query.name" placeholder="请输入公司名称" clearable @change="fetchData" />
        </el-form-item>
        <el-form-item label="公司编码">
          <el-input v-model="query.code" placeholder="请输入公司编码" clearable @change="fetchData" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 表格 -->
      <el-table :data="tableData.records" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="公司名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="code" label="公司编码" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="adminUserId" label="管理员ID" width="100" />
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button
              v-permission="'tenant:edit'"
              size="small"
              :type="row.status === 1 ? 'warning' : 'success'"
              @click="toggleStatus(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="tableData.total"
          :page-sizes="[10, 20, 50]"
          :layout="paginationLayout"
          :small="isSmallScreen"
          @size-change="fetchData"
          @current-change="fetchData"
        />
      </div>
    </el-card>

    <!-- 创建租户弹窗 -->
    <el-dialog v-model="dialogVisible" title="创建公司" width="550px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="公司名称" prop="name">
          <el-input v-model="form.name" placeholder="如：XX科技有限公司" />
        </el-form-item>
        <el-form-item label="公司编码" prop="code">
          <el-input v-model="form.code" placeholder="如：xxtech" />
        </el-form-item>
        <el-form-item label="管理员用户名">
          <el-input v-model="form.adminUsername" placeholder="留空则自动生成：编码+admin" />
        </el-form-item>
        <el-form-item label="管理员密码">
          <el-input v-model="form.adminPassword" placeholder="留空则自动生成6位随机密码" />
        </el-form-item>
      </el-form>

      <!-- 创建结果显示 -->
      <el-alert v-if="createResult" type="success" :closable="false" style="margin-top:12px">
        <p>公司创建成功！</p>
        <p>管理员账号：<strong>{{ createResult.adminUsername }}</strong></p>
        <p>管理员密码：<strong>{{ createResult.adminPassword }}</strong></p>
        <p style="color:#E6A23C">请妥善保管以上信息</p>
      </el-alert>

      <template #footer>
        <el-button @click="dialogVisible = false">关闭</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreate">
          {{ createResult ? '继续创建' : '确认创建' }}

        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { tenantApi } from '@/api/tenant'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const createResult = ref(null)
const formRef = ref()

const screenWidth = ref(window.innerWidth)
const isSmallScreen = computed(() => screenWidth.value < 768)
const paginationLayout = computed(() => {
  if (screenWidth.value < 480) {
    return 'prev, pager, next'
  } else if (screenWidth.value < 768) {
    return 'total, prev, pager, next'
  } else {
    return 'total, sizes, prev, pager, next, jumper'
  }
})

function updateScreenWidth() {
  screenWidth.value = window.innerWidth
}

onMounted(() => {
  window.addEventListener('resize', updateScreenWidth)
  fetchData()
})

onUnmounted(() => {
  window.removeEventListener('resize', updateScreenWidth)
})

const query = reactive({ page: 1, size: 10, name: '', code: '' })
const tableData = reactive({ records: [], total: 0 })

const form = reactive({
  name: '',
  code: '',
  adminUsername: '',
  adminPassword: ''
})

const rules = {
  name: [{ required: true, message: '请输入公司名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入公司编码', trigger: 'blur' }]
}

async function fetchData() {
  loading.value = true
  try {
    const res = await tenantApi.page(query)
    Object.assign(tableData, res.data)
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.name = ''
  query.code = ''
  query.page = 1
  fetchData()
}

function openCreateDialog() {
  createResult.value = null
  resetForm()
  dialogVisible.value = true
}

function resetForm() {
  form.name = ''
  form.code = ''
  form.adminUsername = ''
  form.adminPassword = ''
  formRef.value?.resetFields()
}

async function handleCreate() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const res = await tenantApi.create(form)
    createResult.value = res.data
    ElMessage.success('公司创建成功')
    fetchData()
  } finally {
    submitting.value = false
  }
}

async function toggleStatus(row) {
  const newStatus = row.status === 1 ? 0 : 1
  const action = newStatus === 0 ? '禁用' : '启用'

  await ElMessageBox.confirm(`确认${action}公司「${row.name}」？`, '提示', {
    type: 'warning'
  })

  try {
    await tenantApi.updateStatus(row.id, newStatus)
    ElMessage.success(`公司已${action}`)
    fetchData()
  } catch { /* 取消 */ }
}
</script>

<style scoped>
.page-container {
  width: 100%;
  padding: 16px;
  box-sizing: border-box;
}
.card-header { display: flex; justify-content: space-between; align-items: center; }
.header-actions { display: flex; gap: 8px; }
.page-container :deep(.el-card) {
  width: 100%;
}
.page-container :deep(.el-table) {
  width: 100%;
}
.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 10px;
}
.pagination-wrapper :deep(.el-pagination) {
  flex-wrap: wrap;
  justify-content: flex-end;
}

@media screen and (max-width: 768px) {
  .pagination-wrapper {
    justify-content: center;
  }
  .pagination-wrapper :deep(.el-pagination) {
    justify-content: center;
  }
}
@media screen and (max-width: 480px) {
  .pagination-wrapper {
    justify-content: center;
  }
  .pagination-wrapper :deep(.el-pagination) {
    justify-content: center;
  }
  .pagination-wrapper :deep(.el-pager li) {
    min-width: 28px;
    height: 28px;
    line-height: 28px;
  }
}
</style>
