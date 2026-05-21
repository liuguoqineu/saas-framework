<template>
  <div class="statistics-container">
    <el-card class="filter-card">
      <el-form :inline="true" :model="filterForm" class="filter-form">
        <el-form-item label="统计时间">
          <el-date-picker v-model="filterForm.dateRange" type="daterange" range-separator="至"
            start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD"
            style="width: 260px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="客户统计" name="customer">
          <div class="tab-header">
            <span class="tab-title">客户统计分析</span>
            <el-button @click="handleExport('customer')">导出Excel</el-button>
          </div>

          <el-row :gutter="16" style="margin-bottom: 20px">
            <el-col :span="6">
              <el-statistic title="客户总数" :value="customerData.totalCount" />
            </el-col>
            <el-col :span="6">
              <el-statistic title="有效客户" :value="customerData.validCount" />
            </el-col>
            <el-col :span="6">
              <el-statistic title="无效客户" :value="customerData.invalidCount" />
            </el-col>
            <el-col :span="6">
              <el-statistic title="有效占比" :value="customerData.totalCount > 0 ? ((customerData.validCount / customerData.totalCount) * 100).toFixed(1) + '%' : '0%'" />
            </el-col>
          </el-row>

          <el-divider content-position="left">有效客户统计</el-divider>
          <el-row :gutter="16" style="margin-bottom: 16px">
            <el-col :span="12">
              <div ref="validCategoryChartRef" class="chart-box"></div>
            </el-col>
            <el-col :span="12">
              <div ref="validStatusChartRef" class="chart-box"></div>
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12">
              <div ref="validRegionChartRef" class="chart-box"></div>
            </el-col>
            <el-col :span="12">
              <div ref="validTypeChartRef" class="chart-box"></div>
            </el-col>
          </el-row>

          <el-divider content-position="left">无效客户统计</el-divider>
          <el-row :gutter="16" style="margin-bottom: 16px">
            <el-col :span="12">
              <div ref="invalidCategoryChartRef" class="chart-box"></div>
            </el-col>
            <el-col :span="12">
              <div ref="invalidStatusChartRef" class="chart-box"></div>
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12">
              <div ref="invalidRegionChartRef" class="chart-box"></div>
            </el-col>
            <el-col :span="12">
              <div ref="invalidTypeChartRef" class="chart-box"></div>
            </el-col>
          </el-row>
        </el-tab-pane>

        <el-tab-pane label="报修统计" name="repair">
          <div class="tab-header">
            <span class="tab-title">报修统计分析</span>
            <el-button @click="handleExport('repair')">导出Excel</el-button>
          </div>
          <el-row :gutter="16" style="margin-bottom: 20px">
            <el-col :span="4">
              <el-statistic title="报修总量" :value="repairData.totalCount" />
            </el-col>
            <el-col :span="4">
              <el-statistic title="已解决" :value="repairData.resolvedCount" />
            </el-col>
            <el-col :span="4">
              <el-statistic title="未处理" :value="repairData.unresolvedCount" />
            </el-col>
            <el-col :span="4">
              <el-statistic title="处理中" :value="repairData.processingCount" />
            </el-col>
            <el-col :span="4">
              <el-statistic title="异常数" :value="repairData.exceptionCount" />
            </el-col>
            <el-col :span="4">
              <el-statistic title="解决率" :value="repairData.totalCount > 0 ? ((repairData.resolvedCount / repairData.totalCount) * 100).toFixed(1) + '%' : '0%'" />
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12">
              <div ref="repairTrendChartRef" class="chart-box"></div>
            </el-col>
            <el-col :span="12">
              <div ref="repairFaultTypeChartRef" class="chart-box"></div>
            </el-col>
          </el-row>
          <el-row :gutter="16" style="margin-top: 16px">
            <el-col :span="12">
              <div ref="repairCustomerTypeChartRef" class="chart-box"></div>
            </el-col>
            <el-col :span="12">
              <h4 class="chart-title">高频报修客户 TOP10</h4>
              <el-table :data="repairData.highFrequencyCustomers" stripe border size="small" max-height="350">
                <el-table-column type="index" label="排名" width="60" />
                <el-table-column prop="name" label="客户名称" />
                <el-table-column prop="value" label="报修次数" width="100" />
              </el-table>
              <el-empty v-if="!repairData.highFrequencyCustomers || repairData.highFrequencyCustomers.length === 0" description="暂无数据" />
            </el-col>
          </el-row>
        </el-tab-pane>

        <el-tab-pane label="拜访统计" name="visit">
          <div class="tab-header">
            <span class="tab-title">拜访统计分析</span>
            <el-button @click="handleExport('visit')">导出Excel</el-button>
          </div>
          <el-row :gutter="16" style="margin-bottom: 20px">
            <el-col :span="4">
              <el-statistic title="拜访总数" :value="visitData.totalVisits" />
            </el-col>
            <el-col :span="4">
              <el-statistic title="已完成" :value="visitData.completedVisits" />
            </el-col>
            <el-col :span="4">
              <el-statistic title="完成率" :value="visitData.completionRate + '%'" />
            </el-col>
            <el-col :span="4">
              <el-statistic title="覆盖客户数" :value="visitData.coveredCustomers" />
            </el-col>
            <el-col :span="4">
              <el-statistic title="客户总数" :value="visitData.totalCustomers" />
            </el-col>
            <el-col :span="4">
              <el-statistic title="覆盖率" :value="visitData.coverageRate + '%'" />
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12">
              <div ref="visitPersonChartRef" class="chart-box"></div>
            </el-col>
            <el-col :span="12">
              <div ref="visitMethodChartRef" class="chart-box"></div>
            </el-col>
          </el-row>
          <el-row :gutter="16" style="margin-top: 16px">
            <el-col :span="12">
              <div ref="visitTrendChartRef" class="chart-box"></div>
            </el-col>
            <el-col :span="12">
              <div ref="visitCustomerTypeChartRef" class="chart-box"></div>
            </el-col>
          </el-row>
        </el-tab-pane>

        <el-tab-pane label="合同统计" name="contract">
          <div class="tab-header">
            <span class="tab-title">合同统计分析</span>
            <el-button @click="handleExport('contract')">导出Excel</el-button>
          </div>
          <el-row :gutter="16" style="margin-bottom: 20px">
            <el-col :span="6">
              <el-statistic title="合同总数" :value="contractData.totalCount" />
            </el-col>
            <el-col :span="6">
              <el-statistic title="已生效" :value="contractData.activeCount" />
            </el-col>
            <el-col :span="6">
              <el-statistic title="已终止" :value="contractData.terminatedCount" />
            </el-col>
            <el-col :span="6">
              <el-statistic title="履约率" :value="contractData.totalCount > 0 ? ((contractData.activeCount / contractData.totalCount) * 100).toFixed(1) + '%' : '0%'" />
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12">
              <div ref="contractTypeChartRef" class="chart-box"></div>
            </el-col>
            <el-col :span="12">
              <div ref="contractStatusChartRef" class="chart-box"></div>
            </el-col>
          </el-row>
          <el-row :gutter="16" style="margin-top: 16px">
            <el-col :span="12">
              <div ref="contractTrendChartRef" class="chart-box"></div>
            </el-col>
            <el-col :span="12">
              <div ref="contractRevenueChartRef" class="chart-box"></div>
            </el-col>
          </el-row>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { statisticsApi } from '@/api/statistics'

