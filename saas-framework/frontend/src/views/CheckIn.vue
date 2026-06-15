<template>
  <div class="check-in-container">
    <!-- 打卡卡片 -->
    <el-card class="check-in-card">
      <div class="check-in-header">
        <h2>员工打卡</h2>
        <p class="current-time">{{ currentTime }}</p>
      </div>

      <div v-if="todayCheckIn" class="already-checked">
        <el-result icon="success" title="今日已打卡" :sub-title="`打卡时间: ${todayCheckIn.checkInTime}`">
          <template #extra>
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="打卡地址">{{ todayCheckIn.address }}</el-descriptions-item>
              <el-descriptions-item label="备注">{{ todayCheckIn.remark || '-' }}</el-descriptions-item>
            </el-descriptions>
            <div v-if="todayCheckIn.photoPath" class="photo-preview">
              <el-image :src="getPhotoUrl(todayCheckIn.id)" :preview-src-list="[getPhotoUrl(todayCheckIn.id)]" fit="cover" style="width: 200px; height: 150px; margin-top: 10px;" />
            </div>
          </template>
        </el-result>
      </div>

      <el-form v-else ref="formRef" :model="formData" :rules="formRules" label-width="100px" class="check-in-form">
        <el-form-item label="打卡地址" prop="address">
          <el-input v-model="formData.address" placeholder="请输入打卡地址" />
        </el-form-item>

        <el-form-item label="打卡照片">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            :file-list="fileList"
            list-type="picture-card"
            accept="image/*"
            :limit="1"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>

        <el-form-item label="备注">
          <el-input v-model="formData.remark" type="textarea" :rows="2" placeholder="请输入备注（可选）" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" size="large" @click="handleCheckIn" :loading="submitLoading">
            立即打卡
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

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
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { checkInApi } from '@/api/checkIn'

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref([])
const todayCheckIn = ref(null)
const currentTime = ref('')
const formRef = ref(null)
const uploadRef = ref(null)
const fileList = ref([])
const photoFile = ref(null)

let timeInterval = null

const formData = reactive({
  address: '',
  remark: ''
})

const formRules = {
  address: [{ required: true, message: '请输入打卡地址', trigger: 'blur' }]
}

const filterForm = reactive({
  userName: '',
  timeRange: null
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

// 更新当前时间
function updateCurrentTime() {
  const now = new Date()
  currentTime.value = now.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

// 获取今日打卡状态
async function fetchTodayStatus() {
  try {
    const res = await checkInApi.todayStatus()
    todayCheckIn.value = res.data || null
  } catch (e) {
    console.error(e)
  }
}

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

// 处理文件选择
function handleFileChange(file) {
  const maxSize = 20 * 1024 * 1024
  if (file.size > maxSize) {
    ElMessage.error('文件大小不能超过20MB')
    fileList.value = []
    return false
  }
  photoFile.value = file.raw
}

// 处理文件移除
function handleFileRemove() {
  photoFile.value = null
}

// 打卡
async function handleCheckIn() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitLoading.value = true
  try {
    const fd = new FormData()
    fd.append('address', formData.address)
    if (formData.remark) {
      fd.append('remark', formData.remark)
    }
    if (photoFile.value) {
      fd.append('photo', photoFile.value)
    }

    await checkInApi.checkIn(fd)
    ElMessage.success('打卡成功')

    // 重置表单
    formData.address = ''
    formData.remark = ''
    fileList.value = []
    photoFile.value = null

    // 刷新数据
    fetchTodayStatus()
    fetchList()
  } catch (e) {
    console.error(e)
  } finally {
    submitLoading.value = false
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
  updateCurrentTime()
  timeInterval = setInterval(updateCurrentTime, 1000)
  fetchTodayStatus()
  fetchList()
})

onUnmounted(() => {
  if (timeInterval) {
    clearInterval(timeInterval)
  }
})
</script>

<style scoped>
.check-in-container {
  padding: 0;
}

.check-in-card {
  margin-bottom: 16px;
}

.check-in-header {
  text-align: center;
  margin-bottom: 20px;
}

.check-in-header h2 {
  margin: 0 0 10px 0;
  color: #303133;
}

.current-time {
  font-size: 18px;
  color: #409eff;
  margin: 0;
}

.check-in-form {
  max-width: 500px;
  margin: 0 auto;
}

.already-checked {
  text-align: center;
}

.photo-preview {
  display: flex;
  justify-content: center;
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
