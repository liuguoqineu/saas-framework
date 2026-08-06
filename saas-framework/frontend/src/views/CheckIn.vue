<template>
  <div class="check-in-container">
    <!-- 打卡记录列表 -->
    <el-card class="record-card">
      <div class="table-header">
        <span class="table-title">打卡记录</span>
        <el-form :inline="true" :model="filterForm" class="filter-form">
          <el-form-item label="用户名">
            <el-input v-model="filterForm.userName" placeholder="请输入用户名" clearable style="width: 150px" />
          </el-form-item>
          <el-form-item label="打卡时间">
            <el-date-picker
              v-model="filterForm.timeRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
              style="width: 240px"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe border style="width: 100%">
        <el-table-column prop="userName" label="用户名" min-width="100" />
        <el-table-column prop="checkInTime" label="打卡时间" min-width="160" />
        <el-table-column prop="address" label="打卡地址" min-width="200" show-overflow-tooltip />
        <el-table-column label="打卡照片" min-width="100">
          <template #default="{ row }">
            <el-image
              v-if="row.photoPath"
              :src="getPhotoUrl(row.id)"
              :preview-src-list="[getPhotoUrl(row.id)]"
              fit="cover"
              style="width: 50px; height: 50px;"
            />
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'checkin:delete'" size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :page-sizes="[10, 20, 50, 100]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSearch"
        @current-change="fetchList"
        style="margin-top: 16px; justify-content: flex-end"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { checkInApi } from '@/api/checkIn'

const loading = ref(false)
const tableData = ref([])

const filterForm = reactive({
  userName: '',
  timeRange: null
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

// 获取打卡记录列表
async function fetchList() {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      size: pagination.size,
      userName: filterForm.userName || undefined,
      checkInTimeStart: filterForm.timeRange?.[0] || undefined,
      checkInTimeEnd: filterForm.timeRange?.[1] || undefined
    }
    const res = await checkInApi.page(params)
    tableData.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

// 查询
function handleSearch() {
  pagination.page = 1
  fetchList()
}

// 重置
function handleReset() {
  filterForm.userName = ''
  filterForm.timeRange = null
  handleSearch()
}

// 删除打卡记录
async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定要删除该打卡记录吗？`,
      '删除确认',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    await checkInApi.delete(row.id)
    ElMessage.success('打卡记录已删除')
    fetchList()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

// 获取照片URL
function getPhotoUrl(id) {
  if (!id) return ''
  const token = localStorage.getItem('token')
  return `/api/check-in/${id}/photo?token=${token}`
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.check-in-container {
  padding: 0;
}

.record-card {
  margin-bottom: 16px;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 16px;
}

.table-title {
  font-size: 16px;
  font-weight: 600;
}

.filter-form {
  display: flex;
  flex-wrap: wrap;
  gap: 0;
}

@media (max-width: 768px) {
  .table-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .filter-form :deep(.el-form-item) {
    width: 100%;
  }

  .filter-form :deep(.el-form-item .el-input),
  .filter-form :deep(.el-form-item .el-date-editor) {
    width: 100% !important;
  }
}
</style>