const activeTab = ref('customer')
const filterForm = reactive({
  dateRange: null
})

const customerData = reactive({
  totalCount: 0,
  validCount: 0,
  invalidCount: 0,
  validCustomers: {
    byBusinessCategory: [],
    byCooperationStatus: [],
    byRegion: [],
    byBusinessType: []
  },
  invalidCustomers: {
    byBusinessCategory: [],
    byCooperationStatus: [],
    byRegion: [],
    byBusinessType: []
  }
})

const repairData = reactive({
  totalCount: 0,
  resolvedCount: 0,
  unresolvedCount: 0,
  processingCount: 0,
  exceptionCount: 0,
  byMonth: [],
  byCustomerType: [],
  byFaultType: [],
  highFrequencyCustomers: [],
  highFrequencyFaultTypes: []
})

const visitData = reactive({
  totalVisits: 0,
  completedVisits: 0,
  completionRate: 0,
  coveredCustomers: 0,
  totalCustomers: 0,
  coverageRate: 0,
  byPerson: [],
  byMethod: [],
  byMonth: [],
  byCustomerType: []
})

const contractData = reactive({
  totalCount: 0,
  activeCount: 0,
  terminatedCount: 0,
  byType: [],
  byStatus: [],
  byMonth: [],
  revenueByMonth: []
})

