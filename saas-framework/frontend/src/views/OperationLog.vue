<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>操作日志</span>
          <div class="header-actions">
            <el-button :icon="Refresh" circle @click="fetchData" title="刷新" />
            <el-button v-permission="'log:export'" type="success" @click="handleExport">
              <el-icon style="margin-right:4px"><Download /></el-icon>导出日志
            </el-button>
          </div>
        </div>
      </template>

      <!-- 统计卡片 -->
      <el-row :gutter="16" class="stats-row">
        <el-col :xs="12" :sm="6">
          <div class="stat-card stat-total">
            <div class="stat-icon"><el-icon><Document /></el-icon></div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.total }}</div>
              <div class="stat-label">总记录数</div>
            </div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="stat-card stat-today">
            <div class="stat-icon"><el-icon><Calendar /></el-icon></div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.todayCount }}</div>
              <div class="stat-label">今日操作</div>
            </div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="stat-card stat-create">
            <div class="stat-icon"><el-icon><CirclePlus /></el-icon></div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.createCount }}</div>
              <div class="stat-label">新增操作</div>
            </div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="stat-card stat-delete">
            <div class="stat-icon"><el-icon><Delete /></el-icon></div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.deleteCount }}</div>
              <div class="stat-label">删除操作</div>
            </div>
          </div>
        </el-col>
      </el-row>

      <!-- 搜索栏 -->
      <el-form inline class="filter-form">
        <el-form-item label="操作人">
          <el-input v-model="query.username" placeholder="用户名/真实姓名" clearable style="width:160px"
                    prefix-icon="User" @keyup.enter="handleSearch" @clear="handleSearch" />
        </el-form-item>
        <el-form-item label="操作类型">
          <el-select v-model="query.operation" placeholder="全部" clearable style="width:130px" @change="handleSearch">
            <el-option v-for="item in operationOptions" :key="item.value" :label="item.label" :value="item.value">
              <span style="display:flex;align-items:center;gap:6px">
                <el-tag :type="item.tagType" size="small" effect="light">{{ item.label }}</el-tag>
                {{ item.label }}
              </span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="操作模块">
          <el-select v-model="query.module" placeholder="全部" clearable style="width:120px" @change="handleSearch">
            <el-option v-for="item in moduleOptions" :key="item.value" :label="item.label" :value="item.value">
              <el-icon style="margin-right:4px;color:#909399"><component :is="item.icon" /></el-icon>
              {{ item.label }}
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="操作时间">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            :shortcuts="dateShortcuts"
            style="width:260px"
            @change="handleDateChange"
          />
        </el-form-item>
        <el-form-item label="IP地址">
          <el-input v-model="query.ip" placeholder="IP筛选" clearable style="width:150px"
                    @keyup.enter="handleSearch" @clear="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon style="margin-right:4px"><Search /></el-icon>查询
          </el-button>
          <el-button @click="resetQuery">
            <el-icon style="margin-right:4px"><RefreshLeft /></el-icon>重置
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 表格 -->
      <el-table :data="tableData.records" v-loading="loading" stripe border size="default"
                :header-cell-style="{ background:'#f5f7fa', color:'#303133', fontWeight:600 }"
                empty-text="暂无操作日志数据"
                @row-click="showDetail"
                class="log-table">
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column label="操作人" min-width="140">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="28" class="user-avatar">
                {{ (row.realName || row.username || '?').charAt(0) }}
              </el-avatar>
              <div class="user-info">
                <div class="user-name">{{ row.realName || '-' }}</div>
                <div class="user-account">@{{ row.username || '-' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="operation" label="操作类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="operationTagType(row.operation)" size="small" effect="dark" round>
              {{ operationLabel(row.operation) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="module" label="操作模块" width="100" align="center">
          <template #default="{ row }">
            <span class="module-tag" :class="'module-' + row.module">{{ row.module || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="操作描述" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span :class="{ 'desc-fail': isFailOperation(row) }">{{ row.description || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="requestUrl" label="请求接口" width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <code class="url-code">{{ row.requestUrl || '-' }}</code>
          </template>
        </el-table-column>
        <el-table-column prop="ip" label="IP地址" width="140" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.ip" class="ip-cell">
              <el-icon style="vertical-align:middle;margin-right:2px;color:#909399;font-size:13px"><Monitor /></el-icon>
              {{ row.ip }}
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="操作时间" width="170" sortable>
          <template #default="{ row }">
            <div class="time-cell">
              <el-icon style="color:#909399;vertical-align:middle;margin-right:2px"><Clock /></el-icon>
              {{ row.createTime || '-' }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right" align="center">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click.stop="showDetail(row)">
              详情
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
          :page-sizes="[10, 20, 50, 100]"
          :layout="paginationLayout"
          :small="isSmallScreen"
          background
          @size-change="handlePageChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog title="操作详情" v-model="detailVisible" width="720px" destroy-on-close>
      <div v-if="detailData" class="detail-content">
        <!-- 基本信息 -->
        <el-descriptions title="" :column="2" border class="detail-desc">
          <el-descriptions-item label="操作人">
            <div class="user-cell-inline">
              <el-avatar :size="24" style="background-color:#409eff">
                {{ (detailData.realName || detailData.username || '?').charAt(0) }}
              </el-avatar>
              <span>{{ detailData.realName || '-' }}（{{ detailData.username }}）</span>
            </div>
          </el-descriptions-item>
          <el-descriptions-item label="操作类型">
            <el-tag :type="operationTagType(detailData.operation)" effect="dark" size="small">
              {{ operationLabel(detailData.operation) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="操作模块">
            <span class="module-tag" :class="'module-' + detailData.module">{{ detailData.module || '-' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="操作状态">
            <el-tag :type="isFailOperation(detailData) ? 'danger' : 'success'" size="small" effect="light">
              {{ isFailOperation(detailData) ? '操作失败' : '操作成功' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="操作描述" :span="2">
            <span :class="{ 'desc-fail': isFailOperation(detailData) }">{{ detailData.description || '-' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="请求方法" :span="2">
            <code class="method-code">{{ detailData.method || '-' }}</code>
          </el-descriptions-item>
          <el-descriptions-item label="请求URL" :span="2">
            <code class="url-code-full">{{ detailData.requestUrl || '-' }}</code>
          </el-descriptions-item>
          <el-descriptions-item label="IP地址">{{ detailData.ip || '-' }}</el-descriptions-item>
          <el-descriptions-item label="操作时间">{{ detailData.createTime || '-' }}</el-descriptions-item>
        </el-descriptions>

        <!-- 请求参数 -->
        <div class="params-section">
          <div class="params-header">
            <el-icon><Tickets /></el-icon>
            <span>请求参数</span>
            <el-button size="small" text type="primary" @click="copyParams">
              <el-icon style="margin-right:2px"><CopyDocument /></el-icon>复制
            </el-button>
          </div>
          <pre class="params-pre" ref="paramsPreRef">{{ formatParams(detailData.requestParams) }}</pre>
        </div>
      </div>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="primary" plain @click="copyDetailInfo">复制详情信息</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { operationLogApi } from '@/api/operationLog'
import { ElMessage } from 'element-plus'
import {
  Refresh, Download, Search, RefreshLeft,
  Document, Calendar, CirclePlus, Delete,
  User, Monitor, Clock, Tickets, CopyDocument
} from '@element-plus/icons-vue'

const loading = ref(false)
const detailVisible = ref(false)
const detailData = ref(null)
const dateRange = ref(null)
const paramsPreRef = ref(null)

const screenWidth = ref(window.innerWidth)
const isSmallScreen = computed(() => screenWidth.value < 768)
const paginationLayout = computed(() => {
  if (screenWidth.value < 480) return 'prev, pager, next'
  if (screenWidth.value < 768) return 'total, prev, pager, next'
  return 'total, sizes, prev, pager, next, jumper'
})

function updateScreenWidth() { screenWidth.value = window.innerWidth }

onMounted(() => {
  window.addEventListener('resize', updateScreenWidth)
  fetchData()
})

onUnmounted(() => {
  window.removeEventListener('resize', updateScreenWidth)
})

const query = reactive({
  page: 1,
  size: 10,
  username: '',
  operation: '',
  module: '',
  startTime: '',
  endTime: '',
  ip: ''
})

const tableData = reactive({ records: [], total: 0 })

const stats = reactive({
  total: 0,
  todayCount: 0,
  createCount: 0,
  deleteCount: 0
})

const operationOptions = [
  { value: 'CREATE', label: '新增', tagType: 'success' },
  { value: 'UPDATE', label: '修改', tagType: 'warning' },
  { value: 'DELETE', label: '删除', tagType: 'danger' },
  { value: 'QUERY', label: '查询', tagType: 'info' },
  { value: 'EXPORT', label: '导出', tagType: 'info' },
  { value: 'IMPORT', label: '导入', tagType: 'info' },
  { value: 'LOGIN', label: '登录', tagType: 'info' },
  { value: 'OTHER', label: '其他', tagType: 'info' }
]

const moduleOptions = [
  { value: '角色', label: '角色管理', icon: 'UserFilled' },
  { value: '员工', label: '员工管理', icon: 'Avatar' },
  { value: '客户', label: '客户管理', icon: 'OfficeBuilding' },
  { value: '合同', label: '合同管理', icon: 'Document' },
  { value: '报修', label: '报修管理', icon: 'Tools' },
  { value: '跟进', label: '跟进管理', icon: 'ChatDotRound' },
  { value: '租户', label: '租户管理', icon: 'House' }
]

const dateShortcuts = [
  { text: '今天', value: () => { const end = new Date(); const start = new Date(); return [start, end] } },
  { text: '昨天', value: () => { const end = new Date(); const start = new Date(); start.setTime(start.getTime() - 3600 * 1000 * 24); end.setTime(end.getTime() - 3600 * 1000 * 24); return [start, end] } },
  { text: '最近7天', value: () => { const end = new Date(); const start = new Date(); start.setTime(start.getTime() - 3600 * 1000 * 24 * 7); return [start, end] } },
  { text: '最近30天', value: () => { const end = new Date(); const start = new Date(); start.setTime(start.getTime() - 3600 * 1000 * 24 * 30); return [start, end] } },
  { text: '本月', value: () => { const end = new Date(); const start = new Date(); start.setDate(1); return [start, end] } },
  { text: '上月', value: () => { const end = new Date(); const start = new Date(); start.setMonth(start.getMonth() - 1, 1); end.setDate(0); return [start, end] } }
]

function operationLabel(op) {
  const map = { CREATE: '新增', UPDATE: '修改', DELETE: '删除', QUERY: '查询', EXPORT: '导出', IMPORT: '导入', LOGIN: '登录', OTHER: '其他' }
  return map[op] || op
}

function operationTagType(op) {
  const map = { CREATE: 'success', UPDATE: 'warning', DELETE: 'danger', QUERY: 'info', EXPORT: 'info', IMPORT: 'info', LOGIN: 'info', OTHER: 'info' }
  return map[op] || 'info'
}

function isFailOperation(row) {
  return row.description && row.description.includes('（失败）')
}

function handleDateChange(val) {
  if (val && val.length === 2) {
    query.startTime = val[0] + ' 00:00:00'
    query.endTime = val[1] + ' 23:59:59'
  } else {
    query.startTime = ''
    query.endTime = ''
  }
  handleSearch()
}

function handleSearch() {
  query.page = 1
  fetchData()
}

function resetQuery() {
  query.username = ''
  query.operation = ''
  query.module = ''
  query.ip = ''
  query.startTime = ''
  query.endTime = ''
  dateRange.value = null
  query.page = 1
  fetchData()
}

function handlePageChange() {
  fetchData()
}

async function fetchData() {
  loading.value = true
  try {
    const params = buildQueryParams()
    const res = await operationLogApi.page(params)
    Object.assign(tableData, res.data)
    computeStats(tableData.records)
  } catch (e) {
    console.error('获取操作日志失败:', e)
    ElMessage.error('获取操作日志失败')
  } finally {
    loading.value = false
  }
}

function buildQueryParams() {
  const params = { ...query }
  Object.keys(params).forEach(k => {
    if (params[k] === '' || params[k] === null || params[k] === undefined) delete params[k]
  })
  return params
}

function computeStats(records) {
  stats.total = tableData.total || 0
  const now = new Date().toDateString()
  stats.todayCount = records.filter(r => r.createTime && new Date(r.createTime).toDateString() === now).length
  stats.createCount = records.filter(r => r.operation === 'CREATE').length
  stats.deleteCount = records.filter(r => r.operation === 'DELETE').length
}

function showDetail(row) {
  detailData.value = row
  detailVisible.value = true
}

function formatParams(str) {
  if (!str) return '(无请求参数)'
  try {
    const parsed = JSON.parse(str)
    return JSON.stringify(parsed, null, 2)
  } catch {
    return str
  }
}

async function copyParams() {
  try {
    const text = formatParams(detailData.value?.requestParams)
    await navigator.clipboard.writeText(text)
    ElMessage.success('参数已复制到剪贴板')
  } catch {
    ElMessage.warning('复制失败，请手动选择复制')
  }
}

async function copyDetailInfo() {
  if (!detailData.value) return
  const d = detailData.value
  const info = `【操作日志详情】\n` +
    `操作人：${d.realName || '-'}（${d.username}）\n` +
    `操作类型：${operationLabel(d.operation)}\n` +
    `操作模块：${d.module || '-'}\n` +
    `操作描述：${d.description || '-'}\n` +
    `请求URL：${d.requestUrl || '-'}\n` +
    `IP地址：${d.ip || '-'}\n` +
    `操作时间：${d.createTime || '-'}`
  try {
    await navigator.clipboard.writeText(info)
    ElMessage.success('详情已复制到剪贴板')
  } catch {
    ElMessage.warning('复制失败')
  }
}

async function handleExport() {
  try {
    const params = buildQueryParams()
    const queryString = new URLSearchParams(params).toString()
    const url = `/api/operation-log/export?${queryString}`
    const token = localStorage.getItem('token')

    const res = await fetch(url, {
      headers: { 'Authorization': `Bearer ${token}` }
    })

    if (!res.ok) throw new Error('导出请求失败')

    const blob = await res.blob()
    const blobUrl = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = blobUrl
    const timestamp = new Date().toISOString().replace(/[:.]/g, '').slice(0, -5)
    link.download = `操作日志_${timestamp}.csv`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(blobUrl)
    ElMessage.success(`导出成功，共 ${tableData.total} 条记录`)
  } catch (e) {
    console.error('导出失败:', e)
    ElMessage.error('导出失败，请稍后重试')
  }
}
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
.stat-today .stat-icon { background: linear-gradient(135deg, #67C23A, #529b2e); }
.stat-create .stat-icon { background: linear-gradient(135deg, #E6A23C, #c98e2a); }
.stat-delete .stat-icon { background: linear-gradient(135deg, #F56C6C, #cf4444); }

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

/* 搜索表单 */
.filter-form {
  display: flex;
  flex-wrap: wrap;
  gap: 0;
  margin-bottom: 4px;
}

/* 表格 */
.log-table {
  width: 100%;
}

.log-table :deep(.el-table__body tr) {
  cursor: pointer;
}

.log-table :deep(.el-table__body tr:hover > td) {
  background-color: #ecf5ff !important;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-avatar {
  background: linear-gradient(135deg, #409EFF, #66b1ff);
  font-size: 12px;
  color: #fff;
  flex-shrink: 0;
}

.user-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.user-name {
  font-weight: 500;
  font-size: 13px;
  color: #303133;
}

.user-account {
  font-size: 11px;
  color: #909399;
}

.user-cell-inline {
  display: flex;
  align-items: center;
  gap: 8px;
}

.module-tag {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.module-角色 { background: #ecf5ff; color: #409eff; }
.module-员工 { background: #fdf6ec; color: #e6a23c; }
.module-客户 { background: #f0f9eb; color: #67c23a; }
.module-合同 { background: #ecf5ff; color: #409eff; }
.module-报修 { background: #fef0f0; color: #f56c6c; }
.module-跟进 { background: #f4f4f5; color: #909399; }
.module-租户 { background: #ecf5ff; color: #66b1ff; }

.desc-fail {
  color: #F56C6C;
  font-weight: 500;
}

.url-code {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 11px;
  color: #606266;
  background: #f5f7fa;
  padding: 2px 6px;
  border-radius: 3px;
  word-break: break-all;
}

.url-code-full {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 12px;
  color: #303133;
  background: #f5f7fa;
  padding: 6px 10px;
  border-radius: 4px;
  display: block;
  word-break: break-all;
}

.method-code {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 12px;
  color: #E6A23C;
  background: #fdf6ec;
  padding: 4px 8px;
  border-radius: 4px;
  display: block;
}

.time-cell {
  font-size: 13px;
  color: #606266;
}

.ip-cell {
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 12px;
  color: #606266;
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

/* 详情弹窗 */
.detail-content {
  max-height: 70vh;
  overflow-y: auto;
}

.detail-desc {
  margin-bottom: 16px;
}

.params-section {
  margin-top: 16px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
}

.params-header {
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

.params-header .el-button {
  margin-left: auto;
}

.params-pre {
  max-height: 300px;
  overflow-y: auto;
  white-space: pre-wrap;
  word-break: break-all;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 12px;
  line-height: 1.6;
  color: #303133;
  margin: 0;
  padding: 14px 16px;
  background: #fafbfc;
}

.params-pre::-webkit-scrollbar {
  width: 6px;
}

.params-pre::-webkit-scrollbar-thumb {
  background: #c0c4cc;
  border-radius: 3px;
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

  .filter-form :deep(.el-form-item) {
    width: 100%;
    margin-bottom: 8px !important;
    margin-right: 0 !important;
  }

  .filter-form :deep(.el-form-item .el-input),
  .filter-form :deep(.el-form-item .el-select),
  .filter-form :deep(.el-form-item .el-date-editor) {
    width: 100% !important;
  }

  .pagination-wrapper {
    justify-content: center;
  }

  .pagination-wrapper :deep(.el-pagination) {
    justify-content: center;
  }
}

@media screen and (max-width: 480px) {
  .stat-card .stat-icon {
    width: 38px;
    height: 38px;
    font-size: 18px;
  }

  .stat-info .stat-value {
    font-size: 18px;
  }

  .pagination-wrapper :deep(.el-pager li) {
    min-width: 28px;
    height: 28px;
    line-height: 28px;
  }
}
</style>
