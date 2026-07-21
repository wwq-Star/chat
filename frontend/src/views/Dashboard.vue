<template>
  <div class="dashboard">
    <h1>聊天记录分析系统</h1>
    
    <!-- 状态提示 -->
    <el-alert v-if="showMockDataWarning" title="提示：当前显示的是模拟数据，因为查询失败或数据为空" type="warning" show-icon :closable="false" style="margin-bottom: 20px;"></el-alert>
    
    <!-- 系统状态 -->
    <el-card class="system-status">
      <el-row :gutter="20">
        <el-col :span="8">
          <el-button type="primary" @click="testConnection" :loading="testingConnection">测试连接</el-button>
          <el-button type="success" @click="createTable" :loading="creatingTable">检查/创建表</el-button>
          <el-button type="info" @click="insertTestData" :loading="insertingData">插入测试数据</el-button>
        </el-col>
        <el-col :span="8">
          <span v-if="connectionStatus.success" style="color: #67C23A;">
            ✓ 数据库连接正常
          </span>
          <span v-else style="color: #F56C6C;">
            ✗ 数据库连接异常
          </span>
        </el-col>
        <el-col :span="8" style="text-align: right;">
          <el-button type="text" @click="showDebugInfo = !showDebugInfo">
            {{ showDebugInfo ? '隐藏' : '显示' }}调试信息
          </el-button>
        </el-col>
      </el-row>
    </el-card>
    
    <!-- 数据概览 -->
    <el-row :gutter="20" class="summary-row">
      <el-col :span="6" v-for="item in summaryData" :key="item.title">
        <el-card class="summary-card" :class="{ 'loading': loadingSummary }">
          <div class="card-title">{{ item.title }}</div>
          <div class="card-value" v-if="!loadingSummary">{{ item.value }}</div>
          <div class="card-value" v-else>
            <i class="el-icon-loading"></i>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <!-- 操作按钮 -->
    <el-card class="operation-card">
      <h3>数据管理</h3>
      <div class="operation-buttons">
        <el-upload
          action=""
          :show-file-list="false"
          :before-upload="beforeUpload"
          :http-request="importData"
          :disabled="loadingImport"
          style="display: inline-block; margin-right: 10px;"
        >
          <el-button type="primary" :loading="loadingImport">导入数据 (LOAD DATA)</el-button>
        </el-upload>
        
        <el-upload
          action=""
          :show-file-list="false"
          :before-upload="beforeUpload"
          :http-request="simpleImport"
          :disabled="loadingSimpleImport"
          style="display: inline-block; margin-right: 10px;"
        >
          <el-button type="primary" :loading="loadingSimpleImport">简单导入 (逐行)</el-button>
        </el-upload>
        
        <el-button type="danger" @click="clearData" style="margin-right: 10px;">清空数据</el-button>
        <el-button type="success" @click="refreshAll" :loading="loadingRefresh">刷新数据</el-button>
        <el-button type="warning" @click="runAnalysis" :loading="loadingAnalysis">执行分析</el-button>
      </div>
      
      <!-- 导入进度条 -->
      <div v-if="loadingImport || loadingSimpleImport" class="import-progress">
        <el-progress
          :percentage="importProgress"
          :status="importProgressStatus"
          :stroke-width="20"
          :show-text="true"
          text-inside
        >
          <template slot="format">
            <span style="font-size: 14px; color: #fff;">
              {{ importProgressText }}
            </span>
          </template>
        </el-progress>
        <p class="progress-info">{{ importProgressInfo }}</p>
      </div>
      
      <div v-if="importResult" class="import-result" :class="{ 'success': importResult.success, 'error': !importResult.success }">
        <p><strong>导入结果：</strong>{{ importResult.message }}</p>
        <p v-if="importResult.successCount !== undefined">成功：{{ importResult.successCount }} 条</p>
        <p v-if="importResult.errorCount !== undefined">失败：{{ importResult.errorCount }} 条</p>
        <p v-if="importResult.totalCount !== undefined">当前总数：{{ importResult.totalCount }} 条</p>
      </div>
    </el-card>
    
    <!-- 图表展示 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :span="12">
        <el-card>
          <h3>每日消息数量</h3>
          <div class="chart-container">
            <div v-if="loadingDaily" class="chart-loading">
              <i class="el-icon-loading"></i> 加载中...
            </div>
            <div id="dailyChart" class="chart-wrapper" v-show="!loadingDaily"></div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <h3>消息类型分布</h3>
          <div class="chart-container">
            <div v-if="loadingType" class="chart-loading">
              <i class="el-icon-loading"></i> 加载中...
            </div>
            <div id="typeChart" class="chart-wrapper" v-show="!loadingType"></div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <el-row :gutter="20" class="chart-row">
      <el-col :span="12">
        <el-card>
          <h3>聊天室/群组分布</h3>
          <div class="chart-container">
            <div v-if="loadingGroup" class="chart-loading">
              <i class="el-icon-loading"></i> 加载中...
            </div>
            <div id="groupChart" class="chart-wrapper" v-show="!loadingGroup"></div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <h3>消息长度分布</h3>
          <div class="chart-container">
            <div v-if="loadingLength" class="chart-loading">
              <i class="el-icon-loading"></i> 加载中...
            </div>
            <div id="lengthChart" class="chart-wrapper" v-show="!loadingLength"></div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <el-row :gutter="20" class="chart-row">
      <el-col :span="12">
        <el-card>
          <h3>平台分布</h3>
          <div class="chart-container">
            <div v-if="loadingPlatform" class="chart-loading">
              <i class="el-icon-loading"></i> 加载中...
            </div>
            <div id="platformChart" class="chart-wrapper" v-show="!loadingPlatform"></div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <h3>小时活跃度</h3>
          <div class="chart-container">
            <div v-if="loadingHour" class="chart-loading">
              <i class="el-icon-loading"></i> 加载中...
            </div>
            <div id="hourChart" class="chart-wrapper" v-show="!loadingHour"></div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <!-- 调试信息 -->
    <el-card v-if="showDebugInfo" class="debug-card">
      <h3>调试信息</h3>
      <el-collapse v-model="activeDebugPanel">
        <el-collapse-item title="表结构" name="1">
          <pre>{{ tableSchema }}</pre>
        </el-collapse-item>
        <el-collapse-item title="前5条数据" name="2">
          <pre>{{ previewData }}</pre>
        </el-collapse-item>
        <el-collapse-item title="连接状态" name="3">
          <pre>{{ connectionStatus }}</pre>
        </el-collapse-item>
      </el-collapse>
    </el-card>
    
    <!-- 数据表格 -->
    <el-card class="raw-data-card">
      <h3>数据表格 (共 {{ total }} 条)</h3>
      <el-table :data="tableData" border style="width: 100%" height="600" v-loading="loadingTable">
        <el-table-column label="时间">
          <template slot-scope="scope">
            {{ formatDateTime(scope.row.msgTime || scope.row.msg_time) }}
          </template>
        </el-table-column>
        <el-table-column label="用户ID">
          <template slot-scope="scope">
            {{ scope.row.userId || scope.row.user_id }}
          </template>
        </el-table-column>
        <el-table-column label="群组ID">
          <template slot-scope="scope">
            {{ scope.row.groupId || scope.row.group_id }}
          </template>
        </el-table-column>
        <el-table-column prop="message" label="消息内容" show-overflow-tooltip></el-table-column>
        <el-table-column label="IP地址">
          <template slot-scope="scope">
            {{ scope.row.ipAddress || scope.row.ip_address }}
          </template>
        </el-table-column>
        <el-table-column label="消息长度">
          <template slot-scope="scope">
            {{ scope.row.messageLength || scope.row.message_length }}
          </template>
        </el-table-column>
        <el-table-column label="消息类型">
          <template slot-scope="scope">
            {{ getMessageType(scope.row.messageType || scope.row.message_type) }}
          </template>
        </el-table-column>
        <el-table-column label="年龄">
          <template slot-scope="scope">
            {{ scope.row.userAge || scope.row.user_age }}
          </template>
        </el-table-column>
        <el-table-column label="性别">
          <template slot-scope="scope">
            {{ scope.row.userGender || scope.row.user_gender }}
          </template>
        </el-table-column>
        <el-table-column prop="platform" label="平台"></el-table-column>
        <el-table-column label="操作" fixed="right">
          <template slot-scope="scope">
            <el-button
              type="danger"
              size="mini"
              icon="el-icon-delete"
              @click="handleDelete(scope.row)"
              :loading="scope.row.deleting"
            >删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
          :current-page="currentPage"
          :page-sizes="[10, 20, 50, 100]"
          :page-size="pageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
        ></el-pagination>
      </div>
    </el-card>
  </div>
