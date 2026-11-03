import axios from 'axios'

const baseURL = import.meta.env.VITE_API_URL || '/api'

export const api = axios.create({ baseURL })

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

// Ignore login errors and stale responses belonging to a previous token.
api.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    if (axios.isAxiosError(error) && error.response?.status === 401) {
      const url = error.config?.url ?? ''
      const token = localStorage.getItem('accessToken')
      const header = error.config?.headers?.Authorization
      if (token && !url.startsWith('/auth/') && header === `Bearer ${token}`) {
        window.dispatchEvent(new Event('ooad:auth-expired'))
      }
    }
    return Promise.reject(error)
  },
)
