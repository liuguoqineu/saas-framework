<template>
  <div class="repair-container">
    <el-card class="filter-card">
      <el-form :inline="true" :model="filterForm" class="filter-form">
        <el-form-item label="客户名称">
          <el-input v-model="filterForm.customerName" placeholder="请输入客户名称" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item label="报修时间">
          <el-date-picker v-model="filterForm.repairTimeRange" type="daterange" range-separator="至"
            start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD"
            style="width: 240px" />
        </el-form-item>
        <el-form-item label="报修状态">
          <el-select v-model="filterForm.status" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="item in repairStatusOptions" :key="item.value" :label="item.label"
              :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="运维人员">
          <el-input v-model="filterForm.assigneeName" placeholder="请输入运维人员" clearable style="width: 130px" />
        </el-form-item>
        <el-form-item label="紧急程度">
          <el-select v-model="filterForm.urgency" placeholder="全部" clearable style="width: 100px">
            <el-option v-for="item in urgencyOptions" :key="item.value" :label="item.label"
              :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="报修类型">
          <el-select v-model="filterForm.repairType" placeholder="全部" clearable style="width: 150px">
            <el-option v-for="item in repairTypeOptions" :key="item.value" :label="item.label"
              :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <div class="table-header">
        <span class="table-title">报修列表</span>
        <div class="table-actions">
          <el-button v-permission="'repair:add'" type="primary" @click="handleAdd">新增报修</el-button>
          <el-button @click="handleExport">导出</el-button>
          <el-button type="warning" @click="showStatsDialog">统计</el-button>
        </div>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe border style="width: 100%">
        <el-table-column prop="repairNo" label="报修单号" min-width="140" />
        <el-table-column prop="customerName" label="客户名称" min-width="120" />
        <el-table-column prop="contactPerson" label="联系人" min-width="80" />
        <el-table-column prop="contactPhone" label="联系电话" min-width="120" />
        <el-table-column prop="repairType" label="报修类型" min-width="120" />
        <el-table-column prop="repairContent" label="报修内容" min-width="150" show-overflow-tooltip />
        <el-table-column prop="repairTime" label="报修时间" min-width="160" />
        <el-table-column prop="urgency" label="紧急程度" min-width="90">
          <template #default="{ row }">
            <el-tag :type="row.urgency === '紧急' ? 'danger' : 'info'" size="small">
              {{ row.urgency }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="报修状态" min-width="90">
          <template #default="{ row }">
            <el-tag :type="repairStatusTagType[row.status]" size="small">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="assigneeName" label="运维人员" min-width="90" />
        <el-table-column prop="confirmStatus" label="确认状态" min-width="90">
          <template #default="{ row }">
            <el-tag v-if="row.status === '已解决'" :type="row.confirmStatus === 1 ? 'success' : 'warning'" size="small">
              {{ row.confirmStatus === 1 ? '已确认' : '未确认' }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleDetail(row)">详情</el-button>
            <el-button v-permission="'repair:edit'" size="small" type="primary" @click="handleEdit(row)"
              :disabled="row.status === '已解决' && row.confirmStatus === 1">编辑</el-button>
            <el-button v-permission="'repair:assign'" size="small" type="warning" @click="handleAssign(row)"
              :disabled="row.status !== '未处理' && row.status !== '无法解决'">分配</el-button>
            <el-button v-permission="'repair:process'" size="small" type="success" @click="handleProcess(row)"
              :disabled="row.status !== '处理中'">处理</el-button>
            <el-button v-permission="'repair:delete'" size="small" type="danger"
              @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination v-model:current-page="pagination.page" v-model:page-size="pagination.size"
        :page-sizes="[10, 20, 50, 100]" :total="pagination.total"
        :layout="paginationLayout" @size-change="handleSearch" @current-change="fetchList"
        style="margin-top: 16px; justify-content: flex-end" />
    </el-card>

    <el-dialog v-model="formDialogVisible" :title="formDialogTitle" width="700px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="客户名称" prop="customerId">
              <el-select v-model="formData.customerId" filterable placeholder="请选择客户（自动关联信息）" style="width: 100%"
                @change="handleCustomerChange">
                <el-option v-for="c in customerOptions" :key="c.id" :label="c.name" :value="c.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系人" prop="contactPerson">
              <el-input v-model="formData.contactPerson" placeholder="请输入联系人" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="联系电话" prop="contactPhone">
              <el-input v-model="formData.contactPhone" placeholder="请输入联系电话" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="报修类型" prop="repairType">
              <el-select v-model="formData.repairType" placeholder="请选择报修类型" style="width: 100%">
                <el-option v-for="item in repairTypeOptions" :key="item.value" :label="item.label"
                  :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="报修时间" prop="repairTime">
              <el-date-picker v-model="formData.repairTime" type="datetime" placeholder="请选择报修时间"
                value-format="YYYY-MM-DD HH:mm:ss" format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="紧急程度" prop="urgency">
              <el-select v-model="formData.urgency" placeholder="请选择紧急程度" style="width: 100%">
                <el-option v-for="item in urgencyOptions" :key="item.value" :label="item.label"
                  :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="报修地点" prop="repairAddress">
          <el-input v-model="formData.repairAddress" placeholder="请输入报修地点" />
        </el-form-item>
        <el-form-item label="报修内容" prop="repairContent">
          <el-input v-model="formData.repairContent" type="textarea" :rows="3" placeholder="请输入报修内容，如智慧燃气系统故障、设备问题等" />
        </el-form-item>
        <el-form-item label="故障描述细化" prop="faultDescription">
          <el-input v-model="formData.faultDescription" type="textarea" :rows="2" placeholder="请输入故障描述细化信息" />
        </el-form-item>
        <el-form-item label="现场照片">
          <el-upload :auto-upload="false" :on-change="handleFileChange" :file-list="fileList"
            :on-remove="handleFileRemove" list-type="picture-card" accept="image/*" ref="uploadRef">
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailDialogVisible" title="报修详情" width="900px" destroy-on-close>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="报修单号">{{ detailData.repairNo }}</el-descriptions-item>
        <el-descriptions-item label="客户名称">{{ detailData.customerName }}</el-descriptions-item>
        <el-descriptions-item label="联系人">{{ detailData.contactPerson }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ detailData.contactPhone }}</el-descriptions-item>
        <el-descriptions-item label="报修类型">{{ detailData.repairType }}</el-descriptions-item>
        <el-descriptions-item label="紧急程度">
          <el-tag :type="detailData.urgency === '紧急' ? 'danger' : 'info'" size="small">
            {{ detailData.urgency }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="报修时间">{{ detailData.repairTime }}</el-descriptions-item>
        <el-descriptions-item label="报修地点">{{ detailData.repairAddress || '-' }}</el-descriptions-item>
        <el-descriptions-item label="报修状态">
          <el-tag :type="repairStatusTagType[detailData.status]" size="small">
            {{ detailData.status }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="确认状态">
          <el-tag v-if="detailData.status === '已解决'" :type="detailData.confirmStatus === 1 ? 'success' : 'warning'" size="small">
            {{ detailData.confirmStatus === 1 ? '已确认' : '未确认' }}
          </el-tag>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="报修内容" :span="2">{{ detailData.repairContent }}</el-descriptions-item>
        <el-descriptions-item label="故障描述细化" :span="2">{{ detailData.faultDescription || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">分配信息</el-divider>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="运维人员">{{ detailData.assigneeName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="分配时间">{{ detailData.assignTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="分配人">{{ detailData.assignerName || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">处理信息</el-divider>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="处理时间">{{ detailData.processTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="处理方式">{{ detailData.processMethod || '-' }}</el-descriptions-item>
        <el-descriptions-item label="更换配件">{{ detailData.replacedParts || '-' }}</el-descriptions-item>
        <el-descriptions-item label="故障原因">{{ detailData.faultReason || '-' }}</el-descriptions-item>
      </el-descriptions>

      <template v-if="detailData.isException === 1">
        <el-divider content-position="left">异常信息</el-divider>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="异常原因" :span="2">{{ detailData.exceptionReason || '-' }}</el-descriptions-item>
          <el-descriptions-item label="二次处理计划" :span="2">{{ detailData.secondPlan || '-' }}</el-descriptions-item>
          <el-descriptions-item label="二次处理提醒时间">{{ detailData.secondRemindTime || '-' }}</el-descriptions-item>
        </el-descriptions>
      </template>

      <el-divider content-position="left">附件</el-divider>
      <div style="margin-bottom: 10px">
        <el-upload :auto-upload="false" :on-change="handleDetailFileChange" :show-file-list="false">
          <el-button size="small" type="primary">上传附件</el-button>
        </el-upload>
      </div>
      <el-table :data="detailAttachments" stripe border size="small">
        <el-table-column prop="fileName" label="文件名" />
        <el-table-column prop="fileType" label="类型" width="120" />
        <el-table-column prop="fileSize" label="大小" width="100">
          <template #default="{ row }">{{ formatFileSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="上传时间" width="170" />
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button v-if="isImageFile(row.fileType, row.fileName)" size="small" type="primary" @click="previewImage(row)">查看</el-button>
            <el-button size="small" link type="danger" @click="handleDeleteAttachment(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-divider content-position="left">处理记录</el-divider>
      <el-timeline>
        <el-timeline-item v-for="log in detailProcessLogs" :key="log.id" :timestamp="log.operateTime"
          placement="top">
          <div>
            <strong>{{ log.operatorName }}</strong>
            <el-tag size="small" type="info" style="margin: 0 4px">{{ log.action }}</el-tag>
            <template v-if="log.oldStatus && log.newStatus">
              <el-tag size="small" :type="repairStatusTagType[log.oldStatus]">{{ log.oldStatus }}</el-tag>
              →
              <el-tag size="small" :type="repairStatusTagType[log.newStatus]">{{ log.newStatus }}</el-tag>
            </template>
            <div v-if="log.content" style="color: #606266; margin-top: 4px">{{ log.content }}</div>
          </div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-if="detailProcessLogs.length === 0" description="暂无处理记录" />

      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button v-if="detailData.status === '已解决' && detailData.confirmStatus !== 1"
          v-permission="'repair:confirm'" type="success" @click="handleConfirm(detailData)">确认闭环</el-button>
        <el-button v-if="detailData.status !== '已解决' && detailData.isException !== 1"
          v-permission="'repair:exception'" type="warning" @click="handleException(detailData)">标记异常</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="assignDialogVisible" title="分配报修单" width="500px" destroy-on-close>
      <el-form ref="assignFormRef" :model="assignForm" :rules="assignRules" label-width="100px">
        <el-form-item label="报修单号">
          <span>{{ assignForm.repairNo }}</span>
        </el-form-item>
        <el-form-item label="运维人员" prop="assigneeId">
          <el-select v-model="assignForm.assigneeId" filterable placeholder="请选择运维人员" style="width: 100%"
            @change="handleAssigneeChange">
            <el-option v-for="u in userOptions" :key="u.id" :label="u.realName || u.username" :value="u.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAssignSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="processDialogVisible" title="更新报修进度" width="600px" destroy-on-close>
      <el-form ref="processFormRef" :model="processForm" :rules="processRules" label-width="100px">
        <el-form-item label="当前状态">
          <el-tag :type="repairStatusTagType[processForm.currentStatus]">{{ processForm.currentStatus }}</el-tag>
        </el-form-item>
        <el-form-item label="新状态" prop="status">
          <el-select v-model="processForm.status" placeholder="请选择新状态" style="width: 100%">
            <el-option label="处理中" value="处理中" />
            <el-option label="已解决" value="已解决" />
            <el-option label="无法解决" value="无法解决" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理方式" prop="processMethod">
          <el-input v-model="processForm.processMethod" type="textarea" :rows="2" placeholder="请输入处理方式" />
        </el-form-item>
        <el-form-item label="更换配件" prop="replacedParts">
          <el-input v-model="processForm.replacedParts" placeholder="请输入更换配件信息" />
        </el-form-item>
        <el-form-item label="故障原因" prop="faultReason">
          <el-input v-model="processForm.faultReason" type="textarea" :rows="2" placeholder="请输入故障原因" />
        </el-form-item>
        <el-form-item label="处理现场照片">
          <el-upload :auto-upload="false" :on-change="handleProcessFileChange" :file-list="processFileList"
            list-type="picture-card" accept="image/*">
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="processDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleProcessSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="exceptionDialogVisible" title="标记异常" width="600px" destroy-on-close>
      <el-form ref="exceptionFormRef" :model="exceptionForm" :rules="exceptionRules" label-width="120px">
        <el-form-item label="异常原因" prop="exceptionReason">
          <el-input v-model="exceptionForm.exceptionReason" type="textarea" :rows="3" placeholder="请输入异常原因" />
        </el-form-item>
        <el-form-item label="二次处理计划" prop="secondPlan">
          <el-input v-model="exceptionForm.secondPlan" type="textarea" :rows="2" placeholder="请输入二次处理计划" />
        </el-form-item>
        <el-form-item label="二次处理提醒时间" prop="secondRemindTime">
          <el-date-picker v-model="exceptionForm.secondRemindTime" type="datetime" placeholder="请选择提醒时间"
            value-format="YYYY-MM-DD HH:mm:ss" format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="exceptionDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleExceptionSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="imagePreviewVisible" :title="`图片预览 - ${currentPreviewImage?.fileName || ''}`" width="90%" top="5vh" destroy-on-close>
      <div class="image-preview-container">
        <div class="image-preview-toolbar">
          <el-button-group>
            <el-button size="small" @click="zoomOut">缩小</el-button>
            <el-button size="small" @click="resetZoom">原始大小</el-button>
            <el-button size="small" @click="zoomIn">放大</el-button>
          </el-button-group>
          <el-button-group style="margin-left: 12px;">
            <el-button size="small" :disabled="currentImageIndex <= 0" @click="prevImage">上一张</el-button>
            <el-button size="small" :disabled="currentImageIndex >= previewImages.length - 1" @click="nextImage">下一张</el-button>
          </el-button-group>
        </div>
        <div class="image-preview-content" ref="imagePreviewContentRef" @wheel="handleWheel">
          <img
            v-if="currentPreviewImageUrl"
            :src="currentPreviewImageUrl"
            :style="{ transform: `scale(${imageScale})` }"
            class="preview-image"
          />
          <div v-else class="preview-error">图片加载失败</div>
        </div>
        <div v-if="previewImages.length > 1" class="image-preview-thumbs">
          <div
            v-for="(img, index) in previewImages"
            :key="img.id"
            :class="['thumb-item', { active: index === currentImageIndex }]"
            @click="switchImage(index)"
          >
            <img :src="getThumbnailUrl(img)" :alt="img.fileName" />
          </div>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="statsDialogVisible" title="报修统计" width="800px" destroy-on-close>
      <el-row :gutter="16" style="margin-bottom: 20px">
        <el-col :span="4">
          <el-statistic title="报修总量" :value="statsData.totalCount" />
        </el-col>
        <el-col :span="4">
          <el-statistic title="已解决" :value="statsData.resolvedCount" />
        </el-col>
        <el-col :span="4">
          <el-statistic title="未处理" :value="statsData.unresolvedCount" />
        </el-col>
        <el-col :span="4">
          <el-statistic title="处理中" :value="statsData.processingCount" />
        </el-col>
        <el-col :span="4">
          <el-statistic title="异常数" :value="statsData.exceptionCount" />
        </el-col>
        <el-col :span="4">
          <el-statistic title="解决率" :value="statsData.totalCount > 0 ? ((statsData.resolvedCount / statsData.totalCount) * 100).toFixed(1) + '%' : '0%'" />
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <h4 style="margin-bottom: 10px">高频报修客户 TOP10</h4>
          <el-table :data="statsData.highFrequencyCustomers" stripe border size="small">
            <el-table-column type="index" label="排名" width="60" />
            <el-table-column prop="name" label="客户名称" />
            <el-table-column prop="count" label="报修次数" width="100" />
          </el-table>
          <el-empty v-if="statsData.highFrequencyCustomers.length === 0" description="暂无数据" />
        </el-col>
        <el-col :span="12">
          <h4 style="margin-bottom: 10px">高频故障类型 TOP10</h4>
          <el-table :data="statsData.highFrequencyFaultTypes" stripe border size="small">
            <el-table-column type="index" label="排名" width="60" />
            <el-table-column prop="name" label="故障类型" />
            <el-table-column prop="count" label="报修次数" width="100" />
          </el-table>
          <el-empty v-if="statsData.highFrequencyFaultTypes.length === 0" description="暂无数据" />
        </el-col>
      </el-row>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { repairApi, repairTypeOptions, repairStatusOptions, urgencyOptions, repairStatusTagType } from '@/api/repair'
import { customerApi } from '@/api/customer'
import { userApi } from '@/api/user'

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref([])
const customerOptions = ref([])
const userOptions = ref([])

const filterForm = reactive({
  customerName: '',
  repairTimeRange: null,
  status: '',
  assigneeName: '',
  urgency: '',
  repairType: ''
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

const formDialogVisible = ref(false)
const formDialogTitle = ref('新增报修')
const isEdit = ref(false)
const editId = ref(null)
const formRef = ref(null)
const uploadRef = ref(null)
const fileList = ref([])
const pendingFiles = ref([])

const formData = reactive({
  customerId: null,
  customerName: '',
  contactPerson: '',
  contactPhone: '',
  repairContent: '',
  repairType: '',
  repairTime: '',
  repairAddress: '',
  urgency: '普通',
  faultDescription: ''
})

const formRules = {
  repairContent: [{ required: true, message: '请输入报修内容', trigger: 'blur' }],
  urgency: [{ required: true, message: '请选择紧急程度', trigger: 'change' }]
}

const detailDialogVisible = ref(false)
const detailData = ref({})
const detailAttachments = ref([])
const detailProcessLogs = ref([])

const assignDialogVisible = ref(false)
const assignFormRef = ref(null)
const assignForm = reactive({
  repairId: null,
  repairNo: '',
  assigneeId: null,
  assigneeName: ''
})

const assignRules = {
  assigneeId: [{ required: true, message: '请选择运维人员', trigger: 'change' }]
}

const processDialogVisible = ref(false)
const processFormRef = ref(null)
const processForm = reactive({
  repairId: null,
  currentStatus: '',
  status: '',
  processMethod: '',
  replacedParts: '',
  faultReason: ''
})
const processFileList = ref([])
const processPendingFiles = ref([])

const processRules = {
  status: [{ required: true, message: '请选择新状态', trigger: 'change' }]
}

const exceptionDialogVisible = ref(false)
const exceptionFormRef = ref(null)
const exceptionForm = reactive({
  repairId: null,
  exceptionReason: '',
  secondPlan: '',
  secondRemindTime: ''
})

const exceptionRules = {
  exceptionReason: [{ required: true, message: '请输入异常原因', trigger: 'blur' }]
}

const statsDialogVisible = ref(false)
const statsData = reactive({
  totalCount: 0,
  resolvedCount: 0,
  unresolvedCount: 0,
  processingCount: 0,
  exceptionCount: 0,
  highFrequencyCustomers: [],
  highFrequencyFaultTypes: []
})

async function fetchList() {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      size: pagination.size,
      customerName: filterForm.customerName || undefined,
      repairTimeStart: filterForm.repairTimeRange?.[0] || undefined,
      repairTimeEnd: filterForm.repairTimeRange?.[1] || undefined,
      status: filterForm.status || undefined,
      assigneeName: filterForm.assigneeName || undefined,
      urgency: filterForm.urgency || undefined,
      repairType: filterForm.repairType || undefined
    }
    const res = await repairApi.page(params)
    tableData.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function fetchCustomerOptions() {
  try {
    const res = await customerApi.page({ page: 1, size: 1000 })
    customerOptions.value = (res.data?.records || []).filter(c => c.cooperationStatus !== '无效客户')
  } catch (e) {
    console.error(e)
  }
}

async function fetchUserOptions() {
  try {
    const res = await userApi.list({ postType: 'OPS' })
    userOptions.value = res.data || []
  } catch (e) {
    try {
      const res = await userApi.page({ page: 1, size: 1000, postType: 'OPS' })
      userOptions.value = res.data?.records || []
    } catch (e2) {
      console.error(e2)
    }
  }
}

function handleSearch() {
  pagination.page = 1
  fetchList()
}

function handleReset() {
  Object.assign(filterForm, {
    customerName: '',
    repairTimeRange: null,
    status: '',
    assigneeName: '',
    urgency: '',
    repairType: ''
  })
  handleSearch()
}

function handleAdd() {
  isEdit.value = false
  editId.value = null
  formDialogTitle.value = '新增报修'
  Object.assign(formData, {
    customerId: null,
    customerName: '',
    contactPerson: '',
    contactPhone: '',
    repairContent: '',
    repairType: '',
    repairTime: '',
    repairAddress: '',
    urgency: '普通',
    faultDescription: ''
  })
  fileList.value = []
  pendingFiles.value = []
  formDialogVisible.value = true
}

async function handleEdit(row) {
  isEdit.value = true
  editId.value = row.id
  formDialogTitle.value = '编辑报修'
  try {
    const res = await repairApi.detail(row.id)
    const data = res.data
    Object.assign(formData, {
      customerId: data.customerId,
      customerName: data.customerName || '',
      contactPerson: data.contactPerson || '',
      contactPhone: data.contactPhone || '',
      repairContent: data.repairContent || '',
      repairType: data.repairType || '',
      repairTime: data.repairTime || '',
      repairAddress: data.repairAddress || '',
      urgency: data.urgency || '普通',
      faultDescription: data.faultDescription || ''
    })
    fileList.value = []
    pendingFiles.value = []
    formDialogVisible.value = true
  } catch (e) {
    console.error(e)
  }
}

function handleCustomerChange(customerId) {
  const customer = customerOptions.value.find(c => c.id === customerId)
  if (customer) {
    formData.customerName = customer.name
    formData.contactPerson = customer.contactPerson || formData.contactPerson
    formData.contactPhone = customer.contactPhone || formData.contactPhone
    formData.repairAddress = customer.address || formData.repairAddress
  }
}

function handleFileChange(file) {
  const maxSize = 20 * 1024 * 1024
  if (file.size > maxSize) {
    ElMessage.error('文件大小不能超过20MB')
    return false
  }
  if (!file.status || file.status === 'ready') {
    pendingFiles.value.push(file.raw)
  }
}

function handleFileRemove(file) {
  if (file.raw) {
    const idx = pendingFiles.value.indexOf(file.raw)
    if (idx > -1) pendingFiles.value.splice(idx, 1)
  }
}

async function uploadPendingFiles(repairId, fileType) {
  for (const file of pendingFiles.value) {
    try {
      const fd = new FormData()
      fd.append('file', file)
      fd.append('fileType', fileType || '现场照片')
      await repairApi.uploadAttachment(repairId, fd)
    } catch (e) {
      console.error('附件上传失败', e)
    }
  }
  pendingFiles.value = []
}

async function handleSubmit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitLoading.value = true
  try {
    if (isEdit.value) {
      await repairApi.update(editId.value, formData)
      await uploadPendingFiles(editId.value, '现场照片')
      ElMessage.success('报修信息修改成功')
    } else {
      const res = await repairApi.create(formData)
      const repairId = res.data?.id || editId.value
      if (repairId) {
        await uploadPendingFiles(repairId, '现场照片')
      }
      ElMessage.success('报修单创建成功')
    }
    formDialogVisible.value = false
    fetchList()
  } catch (e) {
    console.error(e)
  } finally {
    submitLoading.value = false
  }
}

async function handleDetail(row) {
  try {
    const [detailRes, attRes, logRes] = await Promise.all([
      repairApi.detail(row.id),
      repairApi.listAttachments(row.id),
      repairApi.listProcessLogs(row.id)
    ])
    detailData.value = detailRes.data || {}
    detailAttachments.value = attRes.data || []
    detailProcessLogs.value = logRes.data || []
    detailDialogVisible.value = true
  } catch (e) {
    console.error(e)
  }
}

async function handleDetailFileChange(file) {
  const maxSize = 20 * 1024 * 1024
  if (file.size > maxSize) {
    ElMessage.error('文件大小不能超过20MB')
    return
  }
  try {
    const fd = new FormData()
    fd.append('file', file.raw)
    fd.append('fileType', '现场照片')
    await repairApi.uploadAttachment(detailData.value.id, fd)
    ElMessage.success('附件上传成功')
    const attRes = await repairApi.listAttachments(detailData.value.id)
    detailAttachments.value = attRes.data || []
  } catch (e) {
    console.error(e)
  }
}

async function handleDeleteAttachment(row) {
  try {
    await ElMessageBox.confirm('确定删除该附件吗？', '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await repairApi.deleteAttachment(row.id)
    ElMessage.success('附件已删除')
    detailAttachments.value = detailAttachments.value.filter(a => a.id !== row.id)
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

function handleAssign(row) {
  assignForm.repairId = row.id
  assignForm.repairNo = row.repairNo
  assignForm.assigneeId = row.assigneeId || null
  assignForm.assigneeName = row.assigneeName || ''
  assignDialogVisible.value = true
}

function handleAssigneeChange(userId) {
  const user = userOptions.value.find(u => u.id === userId)
  if (user) {
    assignForm.assigneeName = user.realName || user.username
  }
}

async function handleAssignSubmit() {
  try {
    await assignFormRef.value.validate()
  } catch {
    return
  }

  submitLoading.value = true
  try {
    await repairApi.assign(assignForm.repairId, {
      assigneeId: assignForm.assigneeId,
      assigneeName: assignForm.assigneeName
    })
    ElMessage.success('报修单分配成功')
    assignDialogVisible.value = false
    fetchList()
  } catch (e) {
    console.error(e)
  } finally {
    submitLoading.value = false
  }
}

function handleProcess(row) {
  processForm.repairId = row.id
  processForm.currentStatus = row.status
  processForm.status = ''
  processForm.processMethod = ''
  processForm.replacedParts = ''
  processForm.faultReason = ''
  processFileList.value = []
  processPendingFiles.value = []
  processDialogVisible.value = true
}

function handleProcessFileChange(file) {
  const maxSize = 20 * 1024 * 1024
  if (file.size > maxSize) {
    ElMessage.error('文件大小不能超过20MB')
    return false
  }
  if (!file.status || file.status === 'ready') {
    processPendingFiles.value.push(file.raw)
  }
}

async function handleProcessSubmit() {
  try {
    await processFormRef.value.validate()
  } catch {
    return
  }

  submitLoading.value = true
  try {
    await repairApi.process(processForm.repairId, {
      status: processForm.status,
      processMethod: processForm.processMethod,
      replacedParts: processForm.replacedParts,
      faultReason: processForm.faultReason
    })

    for (const file of processPendingFiles.value) {
      try {
        const fd = new FormData()
        fd.append('file', file)
        fd.append('fileType', '处理照片')
        await repairApi.uploadAttachment(processForm.repairId, fd)
      } catch (e) {
        console.error('处理照片上传失败', e)
      }
    }
    processPendingFiles.value = []

    ElMessage.success('报修进度更新成功')
    processDialogVisible.value = false
    fetchList()
  } catch (e) {
    console.error(e)
  } finally {
    submitLoading.value = false
  }
}

async function handleConfirm(row) {
  try {
    await ElMessageBox.confirm('确认该报修单已处理完成并闭环？', '确认闭环', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'success'
    })
    await repairApi.confirm(row.id)
    ElMessage.success('报修确认成功')
    detailDialogVisible.value = false
    fetchList()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

function handleException(row) {
  exceptionForm.repairId = row.id
  exceptionForm.exceptionReason = ''
  exceptionForm.secondPlan = ''
  exceptionForm.secondRemindTime = ''
  exceptionDialogVisible.value = true
}

async function handleExceptionSubmit() {
  try {
    await exceptionFormRef.value.validate()
  } catch {
    return
  }

  submitLoading.value = true
  try {
    await repairApi.markException(exceptionForm.repairId, {
      exceptionReason: exceptionForm.exceptionReason,
      secondPlan: exceptionForm.secondPlan,
      secondRemindTime: exceptionForm.secondRemindTime
    })
    ElMessage.success('异常标记成功')
    exceptionDialogVisible.value = false
    detailDialogVisible.value = false
    fetchList()
  } catch (e) {
    console.error(e)
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定要删除报修单「${row.repairNo}」吗？`,
      '删除确认',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    await repairApi.delete(row.id)
    ElMessage.success('报修单已删除')
    fetchList()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

async function showStatsDialog() {
  try {
    const res = await repairApi.stats()
    Object.assign(statsData, res.data || {})
    statsDialogVisible.value = true
  } catch (e) {
    console.error(e)
  }
}

function handleExport() {
  const params = {
    customerName: filterForm.customerName || undefined,
    repairTimeStart: filterForm.repairTimeRange?.[0] || undefined,
    repairTimeEnd: filterForm.repairTimeRange?.[1] || undefined,
    status: filterForm.status || undefined,
    assigneeName: filterForm.assigneeName || undefined,
    urgency: filterForm.urgency || undefined,
    repairType: filterForm.repairType || undefined
  }
  const url = repairApi.getExportUrl(params)
  const token = localStorage.getItem('token')
  fetch(url, { headers: { Authorization: `Bearer ${token}` } })
    .then(res => res.blob())
    .then(blob => {
      const a = document.createElement('a')
      a.href = URL.createObjectURL(blob)
      a.download = '报修列表.xlsx'
      a.click()
      URL.revokeObjectURL(a.href)
    })
    .catch(() => ElMessage.error('导出失败'))
}

function formatFileSize(bytes) {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

// 图片预览相关
const imagePreviewVisible = ref(false)
const currentPreviewImage = ref(null)
const currentImageIndex = ref(0)
const imageScale = ref(1)
const imagePreviewContentRef = ref(null)

function isImageFile(fileType, fileName) {
  const imageExtensions = ['.jpg', '.jpeg', '.png', '.gif', '.bmp', '.webp']
  const imageTypes = ['图片', 'image', 'jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', '现场照片', '处理照片']

  if (fileType) {
    const lowerType = fileType.toLowerCase()
    if (imageTypes.some(type => lowerType.includes(type.toLowerCase()))) {
      return true
    }
  }

  if (fileName) {
    const lowerName = fileName.toLowerCase()
    return imageExtensions.some(ext => lowerName.endsWith(ext))
  }

  return false
}

function previewImage(row) {
  currentPreviewImage.value = row
  currentImageIndex.value = 0

  const imageAttachments = detailAttachments.value.filter(att => isImageFile(att.fileType, att.fileName))
  if (imageAttachments.length > 1) {
    currentImageIndex.value = imageAttachments.findIndex(img => img.id === row.id)
  }

  imageScale.value = 1
  imagePreviewVisible.value = true
}

const previewImages = computed(() => {
  return detailAttachments.value.filter(att => isImageFile(att.fileType, att.fileName))
})

const currentPreviewImageUrl = computed(() => {
  if (!currentPreviewImage.value) return ''
  const token = localStorage.getItem('token')
  return `/api/repair/attachment/${currentPreviewImage.value.id}/download?token=${token}`
})

function getThumbnailUrl(img) {
  const token = localStorage.getItem('token')
  return `/api/repair/attachment/${img.id}/download?token=${token}`
}

function switchImage(index) {
  currentImageIndex.value = index
  currentPreviewImage.value = previewImages.value[index]
  imageScale.value = 1
}

function prevImage() {
  if (currentImageIndex.value > 0) {
    switchImage(currentImageIndex.value - 1)
  }
}

function nextImage() {
  if (currentImageIndex.value < previewImages.value.length - 1) {
    switchImage(currentImageIndex.value + 1)
  }
}

function zoomIn() {
  if (imageScale.value < 5) {
    imageScale.value = Math.min(5, imageScale.value + 0.25)
  }
}

function zoomOut() {
  if (imageScale.value > 0.25) {
    imageScale.value = Math.max(0.25, imageScale.value - 0.25)
  }
}

function resetZoom() {
  imageScale.value = 1
}

function handleWheel(e) {
  e.preventDefault()
  if (e.deltaY < 0) {
    zoomIn()
  } else {
    zoomOut()
  }
}

onMounted(() => {
  fetchList()
  fetchCustomerOptions()
  fetchUserOptions()
})
</script>

<style scoped>
.repair-container {
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

/* 图片预览样式 */
.image-preview-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.image-preview-toolbar {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px 0;
  border-bottom: 1px solid #ebeef5;
}

.image-preview-content {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  max-height: 60vh;
  overflow: auto;
  background: #f5f7fa;
  border-radius: 4px;
  padding: 16px;
}

.preview-image {
  max-width: 100%;
  max-height: 60vh;
  object-fit: contain;
  transition: transform 0.2s ease;
}

.preview-error {
  color: #909399;
  font-size: 14px;
}

.image-preview-thumbs {
  display: flex;
  gap: 8px;
  padding: 8px 0;
  overflow-x: auto;
  border-top: 1px solid #ebeef5;
}

.thumb-item {
  width: 60px;
  height: 60px;
  flex-shrink: 0;
  cursor: pointer;
  border: 2px solid transparent;
  border-radius: 4px;
  overflow: hidden;
  opacity: 0.6;
  transition: all 0.2s ease;
}

.thumb-item:hover {
  opacity: 0.9;
}

.thumb-item.active {
  border-color: #409eff;
  opacity: 1;
}

.thumb-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
</style>