const validCategoryChartRef = ref(null)
const validStatusChartRef = ref(null)
const validRegionChartRef = ref(null)
const validTypeChartRef = ref(null)
const invalidCategoryChartRef = ref(null)
const invalidStatusChartRef = ref(null)
const invalidRegionChartRef = ref(null)
const invalidTypeChartRef = ref(null)
const repairTrendChartRef = ref(null)
const repairFaultTypeChartRef = ref(null)
const repairCustomerTypeChartRef = ref(null)
const visitPersonChartRef = ref(null)
const visitMethodChartRef = ref(null)
const visitTrendChartRef = ref(null)
const visitCustomerTypeChartRef = ref(null)
const contractTypeChartRef = ref(null)
const contractStatusChartRef = ref(null)
const contractTrendChartRef = ref(null)
const contractRevenueChartRef = ref(null)

const chartInstances = []

function getParams() {
  return {
    startDate: filterForm.dateRange?.[0] || undefined,
    endDate: filterForm.dateRange?.[1] || undefined
  }
}

function initChart(domRef) {
  if (!domRef) return null
  const instance = echarts.init(domRef)
  chartInstances.push(instance)
  return instance
}

function disposeCharts() {
  chartInstances.forEach(c => c && c.dispose())
  chartInstances.length = 0
}

function renderBarChart(domRef, title, data, color) {
  const instance = initChart(domRef)
  if (!instance) return
  instance.setOption({
    title: { text: title, left: 'center', textStyle: { fontSize: 14 } },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: data.map(d => d.name), axisLabel: { rotate: data.length > 6 ? 30 : 0 } },
    yAxis: { type: 'value' },
    series: [{
      type: 'bar',
      data: data.map(d => d.value),
      itemStyle: { color: color || '#409EFF' },
      barMaxWidth: 40
    }],
    grid: { left: 40, right: 20, bottom: 40, top: 50 }
  })
}

function renderPieChart(domRef, title, data) {
  const instance = initChart(domRef)
  if (!instance) return
  instance.setOption({
    title: { text: title, left: 'center', textStyle: { fontSize: 14 } },
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { orient: 'vertical', left: 'left', top: 'middle', type: 'scroll' },
    series: [{
      type: 'pie',
      radius: ['35%', '65%'],
      center: ['55%', '55%'],
      data: data.map(d => ({ name: d.name, value: d.value })),
      emphasis: { itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0,0,0,0.5)' } },
      label: { formatter: '{b}\n{d}%' }
    }]
  })
}

function renderLineChart(domRef, title, data, color) {
  const instance = initChart(domRef)
  if (!instance) return
  instance.setOption({
    title: { text: title, left: 'center', textStyle: { fontSize: 14 } },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: data.map(d => d.name), axisLabel: { rotate: data.length > 8 ? 30 : 0 } },
    yAxis: { type: 'value' },
    series: [{
      type: 'line',
      data: data.map(d => d.value),
      smooth: true,
      itemStyle: { color: color || '#67C23A' },
      areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
        { offset: 0, color: color ? color + '40' : 'rgba(103,194,58,0.25)' },
        { offset: 1, color: 'rgba(255,255,255,0.1)' }
      ])}
    }],
    grid: { left: 40, right: 20, bottom: 40, top: 50 }
  })
}

async function fetchCustomerStats() {
  try {
    const res = await statisticsApi.customerStats(getParams())
    Object.assign(customerData, res.data || {})
    if (!customerData.validCustomers) customerData.validCustomers = { byBusinessCategory: [], byCooperationStatus: [], byRegion: [], byBusinessType: [] }
    if (!customerData.invalidCustomers) customerData.invalidCustomers = { byBusinessCategory: [], byCooperationStatus: [], byRegion: [], byBusinessType: [] }
    await nextTick()
    disposeCharts()
    renderBarChart(validCategoryChartRef.value, '有效客户-业务类型分布', customerData.validCustomers.byBusinessCategory, '#409EFF')
    renderPieChart(validStatusChartRef.value, '有效客户-合作状态分布', customerData.validCustomers.byCooperationStatus)
    renderBarChart(validRegionChartRef.value, '有效客户-区域分布', customerData.validCustomers.byRegion, '#E6A23C')
    renderPieChart(validTypeChartRef.value, '有效客户-业务细分类分布', customerData.validCustomers.byBusinessType)
    renderBarChart(invalidCategoryChartRef.value, '无效客户-业务类型分布', customerData.invalidCustomers.byBusinessCategory, '#909399')
    renderPieChart(invalidStatusChartRef.value, '无效客户-合作状态分布', customerData.invalidCustomers.byCooperationStatus)
    renderBarChart(invalidRegionChartRef.value, '无效客户-区域分布', customerData.invalidCustomers.byRegion, '#C0C4CC')
    renderPieChart(invalidTypeChartRef.value, '无效客户-业务细分类分布', customerData.invalidCustomers.byBusinessType)
  } catch (e) {
    console.error(e)
  }
}

