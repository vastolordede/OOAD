import axios from 'axios'

const baseURL = import.meta.env.VITE_API_URL

export const api = axios.create({ baseURL })

api.interceptors.request.use((config) => {
  if (!baseURL) {
    return Promise.reject(new Error('VITE_API_URL must be set before making API requests.'))
  }

  const token = localStorage.getItem('accessToken')

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})
