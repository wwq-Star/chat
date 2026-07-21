import axios from 'axios'

// 创建axios实例
const service = axios.create({
  baseURL: '/api', // 注意：这里使用了代理，所以直接写/api
  timeout: 10000 // 请求超时时间
})

// 请求拦截器
service.interceptors.request.use(
  config => {
    return config
  },
  error => {
    console.log(error) // for debug
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  response => {
    return response.data
  },
  error => {
    console.error('API请求错误:', error)
    // 如果是网络错误或后端未启动，返回更友好的错误信息
    if (error.message === 'Network Error' || error.code === 'ECONNREFUSED') {
      error.response = {
        data: {
          success: false,
          message: '无法连接到后端服务，请确保后端服务已启动（http://localhost:8080）'
        }
      }
    }
    return Promise.reject(error)
  }
)

export default service