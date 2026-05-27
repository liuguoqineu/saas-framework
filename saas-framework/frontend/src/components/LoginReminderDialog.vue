<template>
  <el-dialog
    v-model="visible"
    title="待办提醒"
    width="680px"
    :close-on-click-modal="false"
    :show-close="true"
    @close="handleClose"
  >
    <div class="reminder-summary">
      <el-alert
        :title="`您有 ${totalCount} 条待处理事项需要关注`"
        type="warning"
        :closable="false"
        show-icon
      />
    </div>

    <el-tabs v-model="activeTab" style="margin-top: 16px">
      <el-tab-pane v-if="contractReminders !== null" label="合同到期" name="contract">
        <div v-if="contractReminders.length === 0" class="empty-text">暂无合同到期提醒</div>
        <div v-else class="reminder-list">
          <div
            v-for="item in contractReminders"
            :key="item.id"
            class="reminder-item contract"
            :class="{ 'is-mine': item.isMine === 1 }"
            @click="handleItemClick(item)"
          >
            <div class="item-header">
              <div class="item-tags">
                <el-tag type="danger" size="small">到期</el-tag>
                <el-tag v-if="item.isMine === 1" type="success" size="small">我的</el-tag>
              </div>
              <span class="item-time">{{ formatTime(item.time) }}</span>
            </div>
            <div class="item-content">{{ item.content }}</div>
            <div class="item-person">负责人：{{ item.person || '未指定' }}</div>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane v-if="followUpReminders !== null" label="客户跟进" name="followUp">
        <div v-if="followUpReminders.length === 0" class="empty-text">暂无跟进提醒</div>
        <div v-else class="reminder-list">
          <div
            v-for="item in followUpReminders"
            :key="item.id"
            class="reminder-item followup"
            :class="{ 'is-mine': item.isMine === 1 }"
            @click="handleItemClick(item)"
          >
            <div class="item-header">
              <div class="item-tags">
                <el-tag type="warning" size="small">跟进</el-tag>
                <el-tag v-if="item.isMine === 1" type="success" size="small">我的</el-tag>
              </div>
              <span class="item-time">{{ formatTime(item.time) }}</span>
            </div>
            <div class="item-content">{{ item.content }}</div>
            <div class="item-person">跟进人：{{ item.person || '未指定' }}</div>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane v-if="repairReminders !== null" label="报修处理" name="repair">
        <div v-if="repairReminders.length === 0" class="empty-text">暂无报修提醒</div>
        <div v-else class="reminder-list">
          <div
            v-for="item in repairReminders"
            :key="item.id"
            class="reminder-item repair"
            :class="{ 'is-mine': item.isMine === 1 }"
            @click="handleItemClick(item)"
          >
            <div class="item-header">
              <div class="item-tags">
                <el-tag type="primary" size="small">待处理</el-tag>
                <el-tag v-if="item.isMine === 1" type="success" size="small">我的</el-tag>
              </div>
              <span class="item-time">{{ formatTime(item.time) }}</span>
            </div>
            <div class="item-content">{{ item.content }}</div>
            <div class="item-person">处理人：{{ item.person || '未指定' }}</div>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <template #footer>
      <el-button @click="handleClose">稍后查看</el-button>
      <el-button type="primary" @click="handleViewAll">查看全部</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const emit = defineEmits(['close'])
const router = useRouter()

const visible = ref(false)
const activeTab = ref('contract')
const reminderData = ref({
  totalCount: 0,
  contractReminders: [],
  followUpReminders: [],
  repairReminders: []
})

const totalCount = computed(() => reminderData.value.totalCount)
const contractReminders = computed(() => reminderData.value.contractReminders)
const followUpReminders = computed(() => reminderData.value.followUpReminders)
const repairReminders = computed(() => reminderData.value.repairReminders)

function open(data) {
  reminderData.value = data || { totalCount: 0, contractReminders: null, followUpReminders: null, repairReminders: null }
  if (totalCount.value > 0) {
    if (contractReminders.value && contractReminders.value.length > 0) {
      activeTab.value = 'contract'
    } else if (followUpReminders.value && followUpReminders.value.length > 0) {
      activeTab.value = 'followUp'
    } else if (repairReminders.value && repairReminders.value.length > 0) {
      activeTab.value = 'repair'
    } else if (contractReminders.value !== null) {
      activeTab.value = 'contract'
    } else if (followUpReminders.value !== null) {
      activeTab.value = 'followUp'
    } else if (repairReminders.value !== null) {
      activeTab.value = 'repair'
    }
    visible.value = true
  }
}

function formatTime(time) {
  if (!time) return ''
  const date = new Date(time)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

function handleClose() {
  visible.value = false
  emit('close')
}

function handleViewAll() {
  visible.value = false
  emit('close')
  if (activeTab.value === 'contract') {
    router.push('/contract')
  } else if (activeTab.value === 'followUp') {
    router.push('/customer')
  } else if (activeTab.value === 'repair') {
    router.push('/repair')
  }
}

function handleItemClick(item) {
  visible.value = false
  emit('close')
  if (item.type === 'CONTRACT' && item.relatedId) {
    router.push(`/contract`)
  } else if (item.type === 'FOLLOW_UP' && item.relatedId) {
    router.push(`/customer/${item.relatedId}`)
  } else if (item.type === 'REPAIR' && item.relatedId) {
    router.push(`/repair`)
  }
}

defineExpose({ open })
</script>

<style scoped>
.reminder-summary {
  margin-bottom: 8px;
}

.empty-text {
  text-align: center;
  color: #909399;
  padding: 40px 0;
  font-size: 14px;
}

.reminder-list {
  max-height: 360px;
  overflow-y: auto;
}

.reminder-item {
  padding: 12px;
  margin-bottom: 8px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  border-left: 3px solid transparent;
}

.reminder-item:hover {
  background: #f5f7fa;
}

.reminder-item.contract {
  background: #fef0f0;
  border-left-color: #f56c6c;
}

.reminder-item.followup {
  background: #fdf6ec;
  border-left-color: #e6a23c;
}

.reminder-item.repair {
  background: #ecf5ff;
  border-left-color: #409eff;
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.item-tags {
  display: flex;
  gap: 6px;
  align-items: center;
}

.item-time {
  font-size: 12px;
  color: #909399;
}

.item-content {
  font-size: 14px;
  color: #303133;
  margin-bottom: 4px;
  line-height: 1.5;
}

.item-person {
  font-size: 12px;
  color: #909399;
}

.reminder-item.is-mine {
  border-left-width: 4px;
  box-shadow: 0 2px 8px rgba(103, 194, 58, 0.15);
}
</style>
