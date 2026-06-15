<template>
  <div class="customer-container">
    <el-card class="filter-card">
      <el-form :inline="true" :model="filterForm" class="filter-form">
        <el-form-item label="客户名称">
          <el-input v-model="filterForm.name" placeholder="请输入客户名称" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item label="业务分类">
          <el-select v-model="filterForm.businessCategory" placeholder="请选择" clearable style="width: 110px" @change="onBusinessCategoryChange('filter')">
            <el-option v-for="cat in businessCategories" :key="cat" :label="cat" :value="cat" />
          </el-select>
          <el-select v-model="filterForm.businessType" placeholder="请选择" clearable style="width: 130px; margin-left: 4px" @change="handleSearch">
            <el-option v-for="t in filterBusinessTypes" :key="getOptValue(t)" :label="getOptLabel(t)" :value="getOptValue(t)" />
          </el-select>
        </el-form-item>
        <el-form-item label="合作状态">
          <el-select v-model="filterForm.cooperationStatus" placeholder="全部" clearable style="width: 130px" @change="handleSearch">
            <el-option v-for="opt in dictCooperationStatus" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="filterForm.contactPerson" placeholder="请输入联系人" clearable style="width: 120px" />
        </el-form-item>
        <el-form-item label="运维需求">
          <el-select v-model="filterForm.maintenanceCategory" placeholder="全部" clearable style="width: 130px" @change="handleSearch">
            <el-option v-for="opt in dictMaintenanceCategory" :key="opt.value" :label="opt.label" :value="opt.value" />
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
        <span class="table-title">客户列表</span>
        <div class="table-actions">
          <el-button v-permission="'customer:add'" type="primary" @click="handleAdd">新增客户</el-button>
          <el-button v-permission="'customer:import'" type="success" @click="handleImport">导入</el-button>
          <el-button v-permission="'customer:export'" @click="handleExport">导出</el-button>
        </div>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe border style="width: 100%">
        <el-table-column prop="name" label="客户名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="businessCategory" label="业务分类" min-width="100" />
        <el-table-column prop="businessType" label="站点名称" min-width="110" />
        <el-table-column label="合作状态" min-width="120">
          <template #default="{ row }">
            <el-tag :type="getCooperationTagType(row.cooperationStatus)" size="small">
              {{ row.cooperationStatus || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="contactPerson" label="联系人" min-width="90" />
        <el-table-column prop="contactPhone" label="联系电话" min-width="130" />
        <el-table-column prop="followUpPerson" label="跟进人" min-width="90">
          <template #default="{ row }">{{ row.followUpPerson || '-' }}</template>
        </el-table-column>
        <el-table-column prop="contractExpireDate" label="合同到期" min-width="110">
          <template #default="{ row }">
            <span v-if="row.contractExpireDate" :style="getExpireDateStyle(row.contractExpireDate)">
              {{ row.contractExpireDate }}
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="maintenanceCategory" label="运维需求" min-width="100">
          <template #default="{ row }">
            <el-tag v-if="row.maintenanceCategory" :type="getMaintenanceTagType(row.maintenanceCategory)" size="small">
              {{ getMaintenanceLabel(row.maintenanceCategory) }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleDetail(row)">详情</el-button>
            <el-button v-permission="'followup:list'" size="small" type="warning" @click="handleFollowUp(row)">跟进</el-button>
            <el-button v-permission="'customer:edit'" size="small" type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button v-permission="'customer:assign'" size="small" type="success" @click="openTransferDialog(row)">转移</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination v-model:current-page="pagination.page" v-model:page-size="pagination.size"
        :page-sizes="[10, 20, 50, 100]" :total="pagination.total"
        :layout="paginationLayout" @size-change="handleSearch" @current-change="fetchList"
        style="margin-top: 16px; justify-content: flex-end" />
    </el-card>

    <!-- 新增/编辑客户对话框 -->
    <el-dialog v-model="formDialogVisible" :title="formDialogTitle" width="700px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="客户名称" prop="name">
              <el-input v-model="formData.name" placeholder="请输入客户名称" />
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
            <el-form-item label="跟进人" prop="followUpPerson">
              <el-input v-model="formData.followUpPerson" placeholder="请输入跟进人" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="业务分类" prop="businessCategory">
              <el-select v-model="formData.businessCategory" placeholder="请选择" style="width: 100%" @change="onBusinessCategoryChange('form')">
                <el-option v-for="cat in businessCategories" :key="cat" :label="cat" :value="cat" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="站点名称" prop="businessType">
              <el-select v-model="formData.businessType" placeholder="请先选择一级分类" style="width: 100%">
                <el-option v-for="t in formBusinessTypes" :key="getOptValue(t)" :label="getOptLabel(t)" :value="getOptValue(t)" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="合作状态" prop="cooperationStatus">
              <el-select v-model="formData.cooperationStatus" placeholder="请选择" style="width: 100%">
                <el-option v-for="opt in dictCooperationStatus" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="用气规模" prop="gasScale">
              <el-input v-model="formData.gasScale" placeholder="请输入用气规模" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="运维需求" prop="maintenanceCategory">
              <el-select v-model="formData.maintenanceCategory" placeholder="请选择运维需求" style="width: 100%">
                <el-option v-for="opt in dictMaintenanceCategory" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="客户地址" prop="regionCodes">
              <el-cascader
                v-model="formData.regionCodes"
                :options="regionData"
                placeholder="请选择省/市/区"
                style="width: 100%"
                @change="handleRegionChange"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="详细地址" prop="detailAddress">
              <el-input v-model="formData.detailAddress" placeholder="请输入详细地址（街道、门牌号等）" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="智慧燃气设备信息" prop="smartGasSystem">
          <el-input v-model="formData.smartGasSystem" type="textarea" :rows="2" placeholder="请输入智慧燃气设备信息" />
        </el-form-item>
        <el-form-item label="合同信息" prop="contractInfo">
          <el-input v-model="formData.contractInfo" type="textarea" :rows="2" placeholder="请输入合同相关信息" />
        </el-form-item>
        <el-form-item label="客户备注" prop="remark">
          <el-input v-model="formData.remark" type="textarea" :rows="2" placeholder="请输入客户备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 客户详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="客户详情" width="800px" destroy-on-close>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="客户名称">{{ detailData.name }}</el-descriptions-item>
        <el-descriptions-item label="联系人">{{ detailData.contactPerson }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ detailData.contactPhone }}</el-descriptions-item>
        <el-descriptions-item label="跟进人">{{ detailData.followUpPerson || '-' }}</el-descriptions-item>
        <el-descriptions-item label="业务分类">{{ detailData.businessCategory || '-' }}</el-descriptions-item>
        <el-descriptions-item label="站点名称">{{ detailData.businessType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="合作状态">
          <el-tag :type="getCooperationTagType(detailData.cooperationStatus)" size="small">
            {{ detailData.cooperationStatus || '-' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="用气规模">{{ detailData.gasScale || '-' }}</el-descriptions-item>
        <el-descriptions-item label="运维需求">
          <el-tag v-if="detailData.maintenanceCategory" :type="getMaintenanceTagType(detailData.maintenanceCategory)" size="small">
            {{ getMaintenanceLabel(detailData.maintenanceCategory) }}
          </el-tag>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="客户地址" :span="2">{{ detailData.address || '-' }}</el-descriptions-item>
        <el-descriptions-item label="详细地址" :span="2">{{ detailData.detailAddress || '-' }}</el-descriptions-item>
        <el-descriptions-item label="合同到期">
          <span v-if="detailData.contractExpireDate" :style="getExpireDateStyle(detailData.contractExpireDate)">
            {{ detailData.contractExpireDate }}
          </span>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detailData.createTime }}</el-descriptions-item>
        <el-descriptions-item label="智慧燃气设备信息" :span="2">{{ detailData.smartGasSystem || '-' }}</el-descriptions-item>
        <el-descriptions-item label="合同信息" :span="2">{{ detailData.contractInfo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="客户备注" :span="2">{{ detailData.remark || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">
        客户附件
        <el-upload
          :show-file-list="false"
          :before-upload="beforeUploadAttachment"
          :http-request="handleUploadAttachment"
          accept=".jpg,.jpeg,.png,.pdf,.doc,.docx,.xls,.xlsx"
          style="display: inline-block; margin-left: 12px; vertical-align: middle;"
        >
          <el-button type="primary" size="small">📤 上传</el-button>
        </el-upload>
      </el-divider>
      <el-table :data="detailAttachments" stripe border size="small">
        <el-table-column prop="fileName" label="文件名" />
        <el-table-column prop="fileType" label="类型" width="120" />
        <el-table-column prop="fileSize" label="大小" width="100">
          <template #default="{ row }">{{ formatFileSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="上传时间" width="170" />
        <el-table-column label="操作" width="260">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button v-if="isImageFile(row.fileType, row.fileName)" size="small" type="primary" @click="previewImage(row)">查看</el-button>
              <el-button size="small" type="success" @click="downloadAttachment(row)">下载</el-button>
              <el-button size="small" type="danger" @click="deleteAttachment(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="detailAttachments.length === 0" description="暂无附件，请点击上方按钮上传" />

      <el-divider content-position="left">修改记录</el-divider>
      <el-timeline>
        <el-timeline-item v-for="log in detailModifyLogs" :key="log.id" :timestamp="formatDateTime(log.modifyTime)"
          placement="top">
          <div>
            <strong>{{ log.modifyUser }}</strong> 修改了
            <el-tag size="small" type="info">{{ log.fieldName }}</el-tag>：
            <span style="color: #f56c6c">{{ log.oldValue || '空' }}</span>
            →
            <span style="color: #67c23a">{{ log.newValue || '空' }}</span>
          </div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-if="detailModifyLogs.length === 0" description="暂无修改记录" />
    </el-dialog>

    <!-- 图片预览对话框 -->
    <el-dialog v-model="imagePreviewVisible" :title="`图片预览 - ${currentPreviewImage?.fileName || ''}`" width="90%" top="5vh" destroy-on-close>
      <div class="image-preview-container">
        <div class="image-preview-toolbar">
          <el-button-group>
            <el-button size="small" @click="zoomOut">🔍- 缩小</el-button>
            <el-button size="small" @click="resetZoom">↺ 原始大小</el-button>
            <el-button size="small" @click="zoomIn">🔍+ 放大</el-button>
          </el-button-group>
          <el-button-group style="margin-left: 12px;">
            <el-button size="small" :disabled="currentImageIndex <= 0" @click="prevImage">⬅️ 上一张</el-button>
            <el-button size="small" :disabled="currentImageIndex >= previewImages.length - 1" @click="nextImage">➡️ 下一张</el-button>
          </el-button-group>
        </div>
        <div class="image-preview-content" ref="imagePreviewContentRef" @wheel="handleWheel">
          <img
            :src="currentPreviewImageUrl"
            :style="{ transform: `scale(${imageScale})` }"
            class="preview-image"
            @load="onImageLoad"
            @error="onImageError"
          />
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

    <!-- 跟进记录管理对话框 -->
    <el-dialog v-model="followUpDialogVisible" :title="`跟进记录 - ${currentCustomer?.name || ''}`" width="900px" destroy-on-close>
      <div class="follow-up-header">
        <el-button v-permission="'followup:add'" type="primary" size="small" @click="openAddFollowUpDialog">+ 新增跟进</el-button>
      </div>

      <el-timeline v-if="followUpRecords.length > 0" style="margin-top: 16px">
        <el-timeline-item
          v-for="record in followUpRecords"
          :key="record.id"
          :timestamp="formatDateTime(record.followUpTime)"
          placement="top"
        >
          <el-card shadow="never" class="follow-up-card">
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="跟进人" :span="1">{{ record.followUpPerson || '-' }}</el-descriptions-item>
              <el-descriptions-item label="跟进方式" :span="1">
                <el-tag size="small">{{ followUpMethodMap[record.followUpMethod] || '未知' }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="跟进状态" :span="1">
                <el-tag :type="record.followUpStatus === 1 ? 'warning' : record.followUpStatus === 2 ? 'success' : 'primary'" size="small">
                  {{ followUpStatusMap[record.followUpStatus] || '未知' }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="下一步计划" :span="2">{{ record.nextPlan || '-' }}</el-descriptions-item>
              <el-descriptions-item label="跟进内容" :span="2">
                <div style="white-space:pre-wrap;line-height:1.6">{{ record.followUpContent }}</div>
              </el-descriptions-item>
            </el-descriptions>
            <div class="follow-up-actions" style="margin-top:8px;text-align:right">
              <el-button v-permission="'followup:delete'" size="small" link type="danger" @click="deleteFollowUpRecord(record.id)">删除</el-button>
            </div>
          </el-card>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无跟进记录" />

      <!-- 新增/编辑跟进记录表单 -->
      <el-dialog v-model="addFollowUpDialogVisible" :title="isEditingFollowUp ? '编辑跟进记录' : '新增跟进记录'" width="600px" append-to-body destroy-on-close>
        <el-form ref="followUpFormRef" :model="followUpForm" :rules="followUpFormRules" label-width="100px">
          <el-form-item label="跟进时间" prop="followUpTime">
            <el-date-picker v-model="followUpForm.followUpTime" type="datetime" placeholder="选择跟进时间"
              value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
          </el-form-item>
          <el-form-item label="跟进方式" prop="followUpMethod">
            <el-select v-model="followUpForm.followUpMethod" placeholder="请选择跟进方式" style="width: 100%">
              <el-option v-for="item in followUpMethodOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="跟进状态" prop="followUpStatus">
            <el-select v-model="followUpForm.followUpStatus" placeholder="请选择跟进状态" style="width: 100%">
              <el-option v-for="item in followUpStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="跟进内容" prop="followUpContent">
            <el-input v-model="followUpForm.followUpContent" type="textarea" :rows="4" placeholder="请输入跟进内容" />
          </el-form-item>
          <el-form-item label="下一步计划" prop="nextPlan">
            <el-input v-model="followUpForm.nextPlan" type="textarea" :rows="2" placeholder="请输入下一步计划（可选）" />
          </el-form-item>

          <el-divider content-position="left">变更合作状态</el-divider>

          <el-form-item label="当前状态">
            <el-tag>{{ currentCustomer?.cooperationStatus || '-' }}</el-tag>
          </el-form-item>
          <el-form-item label="新合作状态" prop="newCooperationStatus">
            <el-select v-model="followUpForm.newCooperationStatus" placeholder="请选择" style="width: 100%">
              <el-option v-for="opt in dictCooperationStatus" :key="opt.value" :label="opt.label" :value="opt.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="变更原因" prop="changeReason">
            <el-input v-model="followUpForm.changeReason" type="textarea" :rows="2" placeholder="请输入变更原因" maxlength="200" show-word-limit />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="addFollowUpDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitFollowUp" :loading="followUpSubmitting">确定</el-button>
        </template>
      </el-dialog>
    </el-dialog>

    <!-- 导入客户对话框 -->
    <el-dialog v-model="importDialogVisible" title="导入客户" width="500px" destroy-on-close>
      <el-upload
        ref="importUploadRef"
        :auto-upload="false"
        :limit="1"
        accept=".xlsx,.xls"
        :on-change="handleImportFileChange"
        :on-remove="handleImportFileRemove"
        :file-list="importFileList"
      >
        <el-button type="primary">选择文件</el-button>
        <template #tip>
          <div class="el-upload__tip">仅支持 .xlsx/.xls 格式，单次最多导入1000条</div>
        </template>
      </el-upload>
      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleImportSubmit" :loading="importLoading">开始导入</el-button>
      </template>
    </el-dialog>

    <!-- 客户转移对话框 -->
    <el-dialog v-model="transferDialogVisible" title="转移客户" width="450px" destroy-on-close>
      <el-alert
        :title="`将客户「${transferCustomer?.name || ''}」从「${transferCustomer?.followUpPerson || '未分配'}」转移给其他销售人员`"
        type="info"
        :closable="false"
        style="margin-bottom: 20px;"
      />
      <el-form label-width="100px">
        <el-form-item label="目标人员">
          <el-select
            v-model="transferTargetUserId"
            placeholder="请选择目标销售人员"
            filterable
            style="width: 100%"
            :loading="userListLoading"
          >
            <el-option
              v-for="user in userList"
              :key="user.id"
              :label="`${user.realName} (${user.username})`"
              :value="user.id"
              :disabled="user.id === transferCustomer?.followUpPersonId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="transferRemark"
            type="textarea"
            :rows="3"
            placeholder="请输入转移原因（可选）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="transferDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleTransferSubmit" :loading="transferLoading" :disabled="!transferTargetUserId">
          确认转移
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { customerApi, businessCategoryMap } from '@/api/customer'
import { followUpApi, followUpMethodMap, followUpStatusMap, followUpMethodOptions, followUpStatusOptions } from '@/api/followUp'
import { userApi } from '@/api/user'
import { regionData, CodeToText } from 'element-china-area-data'

const regionCodeToText = CodeToText || {}

const loading = ref(false)
const submitLoading = ref(false)
const importLoading = ref(false)
const followUpSubmitting = ref(false)
const tableData = ref([])

const dictLoading = ref(false)
const dictBusinessCategory = ref([])
const dictBusinessTypeMap = ref({})
const dictCooperationStatus = ref([])
const dictMaintenanceCategory = ref([])

const filterForm = reactive({
  name: '',
  businessCategory: '',
  businessType: '',
  cooperationStatus: '',
  contactPerson: '',
  maintenanceCategory: ''
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

const businessCategories = computed(() => dictBusinessCategory.value.map(item => item.value))

const filterBusinessTypes = computed(() => {
  if (!filterForm.businessCategory) return []
  return dictBusinessTypeMap.value[filterForm.businessCategory] || []
})

const formBusinessTypes = ref([])

function getOptValue(opt) {
  return typeof opt === 'string' ? opt : opt?.value
}

function getOptLabel(opt) {
  return typeof opt === 'string' ? opt : opt?.label || opt?.value
}

function onBusinessCategoryChange(source) {
  if (source === 'filter') {
    filterForm.businessType = ''
  } else {
    formData.businessType = ''
    formBusinessTypes.value = dictBusinessTypeMap.value[formData.businessCategory] || []
  }
}

function getCooperationTagType(status) {
  if (status === '正常履约') return 'success'
  if (status === '终止合作') return 'info'
  if (status === '高潜力') return ''
  if (status === '中潜力') return 'warning'
  if (status === '低潜力') return 'info'
  if (status === '无效客户') return 'danger'
  return 'info'
}

function getMaintenanceTagType(category) {
  if (category === '高频报修') return 'danger'
  if (category === '常规运维') return 'warning'
  return 'success'
}

function getMaintenanceLabel(value) {
  const opt = dictMaintenanceCategory.value.find(o => o.value === value)
  return opt ? opt.label : value
}

function getExpireDateStyle(expireDate) {
  if (!expireDate) return {}
  const expire = new Date(expireDate)
  const today = new Date()
  const oneMonthLater = new Date()
  oneMonthLater.setMonth(oneMonthLater.getMonth() + 1)
  if (expire <= oneMonthLater && expire >= today) {
    return { color: '#f56c6c', fontWeight: 'bold' }
  }
  return {}
}

async function fetchList() {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      size: pagination.size,
      name: filterForm.name || undefined,
      businessCategory: filterForm.businessCategory || undefined,
      businessType: filterForm.businessType || undefined,
      cooperationStatus: filterForm.cooperationStatus || undefined,
      contactPerson: filterForm.contactPerson || undefined,
      maintenanceCategory: filterForm.maintenanceCategory || undefined
    }
    const res = await customerApi.page(params)
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
    name: '',
    businessCategory: '',
    businessType: '',
    cooperationStatus: '',
    contactPerson: '',
    maintenanceCategory: ''
  })
  handleSearch()
}

const formDialogVisible = ref(false)
const formDialogTitle = ref('新增客户')
const isEdit = ref(false)
const editId = ref(null)
const formRef = ref(null)

const formData = reactive({
  name: '',
  contactPerson: '',
  contactPhone: '',
  followUpPerson: '',
  followUpPersonId: null,
  businessCategory: '',
  businessType: '',
  cooperationStatus: '',
  gasScale: '',
  address: '',
  region: '',
  regionCodes: [],
  detailAddress: '',
  smartGasSystem: '',
  contractInfo: '',
  remark: '',
  maintenanceCategory: ''
})

const formRules = {
  name: [{ required: true, message: '请输入客户名称', trigger: 'blur' }],
  contactPerson: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
  contactPhone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }],
  businessCategory: [{ required: true, message: '请选择业务分类', trigger: 'change' }],
  businessType: [{ required: true, message: '请选择站点名称', trigger: 'change' }],
  regionCodes: [{ required: true, type: 'array', min: 1, message: '请选择客户地址', trigger: 'change' }]
}

function handleAdd() {
  isEdit.value = false
  editId.value = null
  formDialogTitle.value = '新增客户'
  Object.assign(formData, {
    name: '',
    contactPerson: '',
    contactPhone: '',
    followUpPerson: '',
    followUpPersonId: null,
    businessCategory: '',
    businessType: '',
    cooperationStatus: '',
    gasScale: '',
    address: '',
    region: '',
    regionCodes: [],
    detailAddress: '',
    smartGasSystem: '',
    contractInfo: '',
    remark: '',
    maintenanceCategory: ''
  })
  formBusinessTypes.value = []
  formDialogVisible.value = true
}

function handleRegionChange(value) {
  if (value && value.length > 0) {
    const regionTexts = value.map(code => regionCodeToText[code] || code)
    formData.address = regionTexts.join('/')
    formData.region = value[value.length - 1]
  } else {
    formData.address = ''
    formData.region = ''
  }
}

function parseAddressToRegionCodes(address) {
  if (!address) return []
  const parts = address.split('/')
  const codes = []
  for (let i = 0; i < parts.length; i++) {
    const part = parts[i]
    for (const [code, text] of Object.entries(regionCodeToText)) {
      if (text === part) {
        if (i === 0 || (i === 1 && codes.length === 1) || (i === 2 && codes.length === 2)) {
          codes.push(code)
          break
        }
      }
    }
  }
  return codes
}

async function handleEdit(row) {
  isEdit.value = true
  editId.value = row.id
  formDialogTitle.value = '编辑客户'
  try {
    const res = await customerApi.detail(row.id)
    const data = res.data
    const regionCodes = parseAddressToRegionCodes(data.address)
    Object.assign(formData, {
      name: data.name || '',
      contactPerson: data.contactPerson || '',
      contactPhone: data.contactPhone || '',
      followUpPerson: data.followUpPerson || '',
      followUpPersonId: data.followUpPersonId || null,
      businessCategory: data.businessCategory || '',
      businessType: data.businessType || '',
      cooperationStatus: data.cooperationStatus || '',
      gasScale: data.gasScale || '',
      address: data.address || '',
      region: data.region || '',
      regionCodes: regionCodes,
      detailAddress: data.detailAddress || '',
      smartGasSystem: data.smartGasSystem || '',
      contractInfo: data.contractInfo || '',
      remark: data.remark || '',
      maintenanceCategory: data.maintenanceCategory || ''
    })

    formBusinessTypes.value = data.businessCategory ? (businessCategoryMap[data.businessCategory] || []) : []

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

  submitLoading.value = true
  try {
    if (isEdit.value) {
      await customerApi.update(editId.value, formData)
      ElMessage.success('客户修改成功')
    } else {
      await customerApi.create(formData)
      ElMessage.success('客户添加成功')
    }
    formDialogVisible.value = false
    fetchList()
  } catch (e) {
    console.error(e)
  } finally {
    submitLoading.value = false
  }
}

const detailDialogVisible = ref(false)
const detailData = ref({})
const detailAttachments = ref([])
const detailModifyLogs = ref([])

async function handleDetail(row) {
  try {
    const [detailRes, attRes, logRes] = await Promise.all([
      customerApi.detail(row.id),
      customerApi.listAttachments(row.id),
      customerApi.listModifyLogs(row.id)
    ])
    detailData.value = detailRes.data || {}
    detailAttachments.value = attRes.data || []
    detailModifyLogs.value = logRes.data || []
    detailDialogVisible.value = true
  } catch (e) {
    console.error(e)
  }
}

async function downloadAttachment(row) {
  const token = localStorage.getItem('token')
  try {
    const response = await fetch(`/api/customer/attachment/${row.id}/download?token=${token}`, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })

    if (!response.ok) {
      const errorData = await response.json()
      throw new Error(errorData.msg || `下载失败 (${response.status})`)
    }

    const blob = await response.blob()
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = row.fileName || 'download'
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)

    ElMessage.success('✅ 文件下载成功')
  } catch (e) {
    console.error('下载失败:', e)
    ElMessage.error(`❌ 下载失败: ${e.message}`)
  }
}

function beforeUploadAttachment(file) {
  const allowedTypes = [
    'image/jpeg', 'image/png', 'application/pdf',
    'application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'application/vnd.ms-excel', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
  ]
  if (!allowedTypes.includes(file.type)) {
    ElMessage.error('仅支持 JPG/PNG/PDF/DOC/DOCX/XLS/XLSX 格式文件')
    return false
  }
  if (file.size > 20 * 1024 * 1024) {
    ElMessage.error('文件大小不能超过 20MB')
    return false
  }
  return true
}

async function handleUploadAttachment(options) {
  try {
    const formData = new FormData()
    formData.append('file', options.file)
    formData.append('fileType', '其他')

    await customerApi.uploadAttachment(detailData.value.id, formData)
    ElMessage.success('✅ 附件上传成功')

    const attRes = await customerApi.listAttachments(detailData.value.id)
    detailAttachments.value = attRes.data || []
  } catch (e) {
    console.error(e)
    ElMessage.error('❌ 上传失败: ' + (e.message || '未知错误'))
  }
}

async function deleteAttachment(row) {
  try {
    await ElMessageBox.confirm(
      `确认删除附件「${row.fileName}」？`,
      '删除确认',
      { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
    )

    await customerApi.deleteAttachment(row.id)
    ElMessage.success('✅ 附件已删除')

    const attRes = await customerApi.listAttachments(detailData.value.id)
    detailAttachments.value = attRes.data || []
  } catch (e) {
    if (e !== 'cancel') {
      console.error(e)
      ElMessage.error('❌ 删除失败')
    }
  }
}

const imagePreviewVisible = ref(false)
const currentPreviewImage = ref(null)
const currentImageIndex = ref(0)
const imageScale = ref(1)
const imagePreviewContentRef = ref(null)

function isImageFile(fileType, fileName) {
  const imageExtensions = ['.jpg', '.jpeg', '.png', '.gif', '.bmp', '.webp']
  const imageTypes = ['图片', 'image', 'jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp']

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

async function previewImage(row) {
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
  return `/api/customer/attachment/${currentPreviewImage.value.id}/download?token=${token}`
})

function getThumbnailUrl(img) {
  const token = localStorage.getItem('token')
  return `/api/customer/attachment/${img.id}/download?token=${token}`
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
  if (currentImageIndex.value < previewImages.value - 1) {
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

function onImageLoad() {
  console.log('图片加载完成')
}

function onImageError() {
  ElMessage.error('❌ 图片加载失败')
}

function formatFileSize(bytes) {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

function formatDateTime(val) {
  if (!val) return '-'
  const d = new Date(val)
  if (isNaN(d.getTime())) return val
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const h = String(d.getHours()).padStart(2, '0')
  const min = String(d.getMinutes()).padStart(2, '0')
  const s = String(d.getSeconds()).padStart(2, '0')
  return `${y}-${m}-${day} ${h}:${min}:${s}`
}

function handleExport() {
  const params = {
    name: filterForm.name || undefined,
    businessCategory: filterForm.businessCategory || undefined,
    businessType: filterForm.businessType || undefined,
    cooperationStatus: filterForm.cooperationStatus || undefined,
    maintenanceCategory: filterForm.maintenanceCategory || undefined
  }
  const url = customerApi.getExportUrl(params)
  fetch(url, { headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } })
    .then(res => res.blob())
    .then(blob => {
      const a = document.createElement('a')
      a.href = URL.createObjectURL(blob)
      a.download = '客户列表.xlsx'
      a.click()
      URL.revokeObjectURL(a.href)
    })
    .catch(() => ElMessage.error('导出失败'))
}

const importDialogVisible = ref(false)
const importUploadRef = ref(null)
const importFileList = ref([])
const importFile = ref(null)

function handleImport() {
  importFileList.value = []
  importFile.value = null
  importDialogVisible.value = true
}

function handleImportFileChange(file, fileListVal) {
  const allowedTypes = [
    'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    'application/vnd.ms-excel'
  ]
  if (!allowedTypes.includes(file.raw.type)) {
    ElMessage.error('仅支持 Excel 文件格式（.xlsx/.xls）')
    importFileList.value = []
    return false
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('文件大小不能超过5MB')
    return false
  }
  importFile.value = file.raw
  importFileList.value = fileListVal.slice(-1)
}

function handleImportFileRemove() {
  importFile.value = null
}

async function handleImportSubmit() {
  if (!importFile.value) {
    ElMessage.warning('请先选择要导入的文件')
    return
  }

  importLoading.value = true
  try {
    const fd = new FormData()
    fd.append('file', importFile.value)
    const res = await customerApi.importCustomers(fd)
    ElMessage.success(res.message || '导入成功')
    importDialogVisible.value = false
    fetchList()
  } catch (e) {
    console.error(e)
  } finally {
    importLoading.value = false
  }
}

// ========== 客户转移功能 ==========
const transferDialogVisible = ref(false)
const transferCustomer = ref(null)
const transferTargetUserId = ref(null)
const transferRemark = ref('')
const transferLoading = ref(false)
const userList = ref([])
const userListLoading = ref(false)

// 打开转移对话框
async function openTransferDialog(row) {
  transferCustomer.value = row
  transferTargetUserId.value = null
  transferRemark.value = ''
  transferDialogVisible.value = true

  // 加载用户列表
  await fetchUserList()
}

// 获取用户列表
async function fetchUserList() {
  userListLoading.value = true
  try {
    const res = await userApi.list()
    userList.value = res.data || []
  } catch (e) {
    console.error('获取用户列表失败:', e)
    ElMessage.error('获取用户列表失败')
    userList.value = []
  } finally {
    userListLoading.value = false
  }
}

// 提交转移
async function handleTransferSubmit() {
  if (!transferTargetUserId.value) {
    ElMessage.warning('请选择目标销售人员')
    return
  }

  // 查找目标用户信息
  const targetUser = userList.value.find(u => u.id === transferTargetUserId.value)
  if (!targetUser) {
    ElMessage.error('目标用户不存在')
    return
  }

  // 确认对话框
  try {
    await ElMessageBox.confirm(
      `确认将客户「${transferCustomer.value.name}」从「${transferCustomer.value.followUpPerson || '未分配'}」转移给「${targetUser.realName || targetUser.username}」？`,
      '确认转移',
      { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' }
    )

    transferLoading.value = true
    try {
      await customerApi.transferCustomer(
        transferCustomer.value.id,
        targetUser.id,
        targetUser.realName || targetUser.username
      )
      ElMessage.success(`✅ 客户已成功转移给 ${targetUser.realName || targetUser.username}`)
      transferDialogVisible.value = false
      fetchList() // 刷新列表
    } catch (e) {
      console.error('转移失败:', e)
      ElMessage.error('❌ 转移失败: ' + (e.response?.data?.msg || e.message || '未知错误'))
    } finally {
      transferLoading.value = false
    }
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

// ========== 跟进记录相关功能 ==========
const followUpDialogVisible = ref(false)
const addFollowUpDialogVisible = ref(false)
const currentCustomer = ref(null)
const followUpRecords = ref([])
const isEditingFollowUp = ref(false)
const editingFollowUpId = ref(null)
const followUpFormRef = ref(null)

const followUpForm = reactive({
  customerId: null,
  followUpTime: '',
  followUpMethod: null,
  followUpContent: '',
  nextPlan: '',
  followUpStatus: 1,
  newCooperationStatus: '',
  changeReason: ''
})

const followUpFormRules = computed(() => ({
  followUpTime: [{ required: true, message: '请选择跟进时间', trigger: 'change' }],
  followUpMethod: [{ required: true, message: '请选择跟进方式', trigger: 'change' }],
  followUpContent: [{ required: true, message: '请输入跟进内容', trigger: 'blur' }],
  followUpStatus: [{ required: true, message: '请选择跟进状态', trigger: 'change' }],
  ...(followUpForm.newCooperationStatus ? {
    changeReason: [{ required: true, message: '请输入变更原因', trigger: 'blur' }]
  } : {})
}))

async function handleFollowUp(row) {
  currentCustomer.value = row
  followUpDialogVisible.value = true
  await fetchFollowUpRecords(row.id)
}

async function fetchFollowUpRecords(customerId) {
  try {
    const res = await followUpApi.listRecordsByCustomerId(customerId)
    followUpRecords.value = res.data || []
  } catch (e) {
    console.error('获取跟进记录失败:', e)
    followUpRecords.value = []
  }
}

function openAddFollowUpDialog() {
  isEditingFollowUp.value = false
  editingFollowUpId.value = null
  const now = new Date()
  const y = now.getFullYear()
  const m = String(now.getMonth() + 1).padStart(2, '0')
  const d = String(now.getDate()).padStart(2, '0')
  const h = String(now.getHours()).padStart(2, '0')
  const min = String(now.getMinutes()).padStart(2, '0')
  const s = String(now.getSeconds()).padStart(2, '0')
  Object.assign(followUpForm, {
    customerId: currentCustomer.value.id,
    followUpTime: `${y}-${m}-${d} ${h}:${min}:${s}`,
    followUpMethod: null,
    followUpContent: '',
    nextPlan: '',
    followUpStatus: 1,
    newCooperationStatus: '',
    changeReason: ''
  })
  addFollowUpDialogVisible.value = true
}

function editFollowUpRecord(record) {
  isEditingFollowUp.value = true
  editingFollowUpId.value = record.id
  Object.assign(followUpForm, {
    customerId: record.customerId,
    followUpTime: record.followUpTime || '',
    followUpMethod: record.followUpMethod,
    followUpContent: record.followUpContent || '',
    nextPlan: record.nextPlan || '',
    followUpStatus: record.followUpStatus
  })
  addFollowUpDialogVisible.value = true
}

async function submitFollowUp() {
  try {
    await followUpFormRef.value.validate()
  } catch {
    return
  }

  followUpSubmitting.value = true
  try {
    if (isEditingFollowUp.value) {
      await followUpApi.updateRecord(editingFollowUpId.value, followUpForm)
      ElMessage.success('跟进记录修改成功')
    } else {
      await followUpApi.createRecord(followUpForm)
      ElMessage.success('跟进记录添加成功，合作状态已同步更新')
    }
    addFollowUpDialogVisible.value = false
    await fetchFollowUpRecords(currentCustomer.value.id)
    fetchList()
  } catch (e) {
    console.error(e)
  } finally {
    followUpSubmitting.value = false
  }
}

async function deleteFollowUpRecord(recordId) {
  try {
    await ElMessageBox.confirm(
      '确定要删除这条跟进记录吗？',
      '删除确认',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    await followUpApi.deleteRecord(recordId)
    ElMessage.success('跟进记录已删除')
    await fetchFollowUpRecords(currentCustomer.value.id)
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

async function loadDicts() {
  dictLoading.value = true
  try {
    const res = await customerApi.getDicts()
    const data = res.data || {}

    dictBusinessCategory.value = (data.businessCategory || []).map(item => ({
      value: item.value,
      label: item.label
    }))

    const businessTypeMap = data.businessTypeMap || {}
    dictBusinessTypeMap.value = {}
    for (const key in businessTypeMap) {
      dictBusinessTypeMap.value[key] = businessTypeMap[key].map(item => ({
        value: item.value,
        label: item.label
      }))
    }

    dictCooperationStatus.value = (data.cooperationStatus || []).map(item => ({
      value: item.value,
      label: item.label
    }))

    dictMaintenanceCategory.value = (data.maintenanceCategory || []).map(item => ({
      value: item.value,
      label: item.label
    }))
  } catch (e) {
    console.error('加载字典数据失败:', e)
  } finally {
    dictLoading.value = false
  }
}

onMounted(async () => {
  await loadDicts()
  fetchList()
})
</script>

<style scoped>
.customer-container {
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

/* 跟进记录样式 */
.follow-up-header {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 8px;
}

.follow-up-card {
  margin-bottom: 4px;
}

.follow-up-header-info {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.follow-up-person {
  font-weight: 600;
  color: #303133;
}

.follow-up-content {
  margin: 8px 0;
  color: #303133;
  line-height: 1.5;
}

.follow-up-next-plan {
  margin: 4px 0;
  color: #909399;
  font-size: 13px;
}

.follow-up-actions {
  margin-top: 8px;
  text-align: right;
}

/* 操作按钮组样式 */
.action-buttons {
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: flex-start;
  flex-wrap: nowrap;
}

.action-buttons .el-button {
  margin-left: 0 !important;
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
  height: 70vh;
}

.image-preview-toolbar {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 16px;
}

.image-preview-content {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: auto;
  background: #fafafa;
  border-radius: 8px;
  padding: 20px;
  min-height: 400px;
}

.preview-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  transition: transform 0.2s ease;
  cursor: grab;
  user-select: none;
}

.preview-image:active {
  cursor: grabbing;
}

.image-preview-thumbs {
  display: flex;
  gap: 8px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-top: 16px;
  overflow-x: auto;
  justify-content: center;
}

.thumb-item {
  width: 60px;
  height: 60px;
  border: 2px solid transparent;
  border-radius: 6px;
  overflow: hidden;
  cursor: pointer;
  opacity: 0.6;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.thumb-item:hover {
  opacity: 0.9;
  transform: scale(1.05);
}

.thumb-item.active {
  border-color: #409eff;
  opacity: 1;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
}

.thumb-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
</style>
