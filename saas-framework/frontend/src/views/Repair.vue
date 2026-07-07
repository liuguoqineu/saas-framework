<template>
  <div class="repair-container">
    <el-card class="filter-card">
      <el-form :inline="true" :model="filterForm" class="filter-form">
        <el-form-item label="客户名称">
          <el-input v-model="filterForm.customerName" placeholder="请输入客户名称" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item label="设备编码">
          <el-input v-model="filterForm.deviceCode" placeholder="请输入设备编码" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item label="故障部位">
          <el-input v-model="filterForm.faultPart" placeholder="请输入故障部位" clearable style="width: 130px" />
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
          <el-button v-permission="'repair:add'" type="success" @click="handleAddDeviceRepair">设备报修</el-button>
          <el-button @click="handleExport">导出</el-button>
          <el-button type="warning" @click="showStatsDialog">统计</el-button>
        </div>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe border style="width: 100%">
        <el-table-column prop="repairNo" label="报修单号" min-width="140" />
        <el-table-column prop="customerName" label="客户名称" min-width="120">
          <template #default="{ row }">{{ row.customerName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="deviceCode" label="设备编码" min-width="120">
          <template #default="{ row }">{{ row.deviceCode || '-' }}</template>
        </el-table-column>
        <el-table-column prop="contactPerson" label="联系人" min-width="80">
          <template #default="{ row }">{{ row.contactPerson || '-' }}</template>
        </el-table-column>
        <el-table-column prop="contactPhone" label="联系电话" min-width="120">
          <template #default="{ row }">{{ row.contactPhone || '-' }}</template>
        </el-table-column>
        <el-table-column prop="repairType" label="报修类型" min-width="120">
          <template #default="{ row }">{{ row.repairType || (isDeviceRepair(row) ? '设备故障' : '-') }}</template>
        </el-table-column>
        <el-table-column prop="faultPart" label="故障部位" min-width="100">
          <template #default="{ row }">{{ row.faultPart || '-' }}</template>
        </el-table-column>
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
            <el-tag :type="repairStatusTagType[row.status] || 'info'" size="small">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="assigneeName" label="运维人员" min-width="90">
          <template #default="{ row }">{{ row.assigneeName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="confirmStatus" label="确认状态" min-width="90">
          <template #default="{ row }">
            <el-tag v-if="row.status === '已解决'" :type="row.confirmStatus === 1 ? 'success' : 'warning'" size="small">
              {{ row.confirmStatus === 1 ? '已确认' : '未确认' }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="340" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleDetail(row)">详情</el-button>
            <template v-if="!isDeviceRepair(row)">
              <el-button v-permission="'repair:edit'" size="small" type="primary" @click="handleEdit(row)"
                :disabled="row.status === '已解决' && row.confirmStatus === 1">编辑</el-button>
              <el-button v-permission="'repair:assign'" size="small" type="warning" @click="handleAssign(row)"
                :disabled="row.status !== '未处理' && row.status !== '无法解决'">分配</el-button>
              <el-button v-permission="'repair:process'" size="small" type="success" @click="handleProcess(row)"
                :disabled="row.status !== '处理中'">处理</el-button>
              <el-button v-permission="'repair:delete'" size="small" type="danger"
                @click="handleDelete(row)">删除</el-button>
            </template>
            <template v-else>
              <el-button v-permission="'repair:process'" size="small" type="success" @click="handleDeviceProcess(row)"
                :disabled="!canProcessDevice(row.status)">处理</el-button>
            </template>
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
        <el-descriptions-item v-if="!isDeviceRepair(detailData)" label="客户名称">{{ detailData.customerName }}</el-descriptions-item>
        <el-descriptions-item v-else label="设备编码">{{ detailData.deviceCode || '-' }}</el-descriptions-item>
        <el-descriptions-item v-if="!isDeviceRepair(detailData)" label="联系人">{{ detailData.contactPerson || '-' }}</el-descriptions-item>
        <el-descriptions-item v-else label="故障部位">{{ detailData.faultPart || '-' }}</el-descriptions-item>
        <el-descriptions-item v-if="!isDeviceRepair(detailData)" label="联系电话">{{ detailData.contactPhone || '-' }}</el-descriptions-item>
        <el-descriptions-item v-else label="故障时间">{{ detailData.faultTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="报修类型">{{ detailData.repairType || (isDeviceRepair(detailData) ? '设备故障' : '-') }}</el-descriptions-item>
        <el-descriptions-item label="紧急程度">
          <el-tag :type="detailData.urgency === '紧急' ? 'danger' : 'info'" size="small">
            {{ detailData.urgency }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="报修时间">{{ detailData.repairTime || '-' }}</el-descriptions-item>
        <el-descriptions-item v-if="!isDeviceRepair(detailData)" label="报修地点">{{ detailData.repairAddress || '-' }}</el-descriptions-item>
        <el-descriptions-item v-else label="维修人员">{{ detailData.repairPerson || detailData.assigneeName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="报修状态">
          <el-tag :type="repairStatusTagType[detailData.status] || 'info'" size="small">
            {{ detailData.status }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="确认状态">
          <el-tag v-if="detailData.status === '已解决'" :type="detailData.confirmStatus === 1 ? 'success' : 'warning'" size="small">
            {{ detailData.confirmStatus === 1 ? '已确认' : '未确认' }}
          </el-tag>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="报修内容" :span="2">{{ detailData.repairContent || '-' }}</el-descriptions-item>
        <el-descriptions-item v-if="!isDeviceRepair(detailData)" label="故障描述细化" :span="2">{{ detailData.faultDescription || '-' }}</el-descriptions-item>
        <el-descriptions-item v-if="isDeviceRepair(detailData)" label="备注" :span="2">{{ detailData.remark || '-' }}</el-descriptions-item>
      </el-descriptions>

      <!-- 设备报修照片 -->
      <template v-if="isDeviceRepair(detailData) && detailData.repairPhotoBefore">
        <el-divider content-position="left">报修照片</el-divider>
        <div class="photo-preview">
          <el-image v-for="(url, idx) in (detailData.repairPhotoBefore || '').split(',').filter(Boolean)" :key="idx"
            :src="url" :preview-src-list="(detailData.repairPhotoBefore || '').split(',').filter(Boolean)"
            :initial-index="idx" fit="contain" style="max-width: 150px; max-height: 120px; margin-right: 8px" />
        </div>
      </template>

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
        <el-descriptions-item v-if="!isDeviceRepair(detailData)" label="更换配件">{{ detailData.replacedParts || '-' }}</el-descriptions-item>
        <el-descriptions-item v-else label="维修开始时间">{{ detailData.repairStartTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="故障原因">{{ detailData.faultReason || '-' }}</el-descriptions-item>
        <el-descriptions-item v-if="isDeviceRepair(detailData)" label="维修结束时间">{{ detailData.repairEndTime || '-' }}</el-descriptions-item>
      </el-descriptions>

      <!-- 设备维修后照片 -->
      <template v-if="isDeviceRepair(detailData) && detailData.repairPhotoAfter">
        <el-divider content-position="left">维修后照片</el-divider>
        <div class="photo-preview">
          <el-image v-for="(url, idx) in (detailData.repairPhotoAfter || '').split(',').filter(Boolean)" :key="idx"
            :src="url" :preview-src-list="(detailData.repairPhotoAfter || '').split(',').filter(Boolean)"
            :initial-index="idx" fit="contain" style="max-width: 150px; max-height: 120px; margin-right: 8px" />
        </div>
      </template>

      <!-- 设备更换信息 -->
      <template v-if="isDeviceRepair(detailData) && detailData.hasReplacement === 1">
        <el-divider content-position="left">更换信息</el-divider>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="更换类型">
            {{ replacementTypeLabel[detailData.replacementType] || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="更换人">{{ detailData.replacePerson || '-' }}</el-descriptions-item>
          <el-descriptions-item label="更换原因" :span="2">{{ detailData.replaceReason || '-' }}</el-descriptions-item>
        </el-descriptions>

        <template v-if="detailData.replacementItems && detailData.replacementItems.length > 0">
          <el-divider content-position="left">更换明细</el-divider>
          <el-table :data="detailData.replacementItems" border size="small">
            <el-table-column type="index" label="序号" width="60" />
            <el-table-column prop="itemType" label="类型" width="80">
              <template #default="{ row }">
                {{ row.itemType === 1 ? '配件' : row.itemType === 2 ? '设备' : '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="oldItemName" label="旧件名称" min-width="100" />
            <el-table-column prop="oldItemModel" label="旧件型号" min-width="100" />
            <el-table-column prop="oldItemStatus" label="旧件状态" width="80">
              <template #default="{ row }">
                {{ oldItemStatusLabel[row.oldItemStatus] || '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="newItemName" label="新件名称" min-width="100" />
            <el-table-column prop="newItemModel" label="新件型号" min-width="100" />
            <el-table-column prop="newItemQty" label="数量" width="70" align="center" />
          </el-table>
        </template>

        <template v-if="detailData.replacePhoto">
          <el-divider content-position="left">更换照片</el-divider>
          <div class="photo-preview">
            <el-image v-for="(url, idx) in (detailData.replacePhoto || '').split(',').filter(Boolean)" :key="idx"
              :src="url" :preview-src-list="(detailData.replacePhoto || '').split(',').filter(Boolean)"
              :initial-index="idx" fit="contain" style="max-width: 150px; max-height: 120px; margin-right: 8px" />
          </div>
        </template>
      </template>

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
              <el-tag size="small" :type="repairStatusTagType[log.oldStatus] || 'info'">{{ log.oldStatus }}</el-tag>
              →
              <el-tag size="small" :type="repairStatusTagType[log.newStatus] || 'info'">{{ log.newStatus }}</el-tag>
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
        <el-button v-if="!isDeviceRepair(detailData) && detailData.status !== '已解决' && detailData.isException !== 1"
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

    <!-- 设备故障报修对话框 -->
    <el-dialog v-model="deviceRepairDialogVisible" title="设备故障报修" width="700px" destroy-on-close>
      <el-form ref="deviceRepairFormRef" :model="deviceRepairForm" :rules="deviceRepairRules" label-width="100px">
        <el-form-item label="选择设备" prop="deviceId">
          <el-select v-model="deviceRepairForm.deviceId" filterable remote reserve-keyword placeholder="请输入设备编码或名称搜索"
            :remote-method="handleDeviceSearch" :loading="deviceSearchLoading" style="width: 100%"
            @change="handleDeviceSelect">
            <el-option v-for="d in deviceOptions" :key="d.id" :label="`${d.deviceCode} - ${d.deviceName}`"
              :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="设备编码">
          <span>{{ deviceRepairForm.deviceCode || '-' }}</span>
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="故障时间" prop="faultTime">
              <el-date-picker v-model="deviceRepairForm.faultTime" type="datetime" placeholder="请选择故障时间"
                value-format="YYYY-MM-DD HH:mm:ss" format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="紧急程度" prop="urgency">
              <el-select v-model="deviceRepairForm.urgency" placeholder="请选择紧急程度" style="width: 100%">
                <el-option label="普通" value="普通" />
                <el-option label="紧急" value="紧急" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="故障部位" prop="faultPart">
          <el-input v-model="deviceRepairForm.faultPart" placeholder="请输入故障部位" />
        </el-form-item>
        <el-form-item label="故障描述" prop="faultDescription">
          <el-input v-model="deviceRepairForm.faultDescription" type="textarea" :rows="3" placeholder="请输入故障描述" />
        </el-form-item>
        <el-form-item label="报修照片">
          <el-upload :auto-upload="false" :on-change="(f) => handlePhotoChange(f, 'repairPhotoBefore')" :file-list="repairPhotoBeforeList"
            :on-remove="(f) => handlePhotoRemove(f, 'repairPhotoBefore')" list-type="picture-card" accept="image/*">
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="deviceRepairForm.remark" type="textarea" :rows="2" placeholder="请输入备注（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="deviceRepairDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleDeviceRepairSubmit" :loading="submitLoading">提交报修</el-button>
      </template>
    </el-dialog>

    <!-- 设备维修处理对话框 -->
    <el-dialog v-model="deviceProcessDialogVisible" title="维修处理" width="900px" destroy-on-close>
      <el-form ref="deviceProcessFormRef" :model="deviceProcessForm" :rules="deviceProcessRules" label-width="110px">
        <el-divider content-position="left">基本信息</el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="维修开始时间" prop="repairStartTime">
              <el-date-picker v-model="deviceProcessForm.repairStartTime" type="datetime" placeholder="请选择开始时间"
                value-format="YYYY-MM-DD HH:mm:ss" format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="维修结束时间" prop="repairEndTime">
              <el-date-picker v-model="deviceProcessForm.repairEndTime" type="datetime" placeholder="请选择结束时间"
                value-format="YYYY-MM-DD HH:mm:ss" format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="维修时长(小时)">
              <el-input-number v-model="deviceProcessForm.repairDuration" :min="0" :precision="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="维修后照片">
              <el-upload :auto-upload="false" :on-change="(f) => handlePhotoChange(f, 'repairPhotoAfter')" :file-list="repairPhotoAfterList"
                :on-remove="(f) => handlePhotoRemove(f, 'repairPhotoAfter')" list-type="picture-card" accept="image/*">
                <el-icon><Plus /></el-icon>
              </el-upload>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="处理方式" prop="processMethod">
          <el-input v-model="deviceProcessForm.processMethod" type="textarea" :rows="2" placeholder="请输入处理方式" />
        </el-form-item>
        <el-form-item label="故障原因" prop="faultReason">
          <el-input v-model="deviceProcessForm.faultReason" type="textarea" :rows="2" placeholder="请输入故障原因" />
        </el-form-item>

        <el-divider content-position="left">更换信息</el-divider>
        <el-form-item label="是否有更换" prop="hasReplacement">
          <el-radio-group v-model="deviceProcessForm.hasReplacement">
            <el-radio :value="0">否</el-radio>
            <el-radio :value="1">是</el-radio>
          </el-radio-group>
        </el-form-item>

        <template v-if="deviceProcessForm.hasReplacement === 1">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="更换类型">
                <el-select v-model="deviceProcessForm.replacementType" placeholder="请选择更换类型" style="width: 100%">
                  <el-option v-for="t in replacementTypeOptions" :key="t.value" :label="t.label" :value="t.value" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="更换人">
                <el-input v-model="deviceProcessForm.replacePerson" placeholder="请输入更换人" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="更换原因">
            <el-input v-model="deviceProcessForm.replaceReason" type="textarea" :rows="2" placeholder="请输入更换原因" />
          </el-form-item>
          <el-form-item label="更换照片">
            <el-upload :auto-upload="false" :on-change="(f) => handlePhotoChange(f, 'replacePhoto')" :file-list="replacePhotoList"
              :on-remove="(f) => handlePhotoRemove(f, 'replacePhoto')" list-type="picture-card" accept="image/*">
              <el-icon><Plus /></el-icon>
            </el-upload>
          </el-form-item>

          <el-divider content-position="left">更换明细</el-divider>
          <div class="replacement-items-header">
            <el-button type="primary" size="small" @click="addReplacementItem">添加明细</el-button>
          </div>
          <el-table :data="deviceProcessForm.replacementItems" border size="small" style="width: 100%">
            <el-table-column label="类型" width="100">
              <template #default="{ row }">
                <el-select v-model="row.itemType" placeholder="类型" size="small" style="width: 80px">
                  <el-option v-for="t in replacementItemTypeOptions" :key="t.value" :label="t.label" :value="t.value" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="旧设备" width="140">
              <template #default="{ row }">
                <el-select v-if="row.itemType === 2" v-model="row.oldDeviceId" filterable remote reserve-keyword
                  placeholder="搜索设备" size="small" :remote-method="handleOldDeviceSearch"
                  style="width: 120px">
                  <el-option v-for="d in row._oldDeviceOptions || []" :key="d.id" :label="d.deviceCode" :value="d.id" />
                </el-select>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="旧件名称" min-width="100">
              <template #default="{ row }">
                <el-input v-model="row.oldItemName" placeholder="名称" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="旧件型号" min-width="100">
              <template #default="{ row }">
                <el-input v-model="row.oldItemModel" placeholder="型号" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="旧件状态" width="100">
              <template #default="{ row }">
                <el-select v-model="row.oldItemStatus" placeholder="状态" size="small" style="width: 80px">
                  <el-option v-for="s in oldItemStatusOptions" :key="s.value" :label="s.label" :value="s.value" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="新设备" width="140">
              <template #default="{ row }">
                <el-select v-if="row.itemType === 2" v-model="row.newDeviceId" filterable remote reserve-keyword
                  placeholder="搜索设备" size="small" :remote-method="(q) => handleNewDeviceSearch(row, q)"
                  style="width: 120px">
                  <el-option v-for="d in row._newDeviceOptions || []" :key="d.id" :label="d.deviceCode" :value="d.id" />
                </el-select>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="新件名称" min-width="100">
              <template #default="{ row }">
                <el-input v-model="row.newItemName" placeholder="名称" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="新件型号" min-width="100">
              <template #default="{ row }">
                <el-input v-model="row.newItemModel" placeholder="型号" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="数量" width="90">
              <template #default="{ row }">
                <el-input-number v-model="row.newItemQty" :min="1" size="small" style="width: 70px" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="70" fixed="right">
              <template #default="{ $index }">
                <el-button size="small" type="danger" link @click="removeReplacementItem($index)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="deviceProcessDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleDeviceProcessSubmit" :loading="submitLoading">提交处理</el-button>
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
import { deviceRepairApi, replacementTypeOptions, replacementTypeLabel, oldItemStatusOptions, oldItemStatusLabel, replacementItemTypeOptions } from '@/api/deviceRepair'
import request from '@/utils/request'

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
  repairType: '',
  deviceCode: '',
  faultPart: ''
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
      repairType: filterForm.repairType || undefined,
      deviceCode: filterForm.deviceCode || undefined,
      faultPart: filterForm.faultPart || undefined
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
    repairType: '',
    deviceCode: '',
    faultPart: ''
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

    // 设备维修且含更换记录时，加载更换记录详情
    if (detailData.value.hasReplacement === 1 && detailData.value.replacementId) {
      try {
        const repRes = await deviceRepairApi.replacementDetail(detailData.value.replacementId)
        const rep = repRes.data || {}
        detailData.value.replacementType = rep.replacementType
        detailData.value.replacePerson = rep.replacePerson
        detailData.value.replaceReason = rep.replaceReason
        detailData.value.replacePhoto = rep.replacePhoto
        detailData.value.replacementItems = rep.items || []
      } catch (e) {
        console.error('加载更换记录失败', e)
      }
    }

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
    repairType: filterForm.repairType || undefined,
    deviceCode: filterForm.deviceCode || undefined,
    faultPart: filterForm.faultPart || undefined
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

// ========== 设备故障报修 ==========
const deviceOptions = ref([])
const deviceSearchLoading = ref(false)
const deviceRepairDialogVisible = ref(false)
const deviceRepairFormRef = ref(null)
const repairPhotoBeforeList = ref([])

const deviceRepairForm = reactive({
  deviceId: null,
  deviceCode: '',
  deviceName: '',
  faultTime: '',
  faultPart: '',
  faultDescription: '',
  repairPhotoBefore: '',
  urgency: '普通',
  remark: ''
})

const deviceRepairRules = {
  deviceId: [{ required: true, message: '请选择设备', trigger: 'change' }],
  faultTime: [{ required: true, message: '请选择故障时间', trigger: 'change' }],
  faultPart: [{ required: true, message: '请输入故障部位', trigger: 'blur' }],
  faultDescription: [{ required: true, message: '请输入故障描述', trigger: 'blur' }],
  urgency: [{ required: true, message: '请选择紧急程度', trigger: 'change' }]
}

const pendingPhotos = reactive({
  repairPhotoBefore: [],
  repairPhotoAfter: [],
  replacePhoto: []
})

function handlePhotoChange(file, field) {
  if (file.raw) {
    pendingPhotos[field].push(file.raw)
  }
}

function handlePhotoRemove(file, field) {
  // 仅移除被删除的文件，而非清空全部
  const fileName = file?.name
  if (fileName) {
    pendingPhotos[field] = pendingPhotos[field].filter(f => f.name !== fileName)
  } else {
    pendingPhotos[field] = []
  }
  // 同步更新 file-list 引用，使 UI 正确刷新
  if (field === 'repairPhotoBefore') repairPhotoBeforeList.value = repairPhotoBeforeList.value.filter(f => f.name !== fileName)
  else if (field === 'repairPhotoAfter') repairPhotoAfterList.value = repairPhotoAfterList.value.filter(f => f.name !== fileName)
  else if (field === 'replacePhoto') replacePhotoList.value = replacePhotoList.value.filter(f => f.name !== fileName)
}

async function uploadPhotos(files, field) {
  const urls = []
  for (const file of files) {
    try {
      const fd = new FormData()
      fd.append('file', file)
      const res = await request.post('/purchase/upload-file', fd, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
      if (res.data) urls.push(res.data)
    } catch (e) {
      console.error(`${field}照片上传失败`, e)
    }
  }
  return urls.join(',')
}

async function handleDeviceSearch(query) {
  if (!query) {
    deviceOptions.value = []
    return
  }
  deviceSearchLoading.value = true
  try {
    const res = await deviceRepairApi.searchDevice({ keyword: query })
    deviceOptions.value = res.data || []
  } catch (e) {
    console.error(e)
  } finally {
    deviceSearchLoading.value = false
  }
}

function handleDeviceSelect(deviceId) {
  const device = deviceOptions.value.find(d => d.id === deviceId)
  if (device) {
    deviceRepairForm.deviceCode = device.deviceCode
    deviceRepairForm.deviceName = device.deviceName
  }
}

function handleAddDeviceRepair() {
  Object.assign(deviceRepairForm, {
    deviceId: null,
    deviceCode: '',
    deviceName: '',
    faultTime: '',
    faultPart: '',
    faultDescription: '',
    repairPhotoBefore: '',
    urgency: '普通',
    remark: ''
  })
  deviceOptions.value = []
  repairPhotoBeforeList.value = []
  pendingPhotos.repairPhotoBefore = []
  deviceRepairDialogVisible.value = true
}

async function handleDeviceRepairSubmit() {
  try {
    await deviceRepairFormRef.value.validate()
  } catch {
    return
  }

  submitLoading.value = true
  try {
    let photoUrl = deviceRepairForm.repairPhotoBefore
    if (pendingPhotos.repairPhotoBefore.length > 0) {
      photoUrl = await uploadPhotos(pendingPhotos.repairPhotoBefore, 'repairPhotoBefore')
    }

    await deviceRepairApi.deviceRepair({
      deviceId: deviceRepairForm.deviceId,
      deviceCode: deviceRepairForm.deviceCode,
      deviceName: deviceRepairForm.deviceName,
      faultTime: deviceRepairForm.faultTime,
      faultPart: deviceRepairForm.faultPart,
      faultDescription: deviceRepairForm.faultDescription,
      repairPhotoBefore: photoUrl,
      urgency: deviceRepairForm.urgency,
      remark: deviceRepairForm.remark
    })
    ElMessage.success('设备报修提交成功')
    deviceRepairDialogVisible.value = false
    fetchList()
  } catch (e) {
    console.error(e)
  } finally {
    submitLoading.value = false
  }
}

// ========== 设备维修处理（含更换记录） ==========
const deviceProcessDialogVisible = ref(false)
const deviceProcessFormRef = ref(null)
const currentDeviceProcessId = ref(null)
const repairPhotoAfterList = ref([])
const replacePhotoList = ref([])

const deviceProcessForm = reactive({
  processMethod: '',
  faultReason: '',
  repairStartTime: '',
  repairEndTime: '',
  repairDuration: null,
  repairPhotoAfter: '',
  hasReplacement: 0,
  replacementType: null,
  replacePerson: '',
  replaceReason: '',
  replacePhoto: '',
  replacementItems: []
})

const deviceProcessRules = {
  processMethod: [{ required: true, message: '请输入处理方式', trigger: 'blur' }],
  faultReason: [{ required: true, message: '请输入故障原因', trigger: 'blur' }]
}

function createEmptyReplacementItem() {
  return {
    itemType: 1,
    oldDeviceId: null,
    oldItemName: '',
    oldItemModel: '',
    oldItemStatus: null,
    newDeviceId: null,
    newItemName: '',
    newItemModel: '',
    newItemQty: 1,
    _oldDeviceOptions: [],
    _newDeviceOptions: []
  }
}

function addReplacementItem() {
  deviceProcessForm.replacementItems.push(createEmptyReplacementItem())
}

function removeReplacementItem(index) {
  deviceProcessForm.replacementItems.splice(index, 1)
}

async function handleOldDeviceSearch(query) {
  if (!query) return
  try {
    const res = await deviceRepairApi.searchDevice({ keyword: query })
    deviceProcessForm.replacementItems.forEach(item => {
      item._oldDeviceOptions = res.data || []
    })
  } catch (e) {
    console.error(e)
  }
}

async function handleNewDeviceSearch(row, query) {
  if (!query) return
  try {
    const res = await deviceRepairApi.searchDevice({ keyword: query })
    row._newDeviceOptions = res.data || []
  } catch (e) {
    console.error(e)
  }
}

function isDeviceRepair(row) {
  return !!row.deviceId
}

function canProcessDevice(status) {
  return status === '待分配' || status === '已分配' || status === '处理中'
}

function handleDeviceProcess(row) {
  currentDeviceProcessId.value = row.id
  Object.assign(deviceProcessForm, {
    processMethod: '',
    faultReason: '',
    repairStartTime: '',
    repairEndTime: '',
    repairDuration: null,
    repairPhotoAfter: '',
    hasReplacement: 0,
    replacementType: null,
    replacePerson: '',
    replaceReason: '',
    replacePhoto: '',
    replacementItems: []
  })
  repairPhotoAfterList.value = []
  replacePhotoList.value = []
  pendingPhotos.repairPhotoAfter = []
  pendingPhotos.replacePhoto = []
  deviceProcessDialogVisible.value = true
}

async function handleDeviceProcessSubmit() {
  try {
    await deviceProcessFormRef.value.validate()
  } catch {
    return
  }

  if (deviceProcessForm.hasReplacement === 1 && deviceProcessForm.replacementItems.length === 0) {
    ElMessage.warning('请至少添加一条更换明细')
    return
  }

  submitLoading.value = true
  try {
    let afterPhotoUrl = deviceProcessForm.repairPhotoAfter
    if (pendingPhotos.repairPhotoAfter.length > 0) {
      afterPhotoUrl = await uploadPhotos(pendingPhotos.repairPhotoAfter, 'repairPhotoAfter')
    }

    let replacePhotoUrl = deviceProcessForm.replacePhoto
    if (deviceProcessForm.hasReplacement === 1 && pendingPhotos.replacePhoto.length > 0) {
      replacePhotoUrl = await uploadPhotos(pendingPhotos.replacePhoto, 'replacePhoto')
    }

    const data = {
      processMethod: deviceProcessForm.processMethod,
      faultReason: deviceProcessForm.faultReason,
      repairStartTime: deviceProcessForm.repairStartTime || undefined,
      repairEndTime: deviceProcessForm.repairEndTime || undefined,
      repairDuration: deviceProcessForm.repairDuration || undefined,
      repairPhotoAfter: afterPhotoUrl || undefined,
      hasReplacement: deviceProcessForm.hasReplacement
    }

    if (deviceProcessForm.hasReplacement === 1) {
      data.replacementType = deviceProcessForm.replacementType
      data.replacePerson = deviceProcessForm.replacePerson
      data.replaceReason = deviceProcessForm.replaceReason
      data.replacePhoto = replacePhotoUrl
      data.replacementItems = deviceProcessForm.replacementItems.map(item => ({
        itemType: item.itemType,
        oldDeviceId: item.itemType === 2 ? item.oldDeviceId : undefined,
        oldItemName: item.oldItemName,
        oldItemModel: item.oldItemModel,
        oldItemStatus: item.oldItemStatus,
        newDeviceId: item.itemType === 2 ? item.newDeviceId : undefined,
        newItemName: item.newItemName,
        newItemModel: item.newItemModel,
        newItemQty: item.newItemQty
      }))
    }

    await deviceRepairApi.deviceProcess(currentDeviceProcessId.value, data)
    ElMessage.success('维修处理提交成功')
    deviceProcessDialogVisible.value = false
    fetchList()
  } catch (e) {
    console.error(e)
  } finally {
    submitLoading.value = false
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

.replacement-items-header {
  margin-bottom: 10px;
}

.photo-preview {
  display: flex;
  justify-content: flex-start;
  padding: 8px 0;
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