async function fetchRepairStats() {
  try {
    const params = { ...getParams(), period: 'month' }
    const res = await statisticsApi.repairStats(params)
    Object.assign(repairData, res.data || {})
    await nextTick()
    disposeCharts()
    renderLineChart(repairTrendChartRef.value, '报修月度趋势', repairData.byMonth, '#F56C6C')
    renderPieChart(repairFaultTypeChartRef.value, '故障类型分布', repairData.byFaultType)
    renderBarChart(repairCustomerTypeChartRef.value, '客户类型报修分布', repairData.byCustomerType, '#E6A23C')
  } catch (e) {
    console.error(e)
  }
}

async function fetchVisitStats() {
  try {
    const res = await statisticsApi.visitStats(getParams())
    Object.assign(visitData, res.data || {})
    await nextTick()
    disposeCharts()
    renderBarChart(visitPersonChartRef.value, '拜访人员统计', visitData.byPerson, '#409EFF')
    renderPieChart(visitMethodChartRef.value, '拜访方式分布', visitData.byMethod)
    renderLineChart(visitTrendChartRef.value, '拜访月度趋势', visitData.byMonth, '#67C23A')
    renderBarChart(visitCustomerTypeChartRef.value, '客户类型拜访分布', visitData.byCustomerType, '#E6A23C')
  } catch (e) {
    console.error(e)
  }
}

async function fetchContractStats() {
  try {
    const res = await statisticsApi.contractStats(getParams())
    Object.assign(contractData, res.data || {})
    await nextTick()
    disposeCharts()
    renderPieChart(contractTypeChartRef.value, '合同类型分布', contractData.byType)
    renderPieChart(contractStatusChartRef.value, '合同状态分布', contractData.byStatus)
    renderLineChart(contractTrendChartRef.value, '合同签订月度趋势', contractData.byMonth, '#409EFF')
    renderBarChart(contractRevenueChartRef.value, '营收月度趋势', contractData.revenueByMonth, '#67C23A')
  } catch (e) {
    console.error(e)
  }
}

function handleTabChange(tab) {
  if (tab === 'customer') fetchCustomerStats()
  else if (tab === 'repair') fetchRepairStats()
  else if (tab === 'visit') fetchVisitStats()
  else if (tab === 'contract') fetchContractStats()
}

function handleQuery() {
  handleTabChange(activeTab.value)
}

function handleReset() {
  filterForm.dateRange = null
  handleQuery()
}

function handleExport(type) {
  const params = getParams()
  let url = ''
  let fileName = ''
  if (type === 'customer') {
    url = statisticsApi.getCustomerExportUrl(params)
    fileName = '客户统计.xlsx'
  } else if (type === 'repair') {
    url = statisticsApi.getRepairExportUrl({ ...params, period: 'month' })
    fileName = '报修统计.xlsx'
  } else if (type === 'visit') {
    url = statisticsApi.getVisitExportUrl(params)
    fileName = '拜访统计.xlsx'
  } else if (type === 'contract') {
    url = statisticsApi.getContractExportUrl(params)
    fileName = '合同统计.xlsx'
  }

  const token = localStorage.getItem('token')
  fetch(url, { headers: { Authorization: `Bearer ${token}` } })
    .then(res => res.blob())
    .then(blob => {
      const a = document.createElement('a')
      a.href = URL.createObjectURL(blob)
      a.download = fileName
      a.click()
      URL.revokeObjectURL(a.href)
    })
    .catch(() => ElMessage.error('导出失败'))
}

function handleResize() {
  chartInstances.forEach(c => c && c.resize())
}

onMounted(() => {
  fetchCustomerStats()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  disposeCharts()
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.statistics-container {
  padding: 0;
}

.filter-card {
  margin-bottom: 16px;
}

.filter-form {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
}

.tab-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.tab-title {
  font-size: 16px;
  font-weight: 600;
}

.chart-box {
  width: 100%;
  height: 350px;
}

.chart-title {
  margin: 0 0 10px;
  font-size: 14px;
  font-weight: 600;
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

  .chart-box {
    height: 280px;
  }
}
</style>
