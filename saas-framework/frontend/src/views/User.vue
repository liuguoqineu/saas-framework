<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>员工管理</span>
          <div class="header-actions">
            <el-button v-permission="'user:add'" type="primary" @click="openCreateDialog">新增员工</el-button>
          </div>
        </div>
      </template>

      <!-- 搜索栏 -->
      <el-form inline>
        <el-form-item label="姓名">
          <el-input v-model="query.realName" placeholder="请输入姓名" clearable @change="fetchData" />
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="query.username" placeholder="请输入用户名" clearable @change="fetchData" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 表格 -->
      <el-table :data="tableData.records" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="用户名" min-width="120" show-overflow-tooltip />
        <el-table-column prop="realName" label="真实姓名" min-width="100" show-overflow-tooltip />
        <el-table-column prop="roleId" label="角色" width="120">
          <template #default="{ row }">
            <el-tag>{{ getRoleName(row.roleId) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="postType" label="岗位类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getPostTypeTagType(row.postType)" size="small">{{ getPostTypeLabel(row.postType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-switch
              v-permission="'user:edit'"
              :model-value="row.status === 1"
              active-text="启用"
              inactive-text="禁用"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'user:edit'" size="small" type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-button v-permission="'user:edit'" size="small" type="warning" @click="handleResetPwd(row)">重置密码</el-button>
            <el-button v-permission="'user:delete'" size="small" type="danger" @click="handleDelete(row)">删除</el-button>
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog :title="isEdit ? '编辑员工' : '新增员工'" v-model="dialogVisible" width="550px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="isEdit" placeholder="登录用户名" />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="员工真实姓名" />
        </el-form-item>
        <el-form-item label="密码" v-if="!isEdit" prop="password">
          <el-input v-model="form.password" placeholder="留空则默认 123456" />
        </el-form-item>
        <el-form-item label="角色" prop="roleId">
          <el-select v-model="form.roleId" placeholder="请选择角色" style="width:100%">
            <el-option v-for="r in roleOptions" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="岗位类型" prop="postType">
          <el-select v-model="form.postType" placeholder="请选择岗位类型" style="width:100%" clearable>
            <el-option label="研发 (DEV)" value="DEV" />
            <el-option label="运维 (OPS)" value="OPS" />
            <el-option label="客服 (CS)" value="CS" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" v-if="isEdit">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          确认{{ isEdit ? '修改' : '创建' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { userApi } from '@/api/user'
import { roleApi } from '@/api/role'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
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
  fetchRoles()
})

onUnmounted(() => {
  window.removeEventListener('resize', updateScreenWidth)
})

const query = reactive({ page: 1, size: 10, realName: '', username: '' })
const tableData = reactive({ records: [], total: 0 })
const roleOptions = ref([])
const roleMap = ref({})

const form = reactive({
  username: '',
  realName: '',
  password: '',
  roleId: null,
  postType: '',
  status: 1
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  roleId: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

function getRoleName(roleId) {
  return roleMap.value[roleId] || `角色${roleId}`
}

function getPostTypeLabel(postType) {
  const map = { DEV: '研发', OPS: '运维', CS: '客服' }
  return map[postType] || postType || '未设置'
}

function getPostTypeTagType(postType) {
  const map = { DEV: '', OPS: 'success', CS: 'warning' }
  return map[postType] || 'info'
}

async function fetchData() {
  loading.value = true
  try {
    const res = await userApi.page(query)
    Object.assign(tableData, res.data)
  } finally {
    loading.value = false
  }
}

async function fetchRoles() {
  const res = await roleApi.page({ page: 1, size: 100 })
  roleOptions.value = res.data.records || []
  const map = {}
  roleOptions.value.forEach(r => { map[r.id] = r.name })
  roleMap.value = map
}

function resetQuery() {
  query.realName = ''
  query.username = ''
  query.page = 1
  fetchData()
}

function openCreateDialog() {
  isEdit.value = false
  editId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(row) {
  isEdit.value = true
  editId.value = row.id
  form.username = row.username
  form.realName = row.realName
  form.roleId = row.roleId
  form.postType = row.postType || ''
  form.password = ''
  form.status = row.status
  dialogVisible.value = true
}

function resetForm() {
  form.username = ''
  form.realName = ''
  form.password = ''
  form.roleId = null
  form.postType = ''
  form.status = 1
  formRef.value?.resetFields()
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (isEdit.value) {
      await userApi.update(editId.value, {
        realName: form.realName,
        roleId: form.roleId,
        postType: form.postType || null,
        status: form.status
      })
      ElMessage.success('员工修改成功')
    } else {
      await userApi.create({
        username: form.username,
        realName: form.realName,
        password: form.password || undefined,
        roleId: form.roleId,
        postType: form.postType || null
      })
      ElMessage.success('员工创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } finally {
    submitting.value = false
  }
}

async function handleStatusChange(row) {
  const newStatus = row.status === 1 ? 0 : 1
  const statusText = newStatus === 1 ? '启用' : '禁用'
  await ElMessageBox.confirm(`确认${statusText}员工「${row.realName}」？`, '提示', { type: 'warning' })
  try {
    await userApi.update(row.id, {
      realName: row.realName,
      roleId: row.roleId,
      status: newStatus
    })
    ElMessage.success(`已${statusText}员工`)
    fetchData()
  } catch { /* 取消 */ }
}

async function handleResetPwd(row) {
  await ElMessageBox.confirm(`确认重置「${row.realName}」的密码为 123456？`, '提示', { type: 'warning' })
  try {
    await userApi.resetPassword(row.id)
    ElMessage.success('密码已重置为 123456')
  } catch { /* 取消 */ }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确认删除员工「${row.realName}」？`, '提示', { type: 'warning' })
  try {
    await userApi.delete(row.id)
    ElMessage.success('员工已删除')
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
