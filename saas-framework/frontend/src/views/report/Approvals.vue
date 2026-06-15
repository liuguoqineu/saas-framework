<template>
  <div class="pending-approvals">
    <el-card>
      <template #header><span>待审批列表</span></template>
      <el-table :data="approvals" v-loading="loading" stripe border style="width: 100%" :header-cell-style="{ textAlign: 'center' }" :cell-style="{ textAlign: 'center' }">
        <el-table-column prop="id" label="编号" min-width="80"/>
        <el-table-column prop="reportId" label="报表编号" min-width="100"/>
        <el-table-column prop="approvalLevel" label="审批级别" min-width="100">
          <template #default="{row}">
            <el-tag type="primary" size="small">第{{ row.approvalLevel }}级</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" min-width="100">
          <template #default="{row}">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="170"/>
        <el-table-column label="操作" min-width="280" fixed="right">
          <template #default="{row}">
            <template v-if="row.status==='PENDING'">
              <el-button size="small" type="success" @click="approve(row.id)">通过</el-button>
              <el-button size="small" type="danger" @click="showReject(row.id)">驳回</el-button>
              <el-button size="small" @click="viewChain(row.reportId)">审批链</el-button>
            </template>
            <template v-else>
              <el-button size="small" @click="viewChain(row.reportId)">审批链</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="rejectVisible" title="驳回原因" width="400px">
      <el-input v-model="rejectComment" type="textarea" :rows="3" placeholder="请输入驳回原因"/>
      <template #footer><el-button @click="rejectVisible=false">取消</el-button><el-button type="danger" @click="doReject">确认驳回</el-button></template>
    </el-dialog>

    <el-dialog v-model="chainVisible" title="审批链" width="600px">
      <el-steps :active="chainActiveStep" align-center>
        <el-step v-for="(item, idx) in chainData" :key="idx"
          :title="'第' + item.approvalLevel + '级'"
          :description="item.approverName || '未知'"
          :status="stepStatus(item.status)"/>
      </el-steps>
      <el-table :data="chainData" style="margin-top: 20px" stripe border size="small" :header-cell-style="{ textAlign: 'center' }" :cell-style="{ textAlign: 'center' }">
        <el-table-column prop="approvalLevel" label="级别" min-width="80">
          <template #default="{row}">第{{ row.approvalLevel }}级</template>
        </el-table-column>
        <el-table-column prop="approverName" label="审批人" min-width="100"/>
        <el-table-column prop="status" label="状态" min-width="100">
          <template #default="{row}">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="comment" label="意见" min-width="150">
          <template #default="{row}">{{ row.comment || '-' }}</template>
        </el-table-column>
        <el-table-column prop="approveTime" label="审批时间" min-width="170">
          <template #default="{row}">{{ row.approveTime || '-' }}</template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { reportApi } from '@/api/report'

const loading = ref(false)
const approvals = ref([])
const rejectVisible = ref(false)
const rejectComment = ref('')
const rejectId = ref(null)
const chainVisible = ref(false)
const chainData = ref([])

const statusLabel = (s) => ({
  PENDING: '待审批', APPROVED: '已通过', REJECTED: '已驳回', SUPERSEDED: '已作废'
}[s] || s)

const statusTagType = (s) => ({
  PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', SUPERSEDED: 'info'
}[s] || 'info')

const stepStatus = (s) => ({
  PENDING: 'process', APPROVED: 'finish', REJECTED: 'error', SUPERSEDED: 'wait'
}[s] || 'wait')

const chainActiveStep = computed(() => {
  if (!chainData.value.length) return 0
  const lastProcessed = chainData.value.filter(i => i.status === 'APPROVED').length
  const hasRejected = chainData.value.some(i => i.status === 'REJECTED')
  if (hasRejected) return chainData.value.findIndex(i => i.status === 'REJECTED')
  return lastProcessed
})

const fetchData = async () => { loading.value = true; try { const res = await reportApi.getPendingApprovals(); approvals.value = res.data } finally { loading.value = false } }

const approve = async (id) => {
  try { await ElMessageBox.confirm('确认通过该报表？', '提示'); await reportApi.approve(id); ElMessage.success('已通过'); fetchData() } catch (e) { /* cancel */ }
}

const showReject = (id) => { rejectId.value = id; rejectComment.value = ''; rejectVisible.value = true }

const doReject = async () => {
  if (!rejectComment.value) { ElMessage.warning('请输入驳回原因'); return }
  try { await reportApi.reject(rejectId.value, { comment: rejectComment.value }); ElMessage.success('已驳回'); rejectVisible.value = false; fetchData() } catch (e) { /* handled */ }
}

const viewChain = async (reportId) => {
  try {
    const res = await reportApi.getApprovalChain(reportId)
    chainData.value = res.data || []
    chainVisible.value = true
  } catch (e) { /* handled */ }
}

onMounted(() => fetchData())
</script>
