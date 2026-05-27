<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>数据备份</span>
          <div class="header-actions">
            <el-button type="primary" :loading="backupLoading" @click="handleManualBackup">
              <el-icon style="margin-right:4px"><Upload /></el-icon>立即备份
            </el-button>
            <el-button :icon="Refresh" circle @click="fetchData" title="刷新" />
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
              <div class="stat-label">总备份数</div>
            </div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="stat-card stat-success">
            <div class="stat-icon"><el-icon><CircleCheck /></el-icon></div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.successCount }}</div>
              <div class="stat-label">成功备份</div>
            </div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="stat-card stat-auto">
            <div class="stat-icon"><el-icon><Timer /></el-icon></div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.autoCount }}</div>
              <div class="stat-label">自动备份</div>
            </div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="stat-card stat-manual">
            <div class="stat-icon"><el-icon><EditPen /></el-icon></div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.manualCount }}</div>
              <div class="stat-label">手动备份</div>
            </div>
          </div>
        </el-col>
      </el-row>

      <!-- 备份列表 -->
      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        style="width: 100%; margin-top: 16px"
      >
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="backupName" label="备份文件名" min-width="250" show-overflow-tooltip />
        <el-table-column prop="backupType" label="备份类型" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="row.backupType === 'AUTO' ? 'success' : 'primary'" size="small">
              {{ row.backupType === 'AUTO' ? '自动备份' : '手动备份' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="backupSize" label="文件大小" width="120" align="center">
          <template #default="{ row }">
            {{ formatFileSize(row.backupSize) }}
          </template>
        </el-table-column>
        <el-table-column prop="createBy" label="创建人" width="120" align="center" />
        <el-table-column prop="createTime" label="备份时间" width="180" align="center" />
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'SUCCESS'"
              type="success"
              size="small"
              @click="handleDownload(row)"
            >
              <el-icon style="margin-right:4px"><Download /></el-icon>下载
            </el-button>
            <el-button
              type="danger"
              size="small"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchData"
          @current-change="fetchData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Upload, Download, Document, CircleCheck, Timer, EditPen } from '@element-plus/icons-vue'
import { backupApi } from '@/api/backup'

const loading = ref(false)
const backupLoading = ref(false)
const tableData = ref([])
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const stats = reactive({
  total: 0,
  successCount: 0,
  autoCount: 0,
  manualCount: 0
})

onMounted(() => {
  fetchData()
})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await backupApi.page({
      page: pagination.page,
      size: pagination.size
    })
    tableData.value = res.data.records
    pagination.total = res.data.total

    calculateStats()
  } catch (error) {
    console.error('获取备份列表失败:', error)
    ElMessage.error('获取备份列表失败')
  } finally {
    loading.value = false
  }
}

const calculateStats = () => {
  stats.total = pagination.total
  stats.successCount = tableData.value.filter(item => item.status === 'SUCCESS').length
  stats.autoCount = tableData.value.filter(item => item.backupType === 'AUTO').length
  stats.manualCount = tableData.value.filter(item => item.backupType === 'MANUAL').length
}

const handleManualBackup = async () => {
  try {
    await ElMessageBox.confirm('确定要立即执行数据库备份吗？', '确认备份', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    backupLoading.value = true
    await backupApi.manualBackup()
    ElMessage.success('数据库备份成功！')
    fetchData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('备份失败:', error)
      ElMessage.error(error.response?.data?.message || '备份失败')
    }
  } finally {
    backupLoading.value = false
  }
}

const handleDownload = async (row) => {
  try {
    const response = await backupApi.download(row.id)

    const blob = new Blob([response.data], { type: 'application/octet-stream' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = row.backupName
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)

    ElMessage.success('文件下载成功')
  } catch (error) {
    console.error('下载失败:', error)
    ElMessage.error(error.message || '下载失败')
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除备份 "${row.backupName}" 吗？删除后无法恢复！`,
      '警告',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await backupApi.delete(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

const formatFileSize = (bytes) => {
  if (!bytes || bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const getStatusType = (status) => {
  const map = {
    SUCCESS: 'success',
    FAILED: 'danger',
    PROCESSING: 'warning'
  }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = {
    SUCCESS: '成功',
    FAILED: '失败',
    PROCESSING: '处理中'
  }
  return map[status] || status
}
</script>

<style scoped>
.page-container {
  padding: 20px;
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

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 8px;
  padding: 16px 20px;
  color: white;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.stat-success {
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
}

.stat-auto {
  background: linear-gradient(135deg, #ee0979 0%, #ff6a00 100%);
}

.stat-manual {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.stat-icon {
  font-size: 32px;
  opacity: 0.9;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  opacity: 0.9;
  margin-top: 4px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}
</style>
