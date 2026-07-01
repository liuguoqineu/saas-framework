<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>员工管理</span>
          <div class="header-actions">
            <!-- 超级管理员：直接新增员工 -->
            <el-button v-if="userStore.isSuperAdmin && hasPermission('user:add')" type="primary" @click="openCreateDialog">新增员工</el-button>
            <!-- 超级管理员：员工申请审核 -->
            <el-button v-if="userStore.isSuperAdmin" type="success" @click="showReviewDialog">申请审核</el-button>
            <!-- 租户管理员：申请新增员工 -->
            <el-button v-if="!userStore.isSuperAdmin && hasPermission('user:add')" type="primary" @click="openRequestDialog">申请新增员工</el-button>
            <!-- 租户管理员：查看申请记录 -->
            <el-button v-if="!userStore.isSuperAdmin" type="info" @click="showMyRequests">申请记录</el-button>
          </div>
        </div>
      </template>

      <!-- 搜索栏 -->
      <el-form inline>
        <el-form-item label="姓名">
          <el-input v-model="query.realName" placeholder="请输入姓名" clearable @change="fetchData" />
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
        <el-table-column prop="tenantId" label="所属租户" width="120" v-if="userStore.isSuperAdmin">
          <template #default="{ row }">
            <el-tag type="info">{{ getTenantName(row.tenantId) }}</el-tag>
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

    <!-- 超级管理员：新增员工弹窗 -->
    <el-dialog title="新增员工" v-model="dialogVisible" width="650px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="登录用户名" />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="员工真实姓名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" placeholder="留空则默认 123456" />
        </el-form-item>
        <el-form-item label="所属租户" prop="tenantId">
          <el-select v-model="form.tenantId" placeholder="请选择租户" style="width:100%">
            <el-option v-for="t in tenantOptions" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
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
        <el-form-item label="资质证书内容">
          <el-input v-model="form.zhizhiContent" type="textarea" :rows="3" placeholder="请填写资质证书相关信息" />
        </el-form-item>
        <el-form-item label="资质证书图片">
          <el-upload
            class="zhizhi-uploader"
            :action="uploadAction"
            :headers="uploadHeaders"
            :show-file-list="false"
            :on-success="handleSuperAdminUploadSuccess"
            :on-error="handleZhizhiUploadError"
            :before-upload="beforeZhizhiUpload"
            accept="image/*"
          >
            <img v-if="form.zhizhiImageUrl" :src="form.zhizhiImageUrl" class="zhizhi-image" />
            <el-icon v-else class="zhizhi-uploader-icon"><Plus /></el-icon>
          </el-upload>
          <div style="color: #999; font-size: 12px; margin-top: 8px">支持上传 jpg/png 图片，最大5MB</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确认创建</el-button>
      </template>
    </el-dialog>

    <!-- 租户管理员：申请新增员工弹窗 -->
    <el-dialog title="申请新增员工" v-model="requestDialogVisible" width="650px" @close="resetRequestForm">
      <el-alert type="info" :closable="false" style="margin-bottom: 16px">
        提交后需等待超级管理员审核通过，员工才会被创建。
      </el-alert>
      <el-form ref="requestFormRef" :model="requestForm" :rules="requestRules" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="requestForm.username" placeholder="登录用户名" />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="requestForm.realName" placeholder="员工真实姓名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="requestForm.password" placeholder="留空则默认 123456" />
        </el-form-item>
        <el-form-item label="角色" prop="roleId">
          <el-select v-model="requestForm.roleId" placeholder="请选择角色" style="width:100%">
            <el-option v-for="r in roleOptions" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="岗位类型">
          <el-select v-model="requestForm.postType" placeholder="请选择岗位类型" style="width:100%" clearable>
            <el-option label="研发 (DEV)" value="DEV" />
            <el-option label="运维 (OPS)" value="OPS" />
            <el-option label="客服 (CS)" value="CS" />
          </el-select>
        </el-form-item>
        <el-form-item label="资质证书内容">
          <el-input v-model="requestForm.zhizhiContent" type="textarea" :rows="3" placeholder="请填写资质证书相关信息" />
        </el-form-item>
        <el-form-item label="资质证书图片">
          <el-upload
            class="zhizhi-uploader"
            :action="uploadAction"
            :headers="uploadHeaders"
            :show-file-list="false"
            :on-success="handleZhizhiUploadSuccess"
            :on-error="handleZhizhiUploadError"
            :before-upload="beforeZhizhiUpload"
            accept="image/*"
          >
            <img v-if="requestForm.zhizhiImageUrl" :src="requestForm.zhizhiImageUrl" class="zhizhi-image" />
            <el-icon v-else class="zhizhi-uploader-icon"><Plus /></el-icon>
          </el-upload>
          <div style="color: #999; font-size: 12px; margin-top: 8px">支持上传 jpg/png 图片，最大5MB</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="requestDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleRequestSubmit">提交申请</el-button>
      </template>
    </el-dialog>

    <!-- 编辑弹窗 -->
    <el-dialog title="编辑员工" v-model="editDialogVisible" width="550px" @close="resetForm">
      <el-form ref="editFormRef" :model="editForm" :rules="rules" label-width="100px">
        <el-form-item label="用户名">
          <el-input :model-value="editForm.username" disabled />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="editForm.realName" placeholder="员工真实姓名" />
        </el-form-item>
        <el-form-item label="角色" prop="roleId">
          <el-select v-model="editForm.roleId" placeholder="请选择角色" style="width:100%">
            <el-option v-for="r in roleOptions" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="岗位类型">
          <el-select v-model="editForm.postType" placeholder="请选择岗位类型" style="width:100%" clearable>
            <el-option label="研发 (DEV)" value="DEV" />
            <el-option label="运维 (OPS)" value="OPS" />
            <el-option label="客服 (CS)" value="CS" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="editForm.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleEditSubmit">确认修改</el-button>
      </template>
    </el-dialog>

    <!-- 申请记录弹窗 -->
    <el-dialog title="员工申请记录" v-model="myRequestsVisible" width="800px">
      <el-table :data="myRequestsData.records" v-loading="myRequestsLoading" stripe>
        <el-table-column prop="username" label="用户名" min-width="100" />
        <el-table-column prop="realName" label="真实姓名" min-width="100" />
        <el-table-column prop="roleId" label="角色" width="120">
          <template #default="{ row }">
            <el-tag>{{ getRoleName(row.roleId) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reviewComment" label="审核意见" min-width="120" show-overflow-tooltip />
        <el-table-column prop="createTime" label="申请时间" width="160" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'PENDING'" size="small" type="warning" @click="handleCancelRequest(row)">撤销</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrapper" style="margin-top: 12px">
        <el-pagination
          v-model:current-page="myRequestsQuery.page"
          v-model:page-size="myRequestsQuery.size"
          :total="myRequestsData.total"
          :page-sizes="[10, 20, 50]"
          layout="total, prev, pager, next"
          @size-change="fetchMyRequests"
          @current-change="fetchMyRequests"
        />
      </div>
    </el-dialog>

    <!-- 员工申请审核弹窗（超级管理员） -->
    <el-dialog title="员工申请审核" v-model="reviewDialogVisible" width="1000px">
      <!-- 筛选栏 -->
      <el-form inline style="margin-bottom: 16px">
        <el-form-item label="状态">
          <el-select v-model="reviewQuery.status" placeholder="全部" clearable @change="fetchReviewData">
            <el-option label="待审核" value="PENDING" />
            <el-option label="已通过" value="APPROVED" />
            <el-option label="已拒绝" value="REJECTED" />
            <el-option label="已撤销" value="CANCELLED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchReviewData">查询</el-button>
          <el-button @click="resetReviewQuery">重置</el-button>
          <el-button v-if="selectedReviewIds.length > 0" type="success" @click="handleBatchReview('APPROVED')">批量通过 ({{ selectedReviewIds.length }})</el-button>
          <el-button v-if="selectedReviewIds.length > 0" type="danger" @click="handleBatchReview('REJECTED')">批量拒绝 ({{ selectedReviewIds.length }})</el-button>
        </el-form-item>
      </el-form>

      <!-- 表格 -->
      <el-table :data="reviewData.records" v-loading="reviewLoading" stripe @selection-change="handleReviewSelectionChange">
        <el-table-column type="selection" width="55" :selectable="row => row.status === 'PENDING'" />
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="用户名" min-width="100" show-overflow-tooltip />
        <el-table-column prop="realName" label="真实姓名" min-width="100" show-overflow-tooltip />
        <el-table-column prop="roleId" label="角色" width="120">
          <template #default="{ row }">
            <el-tag>{{ getRoleName(row.roleId) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="postType" label="岗位" width="100">
          <template #default="{ row }">
            <el-tag :type="getPostTypeTagType(row.postType)" size="small">{{ getPostTypeLabel(row.postType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="tenantId" label="租户" width="120">
          <template #default="{ row }">
            <el-tag type="info">{{ getTenantName(row.tenantId) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="applicantName" label="申请人" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reviewerName" label="审核人" width="100" />
        <el-table-column prop="reviewComment" label="审核意见" min-width="120" show-overflow-tooltip />
        <el-table-column prop="createTime" label="申请时间" width="160" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="info" @click="showDetailDialog(row)">详细</el-button>
            <template v-if="row.status === 'PENDING'">
              <el-button size="small" type="success" @click="handleReview(row, 'APPROVED')">通过</el-button>
              <el-button size="small" type="danger" @click="handleReview(row, 'REJECTED')">拒绝</el-button>
            </template>
            <span v-else style="color: #999; font-size: 12px">已处理</span>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper" style="margin-top: 12px">
        <el-pagination
          v-model:current-page="reviewQuery.page"
          v-model:page-size="reviewQuery.size"
          :total="reviewData.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="fetchReviewData"
          @current-change="fetchReviewData"
        />
      </div>
    </el-dialog>

    <!-- 查看详细弹窗 -->
    <el-dialog title="员工申请详细信息" v-model="detailDialogVisible" width="600px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="申请ID">{{ detailData?.id }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ detailData?.username }}</el-descriptions-item>
        <el-descriptions-item label="真实姓名">{{ detailData?.realName }}</el-descriptions-item>
        <el-descriptions-item label="角色">{{ getRoleName(detailData?.roleId) }}</el-descriptions-item>
        <el-descriptions-item label="岗位">{{ getPostTypeLabel(detailData?.postType) }}</el-descriptions-item>
        <el-descriptions-item label="所属租户">{{ getTenantName(detailData?.tenantId) }}</el-descriptions-item>
        <el-descriptions-item label="申请人">{{ detailData?.applicantName }}</el-descriptions-item>
        <el-descriptions-item label="申请状态">
          <el-tag :type="getStatusType(detailData?.status)">{{ getStatusLabel(detailData?.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="资质证书内容">
          <div v-if="detailData?.zhizhiContent">{{ detailData.zhizhiContent }}</div>
          <span v-else style="color: #999">未提供</span>
        </el-descriptions-item>
        <el-descriptions-item label="资质证书图片">
          <div v-if="detailData?.zhizhiImageUrl">
            <el-image :src="detailData.zhizhiImageUrl" style="max-width: 100%; max-height: 400px" fit="contain" :preview-src-list="[detailData.zhizhiImageUrl]" />
          </div>
          <span v-else style="color: #999">未提供</span>
        </el-descriptions-item>
        <el-descriptions-item label="审核人">{{ detailData?.reviewerName || '未审核' }}</el-descriptions-item>
        <el-descriptions-item label="审核意见">{{ detailData?.reviewComment || '无' }}</el-descriptions-item>
        <el-descriptions-item label="申请时间">{{ detailData?.createTime }}</el-descriptions-item>
        <el-descriptions-item label="审核时间">{{ detailData?.reviewTime || '未审核' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 审核确认弹窗 -->
    <el-dialog :title="reviewAction === 'APPROVED' ? '通过员工申请' : '拒绝员工申请'" v-model="reviewConfirmVisible" width="550px">
      <el-descriptions :column="1" border style="margin-bottom: 16px">
        <el-descriptions-item label="用户名">{{ currentReviewRow?.username }}</el-descriptions-item>
        <el-descriptions-item label="真实姓名">{{ currentReviewRow?.realName }}</el-descriptions-item>
        <el-descriptions-item label="角色">{{ getRoleName(currentReviewRow?.roleId) }}</el-descriptions-item>
        <el-descriptions-item label="岗位">{{ getPostTypeLabel(currentReviewRow?.postType) }}</el-descriptions-item>
        <el-descriptions-item label="所属租户">{{ getTenantName(currentReviewRow?.tenantId) }}</el-descriptions-item>
        <el-descriptions-item label="申请人">{{ currentReviewRow?.applicantName }}</el-descriptions-item>
        <el-descriptions-item label="资质证书">
          <div v-if="currentReviewRow?.zhizhiContent">{{ currentReviewRow.zhizhiContent }}</div>
          <div v-if="currentReviewRow?.zhizhiImageUrl" style="margin-top: 8px">
            <el-image :src="currentReviewRow.zhizhiImageUrl" style="max-width: 100%; max-height: 300px" fit="contain" :preview-src-list="[currentReviewRow.zhizhiImageUrl]" />
          </div>
          <span v-if="!currentReviewRow?.zhizhiContent && !currentReviewRow?.zhizhiImageUrl" style="color: #999">未提供</span>
        </el-descriptions-item>
      </el-descriptions>
      <el-form label-width="80px">
        <el-form-item label="审核意见">
          <el-input v-model="reviewComment" type="textarea" :rows="3" placeholder="请输入审核意见（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewConfirmVisible = false">取消</el-button>
        <el-button :type="reviewAction === 'APPROVED' ? 'success' : 'danger'" :loading="submitting" @click="submitReview">
          确认{{ reviewAction === 'APPROVED' ? '通过' : '拒绝' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { userApi } from '@/api/user'
import { roleApi } from '@/api/role'
import { tenantApi } from '@/api/tenant'
import { employeeRequestApi } from '@/api/employeeRequest'
import { useUserStore } from '@/store/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

const userStore = useUserStore()
const uploadAction = ref('/api/employee-request/upload-zhizhi-image')
const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${localStorage.getItem('token')}`
}))

function hasPermission(permission) {
  if (!permission) return true
  return userStore.isSuperAdmin || userStore.permissions.includes(permission)
}

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const requestDialogVisible = ref(false)
const editDialogVisible = ref(false)
const myRequestsVisible = ref(false)
const myRequestsLoading = ref(false)
const formRef = ref()
const requestFormRef = ref()
const editFormRef = ref()

// 员工申请审核相关状态
const reviewDialogVisible = ref(false)
const reviewConfirmVisible = ref(false)
const reviewLoading = ref(false)
const currentReviewRow = ref(null)
const reviewAction = ref('')
const reviewComment = ref('')
const reviewQuery = reactive({ page: 1, size: 10, status: '' })
const reviewData = reactive({ records: [], total: 0 })
const selectedReviewIds = ref([])
const detailDialogVisible = ref(false)
const detailData = ref(null)

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
  fetchRoles()
  if (userStore.isSuperAdmin) fetchTenants()
})
onUnmounted(() => { window.removeEventListener('resize', updateScreenWidth) })

const query = reactive({ page: 1, size: 10, realName: '' })
const tableData = reactive({ records: [], total: 0 })
const roleOptions = ref([])
const roleMap = ref({})
const tenantOptions = ref([])
const tenantMap = ref({})

// 超级管理员创建表单
const form = reactive({
  username: '', realName: '', password: '', roleId: null, postType: '', tenantId: null,
  zhizhiContent: '', zhizhiImageUrl: ''
})

// 租户管理员申请表单
const requestForm = reactive({
  username: '', realName: '', password: '', roleId: null, postType: '',
  zhizhiContent: '', zhizhiImageUrl: ''
})

// 编辑表单
const editForm = reactive({
  id: null, username: '', realName: '', roleId: null, postType: '', status: 1
})

// 申请记录
const myRequestsQuery = reactive({ page: 1, size: 10 })
const myRequestsData = reactive({ records: [], total: 0 })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  roleId: [{ required: true, message: '请选择角色', trigger: 'change' }],
  tenantId: [{ required: true, message: '请选择租户', trigger: 'change' }]
}

const requestRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  roleId: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

function getRoleName(roleId) { return roleMap.value[roleId] || `角色${roleId}` }
function getTenantName(tenantId) { return tenantMap.value[tenantId] || `租户${tenantId}` }

function getPostTypeLabel(postType) {
  const map = { DEV: '研发', OPS: '运维', CS: '客服' }
  return map[postType] || postType || '未设置'
}

function getPostTypeTagType(postType) {
  const map = { DEV: '', OPS: 'success', CS: 'warning' }
  return map[postType] || 'info'
}

function getStatusType(status) {
  const map = { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', CANCELLED: 'info' }
  return map[status] || 'info'
}

function getStatusLabel(status) {
  const map = { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已拒绝', CANCELLED: '已撤销' }
  return map[status] || status
}

async function fetchData() {
  loading.value = true
  try {
    const res = await userApi.page(query)
    Object.assign(tableData, res.data)
  } finally { loading.value = false }
}

async function fetchRoles() {
  const res = await roleApi.page({ page: 1, size: 100 })
  // 过滤掉系统角色：超级管理员、租户管理员
  roleOptions.value = (res.data.records || []).filter(r => 
    r.name !== '超级管理员' && r.name !== '租户管理员'
  )
  const map = {}
  roleOptions.value.forEach(r => { map[r.id] = r.name })
  roleMap.value = map
}

async function fetchTenants() {
  try {
    const res = await tenantApi.page({ page: 1, size: 100 })
    tenantOptions.value = res.data.records || []
    const map = {}
    tenantOptions.value.forEach(t => { map[t.id] = t.name })
    tenantMap.value = map
  } catch { /* 非超管无权限 */ }
}

function resetQuery() {
  query.realName = ''
  query.page = 1
  fetchData()
}

// 超级管理员：直接创建
function openCreateDialog() {
  resetForm()
  dialogVisible.value = true
}

function resetForm() {
  form.username = ''; form.realName = ''; form.password = ''
  form.roleId = null; form.postType = ''; form.tenantId = null
  form.zhizhiContent = ''; form.zhizhiImageUrl = ''
  formRef.value?.resetFields()
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await userApi.create({
      username: form.username,
      realName: form.realName,
      password: form.password || undefined,
      roleId: form.roleId,
      postType: form.postType || null,
      tenantId: form.tenantId,
      zhizhiContent: form.zhizhiContent || null,
      zhizhiImageUrl: form.zhizhiImageUrl || null
    })
    ElMessage.success('员工创建成功')
    dialogVisible.value = false
    fetchData()
  } finally { submitting.value = false }
}

// 租户管理员：申请创建
function openRequestDialog() {
  resetRequestForm()
  requestDialogVisible.value = true
}

function resetRequestForm() {
  requestForm.username = ''; requestForm.realName = ''; requestForm.password = ''
  requestForm.roleId = null; requestForm.postType = ''
  requestForm.zhizhiContent = ''; requestForm.zhizhiImageUrl = ''
  requestFormRef.value?.resetFields()
}

// 图片上传相关方法
function beforeZhizhiUpload(file) {
  const isImage = file.type.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 < 5

  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    return false
  }
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过 5MB!')
    return false
  }
  return true
}

function handleZhizhiUploadSuccess(response) {
  if (response.code === 200) {
    requestForm.zhizhiImageUrl = response.data.imageUrl
    ElMessage.success('资质证书图片上传成功')
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

function handleSuperAdminUploadSuccess(response) {
  if (response.code === 200) {
    form.zhizhiImageUrl = response.data.imageUrl
    ElMessage.success('资质证书图片上传成功')
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

function handleZhizhiUploadError() {
  ElMessage.error('图片上传失败，请重试')
}

async function handleRequestSubmit() {
  const valid = await requestFormRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await employeeRequestApi.submit({
      username: requestForm.username,
      realName: requestForm.realName,
      password: requestForm.password || undefined,
      roleId: requestForm.roleId,
      postType: requestForm.postType || null,
      zhizhiContent: requestForm.zhizhiContent || null,
      zhizhiImageUrl: requestForm.zhizhiImageUrl || null
    })
    ElMessage.success('员工申请已提交，等待超级管理员审核')
    requestDialogVisible.value = false
    fetchData()
  } finally { submitting.value = false }
}

// 编辑
function openEditDialog(row) {
  editForm.id = row.id
  editForm.username = row.username
  editForm.realName = row.realName
  editForm.roleId = row.roleId
  editForm.postType = row.postType || ''
  editForm.status = row.status
  editDialogVisible.value = true
}

async function handleEditSubmit() {
  const valid = await editFormRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await userApi.update(editForm.id, {
      realName: editForm.realName,
      roleId: editForm.roleId,
      postType: editForm.postType || null,
      status: editForm.status
    })
    ElMessage.success('员工修改成功')
    editDialogVisible.value = false
    fetchData()
  } finally { submitting.value = false }
}

// 申请记录
async function showMyRequests() {
  myRequestsVisible.value = true
  myRequestsQuery.page = 1
  await fetchMyRequests()
}

async function fetchMyRequests() {
  myRequestsLoading.value = true
  try {
    const res = await employeeRequestApi.myRequests(myRequestsQuery)
    Object.assign(myRequestsData, res.data)
  } finally { myRequestsLoading.value = false }
}

async function handleCancelRequest(row) {
  await ElMessageBox.confirm(`确认撤销员工「${row.realName}」的申请？`, '提示', { type: 'warning' })
  try {
    await employeeRequestApi.cancel(row.id)
    ElMessage.success('申请已撤销')
    fetchMyRequests()
  } catch { /* 取消 */ }
}

// 员工申请审核（超级管理员）
async function showReviewDialog() {
  reviewDialogVisible.value = true
  reviewQuery.page = 1
  reviewQuery.status = ''
  await fetchReviewData()
}

async function fetchReviewData() {
  reviewLoading.value = true
  try {
    const res = await employeeRequestApi.page(reviewQuery)
    Object.assign(reviewData, res.data)
  } finally { reviewLoading.value = false }
}

function resetReviewQuery() {
  reviewQuery.status = ''
  reviewQuery.page = 1
  fetchReviewData()
}

function handleReview(row, action) {
  currentReviewRow.value = row
  reviewAction.value = action
  reviewComment.value = ''
  reviewConfirmVisible.value = true
}

async function submitReview() {
  const actionText = reviewAction.value === 'APPROVED' ? '通过' : '拒绝'
  await ElMessageBox.confirm(
    `确认${actionText}员工「${currentReviewRow.value.realName}」的申请？`,
    '提示',
    { type: reviewAction.value === 'APPROVED' ? 'success' : 'warning' }
  )
  submitting.value = true
  try {
    await employeeRequestApi.review(currentReviewRow.value.id, {
      action: reviewAction.value,
      reviewComment: reviewComment.value || undefined
    })
    ElMessage.success(reviewAction.value === 'APPROVED' ? '审核通过，员工已创建' : '已拒绝申请')
    reviewConfirmVisible.value = false
    fetchReviewData()
    // 如果审核通过，刷新员工列表
    if (reviewAction.value === 'APPROVED') {
      fetchData()
    }
  } finally { submitting.value = false }
}

// 批量审核相关方法
function handleReviewSelectionChange(selection) {
  selectedReviewIds.value = selection.map(item => item.id)
}

async function handleBatchReview(action) {
  const actionText = action === 'APPROVED' ? '通过' : '拒绝'
  const count = selectedReviewIds.value.length
  
  await ElMessageBox.confirm(
    `确认批量${actionText} ${count} 条员工申请？`,
    '批量审核',
    { type: action === 'APPROVED' ? 'success' : 'warning' }
  )
  
  submitting.value = true
  try {
    await employeeRequestApi.batchReview({
      ids: selectedReviewIds.value,
      action: action,
      reviewComment: undefined
    })
    ElMessage.success(`批量${actionText}成功`)
    selectedReviewIds.value = []
    fetchReviewData()
    if (action === 'APPROVED') {
      fetchData()
    }
  } finally { submitting.value = false }
}

// 查看详细
async function showDetailDialog(row) {
  try {
    const res = await employeeRequestApi.getById(row.id)
    detailData.value = res.data
    detailDialogVisible.value = true
  } catch (e) {
    ElMessage.error('获取详情失败')
  }
}

async function handleStatusChange(row) {
  const newStatus = row.status === 1 ? 0 : 1
  const statusText = newStatus === 1 ? '启用' : '禁用'
  await ElMessageBox.confirm(`确认${statusText}员工「${row.realName}」？`, '提示', { type: 'warning' })
  try {
    await userApi.update(row.id, { realName: row.realName, roleId: row.roleId, status: newStatus })
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
.page-container { width: 100%; padding: 16px; box-sizing: border-box; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.header-actions { display: flex; gap: 8px; }
.page-container :deep(.el-card) { width: 100%; }
.page-container :deep(.el-table) { width: 100%; }
.pagination-wrapper { margin-top: 20px; display: flex; justify-content: flex-end; flex-wrap: wrap; gap: 10px; }
.pagination-wrapper :deep(.el-pagination) { flex-wrap: wrap; justify-content: flex-end; }

/* 资质证书图片上传样式 */
.zhizhi-uploader :deep(.el-upload) {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: border-color 0.2s;
}
.zhizhi-uploader :deep(.el-upload:hover) {
  border-color: #409eff;
}
.zhizhi-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 120px;
  height: 120px;
  line-height: 120px;
  text-align: center;
}
.zhizhi-image {
  width: 120px;
  height: 120px;
  display: block;
  object-fit: contain;
}

@media screen and (max-width: 768px) {
  .pagination-wrapper { justify-content: center; }
  .pagination-wrapper :deep(.el-pagination) { justify-content: center; }
}
@media screen and (max-width: 480px) {
  .pagination-wrapper { justify-content: center; }
  .pagination-wrapper :deep(.el-pagination) { justify-content: center; }
  .pagination-wrapper :deep(.el-pager li) { min-width: 28px; height: 28px; line-height: 28px; }
}
</style>
