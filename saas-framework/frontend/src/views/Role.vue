<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>角色管理</span>
          <div class="header-actions">
            <el-button :icon="Refresh" circle @click="fetchData" title="刷新" />
            <el-button v-permission="'role:add'" type="primary" @click="openCreateDialog">
              <el-icon style="margin-right:4px"><CirclePlus /></el-icon>新增角色
            </el-button>
          </div>
        </div>
      </template>

      <!-- 统计卡片 -->
      <el-row :gutter="16" class="stats-row">
        <el-col :xs="12" :sm="6">
          <div class="stat-card stat-total">
            <div class="stat-icon"><el-icon><Key /></el-icon></div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.total }}</div>
              <div class="stat-label">角色总数</div>
            </div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="stat-card stat-platform">
            <div class="stat-icon"><el-icon><Monitor /></el-icon></div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.platformCount }}</div>
              <div class="stat-label">平台角色</div>
            </div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="stat-card stat-tenant">
            <div class="stat-icon"><el-icon><OfficeBuilding /></el-icon></div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.tenantCount }}</div>
              <div class="stat-label">租户角色</div>
            </div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="stat-card stat-user">
            <div class="stat-icon"><el-icon><User /></el-icon></div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalUsers }}</div>
              <div class="stat-label">关联用户</div>
            </div>
          </div>
        </el-col>
      </el-row>

      <!-- 表格 -->
      <el-table :data="tableData.records" v-loading="loading" stripe border size="default"
                :header-cell-style="{ background:'#f5f7fa', color:'#303133', fontWeight:600 }"
                empty-text="暂无角色数据"
                class="role-table"
                @row-click="showDetail">
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="name" label="角色名称" min-width="140">
          <template #default="{ row }">
            <div class="role-name-cell">
              <el-icon class="role-icon" :class="'role-icon-' + getRoleType(row.name)"><Medal /></el-icon>
              <span class="role-name-text">{{ row.name }}</span>
              <el-tag v-if="row.tenantId === 0" size="small" type="primary" effect="plain">平台</el-tag>
              <el-tag v-else size="small" type="info" effect="plain">租户</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="用户数" width="100" align="center">
          <template #default="{ row }">
            <el-badge :value="row._userCount || 0" :max="99" type="primary">
              <el-icon style="color:#909399"><User /></el-icon>
            </el-badge>
          </template>
        </el-table-column>
        <el-table-column label="权限数" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="getPermissionTagType(row._permissionCount)" effect="light" round>
              {{ row._permissionCount || 0 }} 个权限
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" sortable />
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <el-button v-permission="'role:edit'" size="small" type="primary" link @click.stop="openEditDialog(row)">
              编辑
            </el-button>
            <el-button v-permission="'role:add'" size="small" type="success" link @click.stop="openCopyDialog(row)">
              复制
            </el-button>
            <el-button v-permission="'role:delete'" size="small" type="danger" link @click.stop="handleDelete(row)">
              删除
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
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="fetchData"
          @current-change="fetchData"
        />
      </div>
    </el-card>

    <!-- 新增/编辑/复制 角色弹窗 -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="620px" destroy-on-close @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="form.name" :placeholder="dialogType === 'copy' ? '将基于「' + copySourceName + '」创建新角色' : '请输入角色名称，如：客户专员'"
                    maxlength="20" show-word-limit />
        </el-form-item>
        <el-form-item label="角色描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="可选，描述该角色的职责范围（仅用于展示）"
                    maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="权限分配">
          <div class="permission-tree-wrapper">
            <div class="tree-toolbar">
              <span class="tree-hint">勾选需要分配给该角色的菜单和按钮权限</span>
              <div class="tree-actions">
                <el-button size="small" text type="primary" @click="checkAllPermissions">全选</el-button>
                <el-button size="small" text @click="clearAllPermissions">清空</el-button>
                <el-button size="small" text type="warning" @click="expandAllTree">{{ treeExpanded ? '收起' : '展开' }}</el-button>
              </div>
            </div>
            <el-tree
              ref="treeRef"
              :data="permissionTree"
              :props="{ label: 'name', children: 'children' }"
              node-key="id"
              show-checkbox
              :default-checked-keys="checkedKeys"
              :default-expanded-keys="expandedKeys"
              :check-strictly="false"
              class="permission-tree"
            >
              <template #default="{ node, data }">
                <span class="tree-node">
                  <el-icon v-if="data.type === 'menu'" style="color:#409eff;margin-right:4px"><FolderOpened /></el-icon>
                  <el-icon v-else style="color:#67c23a;margin-right:4px"><Document /></el-icon>
                  <span>{{ data.name }}</span>
                  <el-tag v-if="data.type === 'button'" size="small" type="info" effect="plain" style="margin-left:4px">{{ data.code }}</el-tag>
                </span>
              </template>
            </el-tree>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          确认{{ dialogType === 'edit' ? '修改' : '创建' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 角色详情弹窗 -->
    <el-dialog title="角色详情" v-model="detailVisible" width="680px" destroy-on-close>
      <div v-if="detailData" class="detail-content">
        <el-descriptions title="" :column="2" border class="detail-desc">
          <el-descriptions-item label="角色名称">
            <span class="detail-role-name">{{ detailData.name }}</span>
            <el-tag :type="detailData.tenantId === 0 ? 'primary' : 'info'" size="small" effect="plain" style="margin-left:6px">
              {{ detailData.tenantId === 0 ? '平台角色' : '租户角色' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="关联用户">
            <el-tag type="primary" effect="plain">{{ detailData.userCount || 0 }} 人</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="权限数量" :span="2">
            <el-tag type="success" effect="plain">{{ (detailData.permissionIds || []).length }} 个权限</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ detailData.createTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ detailData.updateTime || '-' }}</el-descriptions-item>
        </el-descriptions>

        <!-- 已分配权限列表 -->
        <div class="section-box">
          <div class="section-title">
            <el-icon><Key /></el-icon>
            <span>已分配权限（{{ (detailData.permissions || []).length }} 个）</span>
          </div>
          <div v-if="detailData.permissions && detailData.permissions.length > 0" class="permission-list">
            <div v-for="perm in detailData.permissions" :key="perm.id" class="permission-item">
              <el-tag :type="perm.type === 'menu' ? 'primary' : 'success'" size="small" effect="light">
                {{ perm.name }}
              </el-tag>
              <code class="permission-code">{{ perm.code }}</code>
            </div>
          </div>
          <div v-else class="empty-tip">暂未分配任何权限</div>
        </div>
      </div>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button v-permission="'role:edit'" type="primary" plain @click="editFromDetail">编辑角色</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { roleApi } from '@/api/role'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Refresh, CirclePlus, User, Key, Medal, Monitor,
  OfficeBuilding, FolderOpened, Document
} from '@element-plus/icons-vue'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const dialogType = ref('create')
const editId = ref(null)
const copySourceId = ref(null)
const copySourceName = ref('')
const formRef = ref()
const treeRef = ref()
const detailData = ref(null)
const treeExpanded = ref(true)

const query = reactive({ page: 1, size: 10 })
const tableDataRecords = ref([])
const tableData = computed(() => ({ records: tableDataRecords.value, total: tableDataTotal.value }))
const tableDataTotal = ref(0)
const form = reactive({ name: '', description: '', permissionIds: [] })
const permissionTree = ref([])
const checkedKeys = ref([])
const expandedKeys = ref([])

const stats = reactive({
  total: 0,
  platformCount: 0,
  tenantCount: 0,
  totalUsers: 0
})

const dialogTitle = computed(() => {
  const map = { create: '新增角色', edit: '编辑角色', copy: '复制角色' }
  return map[dialogType.value] || '角色'
})

const rules = {
  name: [
    { required: true, message: '请输入角色名称', trigger: 'blur' },
    { min: 2, max: 20, message: '角色名称长度为 2-20 个字符', trigger: 'blur' }
  ]
}

function getRoleType(name) {
  if (!name) return 'default'
  if (name.includes('管理员')) return 'admin'
  if (name.includes('客户')) return 'customer'
  if (name.includes('运维') || name.includes('维修')) return 'repair'
  if (name.includes('拜访') || name.includes('访问')) return 'visit'
  if (name.includes('财务')) return 'finance'
  return 'default'
}

function getPermissionTagType(count) {
  if (!count || count === 0) return 'info'
  if (count <= 5) return 'success'
  if (count <= 15) return 'warning'
  return 'danger'
}

async function fetchData() {
  loading.value = true
  try {
    const res = await roleApi.page(query)
    tableDataTotal.value = res.data.total || 0
    const records = (res.data.records || []).map(r => ({
      ...r,
      _userCount: 0,
      _permissionCount: 0
    }))
    tableDataRecords.value = records
    computeStats(records)
    await preloadUserCounts(records)
    tableDataRecords.value = [...records]
  } finally {
    loading.value = false
  }
}

function computeStats(records) {
  stats.total = tableData.total || 0
  stats.platformCount = records.filter(r => r.tenantId === 0).length
  stats.tenantCount = records.filter(r => r.tenantId !== 0).length
}

async function preloadUserCounts(records) {
  console.log('📊 开始预加载角色详情，共', records.length, '个角色')
  for (let i = 0; i < records.length; i++) {
    try {
      const res = await roleApi.getById(records[i].id)
      console.log(`🔍 角色 ${records[i].id} (${records[i].name}) 详情:`, res.data)
      const permissionIds = res.data.permissionIds || []
      const userCount = res.data.userCount || 0
      console.log(`   ✅ permissionIds长度:`, permissionIds.length, ', userCount:', userCount)
      Object.assign(records[i], {
        _userCount: userCount,
        _permissionCount: permissionIds.length
      })
      console.log(`   📝 更新后 records[${i}]:`, records[i])
    } catch (e) {
      console.warn('❌ 获取角色详情失败:', records[i].id, e)
      Object.assign(records[i], {
        _userCount: 0,
        _permissionCount: 0
      })
    }
  }
  stats.totalUsers = records.reduce((sum, r) => sum + (r._userCount || 0), 0)
  console.log('🎯 预加载完成，最终records:', records)
  console.log('🎯 最终tableData.records:', tableData.records)
}

async function fetchPermissionTree() {
  const res = await roleApi.getPermissionTree()
  permissionTree.value = res.data || []
  expandedKeys.value = collectAllNodeIds(permissionTree.value)
  allLeafIds = collectLeafNodeIds(permissionTree.value)
}

function collectAllNodeIds(nodes) {
  const ids = []
  function traverse(list) {
    if (!list) return
    list.forEach(node => {
      ids.push(node.id)
      traverse(node.children)
    })
  }
  traverse(nodes)
  return ids
}

function collectLeafNodeIds(nodes) {
  const ids = []
  function traverse(list) {
    if (!list) return
    list.forEach(node => {
      if (!node.children || node.children.length === 0) {
        ids.push(node.id)
      }
      traverse(node.children)
    })
  }
  traverse(nodes)
  return ids
}

let allLeafIds = []

function openCreateDialog() {
  dialogType.value = 'create'
  editId.value = null
  copySourceId.value = null
  form.name = ''
  form.description = ''
  form.permissionIds = []
  checkedKeys.value = []
  dialogVisible.value = true
  nextTick(() => {
    treeRef.value?.setCheckedKeys([])
    treeRef.value?.setExpandedKeys(expandedKeys.value)
  })
}

async function openEditDialog(row) {
  dialogType.value = 'edit'
  editId.value = row.id
  copySourceId.value = null
  form.name = row.name
  form.description = ''

  try {
    const roleRes = await roleApi.getById(row.id)
    const allIds = roleRes.data.permissionIds || []
    checkedKeys.value = allIds.filter(id => allLeafIds.includes(id))
  } catch {
    checkedKeys.value = []
  }

  dialogVisible.value = true
  nextTick(() => {
    treeRef.value?.setCheckedKeys(checkedKeys.value, false)
    treeRef.value?.setExpandedKeys(expandedKeys.value)
  })
}

async function openCopyDialog(row) {
  dialogType.value = 'copy'
  editId.value = null
  copySourceId.value = row.id
  copySourceName.value = row.name
  form.name = ''
  form.description = ''

  try {
    const roleRes = await roleApi.getById(row.id)
    const allIds = roleRes.data.permissionIds || []
    checkedKeys.value = allIds.filter(id => allLeafIds.includes(id))
  } catch {
    checkedKeys.value = []
  }

  dialogVisible.value = true
  nextTick(() => {
    treeRef.value?.setCheckedKeys(checkedKeys.value, false)
    treeRef.value?.setExpandedKeys(expandedKeys.value)
  })
}

function resetForm() {
  form.name = ''
  form.description = ''
  form.permissionIds = []
  checkedKeys.value = []
  formRef.value?.resetFields()
}

function checkAllPermissions() {
  treeRef.value?.setCheckedKeys(collectAllNodeIds(permissionTree.value))
}

function clearAllPermissions() {
  treeRef.value?.setCheckedKeys([])
}

function expandAllTree() {
  treeExpanded.value = !treeExpanded.value
  if (treeExpanded.value) {
    treeRef.value?.setExpandedKeys(expandedKeys.value)
  } else {
    treeRef.value?.setExpandedKeys([])
  }
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  const checkedNodes = treeRef.value?.getCheckedNodes(false, true) || []
  const permissionIds = checkedNodes.filter(n => !n.children || n.children.length === 0).map(n => n.id)

  submitting.value = true
  try {
    const data = { name: form.name, permissionIds }
    if (dialogType.value === 'edit') {
      await roleApi.update(editId.value, data)
      ElMessage.success('角色修改成功')
    } else {
      await roleApi.create(data)
      ElMessage.success(dialogType.value === 'copy' ? '角色复制成功' : '角色创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row) {
  const userTip = row._userCount > 0 ? `（当前有 ${row._userCount} 个用户使用此角色）` : ''
  await ElMessageBox.confirm(
    `确认删除角色「${row.name}」？${userTip}`,
    '提示',
    { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
  )
  try {
    await roleApi.delete(row.id)
    ElMessage.success('角色已删除')
    fetchData()
  } catch { /* 取消或后端抛出有用户的异常 */ }
}

async function showDetail(row) {
  try {
    const res = await roleApi.getById(row.id)
    detailData.value = res.data
    detailVisible.value = true
  } catch {
    ElMessage.warning('获取角色详情失败')
  }
}

function editFromDetail() {
  if (!detailData.value) return
  detailVisible.value = false
  openEditDialog({ id: detailData.value.id, name: detailData.value.name })
}

onMounted(() => {
  fetchData()
  fetchPermissionTree()
})
</script>

<style scoped>
.page-container {
  width: 100%;
  padding: 16px;
  box-sizing: border-box;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

/* 统计卡片 */
.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 20px;
  border-radius: 10px;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
  cursor: default;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);
}

.stat-card .stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  flex-shrink: 0;
}

.stat-card .stat-icon .el-icon {
  font-size: 22px;
  color: #fff;
}

.stat-total .stat-icon { background: linear-gradient(135deg, #409EFF, #337ecc); }
.stat-platform .stat-icon { background: linear-gradient(135deg, #7c3aed, #5b21b6); }
.stat-tenant .stat-icon { background: linear-gradient(135deg, #F56C6C, #cf4444); }
.stat-user .stat-icon { background: linear-gradient(135deg, #67C23A, #529b2e); }

.stat-info .stat-value {
  font-size: 26px;
  font-weight: 700;
  line-height: 1.2;
  color: #303133;
}

.stat-info .stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 2px;
}

/* 表格 */
.role-table {
  width: 100%;
}

.role-table :deep(.el-table__body tr) {
  cursor: pointer;
}

.role-table :deep(.el-table__body tr:hover > td) {
  background-color: #ecf5ff !important;
}

.role-name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.role-name-text {
  font-weight: 500;
  font-size: 14px;
}

.role-icon {
  font-size: 18px;
  flex-shrink: 0;
}

.role-icon-admin { color: #e6a23c; }
.role-icon-customer { color: #409eff; }
.role-icon-repair { color: #f56c6c; }
.role-icon-visit { color: #67c23a; }
.role-icon-finance { color: #9b59b6; }
.role-icon-default { color: #909399; }

/* 分页 */
.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

/* 权限树 */
.permission-tree-wrapper {
  width: 100%;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
}

.tree-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 14px;
  background: #fafafa;
  border-bottom: 1px solid #ebeef5;
  flex-wrap: wrap;
  gap: 6px;
}

.tree-hint {
  font-size: 13px;
  color: #909399;
}

.tree-actions {
  display: flex;
  gap: 4px;
}

.permission-tree {
  max-height: 380px;
  overflow-y: auto;
  padding: 10px;
}

.permission-tree::-webkit-scrollbar {
  width: 6px;
}

.permission-tree::-webkit-scrollbar-thumb {
  background: #c0c4cc;
  border-radius: 3px;
}

.tree-node {
  display: flex;
  align-items: center;
  font-size: 13px;
}

/* 详情弹窗 */
.detail-content {
  max-height: 65vh;
  overflow-y: auto;
}

.detail-desc {
  margin-bottom: 16px;
}

.detail-role-name {
  font-weight: 600;
  font-size: 15px;
  color: #303133;
}

.section-box {
  margin-top: 16px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  background: #fafafa;
  border-bottom: 1px solid #ebeef5;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.permission-list {
  padding: 14px 16px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.permission-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.permission-code {
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 11px;
  color: #909399;
  background: #f5f7fa;
  padding: 1px 6px;
  border-radius: 3px;
}

.empty-tip {
  padding: 24px 16px;
  text-align: center;
  color: #909399;
  font-size: 13px;
}

/* 响应式适配 */
@media screen and (max-width: 768px) {
  .page-container { padding: 8px; }

  .stats-row .el-col {
    margin-bottom: 8px;
  }

  .stat-card {
    padding: 12px 14px;
  }

  .stat-info .stat-value {
    font-size: 20px;
  }

  .pagination-wrapper {
    justify-content: center;
  }
}
</style>