</template>

<script>
import axios from 'axios'
import * as echarts from 'echarts'

export default {
  name: 'Dashboard',
  data() {
    return {
      summaryData: [
        { title: '总消息数', value: 0 },
        { title: '用户数', value: 0 },
        { title: '最早时间', value: '无' },
        { title: '最晚时间', value: '无' }
      ],
      tableData: [],
      currentPage: 1,
      pageSize: 10,
      total: 0,
      importResult: null,
      charts: {},
      resizeTimer: null,
      
      // 加载状态
      loadingSummary: false,
      loadingDaily: false,
      loadingGroup: false,
      loadingType: false,
      loadingPlatform: false,
      loadingLength: false,
      loadingHour: false,
      loadingTable: false,
      loadingImport: false,
      loadingSimpleImport: false,
      loadingRefresh: false,
      loadingAnalysis: false,
      testingConnection: false,
      creatingTable: false,
      insertingData: false,
      importProgress: 0,
      importProgressStatus: 'active',
      importProgressText: '准备导入...',
      importProgressInfo: '',
      importProgressTimer: null,
      
      // 调试信息
      showDebugInfo: false,
      activeDebugPanel: ['1', '2', '3'],
      tableSchema: [],
      previewData: [],
      connectionStatus: {},
      
      // 模拟数据提示
      showMockDataWarning: false
    }
  },
  mounted() {
    // 使用 $nextTick 确保 DOM 渲染完成后再初始化图表
    this.$nextTick(() => {
      // 延迟初始化，确保浏览器完成布局计算
      setTimeout(() => {
        this.initCharts()
        // 监听窗口大小变化，自动调整图表
        window.addEventListener('resize', this.handleResize)
      }, 200)
    })
    
    // 页面加载完成后再次确保图表初始化
    window.addEventListener('load', () => {
      setTimeout(() => {
        this.initCharts()
        // 触发所有图表resize
        Object.keys(this.charts).forEach(chartId => {
          if (this.charts[chartId]) {
            this.charts[chartId].resize()
          }
        })
      }, 100)
    })
    
    this.testConnection()
    this.loadTableSchema()
    this.loadPreviewData()
    this.refreshAll()
    this.loadTableData()
  },
  
  beforeDestroy() {
    // 清理事件监听
    window.removeEventListener('resize', this.handleResize)
    // 清理定时器
    if (this.resizeTimer) {
      clearTimeout(this.resizeTimer)
    }
    // 清理进度动画定时器
    this.stopProgressAnimation()
    // 销毁所有图表实例
    Object.keys(this.charts).forEach(chartId => {
      if (this.charts[chartId]) {
        this.charts[chartId].dispose()
      }
    })
  },
  methods: {
    // 处理窗口大小变化
    handleResize() {
      // 防抖处理
      clearTimeout(this.resizeTimer)
      this.resizeTimer = setTimeout(() => {
        Object.keys(this.charts).forEach(chartId => {
          if (this.charts[chartId]) {
            this.charts[chartId].resize()
          }
        })
      }, 200)
    },
    // 初始化图表
    initCharts() {
      // 延迟初始化，确保DOM完全渲染且容器有尺寸
      setTimeout(() => {
        const chartIds = ['dailyChart', 'genderChart', 'typeChart', 'platformChart', 'ageChart', 'hourChart']
        chartIds.forEach(id => {
          this.initSingleChart(id)
        })
      }, 100)
    },
    
    // 初始化单个图表，确保容器有尺寸
    initSingleChart(chartId, retries = 10) {
      this.$nextTick(() => {
        const element = document.getElementById(chartId)
        if (!element) {
          if (retries > 0) {
            setTimeout(() => this.initSingleChart(chartId, retries - 1), 50)
          }
          return
        }
        
        // 检查容器尺寸
        const width = element.offsetWidth || element.clientWidth
        const height = element.offsetHeight || element.clientHeight
        
        if (width === 0 || height === 0) {
          // 容器尺寸为0，延迟重试
          if (retries > 0) {
            setTimeout(() => this.initSingleChart(chartId, retries - 1), 100)
          } else {
            console.warn(`图表容器 ${chartId} 尺寸为0，无法初始化`)
          }
          return
        }
        
        // 如果图表已存在，先销毁
        if (this.charts[chartId]) {
          this.charts[chartId].dispose()
        }
        
        // 初始化图表
        this.charts[chartId] = echarts.init(element)
        console.log(`图表 ${chartId} 初始化成功，尺寸: ${width}x${height}`)
      })
    },
    
    // 渲染图表的辅助方法
    renderChart(chartId, option) {
      // 使用递归延迟查找，直到容器存在且有尺寸
      const tryRender = (retries = 15) => {
        this.$nextTick(() => {
          const element = document.getElementById(chartId)
          if (!element) {
            if (retries > 0) {
              setTimeout(() => tryRender(retries - 1), 50)
            } else {
              console.error(`图表容器 ${chartId} 不存在，已重试15次`)
            }
            return
          }
          
          // 检查容器尺寸
          const width = element.offsetWidth || element.clientWidth
          const height = element.offsetHeight || element.clientHeight
          
          if (width === 0 || height === 0) {
            // 容器尺寸为0，延迟重试
            if (retries > 0) {
              setTimeout(() => tryRender(retries - 1), 100)
            } else {
              console.warn(`图表容器 ${chartId} 尺寸为0，无法渲染`)
            }
            return
          }
          
          if (this.charts[chartId]) {
            // 图表已存在，更新配置
            this.charts[chartId].setOption(option, true) // true表示不合并，完全替换
            // 确保图表尺寸正确
            this.charts[chartId].resize()
          } else {
            // 图表不存在，初始化
            this.charts[chartId] = echarts.init(element)
            this.charts[chartId].setOption(option)
            // 初始化后立即resize确保尺寸正确
            setTimeout(() => {
              if (this.charts[chartId]) {
                this.charts[chartId].resize()
              }
            }, 50)
          }
          console.log(`图表 ${chartId} 渲染完成，尺寸: ${width}x${height}`)
        })
      }
      tryRender()
    },
    
    // 测试连接
    async testConnection() {
      this.testingConnection = true
      try {
        const response = await axios.get('/api/hive/test-connection')
        // 处理响应数据：可能是 response.data 或直接是对象
        const data = response?.data || response || {}
        console.log('连接测试响应:', data)
        
        this.connectionStatus = data
        if (data && data.success) {
          this.$message.success('连接测试成功')
        } else {
          const errorMsg = (data && data.message) || '未知错误，请检查后端日志'
          console.error('连接测试失败:', errorMsg, data)
          this.$message.error('连接测试失败: ' + errorMsg)
          this.connectionStatus = { success: false, message: errorMsg }
        }
      } catch (error) {
        console.error('连接测试异常详情:', error)
        console.error('错误响应:', error.response)
        
        let errorMsg = '未知错误'
        if (error.response && error.response.data) {
          // 后端返回的错误信息
          errorMsg = error.response.data.message || JSON.stringify(error.response.data)
        } else if (error.message) {
          // 网络错误
          if (error.message.includes('Network Error') || error.code === 'ECONNREFUSED') {
            errorMsg = '无法连接到后端服务（http://localhost:8080），请确保后端服务已启动'
          } else {
            errorMsg = error.message
          }
        }
        
        this.$message.error('连接测试异常: ' + errorMsg)
        this.connectionStatus = { success: false, message: errorMsg }
      } finally {
        this.testingConnection = false
      }
    },
    
    // 创建表
    async createTable() {
      this.creatingTable = true
      try {
        const response = await axios.post('/api/hive/create-table')
        // 处理响应数据：可能是 response.data 或直接是对象
        const data = response?.data || response || {}
        console.log('创建表响应:', data)
        
        if (data && data.success) {
          this.$message.success('表创建/检查成功')
        } else {
          const errorMsg = (data && data.message) || '未知错误，请查看后端日志'
          this.$message.error('表创建失败: ' + errorMsg)
        }
      } catch (error) {
        console.error('创建表异常:', error)
        const errorMsg = error.response?.data?.message || error.message || '创建表失败'
        this.$message.error('表创建异常: ' + errorMsg)
      } finally {
        this.creatingTable = false
      }
    },
    
    // 刷新所有数据
    refreshAll() {
      this.loadingRefresh = true
      // 并行调用所有统计接口
      Promise.all([
        this.getSummary(),
        this.getDailyStats(),
        this.getGroupStats(),
        this.getTypeStats(),
        this.getPlatformStats(),
        this.getLengthStats(),
        this.getHourStats()
      ]).finally(() => {
        // 所有请求完成后重置加载状态
        setTimeout(() => {
          this.loadingRefresh = false
        }, 500)
      })
    },
    
    // 执行MapReduce分析
    async runAnalysis() {
      this.loadingAnalysis = true
      try {
        this.$message.info('开始执行MapReduce分析，请稍候...')
        const response = await axios.post('/api/hive/run-analysis')
        const data = response?.data || response || {}
        console.log('执行分析响应:', data)
        
        if (data && data.success) {
          const successCount = data.successCount || 0
          const failCount = data.failCount || 0
          this.$message.success(`分析完成！成功: ${successCount} 个，失败: ${failCount} 个`)
          
          // 分析完成后自动刷新数据
          setTimeout(() => {
            this.refreshAll()
          }, 1000)
        } else {
          const errorMsg = (data && data.message) || '未知错误，请查看后端日志'
          this.$message.error('分析失败: ' + errorMsg)
        }
      } catch (error) {
        console.error('执行分析异常:', error)
        const errorMsg = error.response?.data?.message || error.message || '执行分析失败'
        this.$message.error('执行分析异常: ' + errorMsg)
      } finally {
        this.loadingAnalysis = false
      }
    },
    
    // 获取数据概览
    async getSummary() {
      this.loadingSummary = true
      try {
        const response = await axios.get('/api/hive/data-summary')
        // 响应拦截器应该返回response.data，但为了兼容，同时检查response.data和response本身
        const data = (response && response.data) ? response.data : (response || {})
        console.log('数据概览响应 - 完整对象:', response)
        console.log('数据概览响应 - 提取的data:', data)
        console.log('数据概览响应 - data字段检查:', {
          hasData: !!response.data,
          total_count: data.total_count,
          totalCount: data.totalCount,
          user_count: data.user_count,
          userCount: data.userCount
        })
        
        // 兼容snake_case和camelCase两种命名格式
        const totalCount = data.total_count || data.totalCount || 0
        const userCount = data.user_count || data.userCount || 0
        const minTime = data.min_time || data.minTime || '无'
        const maxTime = data.max_time || data.maxTime || '无'
        
        console.log('数据概览 - 提取的值:', {
          totalCount,
          userCount,
          minTime,
          maxTime
        })
        
        // 直接重新赋值整个数组，确保Vue响应式更新
        this.summaryData = [
          { title: '总消息数', value: totalCount },
          { title: '用户数', value: userCount },
          { title: '最早时间', value: minTime },
          { title: '最晚时间', value: maxTime }
        ]
        
        console.log('数据概览更新完成:', this.summaryData)
        console.log('数据概览 - 检查值:', {
          item0: this.summaryData[0],
          item1: this.summaryData[1],
          item2: this.summaryData[2],
          item3: this.summaryData[3]
        })
      } catch (error) {
        console.error('获取数据概览失败:', error)
        console.error('错误详情:', error.response || error)
        // 出错时显示错误信息
        this.summaryData = [
          { title: '总消息数', value: 0 },
          { title: '用户数', value: 0 },
          { title: '最早时间', value: '获取失败' },
          { title: '最晚时间', value: '获取失败' }
        ]
      } finally {
        // 确保在数据更新后再隐藏加载状态
        this.$nextTick(() => {
          this.loadingSummary = false
          console.log('数据概览 - loadingSummary已设置为false')
        })
      }
    },
    
    // 获取每日统计
    async getDailyStats() {
      this.loadingDaily = true
      try {
        const response = await axios.get('/api/hive/daily-stats')
        // 处理响应数据：可能是 response.data 或直接是数组
        const data = Array.isArray(response) ? response : (response?.data || [])
        console.log('每日统计数据:', data)
        
        // 检查是否为模拟数据
        if (data && data.length > 0 && data[0].isMock) {
          this.showMockDataWarning = true
        }
        
        if (data && data.length > 0) {
          console.log('处理每日统计数据，原始数据:', data)
          // 兼容下划线和驼峰命名
          const dates = data.map(item => {
            const date = item.stat_date || item.statDate || item.date || item.time
            return date ? String(date).substring(0, 10) : '' // 只取日期部分
          })
          const counts = data.map(item => {
            const count = item.message_count || item.messageCount || item.count || 0
            return Number(count) || 0
          })
          
          console.log('每日统计 - 日期:', dates)
          console.log('每日统计 - 数量:', counts)
          
          const option = {
            tooltip: {
              trigger: 'axis',
              axisPointer: {
                type: 'cross'
              }
            },
            xAxis: {
              type: 'category',
              data: dates,
              axisLabel: {
                rotate: 45
              }
            },
            yAxis: {
              type: 'value',
              minInterval: 1
            },
            series: [{
              name: '消息数量',
              data: counts,
              type: 'line',
              smooth: true,
              itemStyle: {
                color: '#5470c6'
              },
              areaStyle: {
                color: {
                  type: 'linear',
                  x: 0,
                  y: 0,
                  x2: 0,
                  y2: 1,
                  colorStops: [{
                    offset: 0, color: 'rgba(84, 112, 198, 0.3)'
                  }, {
                    offset: 1, color: 'rgba(84, 112, 198, 0.1)'
                  }]
                }
              }
            }]
          }
          
          this.renderChart('dailyChart', option)
        } else {
          // 没有数据时显示空图表
          const emptyOption = {
            title: {
              text: '暂无数据',
              left: 'center',
              top: 'center',
              textStyle: {
                color: '#999',
                fontSize: 14
              }
            },
            xAxis: { show: false },
            yAxis: { show: false }
          }
          this.renderChart('dailyChart', emptyOption)
        }
      } catch (error) {
        console.error('获取每日统计失败:', error)
        this.showMockDataWarning = true
      } finally {
        this.loadingDaily = false
        // 数据加载完成后，确保图表resize
        this.$nextTick(() => {
          setTimeout(() => {
            if (this.charts['dailyChart']) {
              this.charts['dailyChart'].resize()
            }
          }, 100)
        })
      }
    },
    
    // 获取聊天室/群组统计
    async getGroupStats() {
      this.loadingGroup = true
      try {
        const response = await axios.get('/api/hive/group-stats')
        // 处理响应数据：可能是 response.data 或直接是数组
        const data = Array.isArray(response) ? response : (response?.data || [])
        console.log('聊天室/群组统计数据:', data)
        
        if (data && data.length > 0) {
          console.log('处理聊天室/群组统计数据，原始数据:', data)
          // 兼容下划线和驼峰命名
          const groups = data.map(item => item.group_id || item.groupId || item.group || '未知')
          const counts = data.map(item => Number(item.message_count || item.messageCount || item.count || 0))
          console.log('聊天室/群组统计 - 群组:', groups)
          console.log('聊天室/群组统计 - 数量:', counts)
          
          const option = {
            tooltip: {
              trigger: 'axis'
            },
            xAxis: {
              type: 'category',
              data: groups,
              axisLabel: {
                rotate: 45
              }
            },
            yAxis: {
              type: 'value'
            },
            series: [{
              data: counts,
              type: 'bar',
              itemStyle: {
                color: '#73c0de'
              }
            }]
          }
          
          this.renderChart('groupChart', option)
        } else {
          const emptyOption = {
            title: {
              text: '暂无数据',
              left: 'center',
              top: 'center'
            }
          }
          this.renderChart('groupChart', emptyOption)
        }
      } catch (error) {
        console.error('获取聊天室/群组统计失败:', error)
      } finally {
        this.loadingGroup = false
        // 数据加载完成后，确保图表resize
        this.$nextTick(() => {
          setTimeout(() => {
            if (this.charts['groupChart']) {
              this.charts['groupChart'].resize()
            }
          }, 100)
        })
      }
    },
    
    // 获取消息类型统计
    async getTypeStats() {
      this.loadingType = true
      try {
        const response = await axios.get('/api/hive/type-stats')
        // 处理响应数据：可能是 response.data 或直接是数组
        const data = Array.isArray(response) ? response : (response?.data || [])
        console.log('类型统计数据:', data)
        
        if (data && data.length > 0) {
          console.log('处理类型统计数据，原始数据:', data)
          // 兼容下划线和驼峰命名
          const chartData = data.map(item => ({
            name: this.getMessageType(item.message_type || item.messageType),
            value: Number(item.message_count || item.messageCount || item.count || 0)
          }))
          console.log('类型统计 - 图表数据:', chartData)
          
          const option = {
            tooltip: {
              trigger: 'item',
              formatter: '{a} <br/>{b}: {c} ({d}%)'
            },
            series: [{
              type: 'pie',
              radius: ['40%', '70%'],
              data: chartData,
              emphasis: {
                itemStyle: {
                  shadowBlur: 10,
                  shadowOffsetX: 0,
                  shadowColor: 'rgba(0, 0, 0, 0.5)'
                }
              }
            }]
          }
          
          this.renderChart('typeChart', option)
        } else {
          const emptyOption = {
            title: {
              text: '暂无数据',
              left: 'center',
              top: 'center'
            }
          }
          this.renderChart('typeChart', emptyOption)
        }
      } catch (error) {
        console.error('获取消息类型统计失败:', error)
      } finally {
        this.loadingType = false
        // 确保加载状态更新后，DOM已渲染
        this.$nextTick(() => {
          if (!this.charts.typeChart) {
            const element = document.getElementById('typeChart')
            if (element) {
              this.charts.typeChart = echarts.init(element)
            }
          }
        })
      }
    },
    
    // 获取平台统计
    async getPlatformStats() {
      this.loadingPlatform = true
      try {
        const response = await axios.get('/api/hive/platform-stats')
        // 处理响应数据：可能是 response.data 或直接是数组
        const data = Array.isArray(response) ? response : (response?.data || [])
        console.log('平台统计数据:', data)
        
        if (data && data.length > 0) {
          console.log('处理平台统计数据，原始数据:', data)
          // 兼容下划线和驼峰命名
          const platforms = data.map(item => item.platform || '未知')
          const counts = data.map(item => Number(item.message_count || item.messageCount || item.count || 0))
          console.log('平台统计 - 平台:', platforms)
          console.log('平台统计 - 数量:', counts)
          
          const option = {
            tooltip: {
              trigger: 'axis'
            },
            xAxis: {
              type: 'category',
              data: platforms
            },
            yAxis: {
              type: 'value'
            },
            series: [{
              data: counts,
              type: 'bar',
              itemStyle: {
                color: '#91cc75'
              }
            }]
          }
          
          this.renderChart('platformChart', option)
        } else {
          const emptyOption = {
            title: {
              text: '暂无数据',
              left: 'center',
              top: 'center'
            }
          }
          this.renderChart('platformChart', emptyOption)
        }
      } catch (error) {
        console.error('获取平台统计失败:', error)
      } finally {
        this.loadingPlatform = false
        // 数据加载完成后，确保图表resize
        this.$nextTick(() => {
          setTimeout(() => {
            if (this.charts['platformChart']) {
              this.charts['platformChart'].resize()
            }
          }, 100)
        })
      }
    },
    
    // 获取消息长度统计
    async getLengthStats() {
      this.loadingLength = true
      try {
        const response = await axios.get('/api/hive/length-stats')
        // 处理响应数据：可能是 response.data 或直接是数组
        const data = Array.isArray(response) ? response : (response?.data || [])
        console.log('消息长度统计数据:', data)
        
        if (data && data.length > 0) {
          console.log('处理消息长度统计数据，原始数据:', data)
          // 兼容下划线和驼峰命名
          const lengthGroups = data.map(item => item.length_group || item.lengthGroup || item.length || '未知')
          const counts = data.map(item => Number(item.message_count || item.messageCount || item.count || 0))
          console.log('消息长度统计 - 长度区间:', lengthGroups)
          console.log('消息长度统计 - 数量:', counts)
          
          const option = {
            tooltip: {
              trigger: 'axis'
            },
            xAxis: {
              type: 'category',
              data: lengthGroups
            },
            yAxis: {
              type: 'value'
            },
            series: [{
              data: counts,
              type: 'bar',
              itemStyle: {
                color: '#fc8452'
              }
            }]
          }
          
          this.renderChart('lengthChart', option)
        } else {
          const emptyOption = {
            title: {
              text: '暂无数据',
              left: 'center',
              top: 'center'
            }
          }
          this.renderChart('lengthChart', emptyOption)
        }
      } catch (error) {
        console.error('获取消息长度统计失败:', error)
      } finally {
        this.loadingLength = false
        // 数据加载完成后，确保图表resize
        this.$nextTick(() => {
          setTimeout(() => {
            if (this.charts['lengthChart']) {
              this.charts['lengthChart'].resize()
            }
          }, 100)
        })
      }
    },
    
    // 获取小时统计
    async getHourStats() {
      this.loadingHour = true
      try {
        const response = await axios.get('/api/hive/hour-stats')
        // 处理响应数据：可能是 response.data 或直接是数组
        const data = Array.isArray(response) ? response : (response?.data || [])
        console.log('小时统计数据:', data)
        
        if (data && data.length > 0) {
          console.log('处理小时统计数据，原始数据:', data)
          // 确保0-23小时都有数据
          const hourMap = {}
          data.forEach(item => {
            const hour = Number(item.hour || 0)
            hourMap[hour] = Number(item.message_count || item.messageCount || item.count || 0)
          })
          console.log('小时统计 - 小时映射:', hourMap)
          
          const hours = []
          const counts = []
          for (let i = 0; i < 24; i++) {
            hours.push(i + '时')
            counts.push(hourMap[i] || 0)
          }
          
          const option = {
            tooltip: {
              trigger: 'axis'
            },
            xAxis: {
              type: 'category',
              data: hours
            },
            yAxis: {
              type: 'value'
            },
            series: [{
              data: counts,
              type: 'line',
              smooth: true,
              areaStyle: {},
              itemStyle: {
                color: '#ee6666'
              }
            }]
          }
          
          this.renderChart('hourChart', option)
        } else {
          const emptyOption = {
            title: {
              text: '暂无数据',
              left: 'center',
              top: 'center'
            }
          }
          this.renderChart('hourChart', emptyOption)
        }
      } catch (error) {
        console.error('获取小时统计失败:', error)
      } finally {
        this.loadingHour = false
        // 数据加载完成后，确保图表resize
        this.$nextTick(() => {
          setTimeout(() => {
            if (this.charts['hourChart']) {
              this.charts['hourChart'].resize()
            }
          }, 100)
        })
      }
    },
    
    // 加载表格数据
    async loadTableData() {
      this.loadingTable = true
      try {
        const response = await axios.get('/api/hive/raw-data', {
          params: {
            page: this.currentPage,
            size: this.pageSize
          }
        })
        // 处理响应数据：可能是 response.data 或直接是对象
        const data = response?.data || response
        // 后端返回格式: { content: [], totalElements: 100, totalPages: 10, ... }
        if (data && Array.isArray(data.content)) {
          // 新格式：包含分页信息
          this.tableData = data.content
          this.total = data.totalElements || 0
        } else if (Array.isArray(data)) {
          // 兼容旧格式：直接返回数组
          this.tableData = data
          this.total = this.tableData.length < this.pageSize ? 
            (this.currentPage - 1) * this.pageSize + this.tableData.length : 
            (this.currentPage - 1) * this.pageSize + this.tableData.length + 1
        } else {
          // 如果返回的不是数组也不是包含content的对象，设置为空数组
          console.warn('返回数据格式不正确:', response)
          this.tableData = []
          this.total = 0
        }
      } catch (error) {
        console.error('加载表格数据失败:', error)
        this.tableData = []
        this.total = 0
      } finally {
        this.loadingTable = false
      }
    },
    
    // 加载表结构
    async loadTableSchema() {
      try {
        const response = await axios.get('/api/hive/table-schema')
        this.tableSchema = response || []
      } catch (error) {
        console.error('加载表结构失败:', error)
      }
    },
    
    // 加载预览数据
    async loadPreviewData() {
      try {
        const response = await axios.get('/api/hive/preview', {
          params: { limit: 5 }
        })
        this.previewData = response || []
      } catch (error) {
        console.error('加载预览数据失败:', error)
      }
    },
    
    // 插入测试数据
    async insertTestData() {
      this.insertingData = true
      try {
        const response = await axios.post('/api/hive/insert-test-data')
        // 处理响应数据：可能是 response.data 或直接是对象
        const data = response?.data || response || {}
        console.log('插入测试数据响应:', data)
        
        this.importResult = data
        if (data && data.success) {
          this.$message.success('测试数据插入成功')
          // 延迟一下确保数据已写入数据库
          setTimeout(() => {
            this.refreshAll()
            this.loadTableData()
            this.loadPreviewData()
          }, 500)
        } else {
          const errorMsg = (data && data.message) || '未知错误，请查看后端日志'
          this.$message.error('测试数据插入失败: ' + errorMsg)
        }
      } catch (error) {
        console.error('插入测试数据异常:', error)
        const errorMsg = error.response?.data?.message || error.message || '插入测试数据失败'
        this.$message.error('插入测试数据异常: ' + errorMsg)
      } finally {
        this.insertingData = false
      }
    },
    
    // 分页大小变化
    handleSizeChange(size) {
      this.pageSize = size
      this.loadTableData()
    },
    
    // 页码变化
    handlePageChange(page) {
      this.currentPage = page
      this.loadTableData()
    },
    
    // 删除单条数据
    async handleDelete(row) {
      try {
        await this.$confirm('确定要删除这条记录吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        // 获取记录ID
        const id = row.id || row.ID
        if (!id) {
          this.$message.error('无法获取记录ID')
          return
        }
        
        // 设置删除中状态
        this.$set(row, 'deleting', true)
        
        try {
          // axios拦截器已经返回response.data，所以response就是数据对象
          const data = await axios.delete(`/api/hive/delete/${id}`)
          console.log('删除响应数据:', data)
          
          // 判断成功：支持多种格式（boolean true, 字符串"true", 数字1等）
          const successValue = data?.success
          const isSuccess = successValue === true || 
                           successValue === 'true' || 
                           successValue === 1 ||
                           (successValue !== undefined && String(successValue).toLowerCase() === 'true')
          
          // 无论判断结果如何，都刷新数据（因为后端可能已经成功删除）
          await this.refreshAll()
          await this.loadTableData()
          await this.loadPreviewData()
          
          // 根据判断结果显示消息
          if (isSuccess) {
            this.$message.success(data?.message || '删除成功')
          } else {
            // 即使判断失败，也提示操作完成（因为数据已刷新）
            this.$message.info('删除操作已完成，请查看数据是否已更新')
          }
        } catch (error) {
          console.error('删除异常:', error)
          const errorMsg = error.response?.data?.message || error.message || '删除失败'
          this.$message.error('删除失败：' + errorMsg)
        } finally {
          this.$set(row, 'deleting', false)
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除操作异常:', error)
        }
      }
    },
    
    // 上传前检查
    beforeUpload(file) {
      const isCSV = file.type === 'text/csv' || file.name.endsWith('.csv') || file.name.endsWith('.txt')
      if (!isCSV) {
        this.$message.error('只能上传CSV或TXT文件！')
        return false
      }
      return true
    },
    
    // 导入数据 (LOAD DATA)
    async importData(file) {
      this.loadingImport = true
      this.importResult = null
      this.importProgress = 0
      this.importProgressStatus = 'active'
      this.importProgressText = '准备导入...'
      this.importProgressInfo = '正在上传文件...'
      
      const formData = new FormData()
      formData.append('file', file.file)
      
      // 模拟进度（因为后端是同步处理，无法实时返回进度）
      this.startProgressAnimation('导入数据')
      
      try {
        const response = await axios.post('/api/hive/import-data', formData, {
          timeout: 300000 // 5分钟超时
        })
        // 处理响应数据：可能是 response.data 或直接是对象
        const data = response?.data || response || {}
        console.log('导入数据响应:', data)
        
        this.stopProgressAnimation()
        this.importProgress = 100
        this.importProgressStatus = data.success ? 'success' : 'exception'
        this.importProgressText = data.success ? '导入完成！' : '导入失败'
        this.importProgressInfo = data.success 
          ? `成功导入 ${data.successCount || 0} 条数据，失败 ${data.errorCount || 0} 条`
          : (data.message || '导入失败')
        
        this.importResult = data
        if (data && data.success) {
          this.$message.success('数据导入成功！')
          this.refreshAll()
          this.loadTableData()
          this.loadPreviewData()
        } else {
          const errorMsg = (data && data.message) || '未知错误，请查看后端日志'
          this.$message.error('数据导入失败：' + errorMsg)
        }
      } catch (error) {
        this.stopProgressAnimation()
        this.importProgress = 100
        this.importProgressStatus = 'exception'
        this.importProgressText = '导入失败'
        this.importProgressInfo = '网络错误或服务器异常'
        
        console.error('导入数据异常:', error)
        const errorMsg = error.response?.data?.message || error.message || '导入数据失败'
        this.$message.error('导入失败：' + errorMsg)
      } finally {
        this.loadingImport = false
        // 3秒后隐藏进度条
        setTimeout(() => {
          if (!this.loadingImport && !this.loadingSimpleImport) {
            this.importProgress = 0
            this.importProgressInfo = ''
          }
        }, 3000)
      }
    },
    
    // 简单导入 (逐行)
    async simpleImport(file) {
      this.loadingSimpleImport = true
      this.importResult = null
      this.importProgress = 0
      this.importProgressStatus = 'active'
      this.importProgressText = '准备导入...'
      this.importProgressInfo = '正在上传文件...'
      
      const formData = new FormData()
      formData.append('file', file.file)
      
      // 模拟进度（因为后端是同步处理，无法实时返回进度）
      this.startProgressAnimation('简单导入')
      
      try {
        const response = await axios.post('/api/hive/simple-import', formData, {
          timeout: 300000 // 5分钟超时
        })
        // 处理响应数据：可能是 response.data 或直接是对象
        const data = response?.data || response || {}
        console.log('简单导入响应:', data)
        
        this.stopProgressAnimation()
        this.importProgress = 100
        this.importProgressStatus = data.success ? 'success' : 'exception'
        this.importProgressText = data.success ? '导入完成！' : '导入失败'
        this.importProgressInfo = data.success 
          ? `成功导入 ${data.successCount || 0} 条数据，失败 ${data.errorCount || 0} 条`
          : (data.message || '导入失败')
        
        this.importResult = data
        if (data && data.success) {
          this.$message.success('数据导入成功！')
          this.refreshAll()
          this.loadTableData()
          this.loadPreviewData()
        } else {
          const errorMsg = (data && data.message) || '未知错误，请查看后端日志'
          this.$message.error('数据导入失败：' + errorMsg)
        }
      } catch (error) {
        this.stopProgressAnimation()
        this.importProgress = 100
        this.importProgressStatus = 'exception'
        this.importProgressText = '导入失败'
        this.importProgressInfo = '网络错误或服务器异常'
        
        console.error('简单导入异常:', error)
        const errorMsg = error.response?.data?.message || error.message || '导入数据失败'
        this.$message.error('导入失败：' + errorMsg)
      } finally {
        this.loadingSimpleImport = false
        // 3秒后隐藏进度条
        setTimeout(() => {
          if (!this.loadingImport && !this.loadingSimpleImport) {
            this.importProgress = 0
            this.importProgressInfo = ''
          }
        }, 3000)
      }
    },
    
    // 开始进度动画
    startProgressAnimation(type) {
      this.importProgress = 10
      this.importProgressStatus = 'active'
      this.importProgressText = `${type}中...`
      
      // 模拟进度：从10%到90%
      let progress = 10
      const interval = setInterval(() => {
        if (progress < 90) {
          progress += Math.random() * 5 // 每次增加0-5%
          if (progress > 90) progress = 90
          this.importProgress = Math.floor(progress)
          
          // 更新提示信息
          if (progress < 30) {
            this.importProgressInfo = '正在解析CSV文件...'
          } else if (progress < 60) {
            this.importProgressInfo = '正在处理数据...'
          } else if (progress < 85) {
            this.importProgressInfo = '正在写入数据库...'
          } else {
            this.importProgressInfo = '即将完成...'
          }
        }
      }, 200) // 每200ms更新一次
      
      this.importProgressTimer = interval
    },
    
    // 停止进度动画
    stopProgressAnimation() {
      if (this.importProgressTimer) {
        clearInterval(this.importProgressTimer)
        this.importProgressTimer = null
      }
    },
    
    // 清空数据
    async clearData() {
      try {
        await this.$confirm('确定要清空所有数据吗？此操作不可恢复！', '警告', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        // axios拦截器已经返回response.data，所以response就是数据对象
        const data = await axios.delete('/api/hive/clear-data')
        console.log('清空数据响应数据:', data)
        
        // 判断成功：支持多种格式（boolean true, 字符串"true", 数字1等）
        const successValue = data?.success
        const isSuccess = successValue === true || 
                         successValue === 'true' || 
                         successValue === 1 ||
                         (successValue !== undefined && String(successValue).toLowerCase() === 'true')
        
        // 无论判断结果如何，都刷新数据（因为后端可能已经成功清空）
        await this.refreshAll()
        await this.loadTableData()
        await this.loadPreviewData()
        
        // 根据判断结果显示消息
        if (isSuccess) {
          this.$message.success(data?.message || '数据已清空')
        } else {
          // 即使判断失败，也提示操作完成（因为数据已刷新）
          this.$message.info('清空操作已完成，请查看数据是否已更新')
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('清空数据异常:', error)
          const errorMsg = error.response?.data?.message || error.message || '清空数据失败'
          this.$message.error('清空数据失败：' + errorMsg)
        }
      }
    },
    
    // 获取消息类型
    getMessageType(type) {
      const types = {
        1: '文本',
        2: '图片',
        3: '语音'
      }
      return types[type] || '未知'
    },
    
    // 格式化日期时间
    formatDateTime(dateTime) {
      if (!dateTime) return ''
      // 如果是字符串，直接返回
      if (typeof dateTime === 'string') {
        return dateTime
      }
      // 如果是数组格式 [2024, 12, 20, 10, 30, 0]
      if (Array.isArray(dateTime)) {
        const [year, month, day, hour, minute, second] = dateTime
        return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')} ${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}:${String(second || 0).padStart(2, '0')}`
      }
      return String(dateTime)
    },
    
    // 窗口大小变化时重绘图表
    handleResize() {
      Object.values(this.charts).forEach(chart => {
        if (chart) {
          chart.resize()
        }
      })
    }
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.handleResize)
    Object.values(this.charts).forEach(chart => {
      if (chart) {
        chart.dispose()
      }
    })
  }
}
</script>

<style scoped>
.dashboard {
  padding: 20px;
  background-color: #f5f5f5;
  min-height: 100vh;
}

h1 {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
}

.system-status {
  margin-bottom: 20px;
}

.summary-row {
  margin-bottom: 30px;
}

.summary-card {
  text-align: center;
  transition: all 0.3s;
}

.summary-card.loading {
  opacity: 0.7;
}

.card-title {
  font-size: 14px;
  color: #666;
  margin-bottom: 10px;
}

.card-value {
  font-size: 24px;
  font-weight: bold;
  color: #333;
  min-height: 30px;
}

.operation-card {
  margin-bottom: 30px;
}

.operation-buttons {
  margin-bottom: 15px;
}

.import-result {
  margin-top: 10px;
  padding: 10px;
  border-radius: 4px;
  border-left: 4px solid #e6a23c;
}

.import-result.success {
  border-left-color: #67c23a;
  background-color: #f0f9eb;
}

.import-result.error {
  border-left-color: #f56c6c;
  background-color: #fef0f0;
}

.import-progress {
  margin-top: 20px;
  padding: 20px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.progress-info {
  margin-top: 10px;
  text-align: center;
  color: #606266;
  font-size: 14px;
}

.chart-row {
  margin-bottom: 20px;
}

.chart-container {
  position: relative;
  width: 100%;
  min-height: 300px;
}

.chart-wrapper {
  width: 100%;
  height: 300px;
}

.chart-loading {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
  z-index: 10;
  color: #909399;
  font-size: 14px;
}

.chart-loading i {
  font-size: 24px;
  margin-right: 8px;
}

.el-card h3 {
  margin-top: 0;
  margin-bottom: 15px;
  color: #303133;
  font-size: 16px;
  font-weight: 500;
}

.debug-card {
  margin-top: 20px;
  margin-bottom: 20px;
}

.debug-card pre {
  background-color: #f8f9fa;
  padding: 10px;
  border-radius: 4px;
  overflow-x: auto;
}

.raw-data-card {
  margin-top: 20px;
}

.pagination {
  margin-top: 20px;
  text-align: center;
}

.el-card {
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}
</style>