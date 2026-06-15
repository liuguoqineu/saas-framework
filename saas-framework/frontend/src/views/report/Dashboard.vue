<template>
  <div class="dashboard">
    <el-row :gutter="16">
      <el-col :span="12">
        <el-card><template #header><span>填报率</span></template>
          <div v-if="overview.fillRate" style="text-align:center">
            <div style="font-size:48px;color:#409EFF">{{ overview.fillRate.rate }}%</div>
            <div style="color:#909399">{{ overview.fillRate.filled }} / {{ overview.fillRate.total }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card><template #header><span>审批通过率</span></template>
          <div v-if="overview.approvalRate" style="text-align:center">
            <div style="font-size:48px;color:#67C23A">{{ overview.approvalRate.rate }}%</div>
            <div style="color:#909399">{{ overview.approvalRate.approved }} / {{ overview.approvalRate.submitted }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top:16px"><template #header><span>岗位类型看板</span></template>
      <el-row :gutter="16">
        <el-col :span="8" v-for="pt in postTypes" :key="pt.value">
          <el-card shadow="hover" @click="loadPostDashboard(pt.value)">
            <div style="text-align:center"><div style="font-size:16px">{{ pt.label }}</div><div style="font-size:32px;color:#E6A23C">{{ postData[pt.value] ? postData[pt.value].total : '-' }}</div></div>
          </el-card>
        </el-col>
      </el-row>
    </el-card>

    <el-card style="margin-top:16px"><template #header><span>逾期列表</span></template>
      <el-table :data="overview.overdueList" size="small">
        <el-table-column prop="userName" label="姓名"/>
        <el-table-column prop="reportType" label="类型" width="100"/>
        <el-table-column prop="reportPeriod" label="周期" width="140"/>
        <el-table-column prop="deadline" label="截止日期" width="180"/>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { reportApi } from '@/api/report'

const overview = ref({})
const postData = reactive({})
const postTypes = [{ value: 'DEV', label: '研发' }, { value: 'OPS', label: '运维' }, { value: 'CS', label: '客服' }]

const loadDashboard = async () => { try { const res = await reportApi.getDashboardOverview(); overview.value = res.data } catch (e) { /* ignore */ } }

const loadPostDashboard = async (pt) => { try { const res = await reportApi.getDashboardByPost(pt); postData[pt] = res.data } catch (e) { /* ignore */ } }

onMounted(() => { loadDashboard(); postTypes.forEach(pt => loadPostDashboard(pt.value)) })
</script>